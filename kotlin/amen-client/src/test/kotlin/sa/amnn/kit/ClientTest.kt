package sa.amnn.kit

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.*

class ClientTest {
    private fun server(vararg responses: MockResponse): Pair<MockWebServer, AmenClient> {
        val s = MockWebServer().apply { responses.forEach { enqueue(it) }; start() }
        return s to AmenClient(Config(apiKey = "test-token", baseUrl = s.url("/").toString().trimEnd('/'), maxRetries = 1))
    }
    private fun json(status: Int, body: String) = MockResponse().setResponseCode(status).setHeader("Content-Type", "application/json").setBody(body)

    @Test fun authHeaderAndPath() = runTest {
        val (s, c) = server(json(200, """{"id":"a1","name":"test"}"""))
        assertEquals("a1", c.account.get().id)
        val r = s.takeRequest(); assertEquals("/api/v1/account", r.path); assertEquals("test-token", r.getHeader("X-API-Token")); s.shutdown()
    }
    @Test fun errorCodesParsed() = runTest {
        val (s, c) = server(json(400, """{"error":["first_name__required"]}"""))
        val e = assertFailsWith<AmenApiError> { c.customers.create(CreateCustomer("", "x", "SA", "5")) }
        assertEquals(400, e.status); assertTrue(e.has("first_name__required")); assertFalse(e.retryable); s.shutdown()
    }
    @Test fun retries429ThenSucceeds() = runTest {
        val (s, c) = server(json(429, """{"error":["rate_limit__exceeded"]}""").setHeader("Retry-After", "0"), json(200, """[{"id":1,"name":"Riyadh"}]"""))
        assertEquals(1, c.lookups.cities()[0].id); assertEquals(2, s.requestCount); s.shutdown()
    }
    @Test fun lifecycleGuardBlocksLocally() = runTest {
        val (s, c) = server(json(200, """{"number":"DL-1","status":"draft"}"""))
        assertFailsWith<AmenLifecycleError> { c.deals.actions.approve("DL-1") }
        assertEquals(1, s.requestCount); s.shutdown()
    }
    @Test fun originOnMutatingRequests() = runTest {
        val (s, c) = server(json(201, """{"id":"w","url":"u","secret_key":"s"}"""))
        assertEquals("s", c.webhooks.create("https://example.com/hook").secretKey)
        assertEquals(c.config.baseUrl, s.takeRequest().getHeader("Origin")); s.shutdown()
    }
    @Test fun snakeCaseAndEpochMillis() {
        assertTrue(json.encodeToString(CreateDeal.serializer(), CreateDeal("product", "t", offerPrice = "1.00")).contains("\"offer_type\""))
        val deal = json.decodeFromString(Deal.serializer(), """{"number":"DL-1","status":"draft","created_at":1679568486000,"unknown":1}""")
        assertEquals(2023, deal.createdAt.toInstant()!!.atZone(java.time.ZoneOffset.UTC).year)
    }
}
