// Back-end proxy pattern: the app NEVER holds the Amen API token.
// It calls YOUR server (BACKEND_URL), which uses amen_client server-side.
// The app's own session token is kept in flutter_secure_storage (Keychain / Keystore).
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;

const backendUrl = String.fromEnvironment('BACKEND_URL', defaultValue: 'https://your-backend.example.com');
final storage = const FlutterSecureStorage();

/// Calls your back end, which proxies to Amen. Never talks to amnn.sa directly.
class BackendApi {
  Future<Map<String, String>> _headers() async => {'Authorization': 'Bearer ${await storage.read(key: 'session_token') ?? ''}', 'Accept': 'application/json'};
  Future<Map<String, dynamic>> createDeal(String title, String price) async {
    final r = await http.post(Uri.parse('$backendUrl/api/deals'), headers: {...await _headers(), 'Content-Type': 'application/json'}, body: jsonEncode({'title': title, 'price': price}));
    if (r.statusCode >= 400) throw Exception('backend ${r.statusCode}: ${r.body}');
    return jsonDecode(r.body) as Map<String, dynamic>;
  }
  Future<Map<String, dynamic>> dealStatus(String number) async {
    final r = await http.get(Uri.parse('$backendUrl/api/deals/$number'), headers: await _headers());
    return jsonDecode(r.body) as Map<String, dynamic>;
  }
}

void main() => runApp(const MaterialApp(home: DealPage()));

class DealPage extends StatefulWidget {
  const DealPage({super.key});
  @override
  State<DealPage> createState() => _DealPageState();
}

class _DealPageState extends State<DealPage> {
  final api = BackendApi();
  String status = 'no deal yet';
  Future<void> _create() async {
    try { final d = await api.createDeal('iPhone 15', '3500.00'); setState(() => status = 'deal ${d['number']}: ${d['status']}'); }
    catch (e) { setState(() => status = 'error: $e'); }
  }
  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: const Text('Amen escrow sample')),
        body: Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
          Text(status), const SizedBox(height: 16),
          FilledButton(onPressed: _create, child: const Text('Create escrow deal')),
        ])),
      );
}
