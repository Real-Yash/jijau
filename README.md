# Jijau UdyamSuite Android wrapper

A minimal Android WebView app for:

https://jijau.udyamsuite.com

## Build APK

Go to **GitHub → Actions → Build Jijau Android APK → Run workflow**.

Wait for the build to finish. Then go to **GitHub → Actions → completed build → Artifacts → Jijau-UdyamSuite-APK**.

Download the artifact ZIP and extract `Jijau-UdyamSuite.apk`. Install that APK on your Android phone.

Android may ask you to allow installation from unknown sources for the browser or file manager you use to open the APK. This is a debug-signed APK intended for testing.

## Replace the launcher icon

The committed launcher icon is a neutral placeholder, not a Jijau logo. Replace these resources when the final logo is available:

* `app/src/main/res/drawable/ic_launcher_foreground.xml`
* `app/src/main/res/drawable/ic_launcher_placeholder.xml`
* `app/src/main/res/mipmap-anydpi/ic_launcher.xml`
* `app/src/main/res/mipmap-anydpi/ic_launcher_round.xml`

Keep the existing `mipmap-anydpi-v26` adaptive icon XML files and point their foreground/background resources at the final supplied artwork.
# jijau
