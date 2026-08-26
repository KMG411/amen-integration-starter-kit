package sa.amnn.kit.resources

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import sa.amnn.kit.AmenClient
import sa.amnn.kit.Withdrawal

class Withdrawals(private val c: AmenClient) {
    suspend fun create(bankAccountId: String, amount: String) = c.request("POST", "/withdrawals/", Withdrawal.serializer(), body = buildJsonObject { put("bank_account_id", bankAccountId); put("amount", amount) })!!
    suspend fun get(n: String) = c.request("GET", "/withdrawals/$n", Withdrawal.serializer())!!
    suspend fun list(params: Map<String, String?> = emptyMap()) = c.page("/withdrawals/", params, "withdrawals", Withdrawal.serializer())
}
