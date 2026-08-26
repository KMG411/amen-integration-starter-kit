package sa.amnn.kit

import kotlinx.coroutines.runBlocking
import kotlin.test.*

/** Mirrors scenario/golden-path.yml. Skipped without sandbox credentials. */
class GoldenPathIntegrationTest {
    private fun phone(p: String) = (p + (System.currentTimeMillis() / 1000).toString().takeLast(7)).take(9)

    @Test fun goldenPath() = runBlocking {
        val a = runCatching { AmenClient(Config.fromEnvironment()) }.getOrNull() ?: return@runBlocking println("SKIP: AMN_API_KEY not set")
        if (a.config.env != "sandbox") return@runBlocking println("SKIP: sandbox only")
        val buyer = a.customers.create(CreateCustomer("Buyer", "Kit", "SA", phone("57")))
        val seller = a.customers.create(CreateCustomer("Seller", "Kit", "SA", phone("58")))
        val deal = a.deals.create(CreateDeal("product", "Starter Kit golden path", "100.00", "0.00", a.lookups.categories().first().id, "Reference deal created by the Amen integration starter kit"))
        val n = deal.number; assertEquals("draft", deal.status)
        assertEquals("draft", a.deals.setParties(n, listOf(buyer.number), listOf(seller.number)).status)
        assertEquals("draft", a.deals.setDeliveryAddress(n, Address(a.lookups.cities().first().id, "King Fahd Rd", "1234", "12211", "Al Olaya", "1")).status)
        assertEquals("requested", a.deals.actions.submit(n).status)
        assertEquals("payment_pending", a.deals.actions.approve(n).status)
        val paid = try { a.deals.actions.payWithWallet(n) } catch (e: AmenApiError) { return@runBlocking println("NEEDS_TOP_UP: ${e.codes}") }
        assertEquals("paid", paid.status)
        assertEquals("executing", a.deals.actions.executionStart(n).status)
        assertEquals("executed", a.deals.actions.executionComplete(n).status)
        assertEquals("completed", a.deals.actions.complete(n).status)
        assertEquals("completed", a.deals.actions.transferSellerAmount(n).status)
    }
}
