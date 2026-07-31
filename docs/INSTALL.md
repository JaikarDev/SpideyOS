# SpideyOS Install Guide

## Device requirements

- Android **8.0+** (API 26) — works on **OnePlus, Samsung, Oppo, Vivo, Realme, Redmi, Xiaomi, Poco, Lava**, and others
- Phone or tablet — UI **auto-adapts** to size and resolution (notch / punch-hole safe)
- Snapdragon or MediaTek both supported
- ~50 MB free storage

## Ready-built APK (this PC)

Copy to your phone and install:

`C:\Users\win\Desktop\Spiderman For android phone\dist\SpideyOS-debug.apk`

## Sideload APK (users)

1. Open the APK on the phone → **Install** (allow unknown apps if asked)
2. Launch **SpideyOS**
3. Enter your name (default demo: Jaikar)
4. Paste Gemini API key (optional but recommended)
5. Tap **Enable Notification Access** → allow SpideyOS
6. Tap **Enable Overlay** → allow “Display over other apps”
7. (Optional) Home button → **Spidey Launcher** → Always

## Developer USB install

1. Enable **Developer options** → **USB debugging**
2. Connect phone via USB
3. From project root:

```bash
./gradlew assembleDebug
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Gemini API key

1. Create a key in Google AI Studio
2. Add to `local.properties` when building, **or** paste in SpideyOS Settings on device
3. Never commit real keys to git

## Gmail / IMAP (live mail)

1. Turn on 2FA on Google Account
2. Create an **App password**
3. In SpideyOS Settings → IMAP host `imap.gmail.com`, your Gmail, app password → Save
4. Open Mail Digest → **Fetch live IMAP / Gmail**
5. Spidey announces: “Spidey here — Jaikar, you’ve got mail!”

## Troubleshooting

| Issue | Fix |
|-------|-----|
| App won’t install | Allow “Install unknown apps” for your browser/Files |
| No Spidey notifications | Re-enable Notification access for SpideyOS |
| Floating Spidey missing | Grant Appear on top / overlay permission |
| Gemini silent | Check API key in Settings / network |
| Launcher not showing | Clear defaults for old launcher; set Spidey Launcher |

## Privacy

Notification content is processed on-device to rewrite titles. Gemini chat sends prompts you type (and optional context you enable) to Google’s API per their terms.
