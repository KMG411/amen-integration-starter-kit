import XCTest
@testable import AmenClient

final class WebhookTests: XCTestCase {
    let secret = "unit-test-secret"

    // Golden vector from a real Amen delivery captured 2026-08-30.
    let goldenSecret = "whsec_test"
    let goldenTimestamp = "2026-08-30T18:53:23.885957+00:00"
    let goldenBody = Data(#"{"event":"deal.status.changed","timestamp":"2026-08-30T18:53:23.885957+00:00","payload":{"number":"D-0000000002","status":"paid"}}"#.utf8)
    let goldenHex = "950ca0ff7494dd435d4dc9d7e7ebe31cf54f0859a28a69a686d77e8db9dfd45c"

    func testGoldenVector() {
        let hex = WebhookSignature.compute(secret: goldenSecret, timestamp: goldenTimestamp, rawBody: goldenBody)
        XCTAssertEqual(hex, goldenHex)
        XCTAssertEqual(WebhookSignature.signatureHeaderValue(secret: goldenSecret, timestamp: goldenTimestamp, rawBody: goldenBody), "sha256=\(goldenHex)")
        XCTAssertTrue(WebhookSignature.verify(secret: goldenSecret, timestamp: goldenTimestamp, rawBody: goldenBody, received: "sha256=\(goldenHex)"))
    }

    func testSignatureRoundtrip() {
        let ts = "2026-08-30T18:53:23.885957+00:00"
        let body = Data(#"{"event":"deal.status.changed","timestamp":"2026-08-30T18:53:23.885957+00:00","payload":{}}"#.utf8)
        let sig = WebhookSignature.compute(secret: secret, timestamp: ts, rawBody: body)
        XCTAssertTrue(WebhookSignature.verify(secret: secret, timestamp: ts, rawBody: body, received: sig))
        XCTAssertTrue(WebhookSignature.verify(secret: secret, timestamp: ts, rawBody: body, received: "sha256=\(sig)"))
        // Tamper with the body.
        XCTAssertFalse(WebhookSignature.verify(secret: secret, timestamp: ts, rawBody: body + Data(" ".utf8), received: sig))
        // Tamper with the timestamp — it is part of the signed message.
        XCTAssertFalse(WebhookSignature.verify(secret: secret, timestamp: ts + "0", rawBody: body, received: sig))
        // Missing signature / missing timestamp.
        XCTAssertFalse(WebhookSignature.verify(secret: secret, timestamp: ts, rawBody: body, received: nil))
        XCTAssertFalse(WebhookSignature.verify(secret: secret, timestamp: nil, rawBody: body, received: sig))
    }

    func testHandlerVerifiesAndDedupes() async {
        let seen = Seen()
        let h = WebhookHandler(secret: secret) { e in await seen.add(e.id) }
        let ts = "2026-08-30T18:53:23.885957+00:00"
        let body = Data(#"{"event":"deal.status.changed","timestamp":"2026-08-30T18:53:23.885957+00:00","payload":{"number":"D-1","status":"paid"}}"#.utf8)
        func headers() -> [String: String] {
            ["X-Webhook-Signature": WebhookSignature.signatureHeaderValue(secret: secret, timestamp: ts, rawBody: body),
             "X-Webhook-Timestamp": ts,
             "X-Webhook-Event": "deal.status.changed"]
        }
        // Bad signature -> 401.
        let bad = await h.handle(headers: ["X-Webhook-Signature": "sha256=bad", "X-Webhook-Timestamp": ts], rawBody: body)
        XCTAssertEqual(bad.status, 401)
        // Missing timestamp header -> 401.
        let noTs = await h.handle(headers: ["X-Webhook-Signature": WebhookSignature.signatureHeaderValue(secret: secret, timestamp: ts, rawBody: body)], rawBody: body)
        XCTAssertEqual(noTs.status, 401)
        // First valid delivery -> 200.
        let first = await h.handle(headers: headers(), rawBody: body)
        XCTAssertEqual(first.status, 200)
        XCTAssertNil(first.body["duplicate"])
        // Duplicate (same timestamp) -> 200 + duplicate.
        let second = await h.handle(headers: headers(), rawBody: body)
        XCTAssertEqual(second.status, 200)
        XCTAssertEqual(second.body["duplicate"] as? Bool, true)
        // De-dupe keyed on the event timestamp.
        let ids = await seen.ids
        XCTAssertEqual(ids, [ts])
    }
}
actor Seen { var ids: [String] = []; func add(_ id: String) { ids.append(id) } }
