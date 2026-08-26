<?php
declare(strict_types=1);
require __DIR__ . '/_common.php';
use Amen\AmenClient;
$amen = new AmenClient();
echo 'wallet: ' . json_encode($amen->account->get()['wallet'] ?? null) . "\n";
$banks = $amen->account->bankAccounts();
if (!$banks) { echo "No linked bank account — link one with \$amen->account->linkBankAccount(\$iban).\n"; exit(0); }
$w = $amen->withdrawals->create($banks[0]['id'], '10.00');
echo "withdrawal {$w['number']}: {$w['status']}\n";
foreach ($amen->withdrawals->list()['items'] as $item) echo " - {$item['number']} {$item['status']} {$item['amount']}\n";
