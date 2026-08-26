# PMS Mobile App — Kotlin Android
## Knowledge Transfer & Design Architecture

> **Purpose:** Complete technical KT for building the **Product Management System (PMS)** as a native Android app in Kotlin, for a developer starting from the existing bare-bones Android project at `D:\Program\AndroidStudio\PMS`.

---

## 1. Existing System Context (Read This First)

The backend already exists and is live. You are building a **mobile client** that consumes it. Do not rebuild the backend.

| Property | Value |
|---|---|
| **Live API Base URL** | `https://productmanagementsystem-eight.vercel.app/api` |
| **Auth Mechanism** | Cookie-based JWT (`jwt-token` HttpOnly + `user-info` non-HttpOnly) |
| **Backend Framework** | Spring Boot 3.2.5 / Java 17 on Render.com |
| **Database** | Neon Serverless PostgreSQL 16 |
| **Web Frontend** | Angular 18 SPA on Vercel |
| **Roles** | `ADMIN` and `SHOPKEEPER` |

### 1.1 Domain Model Quick Reference

```
USER ──owns──► SHOP ──has──► CATEGORY ──classifies──► PRODUCT
                 │                                        │
                 ├──receives──► PURCHASE ──creates──► INVENTORY_BATCH
                 │                                        │
                 └──issues──► SALES_INVOICE ──contains──► SALES_ITEM ◄── depletes ──┘
```

**Business rules the mobile app MUST respect:**
- Stock is tracked per `InventoryBatch` (FIFO). Never assume a single stock counter.
- `availableStock` is a **computed transient field** returned by the API. Do not calculate it locally.
- `PERISHABLE` products have expiry dates. `NON_PERISHABLE` do not.
- `stockStatus` values: `IN_STOCK`, `LOW_STOCK`, `OUT_OF_STOCK`, `EXPIRED`, `PARTIALLY_EXPIRED`.
- Payment modes: `CASH`, `UPI`, `CARD`.

---

## 2. Current Android Project Baseline

```
D:\Program\AndroidStudio\PMS\
├── app\
│   ├── build.gradle.kts          ← Java-only, no Compose, no Coroutines yet
│   └── src\main\
│       └── AndroidManifest.xml
├── gradle\
│   └── libs.versions.toml        ← Version catalog (AGP 9.2.1, compileSdk 36)
```

**What is missing (must be added):**
- Kotlin plugin
- Jetpack Compose
- Navigation Compose
- ViewModel + Hilt DI
- Retrofit + OkHttp
- DataStore (token/session persistence)
- Room (optional offline cache)
- Coil (image loading)

---

## 3. Recommended Technology Stack

| Layer | Library | Version | Reason |
|---|---|---|---|
| **UI** | Jetpack Compose | BOM 2024.09.x | Modern declarative UI, no XML |
| **Navigation** | Navigation Compose | 2.8.x | Type-safe screen routing |
| **State** | ViewModel + StateFlow | Lifecycle 2.8.x | MVVM state management |
| **DI** | Hilt | 2.51.x | Compile-time safe DI |
| **Network** | Retrofit 2 + OkHttp 4 | 2.11.x | REST calls, cookie jar |
| **Serialization** | Gson / Moshi | — | JSON parsing |
| **Session** | OkHttp PersistentCookieJar | — | Auto-manages HttpOnly cookies |
| **Image** | Coil 3 | 3.x | Async profile avatar loading |
| **Async** | Kotlin Coroutines + Flow | 1.8.x | Structured concurrency |
| **Local Cache** | Room (optional) | 2.6.x | Offline product catalog |
| **Build** | AGP 9.2.1, Kotlin 2.0 | compileSdk 36 | Already in project |

---

## 4. Project Architecture: Clean Architecture + MVVM

```
app/src/main/java/com/example/pms/
│
├── core/
│   ├── network/
│   │   ├── ApiClient.kt           ← Retrofit + OkHttp + CookieJar setup
│   │   ├── CookieStore.kt         ← PersistentCookieJar implementation
│   │   └── ApiResult.kt           ← sealed class Success/Error/Loading
│   ├── di/
│   │   ├── NetworkModule.kt        ← @Module Hilt binding for Retrofit
│   │   └── RepositoryModule.kt     ← @Module Hilt binding for repos
│   └── utils/
│       ├── InrFormatter.kt         ← ₹ currency formatting
│       └── Extensions.kt           ← Common Kotlin extensions
│
├── data/
│   ├── remote/
│   │   ├── api/
│   │   │   ├── AuthApi.kt
│   │   │   ├── ProductApi.kt
│   │   │   ├── SalesApi.kt
│   │   │   ├── PurchaseApi.kt
│   │   │   ├── CategoryApi.kt
│   │   │   ├── ShopApi.kt
│   │   │   └── RecommendationApi.kt
│   │   └── dto/
│   │       ├── ProductDto.kt
│   │       ├── SalesInvoiceDto.kt
│   │       ├── PurchaseDto.kt
│   │       └── ...
│   └── repository/
│       ├── AuthRepository.kt
│       ├── ProductRepository.kt
│       ├── SalesRepository.kt
│       ├── PurchaseRepository.kt
│       └── RecommendationRepository.kt
│
├── domain/
│   └── model/
│       ├── Product.kt
│       ├── SalesInvoice.kt
│       ├── InventoryBatch.kt
│       └── ...
│
└── presentation/
    ├── navigation/
    │   ├── AppNavGraph.kt          ← NavHost with all routes
    │   └── Screen.kt               ← sealed class for route definitions
    ├── auth/
    │   ├── LoginScreen.kt
    │   ├── RegisterScreen.kt
    │   └── LoginViewModel.kt
    ├── dashboard/
    │   ├── home/
    │   │   ├── HomeScreen.kt
    │   │   └── HomeViewModel.kt
    │   ├── products/
    │   │   ├── ProductsScreen.kt
    │   │   └── ProductsViewModel.kt
    │   ├── sales/
    │   │   ├── PosScreen.kt
    │   │   ├── SalesHistoryScreen.kt
    │   │   └── SalesViewModel.kt
    │   ├── purchases/
    │   │   ├── PurchasesScreen.kt
    │   │   └── PurchasesViewModel.kt
    │   ├── insights/
    │   │   ├── InsightsScreen.kt
    │   │   └── InsightsViewModel.kt
    │   └── profile/
    │       ├── ProfileScreen.kt
    │       └── ProfileViewModel.kt
    └── components/
        ├── PmsTopBar.kt
        ├── PmsBottomNav.kt
        ├── StatusPill.kt
        ├── MetricCard.kt
        └── ToastSnackbar.kt
```

---

## 5. Authentication & Cookie Strategy

The backend uses **cookie-based JWT**, not header-based Bearer tokens. This is the most important integration detail.

```kotlin
// CookieStore.kt — Persist cookies across app restarts
class PmsCookieJar(context: Context) : CookieJar {
    private val prefs = context.getSharedPreferences("pms_cookies", Context.MODE_PRIVATE)

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies.forEach { cookie ->
            prefs.edit().putString(cookie.name, cookie.toString()).apply()
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return prefs.all.values.mapNotNull { value ->
            Cookie.parse(url, value as? String ?: "")
        }
    }
}
```

**Session state strategy:**
- On login success → `user-info` cookie contains `Name:Role` (URL-encoded).
- Decode this cookie to display user name and conditionally show ADMIN-only nav items.
- On logout → call `POST /api/auth/logout`, then clear the cookie SharedPreferences.

---

## 6. API Integration Map

### 6.1 Auth Endpoints

| Method | Endpoint | Mobile Screen |
|---|---|---|
| `POST` | `/api/auth/login` | `LoginScreen` |
| `POST` | `/api/auth/register` | `RegisterScreen` |
| `POST` | `/api/auth/logout` | Profile/Settings |
| `POST` | `/api/auth/forgot-password` | `ForgotPasswordScreen` |

### 6.2 Core Data Endpoints

| Method | Endpoint | Mobile Screen |
|---|---|---|
| `GET` | `/api/products` | `ProductsScreen` |
| `POST` | `/api/products` | `AddProductScreen` |
| `PUT` | `/api/products/{id}` | `EditProductScreen` |
| `DELETE` | `/api/products/{id}` | `ProductsScreen` |
| `GET` | `/api/products/report` | Inventory Report |
| `GET` | `/api/categories` | Categories, dropdowns |
| `GET` | `/api/shops` | Shop selector |
| `GET` | `/api/purchases` | `PurchasesScreen` |
| `POST` | `/api/purchases` | `NewPurchaseScreen` |
| `GET` | `/api/sales` | `SalesHistoryScreen` |
| `POST` | `/api/sales/create` | `PosScreen` checkout |
| `GET` | `/api/recommendations/restock` | `InsightsScreen` |
| `GET` | `/api/recommendations/discounts` | `InsightsScreen` |
| `GET` | `/api/recommendations/summary` | `HomeScreen` |
| `GET` | `/api/users/me` | `ProfileScreen` |
| `PUT` | `/api/users/me` | `EditProfileScreen` |

### 6.3 Sales Request Structure (POS Checkout)

```kotlin
data class CreateSaleRequest(
    val shopId: Long,
    val paymentMode: String,     // "CASH" | "UPI" | "CARD"
    val items: List<SaleItemRequest>
)

data class SaleItemRequest(
    val productId: Long,
    val quantity: Int,
    val sellingPrice: Double,
    val discountPercent: Double  // 0.0 to 90.0
)
```

---

## 7. Screen-by-Screen Feature Map

### Screen 1: Login
- Email or phone field + password field.
- Show "Connecting to server…" notice if response takes >4 seconds (Render cold-start).
- On 401 → show `Snackbar("Invalid credentials")`.
- Persist cookies on success.

### Screen 2: Home Dashboard
- KPI cards: `Active Products`, `Warehouse Units`, `Low Stock Alerts`, `Expired Batches`.
- Data from `GET /api/recommendations/summary` → `StockHealthSummaryDTO`.
- Pull-to-refresh.
- Quick action buttons: `New Sale`, `Add Stock`, `View Insights`.

### Screen 3: Products
- Searchable, filterable list by shop + category.
- Each card: Product name, SKU, MRP, stock count, `StatusPill` (color-coded).
- FAB → `AddProductSheet` (bottom sheet form).
- Swipe-to-delete with confirmation.
- Edit via long-press or icon button.

### Screen 4: POS Terminal (Sales)
- **Left panel (scrollable):** Product catalog with category filter chips.
- **Cart panel (bottom sheet or right pane on tablet):** Line items, qty stepper (max = `availableStock`), discount %, running total.
- **Payment section:** `CASH` / `UPI` / `CARD` selector. For CASH: tender amount + change display.
- Submit → `POST /api/sales/create`.
- Show receipt summary on success.

### Screen 5: Purchases (Stock Intake)
- Form: Supplier name, product selector, cost price, quantity, expiry date picker.
- Submit → `POST /api/purchases`.
- History list below with expandable batch details.

### Screen 6: Inventory Health & Clearance (Insights)
- Tab 1 — **Restock Queue:** Cards with urgency pill, days-of-stock left, `Create PO` button (navigate to Purchases).
- Tab 2 — **Clearance Engine:** Risk score bars, recommended discount %, `Apply Markdown` button.
- Auto-refresh every 10 seconds using `LaunchedEffect`.

### Screen 7: Profile
- Show avatar (base64 decoded → `Coil`), name, email, phone, role.
- Upload photo → compress to max 200×200px JPEG before base64 encode (same as web: <40KB).
- Change password form.

### Screen 8: Admin — User Management (ADMIN only)
- List all users with status toggle.
- Create admin button.
- Hide from `SHOPKEEPER` role entirely.

---

## 8. State Management Pattern (ViewModel + StateFlow)

```kotlin
// Example: ProductsViewModel
@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductsUiState())
    val uiState: StateFlow<ProductsUiState> = _uiState.asStateFlow()

    init { loadProducts() }

    fun loadProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            productRepository.getProducts()
                .onSuccess { products ->
                    _uiState.update { it.copy(products = products, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                }
        }
    }
}

data class ProductsUiState(
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
```

---

## 9. Navigation Architecture

```kotlin
// Screen.kt
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object Products : Screen("products")
    object Pos : Screen("pos")
    object Purchases : Screen("purchases")
    object Insights : Screen("insights")
    object Profile : Screen("profile")
    object UserManagement : Screen("user_management")

    // with args
    data class ProductDetail(val id: Long) : Screen("product/{id}") {
        companion object { fun route(id: Long) = "product/$id" }
    }
}
```

```kotlin
// AppNavGraph.kt
@Composable
fun AppNavGraph(navController: NavHostController, startDestination: String) {
    NavHost(navController, startDestination) {
        composable(Screen.Login.route) { LoginScreen(navController) }
        composable(Screen.Home.route) { HomeScreen(navController) }
        composable(Screen.Products.route) { ProductsScreen(navController) }
        composable(Screen.Pos.route) { PosScreen(navController) }
        composable(Screen.Purchases.route) { PurchasesScreen(navController) }
        composable(Screen.Insights.route) { InsightsScreen(navController) }
        composable(Screen.Profile.route) { ProfileScreen(navController) }
        // ADMIN only
        composable(Screen.UserManagement.route) { UserManagementScreen(navController) }
    }
}
```

---

## 10. Design Language (Material You + Dark PMS Theme)

Match the web app's dark slate aesthetic on Android using Material 3.

```kotlin
// PmsTheme.kt
val PmsDarkColorScheme = darkColorScheme(
    primary = Color(0xFF6366F1),       // Indigo — same as web --primary
    secondary = Color(0xFF10B981),     // Emerald — success
    tertiary = Color(0xFFF59E0B),      // Amber — warning
    background = Color(0xFF090B10),   // Deep obsidian
    surface = Color(0xFF11141E),       // Slate surface
    surfaceVariant = Color(0xFF161B28),// Elevated surface
    error = Color(0xFFF43F5E),         // Crimson
    onPrimary = Color.White,
    onBackground = Color(0xFFE2E8F0),
    onSurface = Color(0xFFCBD5E1),
)
```

**Component conventions:**
- Use `Card` with `0.dp` elevation (flat) + custom stroke border `0.07f` alpha white.
- `StatusPill` Composable: small rounded chip with dot indicator for stock status.
- No emojis — use `Icons.Outlined.*` from Material Icons Extended.
- Tabular numerals for all currency and stock values: `fontFeatureSettings = "tnum"`.

---

## 11. Dependency Injection Setup (Hilt)

```kotlin
// NetworkModule.kt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideOkHttpClient(@ApplicationContext ctx: Context): OkHttpClient =
        OkHttpClient.Builder()
            .cookieJar(PmsCookieJar(ctx))
            .addInterceptor(HttpLoggingInterceptor().apply { 
                level = HttpLoggingInterceptor.Level.BODY 
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://productmanagementsystem-eight.vercel.app/api/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides @Singleton
    fun provideProductApi(retrofit: Retrofit): ProductApi =
        retrofit.create(ProductApi::class.java)
}
```

---

## 12. libs.versions.toml — Updated Dependencies to Add

```toml
[versions]
agp = "9.2.1"
kotlin = "2.0.21"
compose-bom = "2024.09.03"
hilt = "2.51.1"
hilt-nav = "1.2.0"
retrofit = "2.11.0"
okhttp = "4.12.0"
coil = "3.0.4"
coroutines = "1.8.1"
lifecycle = "2.8.6"
room = "2.6.1"
nav-compose = "2.8.3"
datastore = "1.1.1"
gson = "2.11.0"
# ... existing versions

[libraries]
# Compose
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-material-icons = { group = "androidx.compose.material", name = "material-icons-extended" }
compose-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
activity-compose = { group = "androidx.activity", name = "activity-compose", version = "1.9.0" }

# Navigation
nav-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "nav-compose" }

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-nav = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hilt-nav" }

# Lifecycle / ViewModel
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }

# Network
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-gson = { group = "com.squareup.retrofit2", name = "converter-gson", version.ref = "retrofit" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
gson = { group = "com.google.code.gson", name = "gson", version.ref = "gson" }

# Image
coil-compose = { group = "io.coil-kt.coil3", name = "coil-compose", version.ref = "coil" }
coil-network = { group = "io.coil-kt.coil3", name = "coil-network-okhttp", version.ref = "coil" }

# Coroutines
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }

# DataStore
datastore-prefs = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }

# Room (optional, for offline cache)
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
kotlin-ksp = { id = "com.google.devtools.ksp", version = "2.0.21-1.0.28" }
```

---

## 13. Error Handling Convention

```kotlin
// ApiResult.kt
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}

// Repository pattern
suspend fun <T> safeApiCall(call: suspend () -> Response<T>): ApiResult<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            ApiResult.Success(response.body()!!)
        } else {
            val errMsg = response.errorBody()?.string() ?: "Unknown error"
            ApiResult.Error(errMsg, response.code())
        }
    } catch (e: IOException) {
        ApiResult.Error("Network error: ${e.localizedMessage}")
    } catch (e: Exception) {
        ApiResult.Error("Unexpected error: ${e.localizedMessage}")
    }
}
```

---

## 14. Build Steps to Get Started

1. **Add Kotlin plugin** to root `build.gradle.kts`.
2. **Update `libs.versions.toml`** with the dependencies from Section 12.
3. **Update `app/build.gradle.kts`**: add `composeOptions`, enable `buildFeatures { compose = true }`, add all new dependencies.
4. **Add `@HiltAndroidApp`** annotation to `Application` class.
5. **Set up `MainActivity`** with `setContent { PmsTheme { AppNavGraph(...) } }`.
6. **Add INTERNET permission** to `AndroidManifest.xml`.
7. **Build `NetworkModule`** and `PmsCookieJar` first — nothing works without auth.
8. **Build `LoginScreen` + `LoginViewModel` + `AuthRepository`** — verify you can receive a cookie and hit `/api/products`.
9. Then build screen by screen following Section 7.

---

## 15. Key Business Rules Checklist for Mobile

| Rule | Where Enforced |
|---|---|
| Stock quantity input max = `product.availableStock` | POS `Stepper` Composable |
| PERISHABLE products must show expiry badge | `ProductCard` + `StatusPill` |
| Payment mode required before checkout | `PosScreen` submit button guard |
| Admin-only screens hidden for SHOPKEEPER | NavGraph role check |
| Image upload must be ≤ 40KB base64 | `ProfileViewModel` bitmap compress |
| Restock button navigates to Purchase form with `productId` pre-filled | `InsightsScreen` → `PurchasesScreen` |
| Cookie must be sent on every authenticated request | `OkHttp PmsCookieJar` |
