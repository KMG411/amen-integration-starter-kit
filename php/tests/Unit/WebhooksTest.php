<?php
declare(strict_types=1);
namespace Amen\Tests\Unit;
use Amen\Webhooks\{Handler, Signature};
use PHPUnit\Framework\TestCase;

final class WebhooksTest extends TestCase
{
    public function testSignatureRoundtrip(): void
    {
        $body = '{"id":"e1","event":"deal.paid"}'; $sig = Signature::compute('s3cret', $body);
        $this->assertTrue(Signature::verify('s3cret', $body, $sig));
        $this->assertTrue(Signature::verify('s3cret', $body, "sha256=$sig"));
        $this->assertFalse(Signature::verify('s3cret', $body . ' ', $sig));
        $this->assertFalse(Signature::verify('s3cret', $body, null));
    }
    public function testHandlerVerifiesAndDedupes(): void
    {
        $seen = []; $h = new Handler('s3cret', function ($e) use (&$seen) { $seen[] = $e['id']; });
        $body = json_encode(['id' => 'e1', 'event' => 'deal.paid']); $good = ['X-Signature' => Signature::compute('s3cret', $body)];
        $this->assertSame(401, $h->handle(['X-Signature' => 'bad'], $body)[0]);
        $this->assertSame([200, ['ok' => true]], $h->handle($good, $body));
        $this->assertSame([200, ['ok' => true, 'duplicate' => true]], $h->handle($good, $body));
        $this->assertSame(['e1'], $seen);
    }
}
