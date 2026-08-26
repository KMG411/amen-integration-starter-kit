# Android sample — back-end proxy pattern

An Android app must **never** contain `AMN_API_KEY` (it can be pulled out of the APK). The pattern:

```
Android app ──(your session token, EncryptedSharedPreferences/Keystore)──▶ your backend ──(X-API-Token)──▶ api.amnn.sa
```

* Backend: use `amen-client` (Ktor/Spring) or any server stack in this kit.
* App: Retrofit/OkHttp to **your** backend; store the app's session token with `EncryptedSharedPreferences` (backed by the Android Keystore).
* Online payments: the backend calls `make-payment-online` and returns the HyperPay `checkout_id`; the app shows HyperPay's Android SDK / web checkout with it.
* Optional: OkHttp `CertificatePinner` for your backend host.

```kotlin
// App side (no Amen token anywhere)
interface BackendApi {
    @POST("/api/deals") suspend fun createDeal(@Body body: CreateDealRequest): DealSummary
    @GET("/api/deals/{number}") suspend fun deal(@Path("number") number: String): DealSummary
}
val prefs = EncryptedSharedPreferences.create(context, "session", MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
val client = OkHttpClient.Builder().addInterceptor { chain ->
    chain.proceed(chain.request().newBuilder().header("Authorization", "Bearer ${prefs.getString("session_token", "")}").build())
}.build()
```
