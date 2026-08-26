<?php
/** php examples/02_cancel_and_dispute.php cancel | dispute DL-000123 (deal must be 'completed') */
declare(strict_types=1);
require __DIR__ . '/_common.php';
use Amen\AmenClient;
$amen = new AmenClient(); $mode = $argv[1] ?? 'cancel';
if ($mode === 'cancel') {
    $buyer = $amen->customers->create(['first_name' => 'Buyer', 'last_name' => 'Kit', 'phone_code' => 'SA', 'phone_number' => uniquePhone('57')]);
    $seller = $amen->customers->create(['first_name' => 'Seller', 'last_name' => 'Kit', 'phone_code' => 'SA', 'phone_number' => uniquePhone('58')]);
    $deal = $amen->deals->create(['offer_type' => 'service', 'offer_title' => 'Cancel scenario', 'offer_price' => '50.00', 'offer_category' => $amen->lookups->categories()[0]['id'], 'deal_subject_details' => 'Kit test']);
    $amen->deals->setParties($deal['number'], [$buyer['number']], [$seller['number']]);
    $reason = $amen->lookups->cancelReasons('buyer')[0]['id'];
    step('cancel', $amen->deals->actions->cancel($deal['number'], 'buyer', $reason, 'Changed my mind'));
} else {
    $n = $argv[2];
    step('dispute', $amen->deals->actions->dispute($n, $amen->lookups->disputeReasons()[0]['id'], 'Item not as described'));
    step('dispute-approve', $amen->deals->actions->disputeApprove($n, $amen->lookups->disputeResolutionReasons()[0]['id'], 'Refund the buyer'));
}
