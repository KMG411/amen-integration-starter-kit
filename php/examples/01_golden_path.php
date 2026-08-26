<?php
/** Golden path (scenario/golden-path.yml). `php examples/01_golden_path.php [DL-000123]` — pass a deal number to resume from 'paid'. */
declare(strict_types=1);
require __DIR__ . '/_common.php';
use Amen\{AmenClient, AmenApiError};

$amen = new AmenClient();
echo "environment: {$amen->config->env} ({$amen->config->baseUrl})\n\n";
$continueFromPaid = function (string $n) use ($amen): void {
    step('execution-start', $amen->deals->actions->executionStart($n));
    step('execution-complete', $amen->deals->actions->executionComplete($n));
    step('complete', $amen->deals->actions->complete($n));
    step('transfer-seller-amount (payout)', $amen->deals->actions->transferSellerAmount($n));
    echo "\n🎉 deal $n finished: {$amen->deals->get($n)['status']}\n";
};
if (isset($argv[1])) { $continueFromPaid($argv[1]); exit(0); }

$buyer = $amen->customers->create(['first_name' => 'Buyer', 'last_name' => 'Kit', 'phone_code' => 'SA', 'phone_number' => uniquePhone('57')]);
$seller = $amen->customers->create(['first_name' => 'Seller', 'last_name' => 'Kit', 'phone_code' => 'SA', 'phone_number' => uniquePhone('58')]);
step("customers {$buyer['number']} (buyer), {$seller['number']} (seller)");
$category = $amen->lookups->categories()[0]['id']; $city = $amen->lookups->cities()[0]['id'];
$deal = $amen->deals->create(['offer_type' => 'product', 'offer_category' => $category, 'offer_title' => 'Starter Kit golden path',
    'offer_description' => 'Reference deal created by the Amen integration starter kit', 'offer_price' => '100.00', 'offer_delivery_fee' => '0.00']);
$n = $deal['number']; step("deal $n created", $deal);
step('parties', $amen->deals->setParties($n, [$buyer['number']], [$seller['number']]));
step('delivery address', $amen->deals->setDeliveryAddress($n, ['city' => $city, 'district' => 'Al Olaya', 'street' => 'King Fahd Rd', 'building_number' => '1234', 'unit_number' => '1', 'zip_code' => '12211']));
step('submit', $amen->deals->actions->submit($n));
step('approve', $amen->deals->actions->approve($n));
echo '  allowed payment methods: ' . json_encode($amen->deals->allowedPaymentMethods($n)) . "\n";
try { step('pay with wallet', $amen->deals->actions->payWithWallet($n)); }
catch (AmenApiError $e) {
    $checkout = $amen->deals->actions->payOnline($n, 'mada');
    echo "\n⏸  NEEDS_TOP_UP — wallet payment not possible (" . (implode(', ', $e->codes) ?: $e->status) . ").\n   HyperPay checkout created: " . json_encode($checkout)
       . "\n   Top up the sandbox wallet (GET /api/v1/account → wallet.top_up_account) or complete the checkout, then:\n       php examples/01_golden_path.php $n\n";
    exit(0);
}
$continueFromPaid($n);
