<?php
declare(strict_types=1);
namespace Amen\Resources;
use Amen\AmenClient;
final class Webhooks
{
    public function __construct(private AmenClient $c) {}
    public function list(): array { return $this->c->request('GET', '/web-hooks/'); }
    /** `secret_key` in the response is shown ONLY now — store it in a secret manager immediately. */
    public function create(string $url): array { return $this->c->request('POST', '/web-hooks/', ['json' => ['url' => $url]]); }
    public function delete(string $id): void { $this->c->request('DELETE', "/web-hooks/$id"); }
}
