package sa.amnn.kit.resources

import kotlinx.serialization.json.*
import sa.amnn.kit.*

class Deals(private val c: AmenClient) {
    val actions = DealActions(c, this)
    suspend fun create(body: CreateDeal) = c.request("POST", "/deals/", Deal.serializer(), body = json.encodeToJsonElement(body))!!
    suspend fun get(n: String) = c.request("GET", "/deals/$n", Deal.serializer())!!
    suspend fun update(n: String, body: CreateDeal) = c.request("PUT", "/deals/$n", Deal.serializer(), body = json.encodeToJsonElement(body))!!
    suspend fun delete(n: String) { c.request("DELETE", "/deals/$n", JsonElement.serializer()) }
    suspend fun list(params: Map<String, String?> = emptyMap()) = c.page("/deals/", params, "deals", Deal.serializer())
    suspend fun setParties(n: String, buyers: List<String>, sellers: List<String>) =
        c.request("POST", "/deals/$n/parties/", Deal.serializer(), body = buildJsonObject { put("buyers", JsonArray(buyers.map(::JsonPrimitive))); put("sellers", JsonArray(sellers.map(::JsonPrimitive))) })!!
    suspend fun setDeliveryAddress(n: String, a: Address) = c.request("POST", "/deals/$n/delivery-address", Deal.serializer(), body = json.encodeToJsonElement(a))!!
    suspend fun setBillingAddress(n: String, a: Address) = c.request("POST", "/deals/$n/billing-address", Deal.serializer(), body = json.encodeToJsonElement(a))!!
    suspend fun allowedPaymentMethods(n: String): List<String> =
        c.request("GET", "/deals/$n/allowed-payment-methods/", JsonElement.serializer())?.jsonObject?.get("payment_methods")?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
}

/** POST /deals/{n}/action/* — every method returns the updated Deal (or a Checkout for online payment). */
class DealActions(private val c: AmenClient, private val deals: Deals) {
    companion object {
        /** Which statuses each action may be called from (docs/02-deal-lifecycle.md). */
        val ALLOWED_FROM = mapOf(
            "submit" to setOf("draft"), "approve" to setOf("requested"),
            "make-payment-wallet" to setOf("payment_pending"), "make-payment-online" to setOf("payment_pending"),
            "execution-start" to setOf("paid"), "execution-complete" to setOf("executing"), "complete" to setOf("executed"),
            "transfer-seller-amount" to setOf("completed"), "dispute" to setOf("completed"),
            "dispute-approve" to setOf("disputed"), "dispute-decline" to setOf("disputed"),
            "cancel" to setOf("draft", "requested", "payment_pending", "paid", "executing"),
        )
    }
    private suspend fun <T> act(n: String, action: String, serializer: kotlinx.serialization.KSerializer<T>, body: JsonElement? = null, form: List<AmenClient.Part>? = null, check: Boolean = true): T {
        if (check) {
            val status = deals.get(n).status
            if (status !in ALLOWED_FROM.getValue(action)) throw AmenLifecycleError("action '$action' is not allowed from status '$status' (allowed: ${ALLOWED_FROM.getValue(action).joinToString(", ")})")
        }
        return c.request("POST", "/deals/$n/action/$action", serializer, body = if (form == null) (body ?: JsonObject(emptyMap())) else null, form = form)!!
    }
    private fun form(reason: Int, comment: String) = listOf(AmenClient.Part("reason", reason.toString()), AmenClient.Part("comment", comment))

    suspend fun submit(n: String, check: Boolean = true) = act(n, "submit", Deal.serializer(), check = check)
    suspend fun approve(n: String, price: String? = null, check: Boolean = true) = act(n, "approve", Deal.serializer(), buildJsonObject { price?.let { put("price", it) } }, check = check)
    suspend fun payWithWallet(n: String, check: Boolean = true) = act(n, "make-payment-wallet", Deal.serializer(), check = check)
    suspend fun payOnline(n: String, paymentMethod: String = "mada", check: Boolean = true) = act(n, "make-payment-online", Checkout.serializer(), buildJsonObject { put("payment_method", paymentMethod) }, check = check)
    suspend fun executionStart(n: String, check: Boolean = true) = act(n, "execution-start", Deal.serializer(), check = check)
    suspend fun executionComplete(n: String, check: Boolean = true) = act(n, "execution-complete", Deal.serializer(), check = check)
    suspend fun complete(n: String, check: Boolean = true) = act(n, "complete", Deal.serializer(), check = check)
    suspend fun transferSellerAmount(n: String, check: Boolean = true) = act(n, "transfer-seller-amount", Deal.serializer(), check = check)
    suspend fun cancel(n: String, dealParty: String, reason: Int, comment: String, check: Boolean = true) =
        act(n, "cancel", Deal.serializer(), buildJsonObject { put("deal_party", dealParty); put("reason", reason); put("comment", comment) }, check = check)
    suspend fun dispute(n: String, reason: Int, comment: String, attachments: List<AmenClient.Part> = emptyList(), check: Boolean = true) =
        act(n, "dispute", Deal.serializer(), form = form(reason, comment) + attachments.mapIndexed { i, a -> AmenClient.Part("attachment_${i + 1}", file = a.file, filename = a.filename, mimeType = a.mimeType) }, check = check)
    suspend fun disputeApprove(n: String, reason: Int, comment: String, check: Boolean = true) = act(n, "dispute-approve", Deal.serializer(), form = form(reason, comment), check = check)
    suspend fun disputeDecline(n: String, reason: Int, comment: String, check: Boolean = true) = act(n, "dispute-decline", Deal.serializer(), form = form(reason, comment), check = check)
}
