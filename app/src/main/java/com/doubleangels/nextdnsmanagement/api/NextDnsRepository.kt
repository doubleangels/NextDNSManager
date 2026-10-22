package com.doubleangels.nextdnsmanagement.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Analytics breakdowns offered by the API. */
enum class Breakdown(val path: String) {
    DOMAINS("domains"),
    REASONS("reasons"),
    DEVICES("devices"),
    PROTOCOLS("protocols"),
    QUERY_TYPES("queryTypes"),
    IP_VERSIONS("ipVersions"),
    DNSSEC("dnssec"),
    ENCRYPTION("encryption"),
    COUNTRIES("destinations"),
    COMPANIES("destinations"),
}

/** Lists under a profile that hold `{id, active}` items. */
enum class EntryList(val path: String, val supportsActive: Boolean) {
    DENYLIST("denylist", true),
    ALLOWLIST("allowlist", true),
    BLOCKED_TLDS("security/tlds", false),
    BLOCKLISTS("privacy/blocklists", false),
    NATIVE_TRACKERS("privacy/natives", false),
    SERVICES("parentalControl/services", true),
    CATEGORIES("parentalControl/categories", true),
}

/** Settings objects that are PATCHed with partial updates. */
enum class SettingsSection(val path: String) {
    SECURITY("security"),
    PRIVACY("privacy"),
    PARENTAL_CONTROL("parentalControl"),
    SETTINGS("settings"),
}

class NextDnsRepository(private val api: NextDnsApi) {

    private val checkClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun profiles(): List<Profile> {
        val profiles = mutableListOf<Profile>()
        var cursor: String? = null
        do {
            val response = api.get("profiles", mapOf("cursor" to cursor))
            response.data.objects().forEach { obj ->
                val id = obj.str("id") ?: return@forEach
                profiles += Profile(id, obj.str("name")?.takeIf { it.isNotBlank() } ?: id)
            }
            cursor = response.meta?.obj("pagination")?.str("cursor")
        } while (cursor != null)
        return profiles
    }

    suspend fun renameProfile(profileId: String, name: String) {
        api.patch("profiles/$profileId", buildJsonObject { put("name", name) })
    }

    /** Query counts by status ("default", "blocked", "allowed"). */
    suspend fun statusTotals(profileId: String, range: TimeRange): Map<String, Long> =
        api.get("profiles/$profileId/analytics/status", mapOf("from" to range.from)).data.objects()
            .associate { (it.str("status") ?: "default") to (it.long("queries") ?: 0L) }

    suspend fun statusSeries(profileId: String, range: TimeRange): Series {
        val response = api.get(
            "profiles/$profileId/analytics/status;series",
            mapOf(
                "from" to range.from,
                "interval" to range.intervalSeconds.toString(),
                "timezone" to ZoneId.systemDefault().id,
            ),
        )
        val seriesMeta = response.meta?.obj("series")
        val times = seriesMeta?.get("times").asArray().mapNotNull {
            (it as? JsonPrimitive)?.content?.let { text -> runCatching { Instant.parse(text) }.getOrNull() }
        }
        val values = response.data.objects().associate { (it.str("status") ?: "default") to it.longs("queries") }
        return Series(times, values, seriesMeta?.long("interval") ?: range.intervalSeconds)
    }

    suspend fun breakdown(
        profileId: String,
        breakdown: Breakdown,
        range: TimeRange,
        limit: Int = 10,
        status: String? = null,
    ): List<Stat> {
        val query = buildMap<String, String?> {
            put("from", range.from)
            put("limit", limit.toString())
            put("status", status)
            when (breakdown) {
                Breakdown.COUNTRIES -> put("type", "countries")
                Breakdown.COMPANIES -> put("type", "gafam")
                else -> Unit
            }
        }
        val data = api.get("profiles/$profileId/analytics/${breakdown.path}", query).data
        // Company counts have been documented both as a list and as an object.
        if (breakdown == Breakdown.COMPANIES && data is JsonObject) {
            return data.entries.mapNotNull { (company, value) ->
                (value as? JsonPrimitive)?.content?.toLongOrNull()?.let { Stat(company, company.companyLabel(), it) }
            }.sortedByDescending { it.queries }
        }
        return data.objects().map { it.toStat(breakdown) }
    }

    private fun JsonObject.toStat(breakdown: Breakdown): Stat {
        val queries = long("queries") ?: 0L
        return when (breakdown) {
            Breakdown.DOMAINS -> {
                val domain = str("domain") ?: str("name") ?: "?"
                Stat(domain, domain, queries, str("root")?.takeIf { it != domain })
            }
            Breakdown.REASONS -> Stat(str("id") ?: "?", str("name") ?: str("id") ?: "?", queries)
            Breakdown.DEVICES -> {
                val id = str("id") ?: "?"
                val name = str("name") ?: str("model") ?: if (id == UNIDENTIFIED_DEVICE) "" else id
                Stat(id, name, queries, str("model")?.takeIf { it != name })
            }
            Breakdown.PROTOCOLS -> (str("protocol") ?: "?").let { Stat(it, it, queries) }
            Breakdown.QUERY_TYPES -> Stat(long("type")?.toString() ?: str("name") ?: "?", str("name") ?: "?", queries)
            Breakdown.IP_VERSIONS -> "IPv${long("version") ?: "?"}".let { Stat(it, it, queries) }
            Breakdown.DNSSEC -> (bool("validated") == true).toString().let { Stat(it, it, queries) }
            Breakdown.ENCRYPTION -> (bool("encrypted") == true).toString().let { Stat(it, it, queries) }
            Breakdown.COUNTRIES -> {
                val code = str("code") ?: str("country") ?: "?"
                val sample = this["domains"].asArray().take(3)
                    .mapNotNull { (it as? JsonPrimitive)?.content }.joinToString(", ")
                Stat(code, code.countryLabel(), queries, sample.ifEmpty { null })
            }
            Breakdown.COMPANIES -> (str("company") ?: "?").let { Stat(it, it.companyLabel(), queries) }
        }
    }

    suspend fun logs(
        profileId: String,
        cursor: String? = null,
        search: String? = null,
        status: String? = null,
        limit: Int = 100,
    ): LogPage {
        val response = api.get(
            "profiles/$profileId/logs",
            mapOf(
                "cursor" to cursor,
                "search" to search?.takeIf { it.isNotBlank() },
                "status" to status,
                "limit" to limit.toString(),
            ),
        )
        return LogPage(
            entries = response.data.objects().mapNotNull { LogEntry.parse(it) },
            cursor = response.meta?.obj("pagination")?.str("cursor"),
            streamId = response.meta?.obj("stream")?.str("id"),
        )
    }

    fun streamLogs(profileId: String, fromId: String?): Flow<LogEntry> =
        api.streamLogs(profileId, fromId).mapNotNull { LogEntry.parse(it) }

    suspend fun clearLogs(profileId: String) {
        api.delete("profiles/$profileId/logs")
    }

    suspend fun entries(profileId: String, list: EntryList): List<DomainEntry> =
        api.get("profiles/$profileId/${list.path}").data.objects().mapNotNull { obj ->
            obj.str("id")?.let { DomainEntry(it, obj.bool("active") ?: true) }
        }

    suspend fun addEntry(profileId: String, list: EntryList, id: String) {
        api.post("profiles/$profileId/${list.path}", buildJsonObject {
            put("id", id)
            if (list.supportsActive) put("active", true)
        })
    }

    suspend fun setEntryActive(profileId: String, list: EntryList, id: String, active: Boolean) {
        api.patch("profiles/$profileId/${list.path}/$id", buildJsonObject { put("active", active) })
    }

    suspend fun removeEntry(profileId: String, list: EntryList, id: String) {
        api.delete("profiles/$profileId/${list.path}/$id")
    }

    suspend fun section(profileId: String, section: SettingsSection): JsonObject =
        api.get("profiles/$profileId/${section.path}").data.asObject() ?: JsonObject(emptyMap())

    /** PATCHes [changes] (possibly nested, e.g. `{"logs": {"enabled": true}}`) into a section. */
    suspend fun updateSection(profileId: String, section: SettingsSection, changes: JsonObject) {
        api.patch("profiles/$profileId/${section.path}", changes)
    }

    suspend fun rewrites(profileId: String): List<Rewrite> =
        api.get("profiles/$profileId/rewrites").data.objects().mapNotNull { obj ->
            val name = obj.str("name") ?: return@mapNotNull null
            Rewrite(obj.str("id") ?: name, name, obj.str("content") ?: "", obj.str("type"))
        }

    suspend fun addRewrite(profileId: String, name: String, content: String) {
        api.post("profiles/$profileId/rewrites", buildJsonObject {
            put("name", name)
            put("content", content)
        })
    }

    suspend fun removeRewrite(profileId: String, id: String) {
        api.delete("profiles/$profileId/rewrites/$id")
    }

    /** Asks test.nextdns.io whether this device's DNS currently goes through NextDNS. */
    suspend fun connectionStatus(): ConnectionStatus = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://test.nextdns.io")
            .header("Accept", "application/json")
            .header("Cache-Control", "no-cache")
            .build()
        checkClient.newCall(request).execute().use { response ->
            val obj = NextDnsApi.JSON.parseToJsonElement(response.body.string()).asObject()
                ?: throw ApiException("Unexpected response from test.nextdns.io")
            ConnectionStatus(
                usingNextDns = obj.str("status").equals("ok", ignoreCase = true),
                protocol = obj.str("protocol"),
                profile = obj.str("profile"),
                server = obj.str("server"),
                clientName = obj.str("clientName"),
            )
        }
    }

    companion object {
        const val UNIDENTIFIED_DEVICE = "__UNIDENTIFIED__"

        /** Builds `{"a": {"b": value}}` for the path `a.b`. */
        fun nested(path: List<String>, value: JsonElement): JsonObject =
            path.foldRight(value) { key, acc -> JsonObject(mapOf(key to acc)) } as JsonObject
    }
}

private fun String.countryLabel(): String =
    runCatching { Locale.Builder().setRegion(this).build().displayCountry }.getOrNull()
        ?.takeIf { it.isNotBlank() } ?: this

private fun String.companyLabel(): String = when (lowercase()) {
    "google" -> "Google"
    "facebook" -> "Meta"
    "apple" -> "Apple"
    "amazon" -> "Amazon"
    "microsoft" -> "Microsoft"
    "others" -> "Others"
    else -> replaceFirstChar { it.titlecase() }
}
