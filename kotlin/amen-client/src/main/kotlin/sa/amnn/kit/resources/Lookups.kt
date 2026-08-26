package sa.amnn.kit.resources

import kotlinx.serialization.builtins.ListSerializer
import sa.amnn.kit.AmenClient
import sa.amnn.kit.Lookup

class Lookups(private val c: AmenClient) {
    private val list = ListSerializer(Lookup.serializer())
    suspend fun cities() = c.request("GET", "/cities", list) ?: emptyList()
    suspend fun categories() = c.request("GET", "/categories/", list) ?: emptyList()
    suspend fun disputeReasons() = c.request("GET", "/dispute-reasons/", list) ?: emptyList()
    suspend fun disputeResolutionReasons() = c.request("GET", "/dispute-resolution-reasons/", list) ?: emptyList()
    suspend fun cancelReasons(partyType: String? = null) = c.request("GET", "/cancel-reasons/", list, mapOf("party_type" to partyType)) ?: emptyList()
}
