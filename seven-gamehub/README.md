# SevenX

Standalone Android GameHub for organizing Windows EXE applications and installers.

## Current foundation

- Dark SevenX-themed Jetpack Compose interface.
- Persistent local library for selected EXE files and installers.
- Android Storage Access Framework picker with persisted read permissions.
- Register as an Android "Open with" handler for .exe files shared by file managers.
- Validate the .exe extension, DOS MZ signature, and PE signature at the declared PE offset.
- Copy externally opened EXE files into SevenX private storage so imports survive temporary URI grants.
- Device diagnostics for supported CPU ABI, Android-reported Vulkan features, and total RAM.
- Remove entries from the SevenX library without deleting original files.
- Detect likely installer filenames such as setup.exe and installer.exe.
- Remove a SETUP entry only after a future runtime confirms that the installer process successfully started. The EXE file itself is never deleted by this cleanup policy.
- Request a home-screen shortcut for each library entry.
- Dedicated Windows runtime engine API separated from the library UI.

## Build SevenX

With JDK 17, Android SDK 35 and Gradle 8.9 installed:

```bash
gradle :app:assembleDebug
```

The APK is created at `app/build/outputs/apk/debug/app-debug.apk`.

## Runtime integration status

The Android source of Winlator is now pinned as a Git submodule at `winlator-source`, and CI builds a separate reference APK from that exact upstream revision. This makes the real Wine/Box64 source and native build part of the project workflow.

**This is not yet a working EXE bridge.** Winlator's display Activity is private to its own app, and its exported launcher Activity has no documented public action for launching an arbitrary EXE from SevenX. The separate reference APK therefore does not mean SevenX can already run EXE files. A SevenX-specific runtime bridge, container/prefix lifecycle, process-start callback, and physical-device validation are still required.

See [ENGINE_INTEGRATION.md](ENGINE_INTEGRATION.md) for the pinned source, exact Android integration boundary, licensing notes, and next implementation steps.

No EXE is considered runnable until the bridge is implemented and tested on a physical device.
