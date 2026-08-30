package sa.amnn.kit

import kotlinx.coroutines.test.runTest
import sa.amnn.kit.webhooks.WebhookHandler
import sa.amnn.kit.webhooks.WebhookSignature
import kotlin.test.*

class WebhookTest {
    private val secret = "unit-test-secret"

    // Golden vector captured from a real Amen delivery (verified 2026-08-30).
    private val goldenSecret = "whsec_test"
    private val goldenTs = "2026-08-30T18:53:23.885957+00:00"
    private val goldenBody = """{"event":"deal.status.changed","timestamp":"2026-08-30T18:53:23.885957+00:00","payload":{"number":"D-0000000002","status":"paid"}}"""
    private val goldenHex = "950ca0ff7494dd435d4dc9d7e7ebe31cf54f0859a28a69a686d77e8db9dfd45c"

    @Test fun goldenVector() {
        val computed = WebhookSignature.compute(goldenSecret, goldenTs, goldenBody.toByteArray())
        assertEquals(goldenHex, computed)
        assertTrue(WebhookSignature.verify(goldenSecret, goldenTs, goldenBody.toByteArray(), computed))
        assertTrue(WebhookSignature.verify(goldenSecret, goldenTs, goldenBody.toByteArray(), "sha256=$computed"))
    }

    @Test fun signatureRoundtrip() {
        val ts = "2026-08-30T00:00:00+00:00"
        val body = """{"event":"deal.status.changed","timestamp":"$ts"}""".toByteArray()
        val sig = WebhookSignature.compute(secret, ts, body)
        assertTrue(WebhookSignature.verify(secret, ts, body, sig))
        assertTrue(WebhookSignature.verify(secret, ts, body, "sha256=$sig"))
        // tampered body must fail
        assertFalse(WebhookSignature.verify(secret, ts, body + ' '.code.toByte(), sig))
        // tampered timestamp must fail (timestamp is part of the signed message)
        assertFalse(WebhookSignature.verify(secret, ts + "x", body, sig))
        // missing signature / missing timestamp must fail
        assertFalse(WebhookSignature.verify(secret, ts, body, null))
        assertFalse(WebhookSignature.verify(secret, null, body, sig))
    }

    @Test fun handlerVerifiesDispatchesAndDedupes() = runTest {
        val seen = mutableListOf<String>()
        val types = mutableListOf<String?>()
        val h = WebhookHandler(secret) { seen += it.id; types += it.type }
        val ts = "2026-08-30T18:53:23.885957+00:00"
        val body = """{"event":"deal.status.changed","timestamp":"$ts","payload":{"number":"D-1","status":"paid"}}""".toByteArray()
        val sig = WebhookSignature.compute(secret, ts, body)
        val good = mapOf(
            WebhookSignature.SIGNATURE_HEADER to "sha256=$sig",
            WebhookSignature.TIMESTAMP_HEADER to ts,
            WebhookSignature.EVENT_HEADER to "deal.status.changed",
        )

        // bad signature -> 401
        assertEquals(401, h.handle(good + (WebhookSignature.SIGNATURE_HEADER to "sha256=bad"), body).status)
        // missing timestamp header -> 401
        assertEquals(401, h.handle(mapOf(WebhookSignature.SIGNATURE_HEADER to "sha256=$sig"), body).status)
        // valid -> 200 ok, dispatched once, deduped on the event timestamp
        assertEquals(mapOf("ok" to true), h.handle(good, body).body)
        assertEquals(true, h.handle(good, body).body["duplicate"])
        assertEquals(listOf(ts), seen)
        assertEquals(listOf<String?>("deal.status.changed"), types)
    }
}
