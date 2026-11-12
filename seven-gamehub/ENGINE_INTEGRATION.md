# SevenX runtime integration

## Architecture

SevenX remains the library and launcher UI. Winlator is built as a separate companion APK because upstream Winlator's XServer display Activity is not exported for third-party callers.

The pinned Winlator source is patched during CI by `scripts/apply-sevenx-winlator-bridge.py`. The patch adds an exported, narrowly scoped `com.seven.gamehub.action.RUN_EXE` entry point.

## Launch flow

1. SevenX shares an imported EXE using a FileProvider content URI and a temporary read grant.
2. SevenX supplies a one-shot PendingIntent callback.
3. The Winlator bridge validates the request and lets the user choose an existing container.
4. The bridge copies the EXE into that container's `.wine/drive_c/SevenX` directory. It does not modify or delete the original SevenX import.
5. The bridge starts Winlator's own `XServerDisplayActivity` with its existing `exec_path` flow.
6. `GuestProgramLauncherComponent` reports whether its Wine/Box64 launcher process was created.
7. SevenX removes an installer entry from the library only after a positive callback. Failed or unconfirmed launches leave the entry intact.

## Important boundaries

- The callback confirms creation of Winlator's launcher process, not that the Windows application's main window appeared or that the installer completed successfully.
- If no Winlator container exists, the user must create one in Winlator and retry.
- The runtime is a separate APK; it is not merged into the SevenX APK.
- CI compilation is not physical-device validation. GPU drivers, CPU translation, RAM, Android version, and application-specific dependencies can still prevent a program from working.
- Winlator source and bundled components retain their upstream licenses. Review and preserve required notices and corresponding source when distributing builds.

## Validation checklist

- [ ] SevenX debug APK builds.
- [ ] SevenX-enabled Winlator APK builds.
- [ ] Both APKs install on an ARM64 Android device.
- [ ] SevenX can pass a content URI and read grant to Winlator.
- [ ] Container selection and file copy work.
- [ ] Wine/Box64 launcher callback is delivered.
- [ ] SETUP library entry is removed only after a positive callback.
- [ ] Original imported EXE remains on the device.
- [ ] Test representative 32-bit and 64-bit Windows applications and graphics backends.
