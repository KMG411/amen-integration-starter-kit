<?php
/** Minimal receiver for the PHP built-in server: `php -S 0.0.0.0:8080 examples/04_webhook_receiver.php`
 *  Reads the RAW body (php://input) and verifies it before parsing. In Laravel use $request->getContent(). */
declare(strict_types=1);
require __DIR__ . '/_common.php';
use Amen\Config; use Amen\Webhooks\Handler;
$secret = Config::fromEnv(['apiKey' => getenv('AMN_API_KEY') ?: 'unused-for-receiver'])->webhookSecret ?? throw new RuntimeException('AMN_WEBHOOK_SECRET is required');
$handler = new Handler($secret, fn(array $e) => error_log("📩 {$e['type']} {$e['id']}: " . substr(json_encode($e['data']), 0, 300)));
[$status, $body] = $_SERVER['REQUEST_METHOD'] === 'POST' ? $handler->handle(getallheaders(), file_get_contents('php://input')) : [405, []];
http_response_code($status); header('Content-Type: application/json'); echo json_encode($body);
