# Amen API — PHP reference implementation

PHP 8.1+, **zero runtime dependencies** (ext-curl + ext-json) so the `src/Amen` folder can be dropped into any Laravel/Symfony/plain-PHP project. PHPUnit for tests.

```sh
composer install
cp ../.env.example ../.env        # set AMN_API_KEY
php examples/01_golden_path.php
composer test:unit                # offline
composer test:integration         # sandbox; skipped when AMN_API_KEY is unset
```

```php
use Amen\AmenClient; use Amen\AmenApiError;
$amen = new AmenClient();                                   // config from env
$deal = $amen->deals->create(['offer_type' => 'product', 'offer_title' => 'iPhone 15',
                              'offer_price' => '3500.00', 'offer_delivery_fee' => '25.00', 'offer_category' => 12]);
$amen->deals->setParties($deal['number'], [$buyer['number']], [$seller['number']]);
try { $amen->deals->actions->submit($deal['number']); }
catch (AmenApiError $e) { if ($e->has('deal__delivery_address_required')) { /* … */ } else throw $e; }
```

**Laravel:** bind `AmenClient` as a singleton in a service provider and inject it; put `AMN_*` in `.env`. The webhook route must read `$request->getContent()` (raw body) and be excluded from CSRF middleware.
