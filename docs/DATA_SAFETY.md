# Google Play "Data safety" — draft answers

> Draft to help you fill in the form. **You are responsible for the final answers**; re-check them against Google's current
> AdMob / Gemini data-disclosure guides every time you change SDKs.

## Overview questions
| Question | Answer |
|---|---|
| Does the app collect or share required user data types? | Yes (advertising + optional features below) |
| Is all collected data encrypted in transit? | Yes (HTTPS) |
| Can users request data deletion? | Data is stored only on the device (uninstall / in-app delete). For Google-held data users use Google's tools. |
| Account creation | None |

## Data types
| Play category | Collected | Shared | Optional | Purpose | Notes |
|---|---|---|---|---|---|
| Device or other IDs (Advertising ID) | Yes | Yes (Google AdMob) | No | Advertising, fraud prevention, analytics | Collected by the AdMob SDK |
| Approximate location (IP-derived) | Yes | Yes (Google AdMob) | No | Advertising | Collected by the AdMob SDK |
| App interactions / diagnostics | Yes | Yes (Google AdMob) | No | Advertising, analytics | Collected by the AdMob SDK — verify against the current AdMob guide |
| Photos | Yes, only in Gemini mode | Yes (Google Gemini) | **Yes** | App functionality | Off by default; explicit consent dialog; user's own API key |
| Other app activity (barcode digits) | Yes, when "Check barcodes online" is on | Yes (open product databases) | **Yes** | App functionality | Only the digits; can be switched off |
| Personal info, financial info, health, messages, contacts, audio, files | No | – | – | – | |

Local-only data (history, thumbnails, settings, encrypted API keys) never leaves the device and is not "collected".

## Other declarations
- **Ads:** contains ads (Google AdMob). Consent via UMP.
- **Target audience:** 18+ / general audience, not designed for children.
- **Permissions:** `INTERNET`, `ACCESS_NETWORK_STATE`, `com.google.android.gms.permission.AD_ID` — no camera/location/storage permission.
- **Security practices:** data encrypted in transit; API keys encrypted at rest with Android Keystore.
