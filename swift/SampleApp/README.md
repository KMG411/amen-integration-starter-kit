# iOS sample — back-end proxy pattern

An iOS app must **never** contain `AMN_API_KEY`. The pattern:

```
iPhone app ──(your session token, Keychain)──▶ your backend ──(X-API-Token)──▶ api.amnn.sa
```

* Backend: use `AmenClient` from this package on the server (Vapor/Hummingbird) or any server stack in this kit.
* App: call your backend with `URLSession`; store the app's session token with `Security.framework` (Keychain), not `UserDefaults`.
* Online payments: the backend calls `make-payment-online` and returns the HyperPay `checkout_id`; the app presents HyperPay's iOS SDK / web checkout with it.
* Certificate pinning (optional): implement `URLSessionDelegate.urlSession(_:didReceive:completionHandler:)` for your backend host.

```swift
// App side (no Amen token anywhere)
var req = URLRequest(url: backend.appendingPathComponent("/api/deals"))
req.httpMethod = "POST"; req.setValue("Bearer \(keychain.sessionToken)", forHTTPHeaderField: "Authorization")
```
