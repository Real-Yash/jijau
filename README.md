# Jijau UdyamSuite Android wrapper

A minimal Android WebView app for:

https://jijau.udyamsuite.com

## Build in Android Studio

1. Open this folder in Android Studio.
2. Allow Gradle sync to finish.
3. Build > Build APK(s).
4. APK will appear under `app/build/outputs/apk/debug/app-debug.apk`.

## Command line

If Android SDK + Gradle are installed:

```bash
gradle assembleDebug
```

For production distribution, create a release keystore and build a signed release APK/AAB.

## Build APK with GitHub Actions

Push this project to the `main` branch of a GitHub repository. Open **Actions → Build Android APK → Run workflow**. After the build finishes, download the `Jijau-UdyamSuite-APK` artifact. It contains `app-debug.apk`.
# jijau
