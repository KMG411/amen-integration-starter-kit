package sa.amnn.kit

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Models mirror openapi/openapi.yml. Money is a String ("100.00"); timestamps are epoch milliseconds. Unknown fields are ignored. */
val json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = false }

fun Long?.toInstant() = this?.let { java.time.Instant.ofEpochMilli(it) }

@Serializable data class Customer(val id: String? = null, val number: String, @SerialName("first_name") val firstName: String? = null, @SerialName("last_name") val lastName: String? = null, val status: String? = null, @SerialName("created_at") val createdAt: Long? = null)
@Serializable data class Deal(val id: String? = null, val number: String, val status: String, val price: String? = null, @SerialName("created_at") val createdAt: Long? = null, @SerialName("updated_at") val updatedAt: Long? = null)
@Serializable data class Checkout(val id: Int? = null, val provider: String? = null, val hyperpay: Map<String, String?>? = null, val amount: String? = null)
@Serializable data class Withdrawal(val id: String? = null, val number: String, val status: String, val amount: String? = null)
/** secretKey is returned ONLY at creation — store it in a secret manager immediately. */
@Serializable data class Webhook(val id: String, val url: String, @SerialName("secret_key") val secretKey: String? = null)
@Serializable data class Account(val id: String? = null, val name: String? = null, val wallet: Map<String, kotlinx.serialization.json.JsonElement>? = null)
@Serializable data class Lookup(val id: Int, val name: String? = null)
@Serializable data class BankAccount(val id: String, val iban: String? = null, val status: String? = null)
data class Page<T>(val items: List<T>, val page: Int, val pages: Int, val total: Int)

@Serializable data class CreateCustomer(@SerialName("first_name") val firstName: String, @SerialName("last_name") val lastName: String, @SerialName("phone_code") val phoneCode: String, @SerialName("phone_number") val phoneNumber: String)
@Serializable data class CreateDeal(
    @SerialName("offer_type") val offerType: String, @SerialName("offer_title") val offerTitle: String,
    @SerialName("offer_price") val offerPrice: String? = null, @SerialName("offer_delivery_fee") val offerDeliveryFee: String? = null,
    @SerialName("offer_category") val offerCategory: Int? = null, @SerialName("offer_description") val offerDescription: String? = null,
    @SerialName("deal_subject_details") val dealSubjectDetails: String? = null,
)
@Serializable data class Address(val city: Int, val street: String, @SerialName("building_number") val buildingNumber: String, @SerialName("zip_code") val zipCode: String, val district: String? = null, @SerialName("unit_number") val unitNumber: String? = null)
