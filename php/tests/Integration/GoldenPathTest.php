<?php
/** Mirrors scenario/golden-path.yml. Skipped without sandbox credentials. */
declare(strict_types=1);
namespace Amen\Tests\Integration;
use Amen\{AmenApiError, AmenClient};
use PHPUnit\Framework\TestCase;

final class GoldenPathTest extends TestCase
{
    private AmenClient $amen;
    protected function setUp(): void
    {
        try { $this->amen = new AmenClient(); } catch (\RuntimeException) { $this->markTestSkipped('AMN_API_KEY not set'); }
        if ($this->amen->config->env !== 'sandbox') $this->markTestSkipped('sandbox only');
    }
    private static function phone(string $p): string { return substr($p . substr((string)time(), -7), 0, 9); }

    public function testGoldenPath(): void
    {
        $a = $this->amen;
        $buyer = $a->customers->create(['first_name' => 'Buyer', 'last_name' => 'Kit', 'phone_code' => 'SA', 'phone_number' => self::phone('57')]);
        $seller = $a->customers->create(['first_name' => 'Seller', 'last_name' => 'Kit', 'phone_code' => 'SA', 'phone_number' => self::phone('58')]);
        $deal = $a->deals->create(['offer_type' => 'product', 'offer_category' => $a->lookups->categories()[0]['id'], 'offer_title' => 'Starter Kit golden path',
            'offer_description' => 'Reference deal created by the Amen integration starter kit', 'offer_price' => '100.00', 'offer_delivery_fee' => '10.00']);
        $n = $deal['number']; $this->assertSame('draft', $deal['status']);
        $this->assertSame('draft', $a->deals->setParties($n, [$buyer['number']], [$seller['number']])['status']);
        $this->assertSame('draft', $a->deals->setDeliveryAddress($n, ['city' => $a->lookups->cities()[0]['id'], 'district' => 'Al Olaya', 'street' => 'King Fahd Rd', 'building_number' => '1234', 'unit_number' => '1', 'zip_code' => '12211'])['status']);
        $this->assertSame('requested', $a->deals->actions->submit($n)['status']);
        $this->assertSame('payment_pending', $a->deals->actions->approve($n)['status']);
        try { $paid = $a->deals->actions->payWithWallet($n); } catch (AmenApiError $e) { $this->markTestSkipped('NEEDS_TOP_UP: ' . implode(',', $e->codes)); }
        $this->assertSame('paid', $paid['status']);
        $this->assertSame('executing', $a->deals->actions->executionStart($n)['status']);
        $this->assertSame('executed', $a->deals->actions->executionComplete($n)['status']);
        $this->assertSame('completed', $a->deals->actions->complete($n)['status']);
        $this->assertSame('completed', $a->deals->actions->transferSellerAmount($n)['status']);
    }
}
