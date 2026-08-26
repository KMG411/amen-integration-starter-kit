package sa.amnn.kit.resources

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonElement
import sa.amnn.kit.*

class AccountResource(private val c: AmenClient) {
    suspend fun get() = c.request("GET", "/account", Account.serializer())!!
    suspend fun bankAccounts() = c.request("GET", "/account/bank-accounts/", ListSerializer(BankAccount.serializer())) ?: emptyList()
    suspend fun linkBankAccount(iban: String, proofDocument: AmenClient.Part? = null) =
        c.request("POST", "/account/bank-accounts/", BankAccount.serializer(), form = listOfNotNull(AmenClient.Part("iban", iban), proofDocument))!!
    suspend fun deleteBankAccount(id: String) { c.request("DELETE", "/account/bank-accounts/$id", JsonElement.serializer()) }
}
