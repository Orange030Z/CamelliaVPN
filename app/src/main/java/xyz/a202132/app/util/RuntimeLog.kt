package xyz.a202132.app.util

import android.content.Context
import xyz.a202132.app.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

data class RuntimeLogEntry(
    val id: Long,
    val timestampMillis: Long,
    val level: RuntimeLogLevel,
    val component: String,
    val message: String
)

enum class RuntimeLogLevel {
    INFO,
    WARN,
    ERROR
}

object RuntimeLog {
    private const val MAX_ENTRIES = 1000
    private const val LOG_DIR = "runtime_logs"
    private const val LOG_FILE = "runtime.log"

    private val lock = Any()
    private val nextId = AtomicLong(1L)
    private val _entries = kotlinx.coroutines.flow.MutableStateFlow<List<RuntimeLogEntry>>(emptyList())
    val entries: kotlinx.coroutines.flow.StateFlow<List<RuntimeLogEntry>> = _entries

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        refresh()
    }

    fun info(component: String, message: String) {
        append(RuntimeLogLevel.INFO, component, message, null)
    }

    fun warn(component: String, message: String, throwable: Throwable? = null) {
        append(RuntimeLogLevel.WARN, component, message, throwable)
    }

    fun error(component: String, message: String, throwable: Throwable? = null) {
        append(RuntimeLogLevel.ERROR, component, message, throwable)
    }

    fun refresh() {
        synchronized(lock) {
            val loaded = readLogFile()
            _entries.value = loaded
            val maxId = loaded.maxOfOrNull { it.id } ?: 0L
            nextId.set(maxId + 1L)
        }
    }

    fun clear() {
        synchronized(lock) {
            logFile()?.delete()
            _entries.value = emptyList()
            nextId.set(1L)
        }
    }

    fun exportToCacheFile(context: Context): File {
        val cacheDir = File(context.cacheDir, LOG_DIR)
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val stamp = fileDateFormat().format(Date())
        val suffix = Integer.toHexString(_entries.value.hashCode()).padStart(8, '0')
        val file = File(cacheDir, "FireflyVPN-runtime-$stamp-$suffix.log")
        file.writeText(buildExportText(), Charsets.UTF_8)
        return file
    }

    private fun append(
        level: RuntimeLogLevel,
        component: String,
        message: String,
        throwable: Throwable?
    ) {
        synchronized(lock) {
            val entry = RuntimeLogEntry(
                id = nextId.getAndIncrement(),
                timestampMillis = System.currentTimeMillis(),
                level = level,
                component = sanitizeComponent(component),
                message = sanitizeMessage(
                    buildString {
                        append(message)
                        if (throwable != null) {
                            val exceptionName = throwable.javaClass.simpleName
                            val exceptionMessage = throwable.message.orEmpty()
                            append(" ($exceptionName")
                            if (exceptionMessage.isNotBlank()) {
                                append(": ")
                                append(exceptionMessage)
                            }
                            append(")")
                        }
                    }
                )
            )
            val updated = (_entries.value + entry).takeLast(MAX_ENTRIES)
            _entries.value = updated
            writeLogFile(updated)
        }
    }

    private fun readLogFile(): List<RuntimeLogEntry> {
        val file = logFile() ?: return emptyList()
        if (!file.exists()) return emptyList()
        return runCatching {
            file.readLines(Charsets.UTF_8).mapNotNull { line ->
                val parts = line.split('\t', limit = 5)
                if (parts.size != 5) return@mapNotNull null
                val id = parts[0].toLongOrNull() ?: return@mapNotNull null
                val timestamp = parts[1].toLongOrNull() ?: return@mapNotNull null
                val level = runCatching { RuntimeLogLevel.valueOf(parts[2]) }.getOrNull()
                    ?: return@mapNotNull null
                RuntimeLogEntry(
                    id = id,
                    timestampMillis = timestamp,
                    level = level,
                    component = unescape(parts[3]),
                    message = unescape(parts[4])
                )
            }.takeLast(MAX_ENTRIES)
        }.getOrDefault(emptyList())
    }

    private fun writeLogFile(entries: List<RuntimeLogEntry>) {
        val file = logFile() ?: return
        file.parentFile?.mkdirs()
        file.writeText(
            entries.joinToString(separator = "\n") { entry ->
                listOf(
                    entry.id.toString(),
                    entry.timestampMillis.toString(),
                    entry.level.name,
                    escape(entry.component),
                    escape(entry.message)
                ).joinToString("\t")
            },
            Charsets.UTF_8
        )
    }

    private fun buildExportText(): String {
        val entriesSnapshot = _entries.value
        val timestampFormat = displayDateFormat()
        return buildString {
            appendLine("FireflyVPN Runtime Log")
            appendLine("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("GeneratedAt: ${timestampFormat.format(Date())}")
            appendLine("Entries: ${entriesSnapshot.size}")
            appendLine("Privacy: node links, generated configs, IPs, domains, ports, keys, and app package lists are redacted or omitted.")
            appendLine()
            entriesSnapshot.forEach { entry ->
                append(timestampFormat.format(Date(entry.timestampMillis)))
                append(" ")
                append(entry.level.name.padEnd(5))
                append(" ")
                append(entry.component)
                append(" - ")
                appendLine(entry.message)
            }
        }
    }

    private fun logFile(): File? {
        val context = appContext ?: return null
        return File(File(context.filesDir, LOG_DIR), LOG_FILE)
    }

    private fun sanitizeComponent(component: String): String =
        component.replace(Regex("[^A-Za-z0-9_.-]"), "_").take(48).ifBlank { "App" }

    private fun sanitizeMessage(raw: String): String {
        var value = raw
        val replacements = listOf(
            Regex("(?i)\\b(?:ss|ssr|vmess|vless|trojan|hysteria2?|tuic|wireguard)://\\S+") to "<proxy-link>",
            Regex("(?i)https?://\\S+") to "<url>",
            Regex("(?i)\\b[a-z][a-z0-9+.-]*://\\S+") to "<uri>",
            Regex("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b") to "<ip>",
            Regex("(?i)\\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\b") to "<uuid>",
            Regex("(?i)\\b(server|host|address|sni|path|uuid|password|passwd|token|private_key|public_key|short_id|port)=([^,\\s}]+)") to "\$1=<redacted>",
            Regex("(?i)\\b([A-Za-z0-9.-]+\\.[A-Za-z]{2,})(?::\\d{1,5})?\\b") to "<domain>"
        )
        replacements.forEach { (pattern, replacement) ->
            value = value.replace(pattern, replacement)
        }
        return value
            .replace('\n', ' ')
            .replace('\r', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(600)
            .ifBlank { "<empty>" }
    }

    private fun escape(value: String): String =
        value
            .replace("\\", "\\\\")
            .replace("\t", "\\t")
            .replace("\n", "\\n")
            .replace("\r", "\\r")

    private fun unescape(value: String): String {
        val result = StringBuilder()
        var escaping = false
        value.forEach { char ->
            if (escaping) {
                result.append(
                    when (char) {
                        't' -> '\t'
                        'n' -> '\n'
                        'r' -> '\r'
                        else -> char
                    }
                )
                escaping = false
            } else if (char == '\\') {
                escaping = true
            } else {
                result.append(char)
            }
        }
        if (escaping) result.append('\\')
        return result.toString()
    }

    private fun displayDateFormat(): SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    private fun fileDateFormat(): SimpleDateFormat =
        SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
}
