<?php
declare(strict_types=1);
namespace Amen\Tests\Unit;
use Amen\{AmenApiError, AmenClient, AmenLifecycleError, Config};
use PHPUnit\Framework\TestCase;

final class ClientTest extends TestCase
{
    private static function client(callable $transport): AmenClient
    { return new AmenClient(new Config('sandbox', 'test-token', 'https://sandbox-api.amnn.sa', maxRetries: 1), $transport); }

    public function testAuthHeaderAndBaseUrl(): void
    {
        $c = self::client(function ($m, $url, $headers) { $this->assertSame('https://sandbox-api.amnn.sa/api/v1/account', $url); $this->assertContains('X-API-Token: test-token', $headers); return [200, '{"id":"a1"}']; });
        $this->assertSame(['id' => 'a1'], $c->account->get());
    }
    public function testErrorCodesParsed(): void
    {
        $c = self::client(fn() => [400, '{"error":["first_name__required"]}']);
        try { $c->customers->create([]); $this->fail(); }
        catch (AmenApiError $e) { $this->assertSame(400, $e->status); $this->assertTrue($e->has('first_name__required')); $this->assertFalse($e->isRetryable()); }
    }
    public function testLifecycleGuard(): void
    {
        $calls = 0; $c = self::client(function () use (&$calls) { $calls++; return [200, '{"number":"DL-1","status":"draft"}']; });
        $this->expectException(AmenLifecycleError::class);
        try { $c->deals->actions->approve('DL-1'); } finally { $this->assertSame(1, $calls); }
    }
    public function testOriginOnMutatingRequests(): void
    {
        $c = self::client(function ($m, $u, $headers) { $this->assertContains('Origin: https://sandbox-api.amnn.sa', $headers); return [201, '{"id":"w","secret_key":"s"}']; });
        $this->assertSame('s', $c->webhooks->create('https://example.com/hook')['secret_key']);
    }
}
