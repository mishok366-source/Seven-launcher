# SevenX 0.10.0 — Installer Launch & Compatibility

## Release objective

Allow the Game Manager to hand Windows EXE installers to SevenX Desktop, then make installed applications available as launchable library entries. Validate the hand-off separately from general application compatibility.

## Installer formats

- EXE: initial supported hand-off target. Preserve the original Android file and stage a copy in the selected Wine container.
- MSI: unsupported until an explicit `msiexec /i` path is implemented and tested.
- Other extensions: reject with a clear unsupported-format message rather than passing them to Wine based on the filename alone.

## Required user flow

1. Import an installer without moving or deleting the original device file.
2. Tap Install/Run.
3. Choose an existing SevenX Desktop Wine container.
4. Stage a copy in the managed container directory and request launch through Winlator.
5. Report process-start success separately from installer completion.
6. Keep the original Android file untouched when removing the installer from the SevenX library.

## Engineering requirements

- Use a versioned Game Manager ↔ Desktop intent contract.
- Pass content URIs with temporary read permission; do not assume paths are shared across app sandboxes.
- Validate URI, extension, readability, available storage, container selection and launch result.
- Return actionable errors for unsupported format, missing container, inaccessible URI, insufficient storage or launch failure.
- Never delete the original imported file during staging or library cleanup.
- Do not claim all installers work; compatibility depends on guest architecture, Wine/Box64, runtimes, graphics and application-specific protections.

## Follow-up compatibility milestone

The 0.11.0 target is a practical 6–8 GiB installed setup with optional on-demand packs. See [COMPATIBILITY_PACKS_0.11.0.md](COMPATIBILITY_PACKS_0.11.0.md). VC++, .NET and WebView2 are not considered supported until their installation paths and real device tests are complete.

## Release gates

- [ ] Game Manager hands off an imported EXE using a content URI.
- [ ] Desktop selects a container and stages a copy without deleting the original.
- [ ] Wine process start/failure is reported accurately.
- [ ] MSI remains explicitly unsupported until a tested route exists.
- [ ] Both APKs compile in CI.
- [ ] At least one known simple EXE installer is tested on physical ARM64 Android.
- [ ] Record device, Android, Wine/Box64 versions and observed result.

CI compilation alone does not prove an installer completed or the installed program works.
