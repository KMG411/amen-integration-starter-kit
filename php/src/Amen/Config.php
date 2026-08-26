<?php
declare(strict_types=1);
namespace Amen;

/** Environment-based configuration. Walks up from cwd to find a .env (stack or kit root). */
final class Config
{
    public const BASE_URLS = ['sandbox' => 'https://sandbox-api.amnn.sa', 'live' => 'https://api.amnn.sa'];
    public const API_PREFIX = '/api/v1';

    public function __construct(
        public readonly string $env, public readonly string $apiKey, public readonly string $baseUrl,
        public readonly int $timeoutMs = 20000, public readonly ?string $webhookSecret = null, public readonly int $maxRetries = 3,
    ) {}

    public static function fromEnv(array $overrides = []): self
    {
        self::loadDotenv();
        $env = strtolower($overrides['env'] ?? getenv('AMN_ENV') ?: 'sandbox');
        if (!isset(self::BASE_URLS[$env])) throw new \InvalidArgumentException("AMN_ENV must be 'sandbox' or 'live', got '$env'");
        $key = $overrides['apiKey'] ?? getenv('AMN_API_KEY');
        if (!$key) throw new \RuntimeException('AMN_API_KEY is not set (see .env.example)');
        return new self($env, $key, $overrides['baseUrl'] ?? (getenv('AMN_BASE_URL') ?: self::BASE_URLS[$env]),
            (int)($overrides['timeoutMs'] ?? getenv('AMN_TIMEOUT_MS') ?: 20000),
            $overrides['webhookSecret'] ?? (getenv('AMN_WEBHOOK_SECRET') ?: null), $overrides['maxRetries'] ?? 3);
    }

    /** Minimal .env loader (KEY=VALUE, # comments). Existing environment variables win. */
    private static function loadDotenv(): void
    {
        $dir = getcwd();
        for ($i = 0; $i < 3 && $dir; $i++, $dir = dirname($dir)) {
            $file = $dir . DIRECTORY_SEPARATOR . '.env';
            if (!is_file($file)) continue;
            foreach (file($file, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES) as $line) {
                if ($line[0] === '#' || !str_contains($line, '=')) continue;
                [$k, $v] = explode('=', $line, 2);
                $k = trim($k); $v = trim(preg_replace('/\s+#.*$/', '', $v), " \t\"'");
                if ($v !== '' && getenv($k) === false) putenv("$k=$v");
            }
        }
    }
}
