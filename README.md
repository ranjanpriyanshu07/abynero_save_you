# PhoneGuard

PhoneGuard is an Android security app for everyday phone users in India. It scans the phone
for risky apps, unsafe settings, open Wi-Fi and scam links, explains every problem in simple
English or Hindi, and gives a one-tap **Fix** button that opens the exact Settings screen.

All scanning happens on the device. The only thing that ever leaves the phone is a link the
user chooses to check, which is sent to Google Safe Browsing.

| | |
|---|---|
| Language / UI | Kotlin, Jetpack Compose, Material 3 (dynamic colour on Android 12+) |
| Architecture | MVVM: ViewModel + StateFlow + Repository, Hilt for DI |
| Storage | Room (scan history, link-check counts), DataStore (settings) |
| Background | WorkManager (daily scan, new-app scan) |
| Camera / QR | CameraX + ML Kit barcode scanning (bundled model, works offline) |
| Min / target SDK | 26 / 35 |

---

## Features

1. **App risk scanner**: scores every user-installed app on SMS, call log, accessibility,
   overlay, device admin, notification access, mic, camera, location and "install apps".
   It flags sideloaded apps (installer is not Play Store, Galaxy Store or Amazon Appstore) and
   remote-control apps (AnyDesk, TeamViewer QuickSupport, AirDroid, RustDesk) with a scam warning.
   Levels are Safe, Low, Medium and High, each with plain-language reasons and a suggested action.
2. **Device settings checker**: screen lock, developer options, USB debugging, apps allowed
   to install unknown apps, security-patch age, and a Play Protect hint. Each has a **Fix**
   button with fallbacks for phones whose maker renamed the Settings screen.
3. **Wi-Fi safety**: detects open and WEP networks and warns against banking or UPI on them.
4. **Link, message and QR checker**: paste a link or a whole SMS, share text from any app
   (the share sheet shows "Check an SMS or message"), or scan a QR code. It runs on-device
   red-flag checks (IP-address links, look-alike bank domains, shorteners, non-HTTPS,
   punycode, `user@host` tricks, scam words, UPI payment QR codes), Indian scam-phrase
   detection (fake KYC, digital arrest, UPI collect, fake delivery, OTP requests, etc.) and
   Google Safe Browsing.
5. **Security score**: one 0–100 score with a breakdown (Apps 40 %, Device 35 %,
   Network 15 %, Links 10 %). Any High-risk finding caps the score at 59.
6. **Scheduled scans**: a daily WorkManager scan plus a quick scan when a new app is
   installed. The user is notified only about risks that are new.
7. **Scan history**: saved in Room, with a score-trend chart.
8. **Safety tips**: fake KYC, digital arrest calls, UPI collect requests, fake delivery SMS,
   screen-sharing scams, OTP sharing and fake customer-care numbers, plus the 1930 helpline.

The UI is available in English and Hindi (`values-hi`). On Android 13+, users can pick the
app language in system settings (`locales_config.xml`).

---

## Folder structure

```
PhoneGuard/
├── app/
│   ├── build.gradle.kts            # App module; reads SAFE_BROWSING_API_KEY from local.properties
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml # Permissions with justifications, share target, WorkManager init
│       │   ├── java/com/phoneguard/
│       │   │   ├── PhoneGuardApplication.kt   # Hilt app, WorkManager config, daily scan, new-app receiver
│       │   │   ├── MainActivity.kt            # Compose host; handles share + notification intents
│       │   │   ├── MainViewModel.kt
│       │   │   ├── domain/        # Pure Kotlin (no Android imports) → fully unit-testable
│       │   │   │   ├── model/       # RiskLevel, AppRisk, DeviceCheck, WifiCheck, LinkCheck, SecurityScore, Scan
│       │   │   │   ├── scoring/     # AppRiskScorer, SecurityScoreCalculator, RiskKeys, permission groups
│       │   │   │   ├── url/         # UrlRedFlagChecker + KnownDomains (banks, shorteners, TLDs)
│       │   │   │   ├── message/     # MessageScamAnalyzer (Indian scam phrases, link extraction)
│       │   │   │   ├── device/      # DeviceSettingsEvaluator (patch age, severities)
│       │   │   │   ├── wifi/        # WifiSecurityParser
│       │   │   │   └── repository/  # Repository interfaces
│       │   │   ├── scanner/       # Each scanner behind an interface
│       │   │   │   ├── Scanners.kt                 # AppScanner, DeviceSettingsScanner, WifiScanner, LinkScanner, SafeBrowsingClient
│       │   │   │   ├── AndroidAppScanner.kt
│       │   │   │   ├── AndroidDeviceSettingsScanner.kt
│       │   │   │   ├── AndroidWifiScanner.kt
│       │   │   │   ├── HttpSafeBrowsingClient.kt
│       │   │   │   ├── DefaultLinkScanner.kt
│       │   │   │   └── MessageChecker.kt
│       │   │   ├── data/
│       │   │   │   ├── local/       # Room database, entities, DAOs
│       │   │   │   ├── preferences/ # DataStore
│       │   │   │   └── repository/  # DefaultScanRepository, RoomHistoryRepository
│       │   │   ├── worker/        # DailyScanWorker, NewAppScanWorker, PackageAddedReceiver, RiskNotifier, ScanScheduler
│       │   │   ├── di/            # Hilt modules and qualifiers
│       │   │   └── ui/
│       │   │       ├── theme/       # Colours (navy/teal/amber/red), typography, shapes, dynamic colour
│       │   │       ├── components/  # ScoreGauge, RadarAnimation, ScanPulseButton, ResultCard, RiskChip, SummaryCard, TrendChart, AppIcon …
│       │   │       ├── navigation/  # Routes, bottom bar, NavHost with transitions
│       │   │       ├── screens/     # home, scan, appdetail, tools, history, tips, onboarding
│       │   │       └── util/        # String/icon mapping, SettingsLauncher (Fix intents), formatters
│       │   └── res/
│       │       ├── values/strings.xml       # All English text
│       │       ├── values-hi/strings.xml    # All Hindi text
│       │       ├── values(-night)/          # Window theme and colours
│       │       ├── drawable/, mipmap-anydpi/ # Adaptive + themed launcher icon, notification icon
│       │       └── xml/locales_config.xml
│       └── test/java/com/phoneguard/        # JUnit tests (70 tests)
├── gradle/libs.versions.toml       # Version catalog
├── gradle/wrapper/                 # Gradle 8.11.1 wrapper
├── build.gradle.kts, settings.gradle.kts, gradle.properties
└── local.properties.example
```

---

## Setup

### Requirements

- Android Studio Ladybug (2024.2) or newer, which bundles JDK 17 or newer.
- Android SDK Platform 35 (Android Studio installs it on first sync).
- A phone or emulator running Android 8.0 (API 26) or newer.

### 1. Open the project

Open the project folder in Android Studio and let Gradle sync. Android Studio creates
`local.properties` with your `sdk.dir`.

From the command line, create `local.properties` from the example instead:

```bash
cp local.properties.example local.properties   # then set sdk.dir
```

### 2. Add the Google Safe Browsing API key (optional but recommended)

Without a key the app still works: links are checked with the on-device rules, and the
result says "Google Safe Browsing check is not set up".

1. Go to the [Google Cloud Console](https://console.cloud.google.com/) and create a project
   (or pick an existing one).
2. Open **APIs & Services → Library**, search for **Safe Browsing API** and click **Enable**.
3. Open **APIs & Services → Credentials → Create credentials → API key**.
4. Click the new key and, under **API restrictions**, choose **Restrict key** and select
   only **Safe Browsing API**.
5. Add the key to `local.properties` in the project root:

   ```properties
   SAFE_BROWSING_API_KEY=AIza...your-key...
   ```

6. Sync Gradle and rebuild. The key is injected into `BuildConfig.SAFE_BROWSING_API_KEY` at
   build time. It is never committed: `local.properties` is git-ignored. On CI you can set an
   environment variable with the same name instead.

> **Note:** The Safe Browsing Lookup API (v4) is free but meant for non-commercial use. For a
> commercial release, Google recommends the
> [Web Risk API](https://cloud.google.com/web-risk). `HttpSafeBrowsingClient` sits behind the
> `SafeBrowsingClient` interface so it can be swapped out.

### 3. Build and run

In Android Studio, pick a device and press **Run ▶**. From the command line:

```bash
./gradlew installDebug          # build and install on a connected device
./gradlew testDebugUnitTest     # run the unit tests
./gradlew assembleRelease       # minified release build (sign it before publishing)
```

---

## What you should see (phase by phase)

The project was built in eight phases. This is how to check each one in the running app.

**Phase 1: setup, theme, navigation.**
Launch the app. On first launch you see onboarding (phase 7). After it, there is a bottom bar
with **Home, Scan, Tools, History, Tips**. Switching tabs cross-fades, and detail screens slide
in. Toggle system dark mode: the app switches between a deep-navy dark theme and a light
theme. On Android 12+ it also follows your wallpaper colours (dynamic colour).

**Phase 2: app risk scanner and results.**
Tap **Scan**, then **Scan now**. When the scan finishes, results are grouped as **Fix these
now / Worth checking / Minor things / All good**, with filter chips at the top. Each card has
a coloured stripe and chip, expands to show reasons, and has a **Fix** button (Uninstall /
Review permissions / Turn off access) plus **Details**. Details opens the app screen with
its icon, risk badge, install source, sensitive-permission list with icons, and
**Open settings** / **Uninstall** buttons. To test: install AnyDesk from the Play Store. It
shows as **High risk** with a red scam warning.

**Phase 3: device settings and Fix buttons.**
Turn on Developer options and USB debugging, then scan again. Both appear as issues; **Turn
off** opens Developer options directly. With no screen lock you get a **High risk** item and
**Set a screen lock** opens the lock-screen setup. Come back to the app and the results
refresh by themselves.

**Phase 4: security score and home gauge.**
On **Home**, tap **Scan now**. The button pulses with rings and shows live text ("Checking
apps… 42/87"). When it finishes, the circular gauge animates from 0 to your score in
red → amber → green, with a grade (Good / Okay / At risk). The four cards below (Apps, Device,
Network, Links) show the breakdown with mini score bars.

**Phase 5: Wi-Fi and link/QR checker.**
**Tools → Wi-Fi safety check**: on an open hotspot you see a red "This Wi-Fi is not safe"
card. **Tools → Check a link** and paste `http://sbi-kyc-update.xyz/verify`: you get a red
**Dangerous** verdict with reasons ("Pretends to be SBI…", "Not secure (no https)…").
**Scan QR** opens the camera, explains the permission first, and checks the QR's link.
You can also share an SMS from your messages app to PhoneGuard.

**Phase 6: history, scheduled scans and notifications.**
**History** shows a score-over-time chart, latest/best/change stats and every past scan.
To trigger the new-app alert, install any sideloaded APK that reads SMS (or AnyDesk). You
get a notification "New app may be risky", and tapping it opens that app's details. The
daily scan runs automatically. To run it now while debugging, use Android Studio's
**App Inspection → Background Task Inspector**.

**Phase 7: onboarding, tips, Hindi.**
Clear app data to see the three onboarding pages again, then the permissions screen that
explains each permission. **Tips** lists common Indian scams with "how to stay safe" steps and
a **Call 1930** button. Switch the phone (or, on Android 13+, just the app) to Hindi and
every screen appears in Hindi.

**Phase 8: tests.**
`./gradlew testDebugUnitTest` runs 70 tests covering risk scoring, the URL red-flag checker,
scam-phrase detection, device-setting evaluation, Wi-Fi parsing, the link scanner and the
scan repository's "notify only about new risks" logic.

---

## Permissions and Play Store policy

| Permission | Why | When requested |
|---|---|---|
| `QUERY_ALL_PACKAGES` | Core feature: scanning all installed apps (Play's "device security" permitted use). The app list never leaves the phone. | Install time (normal permission) |
| `POST_NOTIFICATIONS` | Alerts about new risky apps | Permissions screen (Android 13+) |
| `ACCESS_FINE/COARSE_LOCATION` (`maxSdkVersion=30`) | Android 11 and below only reveal Wi-Fi security to apps with location access | Wi-Fi screen, after an explanation (Android 8–11 only) |
| `CAMERA` | QR scanning | When the QR scanner opens, after an explanation |
| `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE` | Safe Browsing lookups, Wi-Fi check | Install time |
| `REQUEST_DELETE_PACKAGES` | The "Uninstall" button opens the system uninstall dialog | Install time (normal permission) |

`READ_SMS` and call-log permissions are **not** used. To check a message, users paste it into
the app or share it to PhoneGuard.

When you publish, fill in the Play Console **Permissions declaration** for `QUERY_ALL_PACKAGES`
using the comment in `AndroidManifest.xml`, and describe the Safe Browsing link check in the
**Data safety** form ("URLs, sent only when the user checks a link, not linked to identity").

---

## How the risk score works

**Per app** (`AppRiskScorer`): each sensitive capability adds points: SMS 25, call log 25,
accessibility 30, device admin 25, notification access 20, overlay 15, install apps 15,
mic 10, camera 8, location 8. Apps from a trusted store get ×0.6, and sideloaded apps get +20.
The point thresholds are Low ≥ 1, Medium ≥ 25 and High ≥ 60. Two rules always give **High**:
any remote-control app, and any sideloaded app with SMS, call log, accessibility, device
admin or notification access (the usual way banking trojans steal OTPs).

**Overall** (`SecurityScoreCalculator`): each category starts at 100.

- Apps: −25 per High, −10 per Medium and −2 per Low app, with Low capped at −10 in total.
- Device: no screen lock −40, USB debugging −20, developer options −10, install-unknown-apps
  −5/−15, old security patch −5/−15/−25.
- Network: open or WEP Wi-Fi scores 40.
- Links: −15 for each dangerous link checked in the last 7 days.

The overall score is the weighted average, capped at 59 while anything is High.

---

## Notes and known limits

- **New-app detection**: Android 8+ does not deliver `PACKAGE_ADDED` to receivers declared in
  the manifest, so `PackageAddedReceiver` is registered at runtime. Apps installed while
  PhoneGuard's process is not running are caught by the next daily scan.
- **Install unknown apps**: Android usually does not let one app read whether *another* app is
  allowed to install apps. PhoneGuard tries, and if it cannot tell, it lists the apps that asked
  for the ability as a Low-risk item.
- **Play Protect**: Android has no public API to read its state, so the app shows a hint and a
  button that opens Play Protect.
- **Adding a language**: copy `values/strings.xml` to `values-<code>/strings.xml`, translate
  it, and add the locale to `res/xml/locales_config.xml`.
