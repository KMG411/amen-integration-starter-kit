# 09 · Common mistakes (and what the kit does instead)

1. **Actions out of order** → the client checks the lifecycle table locally and raises a clear error.
2. **Floats for money** → `Money` helpers; strings with two decimals everywhere.
3. **Token in a mobile app** → mobile examples call a back-end proxy.
4. **Retrying 4xx** → only 429/5xx are retried.
5. **Losing `secret_key`** → the register example stores it immediately and prints a reminder.
6. **Parsing before verifying webhooks** → raw-body verification first.
7. **Ignoring pagination** → resources expose an iterator over pages.
8. **Hard-coded URLs** → `AMN_ENV` selects the base URL.
