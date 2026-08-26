<?php
declare(strict_types=1);
namespace Amen\Webhooks;
/** Signature verification over the RAW body. Header name and algorithm are one configuration point (docs/04-webhooks.md). */
final class Signature
{
    public const HEADER = 'X-Signature';
    public const ALGORITHM = 'sha256';
    public static function compute(string $secret, string $rawBody, string $algorithm = self::ALGORITHM): string { return hash_hmac($algorithm, $rawBody, $secret); }
    /** Accepts hex digests, optionally prefixed like "sha256=<hex>". Constant-time comparison. */
    public static function verify(string $secret, string $rawBody, ?string $received, string $algorithm = self::ALGORITHM): bool
    {
        if (!$received) return false;
        $given = strtolower(trim(str_contains($received, '=') ? explode('=', $received, 2)[1] : $received));
        return hash_equals(self::compute($secret, $rawBody, $algorithm), $given);
    }
}
