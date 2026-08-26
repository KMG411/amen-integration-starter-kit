<?php
declare(strict_types=1);
namespace Amen\Resources;
/** Normalised page: ['items'=>[], 'page'=>0, 'pages'=>1, 'total'=>0] */
final class Page
{
    public static function from(?array $d, string $key): array
    {
        $p = is_array($d['page'] ?? null) ? $d['page'] : ($d ?? []);
        return ['items' => $d[$key] ?? $d['results'] ?? $d['items'] ?? [], 'page' => $p['page'] ?? 0, 'pages' => $p['pages'] ?? 1, 'total' => $p['total'] ?? 0];
    }
}
