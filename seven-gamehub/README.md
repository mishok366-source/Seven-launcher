# SevenX

SevenX is an Android game hub for importing Windows EXE files, keeping a persistent library, and creating Android home-screen shortcuts.

## Current development milestone: 0.7.0

- Dark Jetpack Compose interface.
- EXE and SETUP import with PE header validation.
- Persistent library; removing an entry never deletes the source file.
- Android home-screen shortcuts.
- Device diagnostics.
- SevenX-to-Winlator launch bridge is being built as a separate companion APK.
- SevenX passes a read-granted content URI to the companion runtime.
- Winlator asks the user to choose an existing container, copies the executable into that container, and launches it through its own XServer/Wine startup path.
- The runtime sends a one-shot callback only after its Wine/Box64 launcher process has been created. SevenX removes SETUP entries from the library only after this confirmation; the imported file is never deleted.

## Build outputs

The CI workflow builds two separate APKs:

- `sevenx-debug` — SevenX game hub.
- `winlator-sevenx-runtime-debug` — Winlator companion runtime with the SevenX bridge patch.

Install both APKs to use the bridge. The runtime still requires first-time setup and a configured Winlator container. A successful CI build does not prove compatibility on every device or with every Windows application.

## Device compatibility

The runtime uses Wine and Box64 and depends on the Android device's CPU, Vulkan/OpenGL drivers, memory, and the individual application's requirements. Some Windows applications will not work, even when the runtime installs correctly.

## Project status

The bridge is under CI validation. Physical-device testing is still required before describing EXE launching as production-ready.
