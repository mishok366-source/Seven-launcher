# SevenX

Standalone Android GameHub for organizing Windows EXE applications and installers.

## Current foundation

- Dark SevenX-themed Jetpack Compose interface.
- Persistent local library for selected EXE files and SETUP installers.
- Android Storage Access Framework picker with persisted read permissions.
- Register as an Android `Open with` handler for `.exe` files shared by file managers.
- Validate the `.exe` extension and the Windows `MZ` executable header before importing.
- Copy externally opened EXE files into SevenX private storage so the import survives temporary URI grants.
- Display the device's primary supported CPU ABI in Settings.
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

SevenX does **not yet execute Windows programs**. The Run action still reports that the runtime is not connected. SevenX can now receive EXE files through Android's `ACTION_VIEW` flow, validate the basic PE/DOS header, and import a private copy. This is file intake, not execution.

A real Android-compatible Wine environment is still required, together with the matching x86/x64 translation layer (for example, Box64 on supported ARM64 devices), runtime asset installation, per-app prefixes/containers, graphics and audio integration, process lifecycle handling, and useful error logs. The Winlator project is a relevant technical reference, but its runtime is not embedded or invoked by SevenX yet. Its source and third-party components have their own licenses and must be reviewed before any code or binary redistribution.

No EXE is considered runnable until a runtime is actually integrated and the app is tested on a physical device.

The intended first compatibility checks are small, ordinary Windows desktop applications before attempting larger launchers or games. Compatibility depends on the device CPU, Android version, GPU/driver support, and each application's requirements. No EXE launch is considered supported until it has been tested on-device.
