// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "AmenClient",
    platforms: [.macOS(.v13), .iOS(.v16)],
    products: [.library(name: "AmenClient", targets: ["AmenClient"]), .executable(name: "golden-path", targets: ["GoldenPath"])],
    targets: [
        .target(name: "AmenClient"),
        .executableTarget(name: "GoldenPath", dependencies: ["AmenClient"]),
        .testTarget(name: "AmenClientTests", dependencies: ["AmenClient"]),
    ]
)
