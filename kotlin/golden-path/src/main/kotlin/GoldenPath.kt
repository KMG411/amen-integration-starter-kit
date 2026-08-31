// Golden path (scenario/golden-path.yml). `gradle :golden-path:run [--args=DL-000123]` — pass a deal number to resume from 'paid'.
import kotlinx.coroutines.runBlocking
import sa.amnn.kit.*
import kotlin.system.exitProcess

fun phone(p: String) = (p + (System.currentTimeMillis() / 1000).toString().takeLast(7)).take(9)
fun step(label: String, d: Deal? = null) = println("✔ $label${d?.let { " → status=${it.status}" } ?: ""}")

fun main(args: Array<String>) = runBlocking {
    val amen = AmenClient(Config.fromEnvironment())
    println("environment: ${amen.config.env} (${amen.config.baseUrl})\n")
    suspend fun continueFromPaid(n: String) {
        step("execution-start", amen.deals.actions.executionStart(n))
        step("execution-complete", amen.deals.actions.executionComplete(n))
        step("complete", amen.deals.actions.complete(n))
        step("transfer-seller-amount (payout)", amen.deals.actions.transferSellerAmount(n))
        println("\n🎉 deal $n finished: ${amen.deals.get(n).status}")
    }
    if (args.isNotEmpty()) { continueFromPaid(args[0]); return@runBlocking }

    val buyer = amen.customers.create(CreateCustomer("Buyer", "Kit", "SA", phone("57")))
    val seller = amen.customers.create(CreateCustomer("Seller", "Kit", "SA", phone("58")))
    step("customers ${buyer.number} (buyer), ${seller.number} (seller)")
    val category = amen.lookups.categories().first().id; val city = amen.lookups.cities().first().id
    val deal = amen.deals.create(CreateDeal("product", "Starter Kit golden path", offerPrice = "100.00", offerDeliveryFee = "10.00", offerCategory = category, offerDescription = "Reference deal created by the Amen integration starter kit"))
    val n = deal.number; step("deal $n created", deal)
    step("parties", amen.deals.setParties(n, listOf(buyer.number), listOf(seller.number)))
    step("delivery address", amen.deals.setDeliveryAddress(n, Address(city, "King Fahd Rd", "1234", "12211", "Al Olaya", "1")))
    step("submit", amen.deals.actions.submit(n))
    step("approve", amen.deals.actions.approve(n))
    println("  allowed payment methods: ${amen.deals.allowedPaymentMethods(n)}")
    try { step("pay with wallet", amen.deals.actions.payWithWallet(n)) }
    catch (e: AmenApiError) {
        val checkout = amen.deals.actions.payOnline(n, "mada")
        println("\n⏸  NEEDS_TOP_UP — wallet payment not possible (${e.codes.ifEmpty { listOf(e.status) }.joinToString(", ")}).\n   HyperPay checkout created: $checkout\n   Top up the sandbox wallet (GET /api/v1/account → wallet.top_up_account) or complete the checkout, then:\n       gradle :golden-path:run --args=$n")
        exitProcess(0)
    }
    continueFromPaid(n)
}
