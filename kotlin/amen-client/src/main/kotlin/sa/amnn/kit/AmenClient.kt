package sa.amnn.kit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import sa.amnn.kit.resources.*
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/** AmenClient — the one place that knows about auth headers, base URL, timeouts and retries. Suspend API; call from coroutines. */
class AmenClient(val config: Config, httpClient: OkHttpClient? = null) {
    private val http = httpClient ?: OkHttpClient.Builder().callTimeout(config.timeoutMs, TimeUnit.MILLISECONDS).build()
    val lookups = Lookups(this); val account = AccountResource(this); val customers = Customers(this)
    val deals = Deals(this); val withdrawals = Withdrawals(this); val webhooks = Webhooks(this)

    /** Multipart part: text value or file. */
    class Part(val name: String, val value: String? = null, val file: ByteArray? = null, val filename: String? = null, val mimeType: String = "application/octet-stream")

    suspend fun <T> request(method: String, path: String, serializer: KSerializer<T>, params: Map<String, String?> = emptyMap(), body: JsonElement? = null, form: List<Part>? = null): T? {
        val url = (config.baseUrl + Config.API_PREFIX + path).toHttpUrlBuilder().apply { params.forEach { (k, v) -> v?.let { addQueryParameter(k, it) } } }.build()
        val b = Request.Builder().url(url).header("X-API-Token", config.apiKey).header("Accept", "application/json").header("User-Agent", "amen-starter-kit-kotlin/0.1")
        if (method != "GET") b.header("Origin", config.baseUrl).header("Referer", config.baseUrl)   // origin checks on mutating calls
        val reqBody: RequestBody? = when {
            form != null -> MultipartBody.Builder().setType(MultipartBody.FORM).apply {
                form.forEach { p -> if (p.file != null) addFormDataPart(p.name, p.filename, p.file.toRequestBody(p.mimeType.toMediaType())) else addFormDataPart(p.name, p.value ?: "") }
            }.build()
            body != null -> body.toString().toRequestBody("application/json".toMediaType())
            method == "GET" -> null
            else -> ByteArray(0).toRequestBody(null)
        }
        val req = b.method(method, reqBody).build()

        var attempt = 0
        while (true) {
            attempt++
            val (status, text, retryAfter) = try {
                withContext(Dispatchers.IO) { http.newCall(req).execute().use { Triple(it.code, it.body?.string() ?: "", it.header("Retry-After")) } }
            } catch (e: IOException) {
                if (attempt > config.maxRetries) throw e; delay(backoff(attempt)); continue
            }
            if (status < 400) return if (text.isBlank()) null else json.decodeFromString(serializer, text)
            val err = toError(status, text, method, Config.API_PREFIX + path)
            if (err.retryable && attempt <= config.maxRetries) { delay(backoff(attempt, retryAfter)); continue }
            throw err
        }
    }

    /** Lenient page decoding: `{ <key>: [...], page, pages, total }` or a nested `page` object. */
    suspend fun <T> page(path: String, params: Map<String, String?>, key: String, serializer: KSerializer<T>): Page<T> {
        val n = request("GET", path, JsonElement.serializer(), params)?.jsonObject ?: return Page(emptyList(), 0, 1, 0)
        val arr = (n[key] ?: n["results"] ?: n["items"])?.jsonArray ?: JsonArray(emptyList())
        val meta = (n["page"] as? JsonObject) ?: n
        fun i(k: String, d: Int) = meta[k]?.jsonPrimitive?.intOrNull ?: d
        return Page(arr.map { json.decodeFromJsonElement(serializer, it) }, i("page", 0), i("pages", 1), i("total", 0))
    }

    companion object {
        internal fun toError(status: Int, body: String, method: String, path: String): AmenApiError {
            val codes = runCatching { (json.parseToJsonElement(body).jsonObject["error"]) }.getOrNull()?.let { e ->
                if (e is JsonArray) e.map { it.jsonPrimitive.content } else listOf(e.jsonPrimitive.content) } ?: emptyList()
            return AmenApiError(status, codes, method, path, body)
        }
        internal fun backoff(attempt: Int, retryAfter: String? = null): Long =
            retryAfter?.toLongOrNull()?.times(1000) ?: (minOf(1L shl attempt, 20L) * 1000 + Random.nextLong(1000))
        private fun String.toHttpUrlBuilder() = HttpUrl.get(this).newBuilder()
    }
}
