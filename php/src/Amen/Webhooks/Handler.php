<?php
declare(strict_types=1);
namespace Amen\Webhooks;
/**
 * Framework-agnostic webhook handler: call handle($headers, $rawBody).
 *
 * Verifies the raw body against the X-Webhook-Signature / X-Webhook-Timestamp headers,
 * de-duplicates on the top-level envelope `timestamp` (Amen sends no event id), then dispatches.
 * Envelope shape: { event, timestamp, payload }.
 */
final class Handler
{
    /** @param callable(array $event): void $onEvent  event = ['id','type','data','raw'] */
    public function __construct(private string $secret, private $onEvent, private array $seen = []) {}

    /** @return array{0:int,1:array} [httpStatus, responseBody] */
    public function handle(array $headers, string $rawBody): array
    {
        $sig = $this->header($headers, Signature::HEADER_SIGNATURE);
        $ts = $this->header($headers, Signature::HEADER_TIMESTAMP) ?? '';
        if (!Signature::verify($this->secret, $ts, $rawBody, $sig)) return [401, ['error' => 'invalid signature']];
        $data = json_decode($rawBody, true);
        if (!is_array($data)) return [400, ['error' => 'invalid json']];
        $type = $data['event'] ?? $data['type'] ?? null;
        // No event id in Amen deliveries: de-dupe on the envelope timestamp (fall back to the header).
        $id = (string)($data['timestamp'] ?? $ts);
        if ($id !== '' && isset($this->seen[$id])) return [200, ['ok' => true, 'duplicate' => true]];
        if ($id !== '') $this->seen[$id] = true;
        ($this->onEvent)(['id' => $id, 'type' => $type, 'data' => $data, 'raw' => $rawBody]);
        return [200, ['ok' => true]];
    }

    /** Case-insensitive header lookup; returns the first value for array-valued headers. */
    private function header(array $headers, string $name): ?string
    {
        foreach ($headers as $k => $v) if (strcasecmp($k, $name) === 0) return is_array($v) ? ($v[0] ?? null) : $v;
        return null;
    }
}
