import Foundation

/// Environment-based configuration. On iOS pass values explicitly — never ship the API key in the app.
public struct Config: Sendable {
    public static let baseURLs = ["sandbox": "https://sandbox-api.amnn.sa", "live": "https://api.amnn.sa"]
    public static let apiPrefix = "/api/v1"

    public var env: String
    public var apiKey: String
    public var baseURL: URL
    public var timeout: TimeInterval
    public var webhookSecret: String?
    public var maxRetries: Int

    public init(env: String = "sandbox", apiKey: String, baseURL: URL? = nil, timeout: TimeInterval = 20, webhookSecret: String? = nil, maxRetries: Int = 3) {
        self.env = env; self.apiKey = apiKey; self.baseURL = baseURL ?? URL(string: Config.baseURLs[env]!)!
        self.timeout = timeout; self.webhookSecret = webhookSecret; self.maxRetries = maxRetries
    }

    /// Reads AMN_* from the process environment, falling back to a `.env` in cwd or up to 3 parents.
    public static func fromEnvironment() throws -> Config {
        let file = loadDotenv()
        func get(_ k: String) -> String? { ProcessInfo.processInfo.environment[k] ?? file[k] }
        let env = (get("AMN_ENV") ?? "sandbox").lowercased()
        guard let defaultBase = baseURLs[env] else { throw ConfigError.invalid("AMN_ENV must be 'sandbox' or 'live', got '\(env)'") }
        guard let key = get("AMN_API_KEY"), !key.isEmpty else { throw ConfigError.invalid("AMN_API_KEY is not set (see .env.example)") }
        let ms = Double(get("AMN_TIMEOUT_MS") ?? "") ?? 20000
        return Config(env: env, apiKey: key, baseURL: URL(string: get("AMN_BASE_URL") ?? defaultBase), timeout: ms / 1000,
                      webhookSecret: get("AMN_WEBHOOK_SECRET").flatMap { $0.isEmpty ? nil : $0 })
    }

    static func loadDotenv() -> [String: String] {
        var dir = URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
        for _ in 0..<4 {
            let f = dir.appendingPathComponent(".env")
            if let text = try? String(contentsOf: f, encoding: .utf8) {  // nearest .env only
                var out: [String: String] = [:]
                for line in text.split(separator: "\n") {
                    let l = line.trimmingCharacters(in: .whitespaces)
                    guard !l.isEmpty, !l.hasPrefix("#"), let eq = l.firstIndex(of: "=") else { continue }
                    let v = l[l.index(after: eq)...].replacingOccurrences(of: #"\s+#.*$"#, with: "", options: .regularExpression).trimmingCharacters(in: .whitespaces)
                    if !v.isEmpty { out[String(l[..<eq]).trimmingCharacters(in: .whitespaces)] = v }
                }
                return out   // do not leak a parent project's config
            }
            dir = dir.deletingLastPathComponent()
        }
        return [:]
    }

    public enum ConfigError: Error, CustomStringConvertible { case invalid(String); public var description: String { if case .invalid(let m) = self { return m }; return "" } }
}
