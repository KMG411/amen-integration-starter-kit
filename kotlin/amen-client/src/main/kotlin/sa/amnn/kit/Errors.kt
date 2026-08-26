package sa.amnn.kit

/** Any non-2xx response. [codes] holds the API's error codes, e.g. "price__required". */
class AmenApiError(val status: Int, val codes: List<String>, val method: String, val path: String, val body: String) :
    RuntimeException("$status $method $path: ${if (codes.isEmpty()) body else codes.joinToString(", ")}") {
    fun has(code: String) = code in codes
    val retryable get() = status == 429 || status >= 500
}

/** Thrown locally, before any HTTP call, when an action is not valid for the deal's status. */
class AmenLifecycleError(message: String) : IllegalStateException(message)
