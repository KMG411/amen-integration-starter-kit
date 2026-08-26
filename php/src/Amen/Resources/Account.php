<?php
declare(strict_types=1);
namespace Amen\Resources;
use Amen\AmenClient;
final class Account
{
    public function __construct(private AmenClient $c) {}
    public function get(): array { return $this->c->request('GET', '/account'); }
    public function bankAccounts(): array { return $this->c->request('GET', '/account/bank-accounts/'); }
    public function linkBankAccount(string $iban, ?string $proofDocumentPath = null): array
    { $form = ['iban' => $iban]; if ($proofDocumentPath) $form['proof_document'] = new \CURLFile($proofDocumentPath); return $this->c->request('POST', '/account/bank-accounts/', ['form' => $form]); }
    public function deleteBankAccount(string $id): void { $this->c->request('DELETE', "/account/bank-accounts/$id"); }
}
