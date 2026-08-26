import XCTest
@testable import AmenClient

final class WebhookTests: XCTestCase {
    let secret = "unit-test-secret"
    func testSignatureRoundtrip() {
        let body = Data(#"{"id":"e1","event":"deal.paid"}"#.utf8); let sig = WebhookSignature.compute(secret: secret, rawBody: body)
        XCTAssertTrue(WebhookSignature.verify(secret: secret, rawBody: body, received: sig))
        XCTAssertTrue(WebhookSignature.verify(secret: secret, rawBody: body, received: "sha256=\(sig)"))
        XCTAssertFalse(WebhookSignature.verify(secret: secret, rawBody: body + Data(" ".utf8), received: sig))
        XCTAssertFalse(WebhookSignature.verify(secret: secret, rawBody: body, received: nil))
    }
    func testHandlerVerifiesAndDedupes() async {
        let seen = Seen(); let h = WebhookHandler(secret: secret) { e in await seen.add(e.id) }
        let body = Data(#"{"id":"e1","event":"deal.paid"}"#.utf8); let good = ["X-Signature": WebhookSignature.compute(secret: secret, rawBody: body)]
        let bad = await h.handle(headers: ["X-Signature": "bad"], rawBody: body); XCTAssertEqual(bad.status, 401)
        let first = await h.handle(headers: good, rawBody: body); XCTAssertEqual(first.status, 200)
        let second = await h.handle(headers: good, rawBody: body); XCTAssertEqual(second.body["duplicate"] as? Bool, true)
        let ids = await seen.ids; XCTAssertEqual(ids, ["e1"])
    }
}
actor Seen { var ids: [String] = []; func add(_ id: String) { ids.append(id) } }
