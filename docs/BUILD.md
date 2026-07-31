# SpideyOS — Build checklist (Windows)

## Option A — Android Studio (recommended)

1. Install [Android Studio](https://developer.android.com/studio) with SDK **35** and JDK **17+**
2. Open this folder as a project
3. Copy `local.properties.example` → `local.properties`
4. Set `sdk.dir` to your SDK (Studio usually writes this automatically)
5. Optional: `GEMINI_API_KEY=...`
6. Run **app** on a USB Android **15+** device (USB debugging on)

```powershell
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Option B — Command-line SDK

If Studio is not installed, install command-line tools into `%LOCALAPPDATA%\Android\Sdk`, then:

```powershell
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

Create `local.properties`:

```properties
sdk.dir=C:\\Users\\YOUR_USER\\AppData\\Local\\Android\\Sdk
GEMINI_API_KEY=
```

## After install on phone

1. Open SpideyOS → set name **Jaikar** (or yours)
2. Enable Notification access + Overlay
3. Optional: set as Home / default launcher
4. Settings → Gemini key + IMAP (Gmail app password)
5. Demo: Mail Digest → simulate notification → hear Spidey’s “you’ve got mail”

See [INSTALL.md](INSTALL.md) and LinkedIn kit in this `docs/` folder.
