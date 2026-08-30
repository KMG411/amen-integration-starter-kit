<?php
declare(strict_types=1);
namespace Amen\Webhooks;
/**
 * Signature verification for Amen webhook deliveries.
 *
 * Real Amen scheme (verified 2026-08-30):
 *   - Header  X-Webhook-Signature: sha256=<hex>   (HMAC-SHA256, hex, "sha256=" prefixed)
 *   - Header  X-Webhook-Timestamp: <ISO-8601>
 *   - Header  X-Webhook-Event:     <event type>   (also present as "event" in the JSON body)
 *   - signedMessage = timestamp . "." . rawBody
 *   - signature     = "sha256=" . hash_hmac('sha256', signedMessage, secret)
 */
final class Signature
{
    public const HEADER_SIGNATURE = 'X-Webhook-Signature';
    public const HEADER_TIMESTAMP = 'X-Webhook-Timestamp';
    public const HEADER_EVENT = 'X-Webhook-Event';
    public const ALGORITHM = 'sha256';

    /** Hex HMAC over `timestamp . "." . rawBody`. Returns the bare hex digest (no "sha256=" prefix). */
    public static function compute(string $secret, string $timestamp, string $rawBody, string $algorithm = self::ALGORITHM): string
    {
        return hash_hmac($algorithm, $timestamp . '.' . $rawBody, $secret);
    }

    /** Convenience: the digest with the "sha256=" prefix, exactly as sent in the X-Webhook-Signature header. */
    public static function sign(string $secret, string $timestamp, string $rawBody, string $algorithm = self::ALGORITHM): string
    {
        return $algorithm . '=' . self::compute($secret, $timestamp, $rawBody, $algorithm);
    }

    /**
     * Constant-time verification. Accepts hex digests, optionally prefixed like "sha256=<hex>".
     * Returns false when either the received signature or the timestamp is empty.
     */
    public static function verify(string $secret, string $timestamp, string $rawBody, ?string $received, string $algorithm = self::ALGORITHM): bool
    {
        if ($received === null || $received === '' || $timestamp === '') return false;
        $given = strtolower(trim(str_contains($received, '=') ? explode('=', $received, 2)[1] : $received));
        return hash_equals(self::compute($secret, $timestamp, $rawBody, $algorithm), $given);
    }
}
