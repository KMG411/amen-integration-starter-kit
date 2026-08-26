#!/usr/bin/env python3
"""Minimal webhook receiver (stdlib only). Verifies the raw body, de-duplicates, logs the event.

    AMN_WEBHOOK_SECRET=... python examples/04_webhook_receiver.py   # listens on :8080/webhook
Expose it publicly (e.g. a tunnel) and register the URL with amen.webhooks.create(url).
"""
import json, os
from http.server import BaseHTTPRequestHandler, HTTPServer
from amen.config import Config
from amen.webhooks import WebhookHandler

Config.from_env  # loads .env
from dotenv import load_dotenv; load_dotenv(); load_dotenv("../.env")
secret = os.environ["AMN_WEBHOOK_SECRET"]
handler = WebhookHandler(secret, on_event=lambda e: print(f"📩 {e.type or '?'} {e.id}: {json.dumps(e.data)[:300]}"))


class H(BaseHTTPRequestHandler):
    def do_POST(self):
        raw = self.rfile.read(int(self.headers.get("Content-Length", 0)))
        code, body = handler.handle(dict(self.headers), raw)
        self.send_response(code); self.send_header("Content-Type", "application/json"); self.end_headers()
        self.wfile.write(json.dumps(body).encode())


print("listening on http://0.0.0.0:8080/webhook")
HTTPServer(("0.0.0.0", 8080), H).serve_forever()
