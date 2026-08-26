<?php
declare(strict_types=1);
require_once __DIR__ . '/../vendor/autoload.php';
function uniquePhone(string $prefix): string { return substr($prefix . substr((string)time(), -7), 0, 9); }
function step(string $label, ?array $deal = null): void { echo "✔ $label" . ($deal ? " → status={$deal['status']}" : '') . PHP_EOL; }
