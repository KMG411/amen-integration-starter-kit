/// Thin typed views over API JSON. Unknown fields stay available in [raw].
/// Money is a String ("100.00"); API timestamps are epoch **milliseconds**.
/// Parse an API timestamp: ISO-8601 string (e.g. "2026-08-26T18:04:42.825Z") or epoch-ms int.
DateTime? toDate(Object? v) => v is int ? DateTime.fromMillisecondsSinceEpoch(v, isUtc: true) : (v is String && v.isNotEmpty ? DateTime.tryParse(v) : null);

class Customer {
  final Map<String, dynamic> raw;
  Customer(this.raw);
  String get number => raw['number'] as String;
  String get firstName => raw['first_name'] as String? ?? '';
  String get lastName => raw['last_name'] as String? ?? '';
  DateTime? get createdAt => toDate(raw['created_at']);
}

class Deal {
  final Map<String, dynamic> raw;
  Deal(this.raw);
  String get number => raw['number'] as String;
  String get status => raw['status'] as String;
  String? get price => raw['price'] as String?;
  DateTime? get createdAt => toDate(raw['created_at']);
}

class Checkout {
  final Map<String, dynamic> raw;
  Checkout(this.raw);
  String? get provider => raw['provider'] as String?;
  String? get hyperpayCheckoutId => (raw['hyperpay'] as Map?)?['checkout_id'] as String?;
  String? get amount => raw['amount'] as String?;
}

class Withdrawal {
  final Map<String, dynamic> raw;
  Withdrawal(this.raw);
  String get number => raw['number'] as String;
  String get status => raw['status'] as String;
  String? get amount => raw['amount'] as String?;
}

class Webhook {
  final Map<String, dynamic> raw;
  Webhook(this.raw);
  String get id => raw['id'] as String;
  String get url => raw['url'] as String;
  /// Returned ONLY at creation — store it in a secret manager immediately.
  String? get secretKey => raw['secret_key'] as String?;
}

class Page<T> {
  final List<T> items;
  final int page, pages, total;
  Page(this.items, {this.page = 0, this.pages = 1, this.total = 0});
  static Page<T> from<T>(Map<String, dynamic>? d, String key, T Function(Map<String, dynamic>) map) {
    final p = d?['page'] is Map ? d!['page'] as Map : (d ?? {});
    final list = (d?[key] ?? d?['results'] ?? d?['items'] ?? []) as List;
    return Page(list.map((e) => map(e as Map<String, dynamic>)).toList(), page: p['page'] as int? ?? 0, pages: p['pages'] as int? ?? 1, total: p['total'] as int? ?? 0);
  }
}
