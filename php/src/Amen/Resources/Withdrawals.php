<?php
declare(strict_types=1);
namespace Amen\Resources;
use Amen\AmenClient;
final class Withdrawals
{
    public function __construct(private AmenClient $c) {}
    public function create(string $bankAccountId, string $amount): array { return $this->c->request('POST', '/withdrawals/', ['json' => ['bank_account_id' => $bankAccountId, 'amount' => $amount]]); }
    public function get(string $n): array { return $this->c->request('GET', "/withdrawals/$n"); }
    public function list(array $params = []): array { return Page::from($this->c->request('GET', '/withdrawals/', ['params' => $params]), 'withdrawals'); }
}
