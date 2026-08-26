package sa.amnn.kit

import java.io.File

/** Environment-based configuration. Walks up from cwd (max 3 levels) to find a .env. Existing env vars win. */
data class Config(
    val env: String = "sandbox", val apiKey: String, val baseUrl: String = BASE_URLS.getValue(env),
    val timeoutMs: Long = 20_000, val webhookSecret: String? = null, val maxRetries: Int = 3,
) {
    companion object {
        val BASE_URLS = mapOf("sandbox" to "https://sandbox-api.amnn.sa", "live" to "https://api.amnn.sa")
        const val API_PREFIX = "/api/v1"

        fun fromEnvironment(): Config {
            val file = loadDotenv()
            fun get(k: String) = System.getenv(k) ?: file[k]
            val env = (get("AMN_ENV") ?: "sandbox").lowercase()
            require(env in BASE_URLS) { "AMN_ENV must be 'sandbox' or 'live', got '$env'" }
            val key = get("AMN_API_KEY")?.takeIf { it.isNotBlank() } ?: error("AMN_API_KEY is not set (see .env.example)")
            return Config(env, key, get("AMN_BASE_URL") ?: BASE_URLS.getValue(env), get("AMN_TIMEOUT_MS")?.toLongOrNull() ?: 20_000,
                get("AMN_WEBHOOK_SECRET")?.takeIf { it.isNotBlank() })
        }

        internal fun loadDotenv(): Map<String, String> {
            var dir: File? = File("").absoluteFile
            repeat(3) {
                val f = File(dir, ".env")
                if (f.isFile) return f.readLines().mapNotNull { line ->
                    val l = line.trim(); val eq = l.indexOf('=')
                    if (l.isEmpty() || l.startsWith("#") || eq < 0) null
                    else l.substring(0, eq).trim() to l.substring(eq + 1).replace(Regex("\\s+#.*$"), "").trim()
                }.filter { it.second.isNotEmpty() }.toMap()
                dir = dir?.parentFile ?: return emptyMap()
            }
            return emptyMap()
        }
    }
}
