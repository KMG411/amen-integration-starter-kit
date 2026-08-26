/** Express receiver. Uses express.raw() so the signature is verified over the exact bytes Amen sent. Listens on :8080/webhook */
import express from "express";
import { loadConfig, WebhookHandler } from "../src/amen/index.js";
const { webhookSecret } = loadConfig({ apiKey: process.env.AMN_API_KEY ?? "unused-for-receiver" });
if (!webhookSecret) throw new Error("AMN_WEBHOOK_SECRET is required");
const handler = new WebhookHandler(webhookSecret, (e) => console.log(`📩 ${e.type ?? "?"} ${e.id}:`, JSON.stringify(e.data).slice(0, 300)));
const app = express();
app.post("/webhook", express.raw({ type: "*/*" }), async (req, res) => { const { status, body } = await handler.handle(req.headers, req.body); res.status(status).json(body); });
app.listen(8080, () => console.log("listening on http://0.0.0.0:8080/webhook"));
