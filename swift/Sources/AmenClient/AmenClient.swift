import Foundation
#if canImport(FoundationNetworking)
import FoundationNetworking
#endif

/// Transport abstraction so tests can inject responses. Default is URLSession.
public protocol HTTPTransport: Sendable { func send(_ request: URLRequest) async throws -> (Data, HTTPURLResponse) }
public struct URLSessionTransport: HTTPTransport {
    private let session: URLSession
    public init() {
        let cfg = URLSessionConfiguration.ephemeral   // do not let URLSession manage cookies; we set csrftoken ourselves
        cfg.httpShouldSetCookies = false
        cfg.httpCookieStorage = nil
        session = URLSession(configuration: cfg)
    }
    public func send(_ request: URLRequest) async throws -> (Data, HTTPURLResponse) {
        let (data, resp) = try await session.data(for: request)
        return (data, resp as! HTTPURLResponse)
    }
}

public enum Method: String { case GET, POST, PUT, DELETE }
public struct MultipartFile { public let field: String; public let filename: String; public let data: Data; public let mimeType: String
    public init(field: String, filename: String, data: Data, mimeType: String = "application/octet-stream") { self.field = field; self.filename = filename; self.data = data; self.mimeType = mimeType } }

/// AmenClient — the one place that knows about auth headers, base URL, timeouts and retries.
public final class AmenClient: @unchecked Sendable {
    public let config: Config
    let transport: HTTPTransport
    private let csrf = UUID().uuidString.replacingOccurrences(of: "-", with: "").lowercased()   // 32 hex chars — Django CSRF token format
    public private(set) lazy var lookups = Lookups(self)
    public private(set) lazy var account = AccountResource(self)
    public private(set) lazy var customers = Customers(self)
    public private(set) lazy var deals = Deals(self)
    public private(set) lazy var withdrawals = Withdrawals(self)
    public private(set) lazy var webhooks = Webhooks(self)

    public init(_ config: Config, transport: HTTPTransport = URLSessionTransport()) { self.config = config; self.transport = transport }
    public convenience init() throws { self.init(try Config.fromEnvironment()) }

    /// Core request. `json` is encoded with snake_case keys; `form` sends multipart/form-data.
    public func request<T: Decodable>(_ method: Method, _ path: String, json: (some Encodable)? = Optional<Empty>.none,
                                      params: [String: String?] = [:], form: [String: String]? = nil, files: [MultipartFile] = []) async throws -> T {
        var comps = URLComponents(url: config.baseURL.appendingPathComponent(Config.apiPrefix + path), resolvingAgainstBaseURL: false)!
        let q = params.compactMap { k, v in v.map { URLQueryItem(name: k, value: $0) } }
        if !q.isEmpty { comps.queryItems = q }
        var req = URLRequest(url: comps.url!, timeoutInterval: config.timeout)
        req.httpMethod = method.rawValue
        req.setValue(config.apiKey, forHTTPHeaderField: "X-API-Token")
        req.setValue("application/json", forHTTPHeaderField: "Accept")
        req.setValue("amen-starter-kit-swift/0.1", forHTTPHeaderField: "User-Agent")
        req.setValue("en", forHTTPHeaderField: "Accept-Language")
        req.setValue("csrftoken=\(csrf)", forHTTPHeaderField: "Cookie")
        if method != .GET {  // Django CSRF double-submit: token in both the X-CSRFToken header and the csrftoken cookie
            req.setValue(csrf, forHTTPHeaderField: "X-CSRFToken")
            req.setValue(config.baseURL.absoluteString, forHTTPHeaderField: "Origin")
            req.setValue(config.baseURL.absoluteString, forHTTPHeaderField: "Referer")
        }
        if let form { let b = "----AmenKit\(UUID().uuidString)"; req.setValue("multipart/form-data; boundary=\(b)", forHTTPHeaderField: "Content-Type"); req.httpBody = Self.multipart(form, files, boundary: b) }
        else if let json { req.setValue("application/json", forHTTPHeaderField: "Content-Type"); req.httpBody = try snakeEncoder.encode(json) }

        var attempt = 0
        while true {
            attempt += 1
            let data: Data, resp: HTTPURLResponse
            do { (data, resp) = try await transport.send(req) }
            catch { if attempt > config.maxRetries { throw error }; try await Task.sleep(nanoseconds: Self.backoff(attempt)); continue }
            if resp.statusCode < 400 {
                if T.self == Empty.self || data.isEmpty { return try snakeDecoder.decode(T.self, from: data.isEmpty ? Data("{}".utf8) : data) }
                return try snakeDecoder.decode(T.self, from: data)
            }
            let err = Self.toError(resp.statusCode, data, method, Config.apiPrefix + path)
            if err.retryable && attempt <= config.maxRetries { try await Task.sleep(nanoseconds: Self.backoff(attempt, resp.value(forHTTPHeaderField: "Retry-After"))); continue }
            throw err
        }
    }

    static func toError(_ status: Int, _ data: Data, _ method: Method, _ path: String) -> AmenApiError {
        let body = String(decoding: data, as: UTF8.self)
        var codes: [String] = []
        if let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any] { if let a = obj["error"] as? [Any] { codes = a.map { "\($0)" } } else if let s = obj["error"] as? String { codes = [s] } }
        return AmenApiError(status: status, codes: codes, method: method.rawValue, path: path, body: body)
    }
    static func backoff(_ attempt: Int, _ retryAfter: String? = nil) -> UInt64 {
        if let ra = retryAfter.flatMap(Double.init) { return UInt64(ra * 1e9) }
        return UInt64((min(pow(2.0, Double(attempt)), 20) + Double.random(in: 0..<1)) * 1e9)
    }
    static func multipart(_ fields: [String: String], _ files: [MultipartFile], boundary: String) -> Data {
        var d = Data()
        for (k, v) in fields { d.append("--\(boundary)\r\nContent-Disposition: form-data; name=\"\(k)\"\r\n\r\n\(v)\r\n".data(using: .utf8)!) }
        for f in files { d.append("--\(boundary)\r\nContent-Disposition: form-data; name=\"\(f.field)\"; filename=\"\(f.filename)\"\r\nContent-Type: \(f.mimeType)\r\n\r\n".data(using: .utf8)!); d.append(f.data); d.append("\r\n".data(using: .utf8)!) }
        d.append("--\(boundary)--\r\n".data(using: .utf8)!)
        return d
    }
}
