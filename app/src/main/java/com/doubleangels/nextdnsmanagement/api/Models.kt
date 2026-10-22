package com.doubleangels.nextdnsmanagement.api

import java.time.Instant

data class Profile(val id: String, val name: String)

/** One row of an analytics breakdown. [key] is the API id, [label] what is shown. */
data class Stat(val key: String, val label: String, val queries: Long, val detail: String? = null)

/** Time series: one value list per [Stat.key], aligned with [times]. */
data class Series(val times: List<Instant>, val values: Map<String, List<Long>>, val intervalSeconds: Long)

data class LogEntry(
    val timestamp: Instant,
    val domain: String,
    val root: String?,
    val status: String,
    val protocol: String?,
    val encrypted: Boolean?,
    val deviceName: String?,
    val clientIp: String?,
    val reasons: List<String>,
    val tracker: String?,
) {
    companion object

    val isBlocked get() = status == "blocked"
    val isAllowed get() = status == "allowed"
}

data class LogPage(val entries: List<LogEntry>, val cursor: String?, val streamId: String?)

/** A domain on the allowlist or denylist. */
data class DomainEntry(val id: String, val active: Boolean)

data class Rewrite(val id: String, val name: String, val content: String, val type: String?)

/** Result of https://test.nextdns.io for this device's current connection. */
data class ConnectionStatus(
    val usingNextDns: Boolean,
    val protocol: String?,
    val profile: String?,
    val server: String?,
    val clientName: String?,
) {
    val encrypted: Boolean get() = protocol?.uppercase() in setOf("DOH", "DOT", "DOQ")
}

/** Ranges offered for analytics, with a series interval that gives a readable chart. */
enum class TimeRange(val from: String, val intervalSeconds: Long) {
    DAY("-24h", 3_600),
    WEEK("-7d", 21_600),
    MONTH("-30d", 86_400),
    QUARTER("-90d", 86_400 * 3L),
}

fun LogEntry.Companion.parse(obj: kotlinx.serialization.json.JsonObject): LogEntry? {
    val timestamp = obj.str("timestamp")?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
    val domain = obj.str("domain") ?: return null
    val device = obj.obj("device")
    return LogEntry(
        timestamp = timestamp,
        domain = domain,
        root = obj.str("root"),
        status = obj.str("status") ?: "default",
        protocol = obj.str("protocol"),
        encrypted = obj.bool("encrypted"),
        deviceName = device?.str("name") ?: device?.str("model"),
        clientIp = obj.str("clientIp"),
        reasons = obj["reasons"].objects().mapNotNull { it.str("name") ?: it.str("id") },
        tracker = obj.str("tracker"),
    )
}
