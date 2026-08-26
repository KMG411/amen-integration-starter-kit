package sa.amnn.kit.resources

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.encodeToJsonElement
import sa.amnn.kit.*

class Customers(private val c: AmenClient) {
    suspend fun create(body: CreateCustomer) = c.request("POST", "/customers/", Customer.serializer(), body = json.encodeToJsonElement(body))!!
    suspend fun get(customerNumber: String) = c.request("GET", "/customers/$customerNumber", Customer.serializer())!!
    suspend fun list(params: Map<String, String?> = emptyMap()) = c.page("/customers/", params, "customers", Customer.serializer())
    /** Iterate every page — never process only the first page by accident. */
    fun all(filters: Map<String, String?> = emptyMap()): Flow<Customer> = flow {
        var page = 0
        while (true) {
            val p = list(filters + ("page" to page.toString()))
            p.items.forEach { emit(it) }
            if (page + 1 >= p.pages || p.items.isEmpty()) return@flow
            page++
        }
    }
}
