# SmartTrace — Development & release notes

## Stack
Kotlin · Jetpack Compose (Material 3) · AppCompat per-app language · `minSdk 26` · `compileSdk/targetSdk 36` (Google Play requires API 36 for new apps and updates since 31 Aug 2026).

| Area | Implementation |
|---|---|
| Offline text | ML Kit Text Recognition (Latin + Chinese, bundled models) with 4-orientation retry |
| Codes | ML Kit Barcode Scanning (bundled) → GS1 check digit, country prefix, GS1 Digital Link, URL risk flags |
| Visual hints | ML Kit Image Labeling (bundled), weak category hint only |
| AI (optional) | Gemini REST `generateContent`, model `gemini-3.5-flash-lite` (editable in *Settings → Gemini API keys*) |
| Keys | AES-256-GCM key in Android Keystore, masked display (first 3 … last 3), tried in order with cool-down on 429/403 |
| Verification | Open Food Facts / Beauty / Products Facts, UPCitemdb trial (only the barcode digits are sent) |
| Scoring | `verify/RiskEngine.kt` — rule-based, never claims "genuine" |
| TTS | `TextToSpeech`, default 2×, up to 4× (some engines cap the rate), paragraph highlight |
| Updates | Play In-App Update, **Flexible** (`InAppUpdate.kt`) |
| Ads | AdMob adaptive banners (history + settings only) + UMP consent + "Ad privacy choices" |
| Storage | SQLite history + private JPEG thumbnails. No Room/KSP, no network DB |
| Icons | All hand-drawn vectors in `res/drawable/ic_*.xml` (no icon library) |

Pipeline: `AnalysisPipeline.kt` → prepare image → barcodes → (OCR + parser + hints **or** Gemini) → verify codes → `RiskEngine`.
Layer 7 (learning/enrolling products) is intentionally **not** implemented.

## Build
```bash
# Android Studio (Ladybug+/Meta) opens the project directly.
# CLI: install Gradle 8.11.1 once, then
gradle :app:assembleDebug
# optional: generate the wrapper jar
gradle wrapper --gradle-version 8.11.1
```
JDK 17 is required. The CI workflow uses `gradle/actions/setup-gradle` so no wrapper jar is needed.

## GitHub setup (auto build)
1. Create the repo and push everything (`.github/workflows/android.yml` runs on push/PR/tag).
2. **Pages:** *Settings → Pages → Source = GitHub Actions*. The privacy policy then lives at
   `https://<user>.github.io/<repo>/privacy-policy.html` — CI injects this URL into the app (`PRIVACY_URL`). Use the same URL in Play Console.
3. **Signing (optional, needed for the AAB):** create a keystore and store it as secrets:
   ```bash
   keytool -genkeypair -v -keystore release.jks -alias smarttrace -keyalg RSA -keysize 2048 -validity 10000
   base64 -w0 release.jks     # paste into KEYSTORE_BASE64
   ```
   Secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`, `ADMOB_APP_ID`, `ADMOB_BANNER_ID`.
   Without them CI still builds + lints the debug APK. Push a tag `v1.0.0` to publish a GitHub Release with APK + AAB.
4. Keep the keystore safe. Enrol in **Play App Signing** when you upload the first AAB.

## AdMob
Default ids are Google's **test** ids (debug and release) so nothing real is shown by accident.
Put your real ids in the secrets above (or `-PADMOB_APP_ID=… -PADMOB_BANNER_ID=…`). Create the consent message in
*AdMob → Privacy & messaging* (GDPR + US states) so the UMP form appears.

## Play Console checklist
- Target API 36 ✔, 64-bit ✔, AAB ✔, `bundle { language { enableSplit = false } }` keeps all 8 languages for the in-app switch.
- Store listing: copy from `README.md`; icon `store/icon-512.png`; feature graphic `store/feature-graphic-1024x500.png`; screenshots in `store/screenshots/<lang>/`.
  **Replace the mock-up screenshots with real ones taken on a device/emulator before publishing** (Play requires screenshots that show the real app).
- Privacy policy URL (above). Data safety: see `docs/DATA_SAFETY.md`.
- Ads declaration: *Contains ads = Yes*. Target audience: 18+ / not for children. Content rating questionnaire: utility app.
- Test In-App Update with an internal-testing build installed from Play (it does nothing for sideloaded APKs).
- Third-party brands: do not use brand names/logos in listing text or screenshots; the sample screenshots use neutral names.

## Things to know
- **Not compiled in the authoring environment** (no network). The first CI run is the real compile; send the log if anything fails.
- Gemini model id/limits change over time — edit the model name in the app (*Settings → Gemini API keys → Model*) without a new release.
- Free Gemini quota is limited and Google's terms apply; keys belong to the user, never to the app.
- Chinese OCR model adds a few MB to the APK. To slim it, switch to the unbundled model (needs Google Play services model download).
- Open product databases are incomplete: "not found" is shown as *info*, never as a fake signal.
- The Vietnamese sub-label rule (imported goods) is shown only when the UI language is Vietnamese. Have it reviewed against current Vietnamese labelling regulations before relying on it.
