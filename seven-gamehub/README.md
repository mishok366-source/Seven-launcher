# SevenX

Standalone Android GameHub for organizing Windows EXE applications and installers.

## Current foundation

- Dark SevenX-themed Jetpack Compose interface.
- Persistent local library for selected EXE files and SETUP installers.
- Android Storage Access Framework picker with persisted read permissions.
- Remove entries from the SevenX library without deleting the original files.
- Request a home-screen shortcut for each library entry.
- A pinned shortcut opens SevenX and resolves the selected library entry.
- Separate Games and Settings screens.
- Explicit status messaging while the Windows compatibility runtime is not yet installed.

## Build

With JDK 17 and Android SDK 35 installed:

```bash
gradle :app:assembleDebug
```

The APK is created at `app/build/outputs/apk/debug/app-debug.apk`.

## Windows runtime status

SevenX does **not yet execute Windows programs**. The Run action currently reports that the runtime is not connected. The next development stage is a real Android-compatible Wine environment with the required x86/x64 translation layer, per-app launch configuration, and useful error logs.

The intended first compatibility checks are small, ordinary Windows desktop applications before attempting larger launchers or games. Compatibility depends on the device CPU, Android version, GPU/driver support, and each application's requirements. No EXE launch is considered supported until it has been tested on-device.
