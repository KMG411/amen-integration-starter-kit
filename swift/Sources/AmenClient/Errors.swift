import Foundation

/// Any non-2xx response. `codes` holds the API's error codes, e.g. "price__required".
public struct AmenApiError: Error, CustomStringConvertible {
    public let status: Int
    public let codes: [String]
    public let method: String
    public let path: String
    public let body: String
    public func has(_ code: String) -> Bool { codes.contains(code) }
    public var retryable: Bool { status == 429 || status >= 500 }
    public var description: String { "\(status) \(method) \(path): \(codes.isEmpty ? body : codes.joined(separator: ", "))" }
}

/// Thrown locally, before any HTTP call, when an action is not valid for the deal's status.
public struct AmenLifecycleError: Error, CustomStringConvertible { public let description: String }
