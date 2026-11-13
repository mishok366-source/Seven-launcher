# SevenX 0.10.0 — Installer Launch & Compatibility

## Release objective

Make the Game Manager launch Windows installer files through SevenX Desktop, then make the installed application available as a launchable library entry. This release must validate the entire handoff rather than treating a successful APK build as proof of runtime compatibility.

## Installer formats

- EXE: first-class target. Preserve the original file on Android; stage a copy inside the selected Wine container before execution.
- MSI: add only after an explicit, tested `msiexec /i` launch path is implemented.
- Other file extensions: do not pass to Wine based on filename alone. Show a clear unsupported-format message.

## Required user flow

1. Import an installer into the SevenX library without moving or deleting the original device file.
2. Tap Install/Run.
3. Choose an existing SevenX Desktop Wine container, or create one in SevenX Desktop.
4. Copy the installer into a managed container staging directory and launch it through Wine.
5. Return to SevenX after the installer exits; refresh the library and allow the user to choose the installed executable/shortcut.
6. Keep the original Android file untouched when removing the installer from the SevenX library.

## Engineering requirements

- Define and document a versioned Android intent contract between Game Manager and SevenX Desktop.
- Pass content URIs with temporary read permission; do not assume arbitrary filesystem paths are accessible across app sandboxes.
- Validate the URI, extension, file readability, available storage, container selection, and launch result.
- Avoid silently reporting success before the Wine process actually starts.
- Return actionable errors for missing container, unsupported format, inaccessible URI, insufficient storage, or runtime launch failure.
- Never delete the original imported file as part of staging, install, or library cleanup.
- Do not claim all installers work: compatibility depends on guest architecture, Wine/Box64, required runtimes, graphics and application-specific protections.

## Release gates

- [ ] Game Manager can hand off an imported EXE using a content URI.
- [ ] SevenX Desktop receives the URI and asks for a container when needed.
- [ ] EXE is staged without deleting the original Android file.
- [ ] Wine process start/failure is reported accurately to Game Manager.
- [ ] MSI has a separate verified launch path, or is clearly marked unsupported.
- [ ] Library removal does not delete the device file.
- [ ] Both APKs compile in CI.
- [ ] Physical-device test of at least one known, simple EXE installer on ARM64 Android.
- [ ] Document exact device, Android version, Wine/Box64 versions and test result.

## Known status at kickoff

The 0.9.0 runtime patch adds a SevenX bridge activity that accepts the `com.seven.gamehub.action.RUN_EXE` action with a content URI and stages an EXE into a selected container. However, end-to-end Game Manager invocation and device-side installer execution have not yet been proven. The bridge currently rejects non-EXE extensions. This release must close and test that gap; it must not advertise generic SETUP/MSI support prematurely.
