<?php
declare(strict_types=1);
namespace Amen;

use Amen\Resources\{Account, Customers, Deals, Lookups, Webhooks, Withdrawals};

/** AmenClient — the one place that knows about auth headers, base URL, timeouts and retries. */
final class AmenClient
{
    public readonly Config $config;
    public readonly Lookups $lookups; public readonly Account $account; public readonly Customers $customers;
    public readonly Deals $deals; public readonly Withdrawals $withdrawals; public readonly Webhooks $webhooks;
    /** @var callable(string $method, string $url, array $headers, ?string $body): array{0:int,1:string} */
    private $transport;
    private readonly string $csrf;

    public function __construct(?Config $config = null, ?callable $transport = null)
    {
        $this->config = $config ?? Config::fromEnv();
        $this->transport = $transport ?? [$this, 'curl'];
        $this->csrf = bin2hex(random_bytes(16));   // 32 hex chars — Django CSRF token format
        $this->lookups = new Lookups($this); $this->account = new Account($this); $this->customers = new Customers($this);
        $this->deals = new Deals($this); $this->withdrawals = new Withdrawals($this); $this->webhooks = new Webhooks($this);
    }

    /** @param array{json?:mixed, params?:array, form?:array} $opts  form = multipart fields (values may be CURLFile) */
    public function request(string $method, string $path, array $opts = []): mixed
    {
        $url = $this->config->baseUrl . Config::API_PREFIX . $path;
        if (!empty($opts['params'])) $url .= '?' . http_build_query(array_filter($opts['params'], fn($v) => $v !== null));
        $headers = ['X-API-Token: ' . $this->config->apiKey, 'Accept: application/json', 'Accept-Language: en', 'User-Agent: amen-starter-kit-php/0.1', 'Cookie: csrftoken=' . $this->csrf];
        if ($method !== 'GET') {  // Django CSRF double-submit: token in both the X-CSRFToken header and the csrftoken cookie
            $headers[] = 'X-CSRFToken: ' . $this->csrf;
            $headers[] = 'Origin: ' . $this->config->baseUrl;
            $headers[] = 'Referer: ' . $this->config->baseUrl;
        }
        $body = null;
        if (isset($opts['form'])) { $body = $opts['form']; }                       // curl builds multipart from an array
        elseif (array_key_exists('json', $opts)) { $body = json_encode($opts['json']); $headers[] = 'Content-Type: application/json'; }

        for ($attempt = 1; ; $attempt++) {
            try { [$status, $text] = ($this->transport)($method, $url, $headers, $body); }
            catch (\RuntimeException $e) { if ($attempt > $this->config->maxRetries) throw $e; $this->sleep($attempt); continue; }
            if ($status < 400) return $text === '' ? null : json_decode($text, true);
            $decoded = json_decode($text, true);
            $raw = is_array($decoded) ? ($decoded['error'] ?? []) : [];
            $err = new AmenApiError($status, array_map('strval', is_array($raw) ? $raw : [$raw]), $method, Config::API_PREFIX . $path, $decoded ?? $text);
            if ($err->isRetryable() && $attempt <= $this->config->maxRetries) { $this->sleep($attempt); continue; }
            throw $err;
        }
    }

    /** @return array{0:int,1:string} */
    private function curl(string $method, string $url, array $headers, mixed $body): array
    {
        $ch = curl_init($url);
        curl_setopt_array($ch, [CURLOPT_CUSTOMREQUEST => $method, CURLOPT_HTTPHEADER => $headers, CURLOPT_RETURNTRANSFER => true,
            CURLOPT_TIMEOUT_MS => $this->config->timeoutMs, CURLOPT_FOLLOWLOCATION => false]);
        if ($body !== null) curl_setopt($ch, CURLOPT_POSTFIELDS, $body);
        $text = curl_exec($ch);
        if ($text === false) { $e = curl_error($ch); curl_close($ch); throw new \RuntimeException("network error: $e"); }
        $status = (int)curl_getinfo($ch, CURLINFO_RESPONSE_CODE); curl_close($ch);
        return [$status, (string)$text];
    }

    private function sleep(int $attempt): void { usleep((int)(min(2 ** $attempt, 20) * 1_000_000 + random_int(0, 1_000_000))); }
}
