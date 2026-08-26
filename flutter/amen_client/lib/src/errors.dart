/// Any non-2xx response. [codes] holds the API's error codes, e.g. `price__required`.
class AmenApiError implements Exception {
  final int status;
  final List<String> codes;
  final String method, path;
  final Object? body;
  AmenApiError(this.status, this.codes, this.method, this.path, [this.body]);
  bool has(String code) => codes.contains(code);
  bool get retryable => status == 429 || status >= 500;
  @override
  String toString() => '$status $method $path: ${codes.isEmpty ? body : codes.join(', ')}';
}

/// Thrown locally, before any HTTP call, when an action is not valid for the deal's status.
class AmenLifecycleError implements Exception {
  final String message;
  AmenLifecycleError(this.message);
  @override
  String toString() => message;
}
