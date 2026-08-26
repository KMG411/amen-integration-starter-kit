/** Minimal receiver with node:http (no framework). Verifies raw body, de-duplicates, logs. Listens on :8080/webhook. */
import { createServer } from "node:http";
import { loadConfig, WebhookHandler } from "../src/amen/index.js";
const { webhookSecret } = loadConfig({ apiKey: process.env.AMN_API_KEY ?? "unused-for-receiver" });
if (!webhookSecret) throw new Error("AMN_WEBHOOK_SECRET is required");
const handler = new WebhookHandler(webhookSecret, (e) => console.log(`📩 ${e.type ?? "?"} ${e.id}:`, JSON.stringify(e.data).slice(0, 300)));
createServer(async (req, res) => {
  const chunks: Buffer[] = []; for await (const c of req) chunks.push(c as Buffer);
  const { status, body } = req.method === "POST" ? await handler.handle(req.headers, Buffer.concat(chunks)) : { status: 405, body: {} };
  res.writeHead(status, { "Content-Type": "application/json" }); res.end(JSON.stringify(body));
}).listen(8080, () => console.log("listening on http://0.0.0.0:8080/webhook"));
