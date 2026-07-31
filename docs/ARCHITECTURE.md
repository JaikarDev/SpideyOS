# SpideyOS Architecture

## Overview

SpideyOS is an Android 15+ (API 35) companion suite distributed as a single APK. It does **not** replace the full system ROM. It provides:

| Module | Role |
|--------|------|
| Spidey Launcher | Home screen, app grid, web-themed chrome |
| Spidey Assistant | Floating overlay + Gemini persona |
| Notification Rewriter | `NotificationListenerService` → Spidey-voiced alerts |
| Web Messages | Themed messaging UI with web-bubble animations |
| Mail Digest | Demo inbox + live IMAP/Gmail fetch → Spidey announcements |
| Peter Camera | CameraX UI with comic/web overlays |

## High-level flow

```
User → Spidey Launcher
         ├─→ Opens apps / modules
         └─→ Spidey Assistant Overlay ──→ Gemini API
System notifications → NotificationListener → Spidey voice copy → Status bar / overlay
Mail / SMS events → Digest / Messages modules → Spidey announcements
IMAP (Gmail app password) → ImapMailFetcher → Mail Digest → Spidey voice
Camera → CameraX pipeline → Spidey photo reactions
```

## Why this architecture

- **Works on all Android 15+ devices** without root or custom ROM
- **Sideloadable** via GitHub Releases
- Modules share one theme system (red/blue web tokens) and one user profile (display name default: Jaikar)
- Snapdragon and MediaTek both run standard ART + Compose; performance via baseline profiles and lightweight animation

## Key Android components

- `MainActivity` — Compose host / launcher entry (`HOME` category)
- `SpideyOverlayService` — system overlay for floating assistant
- `SpideyNotificationListener` — notification access
- `GeminiClient` — HTTP / SDK calls with Spidey system prompt
- `SettingsDataStore` — name, API key flag, theme intensity, module toggles

## Security & privacy

- Notification access and overlay permission are optional and explained in onboarding
- Gemini API key stored in local encrypted prefs / BuildConfig for debug; never commit secrets
- Unofficial fan project disclaimer in README and About screen

## Trademark

SpideyOS is an unofficial, fan-inspired project and is **not affiliated with Marvel Entertainment or Sony**.
