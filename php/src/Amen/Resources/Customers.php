<?php
declare(strict_types=1);
namespace Amen\Resources;
use Amen\AmenClient;
final class Customers
{
    public function __construct(private AmenClient $c) {}
    /** @param array{first_name:string,last_name:string,phone_code:string,phone_number:string} $body */
    public function create(array $body): array { return $this->c->request('POST', '/customers/', ['json' => $body]); }
    public function get(string $customerNumber): array { return $this->c->request('GET', "/customers/$customerNumber"); }
    public function list(array $params = []): array { return Page::from($this->c->request('GET', '/customers/', ['params' => $params]), 'customers'); }
    /** Iterate every page — never process only the first page by accident. */
    public function iterAll(array $filters = []): \Generator
    { for ($page = 0; ; $page++) { $p = $this->list($filters + ['page' => $page]); yield from $p['items']; if ($page + 1 >= $p['pages'] || !$p['items']) return; } }
}
