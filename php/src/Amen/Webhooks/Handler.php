<?php
declare(strict_types=1);
namespace Amen\Webhooks;
/** Framework-agnostic: call handle($headers, $rawBody). Verifies raw body first, de-duplicates by id, dispatches. */
final class Handler
{
    /** @param callable(array $event): void $onEvent  event = ['id','type','data','raw'] */
    public function __construct(private string $secret, private $onEvent, private array $seen = []) {}

    /** @return array{0:int,1:array} [httpStatus, responseBody] */
    public function handle(array $headers, string $rawBody): array
    {
        $sig = null; foreach ($headers as $k => $v) if (strcasecmp($k, Signature::HEADER) === 0) $sig = is_array($v) ? $v[0] : $v;
        if (!Signature::verify($this->secret, $rawBody, $sig)) return [401, ['error' => 'invalid signature']];
        $data = json_decode($rawBody, true);
        if (!is_array($data)) return [400, ['error' => 'invalid json']];
        $id = (string)($data['id'] ?? $data['event_id'] ?? '');
        if ($id !== '' && isset($this->seen[$id])) return [200, ['ok' => true, 'duplicate' => true]];
        if ($id !== '') $this->seen[$id] = true;
        ($this->onEvent)(['id' => $id, 'type' => $data['event'] ?? $data['type'] ?? null, 'data' => $data, 'raw' => $rawBody]);
        return [200, ['ok' => true]];
    }
}
