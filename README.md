# SpideyOS

**Unofficial fan / inspired Android companion suite.** Not affiliated with Marvel Entertainment or Sony.

> What if your Android phone didn’t just *look* like Spidey — what if Spidey *lived* on it?

SpideyOS is an **Android 15+ (API 35)** experience:

- Spidey-themed **launcher**
- Floating **Spidey assistant** powered by **Gemini**
- Notifications in Spidey’s voice (“Hey Jaikar, you’ve got mail”)
- **Web-style messages**, **mail digest**, and **Peter Parker camera**

Works on phones and tablets across **OnePlus, Samsung, Oppo, Vivo, Realme, Redmi, Xiaomi, Poco, Lava** and more (Android **8+** / API 26, targets API 35). Layouts **auto-adapt** to screen size and resolution. **No root required.**

## Requirements

- Android **8.0+** (Oreo) — best experience on Android 12–15
- Optional: Gemini API key (for Spidey chat)
- Permissions: Notification access, Display over other apps (for full experience)

## Install (GitHub Release)

1. Download the latest APK from **Releases**
2. Allow install from that source
3. Open SpideyOS → complete onboarding
4. (Optional) Set **Spidey Launcher** as default Home app
5. Enable **Notification access** and **Appear on top**

### Build from source

```bash
# Needs Android Studio / SDK 35 + JDK 17
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Set your Gemini key in `local.properties`:

```properties
GEMINI_API_KEY=your_key_here
```

## Modules

| Module | Description |
|--------|-------------|
| Launcher | Web-themed home & app grid |
| Assistant | Floating Spidey + Gemini persona |
| Notifications | Spidey-voiced rewriter |
| Messages | Spider-web bubbles |
| Mail | Digest + Spidey alerts |
| Peter Camera | CameraX comic UI |

## Docs (LinkedIn-ready)

- [Case Study PDF](docs/SpideyOS-Case-Study.pdf)
- [LinkedIn posts](docs/LINKEDIN-POST.md)
- [Carousel script](docs/LINKEDIN-CAROUSEL.md)
- [Skills & impact](docs/SKILLS-AND-IMPACT.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Install guide](docs/INSTALL.md)

## Disclaimer

SpideyOS is an unofficial educational / portfolio project. Do not use copyrighted Marvel artwork. Replace placeholder art with original or licensed assets before public branding beyond fair-use parody/fan context.

## Author

**Jaikar** — built in public for portfolio & community.

## License

See [LICENSE](LICENSE).
