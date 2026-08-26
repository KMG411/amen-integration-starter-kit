import XCTest
@testable import AmenClient

struct MockTransport: HTTPTransport {
    let handler: @Sendable (URLRequest) -> (Int, String)
    func send(_ r: URLRequest) async throws -> (Data, HTTPURLResponse) {
        let (status, body) = handler(r)
        return (Data(body.utf8), HTTPURLResponse(url: r.url!, statusCode: status, httpVersion: nil, headerFields: ["Content-Type": "application/json"])!)
    }
}
func client(_ h: @escaping @Sendable (URLRequest) -> (Int, String)) -> AmenClient { AmenClient(Config(apiKey: "test-token", maxRetries: 1), transport: MockTransport(handler: h)) }

final class ClientTests: XCTestCase {
    func testAuthHeaderAndBaseURL() async throws {
        let acc = try await client { r in
            XCTAssertEqual(r.url?.absoluteString, "https://sandbox-api.amnn.sa/api/v1/account"); XCTAssertEqual(r.value(forHTTPHeaderField: "X-API-Token"), "test-token")
            return (200, #"{"id":"a1","name":"test"}"#)
        }.account.get()
        XCTAssertEqual(acc.id, "a1")
    }
    func testErrorCodesParsed() async {
        do { _ = try await client { _ in (400, #"{"error":["first_name__required"]}"#) }.customers.create(.init(firstName: "", lastName: "x", phoneCode: "SA", phoneNumber: "5")); XCTFail() }
        catch let e as AmenApiError { XCTAssertEqual(e.status, 400); XCTAssertTrue(e.has("first_name__required")); XCTAssertFalse(e.retryable) } catch { XCTFail("\(error)") }
    }
    func testLifecycleGuardBlocksLocally() async {
        let counter = Counter()
        do { _ = try await client { _ in counter.inc(); return (200, #"{"number":"DL-1","status":"draft"}"#) }.deals.actions.approve("DL-1"); XCTFail() }
        catch is AmenLifecycleError { XCTAssertEqual(counter.n, 1) } catch { XCTFail("\(error)") }
    }
    func testCsrfAndOriginOnMutatingRequests() async throws {
        let wh = try await client { r in
            XCTAssertEqual(r.value(forHTTPHeaderField: "Origin"), "https://sandbox-api.amnn.sa")
            let csrf = r.value(forHTTPHeaderField: "X-CSRFToken") ?? ""
            XCTAssertEqual(csrf.count, 32)
            XCTAssertEqual(r.value(forHTTPHeaderField: "Cookie"), "csrftoken=\(csrf)")
            return (201, #"{"id":"w","url":"u","secret_key":"s"}"#)
        }.webhooks.create(url: "https://example.com/hook")
        XCTAssertEqual(wh.secretKey, "s")
    }
    func testSnakeCaseEncodingAndIsoTimestamp() throws {
        let data = try snakeEncoder.encode(CreateDeal(offerType: .product, offerTitle: "t", offerPrice: "1.00"))
        XCTAssertTrue(String(decoding: data, as: UTF8.self).contains("\"offer_type\""))
        let deal = try snakeDecoder.decode(Deal.self, from: Data(#"{"number":"DL-1","status":"draft","created_at":"2026-08-26T18:04:42.825Z"}"#.utf8))
        XCTAssertEqual(Calendar(identifier: .gregorian).component(.year, from: deal.created!), 2026)
    }
}
final class Counter: @unchecked Sendable { var n = 0; func inc() { n += 1 } }
