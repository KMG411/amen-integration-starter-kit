<?php
declare(strict_types=1);
namespace Amen\Tests\Unit;
use Amen\Webhooks\{Handler, Signature};
use PHPUnit\Framework\TestCase;

final class WebhooksTest extends TestCase
{
    // Golden vector captured from a real Amen delivery (2026-08-30).
    private const SECRET = 'whsec_test';
    private const TS = '2026-08-30T18:53:23.885957+00:00';
    private const BODY = '{"event":"deal.status.changed","timestamp":"2026-08-30T18:53:23.885957+00:00","payload":{"number":"D-0000000002","status":"paid"}}';
    private const GOLDEN = '950ca0ff7494dd435d4dc9d7e7ebe31cf54f0859a28a69a686d77e8db9dfd45c';

    public function testGoldenVector(): void
    {
        $this->assertSame(self::GOLDEN, Signature::compute(self::SECRET, self::TS, self::BODY));
        $this->assertSame('sha256=' . self::GOLDEN, Signature::sign(self::SECRET, self::TS, self::BODY));
    }

    public function testSignatureRoundtripAndPrefix(): void
    {
        $sig = Signature::compute(self::SECRET, self::TS, self::BODY);
        $this->assertTrue(Signature::verify(self::SECRET, self::TS, self::BODY, $sig));
        $this->assertTrue(Signature::verify(self::SECRET, self::TS, self::BODY, "sha256=$sig"));
    }

    public function testTamperedBodyOrTimestampFails(): void
    {
        $sig = Signature::compute(self::SECRET, self::TS, self::BODY);
        $this->assertFalse(Signature::verify(self::SECRET, self::TS, self::BODY . ' ', $sig), 'tampered body');
        $this->assertFalse(Signature::verify(self::SECRET, self::TS . 'x', self::BODY, $sig), 'tampered timestamp');
        $this->assertFalse(Signature::verify('wrong', self::TS, self::BODY, $sig), 'wrong secret');
    }

    public function testMissingSignatureOrTimestampFails(): void
    {
        $sig = Signature::compute(self::SECRET, self::TS, self::BODY);
        $this->assertFalse(Signature::verify(self::SECRET, self::TS, self::BODY, null), 'missing signature');
        $this->assertFalse(Signature::verify(self::SECRET, self::TS, self::BODY, ''), 'empty signature');
        $this->assertFalse(Signature::verify(self::SECRET, '', self::BODY, $sig), 'missing timestamp');
    }

    public function testHandlerVerifiesDedupesAndDispatches(): void
    {
        $seen = [];
        $h = new Handler(self::SECRET, function ($e) use (&$seen) { $seen[] = [$e['type'], $e['id']]; });
        $good = [
            'X-Webhook-Signature' => Signature::sign(self::SECRET, self::TS, self::BODY),
            'X-Webhook-Timestamp' => self::TS,
            'X-Webhook-Event' => 'deal.status.changed',
        ];

        // Bad signature -> 401.
        $this->assertSame(401, $h->handle(['X-Webhook-Signature' => 'bad', 'X-Webhook-Timestamp' => self::TS], self::BODY)[0]);
        // Missing timestamp -> 401 even with an otherwise-valid signature.
        $this->assertSame(401, $h->handle(['X-Webhook-Signature' => $good['X-Webhook-Signature']], self::BODY)[0]);
        // Valid -> 200 ok.
        $this->assertSame([200, ['ok' => true]], $h->handle($good, self::BODY));
        // Replay with same envelope timestamp -> 200 duplicate, no second dispatch.
        $this->assertSame([200, ['ok' => true, 'duplicate' => true]], $h->handle($good, self::BODY));
        $this->assertSame([['deal.status.changed', self::TS]], $seen);
    }

    public function testHandlerCaseInsensitiveHeadersAndBareHexSignature(): void
    {
        $h = new Handler(self::SECRET, fn($e) => null);
        $headers = [
            'x-webhook-signature' => Signature::compute(self::SECRET, self::TS, self::BODY), // bare hex, no prefix
            'X-WEBHOOK-TIMESTAMP' => self::TS,
        ];
        $this->assertSame([200, ['ok' => true]], $h->handle($headers, self::BODY));
    }

    public function testHandlerInvalidJsonAfterValidSignature(): void
    {
        $body = 'not-json';
        $h = new Handler(self::SECRET, fn($e) => null);
        $headers = [
            'X-Webhook-Signature' => Signature::sign(self::SECRET, self::TS, $body),
            'X-Webhook-Timestamp' => self::TS,
        ];
        $this->assertSame(400, $h->handle($headers, $body)[0]);
    }
}
