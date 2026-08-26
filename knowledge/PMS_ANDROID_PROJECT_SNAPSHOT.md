# PMS Android Project — Complete Knowledge Base
## Project Location: `D:\Program\AndroidStudio\PMS`
## Scanned: 2026-08-26

> **Purpose:** Complete snapshot of every file in the Android project so any future developer or AI agent can understand the exact current state without needing to re-read the filesystem.

---

## 1. Project Identity

| Property | Value |
|---|---|
| **Project name** | `PMS` |
| **Application ID** | `com.example.pms` |
| **Root namespace** | `com.example.pms` |
| **Version code** | `1` |
| **Version name** | `1.0` |
| **Min SDK** | `24` (Android 7.0 Nougat) |
| **Target SDK** | `36` |
| **Compile SDK** | `36` |
| **Build Tool** | Android Gradle Plugin `9.2.1` |
| **Gradle version** | `9.4.1` |
| **JVM Toolchain version** | `21` (JDK 21, from Foojay resolver) |
| **Java source compatibility** | `VERSION_11` |
| **Project location** | `D:\Program\AndroidStudio\PMS` |
| **State** | ⚠️ **Empty starter** — No Compose, no network, no DI, no navigation |

---

## 2. Complete File Tree (Source + Config, Excluding Build Artifacts)

```
D:\Program\AndroidStudio\PMS\
├── .gitignore
├── gradle.properties
├── gradlew                       (Unix shell)
├── gradlew.bat                   (Windows shell)
├── local.properties              (gitignored — contains SDK path)
├── settings.gradle.kts
├── build.gradle.kts              ← Root build file
│
├── gradle\
│   ├── libs.versions.toml        ← Version catalog
│   ├── gradle-daemon-jvm.properties
│   └── wrapper\
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
│
└── app\
    ├── .gitignore
    ├── build.gradle.kts          ← App module build file
    └── src\
        ├── androidTest\java\com\example\pms\   (empty — no test files)
        ├── test\java\com\example\pms\          (empty — no test files)
        └── main\
            ├── AndroidManifest.xml
            ├── keepRules\
            │   └── rules.keep
            ├── java\com\example\pms\
            │   └── MainActivity.kt             ← ONLY Kotlin source file
            └── res\
                ├── drawable\
                │   ├── ic_launcher_background.xml
                │   └── ic_launcher_foreground.xml
                ├── layout\
                │   └── activity_main.xml       (Hello World TextView)
                ├── mipmap-*/                   (launcher icons in webp)
                ├── values\
                │   ├── colors.xml
                │   ├── strings.xml
                │   └── themes.xml
                ├── values-night\themes.xml
                └── xml\
                    ├── backup_rules.xml
                    └── data_extraction_rules.xml
```

---

## 3. Complete File Contents

### 3.1 `settings.gradle.kts`

```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "PMS"
include(":app")
```

### 3.2 `build.gradle.kts` (root)

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
}
```

### 3.3 `gradle/libs.versions.toml` (CURRENT — before any additions)

```toml
[versions]
agp = "9.2.1"
coreKtx = "1.15.0"
junit = "4.13.2"
junitVersion = "1.3.0"
espressoCore = "3.7.0"
appcompat = "1.7.0"
material = "1.12.0"
activityKtx = "1.9.0"
constraintlayout = "2.2.1"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
material = { group = "com.google.android.material", name = "material", version.ref = "material" }
androidx-activity-ktx = { group = "androidx.activity", name = "activity-ktx", version.ref = "activityKtx" }
androidx-constraintlayout = { group = "androidx.constraintlayout", name = "constraintlayout", version.ref = "constraintlayout" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
```

> WARNING: Missing kotlin-android, kotlin-compose, hilt, ksp plugins. No Retrofit, Compose, Coroutines, or Hilt libraries.

### 3.4 `app/build.gradle.kts` (CURRENT)

```kotlin
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.pms"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.pms"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
```

> Missing: kotlinOptions, buildFeatures { compose = true }, Hilt plugin, KSP plugin.

### 3.5 `gradle/gradle-wrapper.properties`

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.4.1-bin.zip
```

### 3.6 `gradle.properties`

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.configuration-cache=true
kotlin.code.style=official
```

> NOTE: configuration-cache=true is set. KSP 2.0.21-1.0.28 supports it. Do not use older KSP versions.

### 3.7 `app/src/main/AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">
    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.PMS">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

> MISSING: android.permission.INTERNET — without this ALL network calls silently fail.
> MISSING: android:name=".PmsApplication" on the application tag (needed for Hilt).

### 3.8 `app/src/main/java/com/example/pms/MainActivity.kt`

```kotlin
package com.example.pms

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
```

> Must be replaced: extends AppCompatActivity (XML-based). For Compose: extend ComponentActivity, call setContent { }.

### 3.9 `res/values/themes.xml`

```xml
<style name="Base.Theme.PMS" parent="Theme.Material3.DayNight.NoActionBar">
    <!-- colorPrimary etc. to be customized -->
</style>
<style name="Theme.PMS" parent="Base.Theme.PMS" />
```

> Already uses Material 3 DayNight NoActionBar — correct base for Compose.

### 3.10 `res/values/colors.xml`

```xml
<resources>
    <color name="black">#FF000000</color>
    <color name="white">#FFFFFFFF</color>
</resources>
```

### 3.11 `res/values/strings.xml`

```xml
<resources>
    <string name="app_name">PMS</string>
</resources>
```

---

## 4. What Exists vs What Is Missing

### Already Present

| Item | Detail |
|---|---|
| Android Gradle Plugin | 9.2.1 — latest stable |
| Gradle | 9.4.1 — latest stable |
| JDK Toolchain | JDK 21 via Foojay resolver |
| Material 3 theme | Theme.Material3.DayNight.NoActionBar already set |
| enableEdgeToEdge() | Called in MainActivity — correct modern pattern |
| compileSdk = 36 | Latest Android API |
| Version catalog | libs.versions.toml structure in place |
| windowSoftInputMode="adjustResize" | Already in manifest — good for keyboard + forms |

### Not Yet Added

| Missing Item | Why It's Needed |
|---|---|
| kotlin-android plugin | Required to compile Kotlin |
| kotlin-compose plugin (K2) | Compose Kotlin compiler |
| hilt + ksp plugins | Dependency injection |
| buildFeatures { compose = true } | Enables Compose |
| INTERNET permission in manifest | All API calls silently fail without this |
| android:name=".PmsApplication" | Required for @HiltAndroidApp |
| Jetpack Compose BOM | ui, material3, tooling, activity-compose |
| Navigation Compose | Screen routing |
| Hilt Android | DI container |
| Retrofit 2 | HTTP client |
| OkHttp 4 + logging interceptor | HTTP layer + cookie jar |
| Gson | JSON parsing |
| Coroutines | Async API calls |
| ViewModel + Lifecycle | State management |
| DataStore Preferences | Session/cookie persistence |
| Coil 3 | Profile image loading |
| PmsApplication.kt | @HiltAndroidApp Application class |
| AppNavGraph.kt | NavHost for all screens |
| NetworkModule.kt | Hilt DI module for Retrofit |
| PmsCookieJar.kt | HttpOnly JWT cookie persistence |

---

## 5. Critical Integration Notes

Backend live URL: `https://productmanagementsystem-eight.vercel.app/api`

### Auth: Cookie-Based JWT (Not Bearer Token)
The backend uses HttpOnly cookies — NOT Authorization headers.
OkHttp must be configured with a persistent CookieJar.
Without PmsCookieJar, every API call after login returns 401 Unauthorized.

Two cookies are set on login:
- "jwt-token" — HttpOnly, sent automatically by OkHttp CookieJar
- "user-info" — URL-encoded "Name:ROLE" — readable, use to display user name and control admin nav visibility

### Backend Cold-Start (Render.com free tier)
Backend may take 20–45 seconds to respond after inactivity.
Show a warm-up notice if login request takes >4 seconds.

### Profile Image Compression
Images must be compressed to less than 40KB base64 before upload.
Use Bitmap.createScaledBitmap() + compress(JPEG, 80).

### FIFO Stock
availableStock is computed by the backend. Never compute it locally.

---

## 6. Ready-to-Use Replacement Files

### Updated `gradle/libs.versions.toml`

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
nav-compose = "2.8.3"
datastore = "1.1.1"
gson = "2.11.0"
ksp = "2.0.21-1.0.28"
coreKtx = "1.15.0"
junit = "4.13.2"
junitVersion = "1.3.0"
espressoCore = "3.7.0"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-material-icons = { group = "androidx.compose.material", name = "material-icons-extended" }
compose-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
activity-compose = { group = "androidx.activity", name = "activity-compose", version = "1.9.0" }
nav-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "nav-compose" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-nav = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hilt-nav" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-gson = { group = "com.squareup.retrofit2", name = "converter-gson", version.ref = "retrofit" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
gson = { group = "com.google.code.gson", name = "gson", version.ref = "gson" }
coil-compose = { group = "io.coil-kt.coil3", name = "coil-compose", version.ref = "coil" }
coil-network = { group = "io.coil-kt.coil3", name = "coil-network-okhttp", version.ref = "coil" }
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
datastore-prefs = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
kotlin-ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

### Updated `app/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.ksp)
}

android {
    namespace = "com.example.pms"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.pms"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.preview)
    implementation(libs.activity.compose)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.nav.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.nav)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.coil.network)
    implementation(libs.datastore.prefs)
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
```

### `PmsApplication.kt` (New File)

```kotlin
package com.example.pms

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PmsApplication : Application()
```

### Updated `MainActivity.kt`

```kotlin
package com.example.pms

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.pms.presentation.navigation.AppNavGraph
import com.example.pms.presentation.theme.PmsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PmsTheme {
                AppNavGraph()
            }
        }
    }
}
```

---

## 7. Known Gotchas

| Gotcha | Detail |
|---|---|
| configuration-cache=true | KSP 2.0.21-1.0.28 supports it. Do not downgrade KSP or Gradle cache breaks. |
| AppCompatActivity vs ComponentActivity | Current MainActivity extends AppCompatActivity. Must change to ComponentActivity for Compose. |
| activity_main.xml | This layout file will be unused once setContent {} is added. Delete it to keep project clean. |
| No Kotlin plugin in root build | Root build.gradle.kts only has android.application. Kotlin plugin must be added via kotlin-android alias. |
| Cookie auth not Bearer auth | Backend uses HttpOnly cookies. OkHttp default is CookieJar.NO_COOKIES. Must supply custom PmsCookieJar. |
| Render.com cold start | Free tier backend sleeps. Login may take 45 seconds. Always show loading notice after 4s. |
| Profile avatar size | Compress to less than 40KB base64 before upload. |
| FIFO stock | availableStock is computed by backend. Never compute locally. |
