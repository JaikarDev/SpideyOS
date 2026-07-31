# SpideyOS Install Guide

## Device requirements

- Android **15** (API 35) or newer
- Phone or tablet (Snapdragon or MediaTek supported)
- ~50 MB free storage

## Sideload APK (users)

1. Download `SpideyOS-vX.Y.Z.apk` from GitHub **Releases**
2. On your phone: open the APK → **Install**
3. Launch **SpideyOS**
4. Enter your name (default demo: Jaikar)
5. Paste Gemini API key (optional but recommended)
6. Tap **Enable Notification Access** → allow SpideyOS
7. Tap **Enable Overlay** → allow “Display over other apps”
8. (Optional) Home button → **Spidey Launcher** → Always

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
