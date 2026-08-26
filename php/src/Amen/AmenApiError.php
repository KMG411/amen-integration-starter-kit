<?php
declare(strict_types=1);
namespace Amen;

/** Any non-2xx response. `$codes` holds the API's error codes, e.g. "price__required". */
class AmenApiError extends \RuntimeException
{
    /** @param string[] $codes */
    public function __construct(public readonly int $status, public readonly array $codes, public readonly string $method,
                                public readonly string $path, public readonly mixed $body = null)
    {
        parent::__construct("$status $method $path: " . (implode(', ', $codes) ?: json_encode($body)), $status);
    }
    public function has(string $code): bool { return in_array($code, $this->codes, true); }
    public function isRetryable(): bool { return $this->status === 429 || $this->status >= 500; }
}

/** Thrown locally, before any HTTP call, when an action is not valid for the deal's status. */
class AmenLifecycleError extends \LogicException {}
