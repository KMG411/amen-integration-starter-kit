package sa.amnn.kit

import kotlinx.coroutines.test.runTest
import sa.amnn.kit.webhooks.WebhookHandler
import sa.amnn.kit.webhooks.WebhookSignature
import kotlin.test.*

class WebhookTest {
    private val secret = "unit-test-secret"
    @Test fun signatureRoundtrip() {
        val body = """{"id":"e1","event":"deal.paid"}""".toByteArray(); val sig = WebhookSignature.compute(secret, body)
        assertTrue(WebhookSignature.verify(secret, body, sig)); assertTrue(WebhookSignature.verify(secret, body, "sha256=$sig"))
        assertFalse(WebhookSignature.verify(secret, body + ' '.code.toByte(), sig)); assertFalse(WebhookSignature.verify(secret, body, null))
    }
    @Test fun handlerVerifiesAndDedupes() = runTest {
        val seen = mutableListOf<String>(); val h = WebhookHandler(secret) { seen += it.id }
        val body = """{"id":"e1","event":"deal.paid"}""".toByteArray(); val good = mapOf("X-Signature" to WebhookSignature.compute(secret, body))
        assertEquals(401, h.handle(mapOf("X-Signature" to "bad"), body).status)
        assertEquals(mapOf("ok" to true), h.handle(good, body).body)
        assertEquals(true, h.handle(good, body).body["duplicate"])
        assertEquals(listOf("e1"), seen)
    }
}
