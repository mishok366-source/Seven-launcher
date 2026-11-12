# SevenX and SevenX Desktop integration

## Product boundary

SevenX is the game manager. It owns the persistent game library, EXE/SETUP imports, shortcuts, launch history, favorites, and per-game launch profiles.

SevenX Desktop is the Windows environment, built from the pinned Winlator source. It owns the Wine/Box64 runtime, containers, desktop session, Windows application shortcuts, file manager, graphics/audio/input configuration, and low-level compatibility settings.

Do not duplicate Wine/Box64, DXVK/VKD3D, graphics-driver, audio-driver, or container-engine settings in SevenX. SevenX should expose game-specific overrides and library management only; SevenX Desktop remains the source of truth for runtime-wide settings.

## Desktop and application launch

The maintained CI patch:
1. Rebrands the Winlator companion application as **SevenX Desktop** and applies a separate dark-purple theme.
2. Adds a **Рабочий стол** entry to the runtime navigation.
3. Starts a Wine desktop session in the selected container without an application-specific `exec_path`.
4. Renames the shortcut list entry to **Приложения**; the existing Windows shortcut and file-manager workflows remain available.
5. Keeps the original container and engine settings screens in SevenX Desktop.

The desktop entry uses the same XServer/Wine environment as normal application launches. It is not a separate emulator or a fake desktop screenshot.

## SevenX-to-runtime launch flow

1. SevenX shares an imported EXE using a FileProvider content URI and a temporary read grant.
2. SevenX supplies a one-shot PendingIntent callback.
3. The runtime bridge validates the request and lets the user choose an existing container.
4. The bridge copies the EXE into that container's `.wine/drive_c/SevenX` directory.
5. The bridge starts Winlator's own `XServerDisplayActivity` with its existing `exec_path` flow.
6. `GuestProgramLauncherComponent` reports whether its Wine/Box64 launcher process was created.
7. SevenX removes an installer entry from its library only after a positive callback. The imported file is never deleted.

## Important boundaries

- The callback confirms creation of Winlator's launcher process, not that the Windows application's main window appeared or that the installer completed successfully.
- A Winlator container must exist before launching games. The runtime's desktop menu can be used to create and configure one.
- The runtime is a separate APK, not merged into SevenX.
- CI compilation is not physical-device validation. GPU drivers, CPU translation, RAM, Android version, and application-specific dependencies can still prevent a program from working.
- Winlator source and bundled components retain their upstream licenses. Preserve required notices and corresponding source when distributing builds.

## Validation checklist

- [ ] SevenX manager APK builds.
- [ ] SevenX Desktop runtime APK builds.
- [ ] Both APKs install on an ARM64 Android device.
- [ ] SevenX Desktop opens its redesigned UI.
- [ ] Desktop menu starts a Wine desktop in a selected container.
- [ ] Windows shortcut applications and file manager open.
- [ ] SevenX can pass a content URI and read grant to the runtime.
- [ ] Container selection and EXE copy work.
- [ ] Wine/Box64 launcher callback is delivered.
- [ ] SETUP library entry is removed only after a positive callback.
- [ ] Original imported EXE remains on the device.
- [ ] Test representative 32-bit and 64-bit applications and graphics backends.
