<?php
declare(strict_types=1);
namespace Amen\Resources;
use Amen\AmenClient;
final class Lookups
{
    public function __construct(private AmenClient $c) {}
    public function countryCodes(): array { return $this->c->request('GET', '/allowed-country-codes/'); }
    public function cities(): array { return $this->c->request('GET', '/cities'); }
    public function categories(): array { return $this->c->request('GET', '/categories/'); }
    public function disputeReasons(): array { return $this->c->request('GET', '/dispute-reasons/'); }
    public function disputeResolutionReasons(): array { return $this->c->request('GET', '/dispute-resolution-reasons/'); }
    public function cancelReasons(?string $partyType = null): array { return $this->c->request('GET', '/cancel-reasons/', ['params' => ['party_type' => $partyType]]); }
}
