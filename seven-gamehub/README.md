# Seven GameHub

Standalone Android app foundation for a Windows-game launcher.

## Current foundation

- Dark Seven-themed Jetpack Compose interface.
- Game library and settings screens.
- Android document picker for selecting local EXE files.
- Selected EXE names appear in the in-memory library.
- Explicit placeholder state for the not-yet-integrated Wine/Proton runtime.

## Build

With JDK 17 and Android SDK 35 installed:

```bash
gradle :app:assembleDebug
```

The APK is created at `app/build/outputs/apk/debug/app-debug.apk`.

## Runtime status

This is the UI and Android project foundation only. It does **not** execute Windows programs yet. The next stage is integrating a legally distributable Android-compatible Wine runtime, then implementing per-game prefixes, process launch, graphics backends, and logs.
