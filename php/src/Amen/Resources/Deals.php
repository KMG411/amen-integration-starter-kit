<?php
declare(strict_types=1);
namespace Amen\Resources;
use Amen\{AmenClient, AmenLifecycleError};

/** POST /deals/{n}/action/* — every method returns the updated deal (or a checkout for online payment). */
final class DealActions
{
    /** Which statuses each action may be called from (docs/02-deal-lifecycle.md). */
    public const ALLOWED_FROM = [
        'submit' => ['draft'], 'approve' => ['requested'],
        'make-payment-wallet' => ['payment_pending'], 'make-payment-online' => ['payment_pending'],
        'execution-start' => ['paid'], 'execution-complete' => ['executing'], 'complete' => ['executed'],
        'transfer-seller-amount' => ['completed'], 'dispute' => ['completed'],
        'dispute-approve' => ['disputed'], 'dispute-decline' => ['disputed'],
        'cancel' => ['draft', 'requested', 'payment_pending', 'paid', 'executing'],
    ];
    public function __construct(private AmenClient $c, private Deals $deals) {}

    private function act(string $n, string $action, array $opts = [], bool $check = true): array
    {
        if ($check) {
            $status = $this->deals->get($n)['status'] ?? null;
            if (!in_array($status, self::ALLOWED_FROM[$action], true))
                throw new AmenLifecycleError("action '$action' is not allowed from status '$status' (allowed: " . implode(', ', self::ALLOWED_FROM[$action]) . ')');
        }
        return $this->c->request('POST', "/deals/$n/action/$action", $opts) ?? [];
    }
    public function submit(string $n, bool $check = true): array { return $this->act($n, 'submit', [], $check); }
    public function approve(string $n, ?string $price = null, bool $check = true): array { return $this->act($n, 'approve', ['json' => $price ? ['price' => $price] : new \stdClass()], $check); }
    public function payWithWallet(string $n, bool $check = true): array { return $this->act($n, 'make-payment-wallet', [], $check); }
    public function payOnline(string $n, string $paymentMethod = 'mada', bool $check = true): array { return $this->act($n, 'make-payment-online', ['json' => ['payment_method' => $paymentMethod]], $check); }
    public function executionStart(string $n, bool $check = true): array { return $this->act($n, 'execution-start', [], $check); }
    public function executionComplete(string $n, bool $check = true): array { return $this->act($n, 'execution-complete', [], $check); }
    public function complete(string $n, bool $check = true): array { return $this->act($n, 'complete', [], $check); }
    public function transferSellerAmount(string $n, bool $check = true): array { return $this->act($n, 'transfer-seller-amount', [], $check); }
    public function cancel(string $n, string $dealParty, int $reason, string $comment, bool $check = true): array
    { return $this->act($n, 'cancel', ['json' => ['deal_party' => $dealParty, 'reason' => $reason, 'comment' => $comment]], $check); }
    /** @param string[] $attachmentPaths */
    public function dispute(string $n, int $reason, string $comment, array $attachmentPaths = [], bool $check = true): array
    { $form = ['reason' => $reason, 'comment' => $comment]; foreach ($attachmentPaths as $i => $p) $form['attachment_' . ($i + 1)] = new \CURLFile($p); return $this->act($n, 'dispute', ['form' => $form], $check); }
    public function disputeApprove(string $n, int $reason, string $comment, bool $check = true): array { return $this->act($n, 'dispute-approve', ['form' => ['reason' => $reason, 'comment' => $comment]], $check); }
    public function disputeDecline(string $n, int $reason, string $comment, bool $check = true): array { return $this->act($n, 'dispute-decline', ['form' => ['reason' => $reason, 'comment' => $comment]], $check); }
}

final class Deals
{
    public readonly DealActions $actions;
    public function __construct(private AmenClient $c) { $this->actions = new DealActions($c, $this); }
    public function create(array $body): array { return $this->c->request('POST', '/deals/', ['json' => $body]); }
    public function get(string $n): array { return $this->c->request('GET', "/deals/$n"); }
    public function update(string $n, array $body): array { return $this->c->request('PUT', "/deals/$n", ['json' => $body]); }
    public function delete(string $n): void { $this->c->request('DELETE', "/deals/$n"); }
    public function list(array $params = []): array { return Page::from($this->c->request('GET', '/deals/', ['params' => $params]), 'deals'); }
    public function iterAll(array $filters = []): \Generator
    { for ($page = 0; ; $page++) { $p = $this->list($filters + ['page' => $page]); yield from $p['items']; if ($page + 1 >= $p['pages'] || !$p['items']) return; } }
    public function setParties(string $n, array $buyers, array $sellers): array { return $this->c->request('POST', "/deals/$n/parties/", ['json' => ['buyers' => $buyers, 'sellers' => $sellers]]); }
    public function setDeliveryAddress(string $n, array $address): array { return $this->c->request('POST', "/deals/$n/delivery-address", ['json' => $address]); }
    public function setBillingAddress(string $n, array $address): array { return $this->c->request('POST', "/deals/$n/billing-address", ['json' => $address]); }
    public function allowedPaymentMethods(string $n): array { return $this->c->request('GET', "/deals/$n/allowed-payment-methods/")['payment_methods'] ?? []; }
}
