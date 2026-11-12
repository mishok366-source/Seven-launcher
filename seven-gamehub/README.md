# SevenX

Standalone Android GameHub for organizing Windows EXE applications and installers.

## Current foundation

- Dark SevenX-themed Jetpack Compose interface.
- Persistent local library for selected EXE files and SETUP installers.
- Android Storage Access Framework picker with persisted read permissions.
- Register as an Android `Open with` handler for `.exe` files shared by file managers.
- Validate the `.exe` extension, the DOS `MZ` signature, and the `PE\0\0` signature at the DOS header's `e_lfanew` offset before importing. Malformed or truncated headers and unreasonable PE offsets are rejected.
- Copy externally opened EXE files into SevenX private storage so the import survives temporary URI grants.
- Display the device's primary supported CPU ABI in Settings.
- Inspect all supported ABIs and report whether ARM64 is available.
- Detect Android-reported Vulkan hardware level/version features and total device RAM for runtime compatibility diagnostics.
- Show a concise device compatibility summary in Settings without claiming that Windows execution is available.
- Remove entries from the SevenX library without deleting the original files.
- Detect likely installer filenames such as `setup.exe` and `installer.exe` when importing.
- Remove a SETUP entry from the SevenX library only after the Windows runtime reports that the installer process successfully started. The EXE file is never deleted by this cleanup policy.
- Keep the installer in the library when the runtime is unavailable or launch fails.
- Request a home-screen shortcut for each library entry.
- A pinned shortcut opens SevenX and resolves the selected library entry.
- Separate Games and Settings screens.
- A dedicated Windows runtime engine API, separated from the library UI.
- Live runtime availability messaging in Settings.
- An integration and licensing checklist for the planned Wine + Box64 backend.

## Build

With JDK 17 and Android SDK 35 installed:

```bash
gradle :app:assembleDebug
```

The APK is created at `app/build/outputs/apk/debug/app-debug.apk`.

## Windows runtime status

SevenX does **not yet execute Windows programs**. The Run action reports that the runtime is not connected. The 0.5.0 build adds device-side runtime diagnostics (ABI, Android-reported Vulkan features, and total RAM) and keeps the Windows runtime behind a dedicated engine API. The launch-result contract removes installer entries from the library only after a confirmed successful process start. Since Wine + Box64 are not integrated yet, the current runtime always reports unavailable and no installer entry is automatically removed. SevenX can now receive EXE files through Android's `ACTION_VIEW` flow, validate the DOS header and the PE signature at the declared PE offset, and import a private copy. This is file intake, not execution. The diagnostics are preliminary capability indicators only; they do not guarantee Wine/Box64 compatibility or graphics performance.

A real Android-compatible Wine environment is still required, together with the matching x86/x64 translation layer (for example, Box64 on supported ARM64 devices), runtime asset installation, per-app prefixes/containers, graphics and audio integration, process lifecycle handling, and useful error logs. The Winlator project is a technical reference, but its runtime is not embedded or invoked by SevenX. Its application source declares LGPL-2.1 and the project uses separate submodules and third-party components; every component must be reviewed independently before reuse or redistribution. See [ENGINE_INTEGRATION.md](ENGINE_INTEGRATION.md) for the current architecture and licensing checklist.

No EXE is considered runnable until a runtime is actually integrated and the app is tested on a physical device.

The intended first compatibility checks are small, ordinary Windows desktop applications before attempting larger launchers or games. Compatibility depends on the device CPU, Android version, GPU/driver support, and each application's requirements. No EXE launch is considered supported until it has been tested on-device.
