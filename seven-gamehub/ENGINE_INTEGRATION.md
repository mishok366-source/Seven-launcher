# SevenX Windows Runtime Integration

Status: runtime source pinned and a separate reference APK build is being added to CI. The runtime is **not yet callable directly from SevenX**.

## Pinned runtime source

SevenX tracks the Android application source from:

- Repository: https://github.com/brunodev85/winlator-app
- Pinned upstream commit: `a030f552f452158a2db64fdb32b490fa19c0b48d`
- License declared by upstream: LGPL-2.1
- Submodule path: `seven-gamehub/winlator-source`

The pinned source contains the Android app, its native CMake sources, and runtime assets. The CI pipeline builds it as a separate reference APK so native build and packaging failures become visible independently from SevenX's own APK.

## Important integration boundary

This is a separate Android application, not a library that SevenX can simply call in-process.

The inspected upstream manifest declares `com.winlator.MainActivity` exported, but `com.winlator.XServerDisplayActivity` is not exported. The upstream MainActivity currently handles its own navigation/container extras and does not expose a documented public action for launching an arbitrary EXE supplied by another app. Therefore, simply sending an Intent or a file URI from SevenX does not launch the selected EXE.

The CI reference APK is an integration/build milestone only. It must not be described as an embedded SevenX runtime or as proof that SevenX can execute EXE files.

## Required bridge work

1. Maintain a SevenX-specific, pinned patch/fork of the runtime rather than modifying upstream at build time without review.
2. Add a narrow, explicit Android bridge that accepts a content URI with a temporary read grant and validates/copies the selected EXE into a runtime-owned location.
3. Select or create a container/prefix and generate a runtime shortcut for the requested executable.
4. Start the runtime display/process lifecycle through runtime-owned code; do not attempt to launch the non-exported display Activity from SevenX.
5. Return an explicit callback/result only after the Windows process has actually been created. An Activity opening or a request being accepted is not a successful launch.
6. Keep source EXEs untouched. Remove a SETUP entry from the SevenX library only after a confirmed process-start callback; never delete the underlying file.
7. Test first with a small, user-provided Windows desktop executable on physical ARM64 Android hardware.

## Existing SevenX contract

`WindowsRuntimeEngine` remains the only boundary between the library UI and the runtime. Its `STARTED` result is reserved for confirmed process creation. Until the bridge and callback exist, SevenX must continue to report the runtime as unavailable and must not auto-remove installer entries.

## Licensing and redistribution

The upstream application repository declares LGPL-2.1. The pinned source includes runtime assets and native components whose individual licenses and notices must be reviewed before distributing a combined or modified package. Preserve upstream notices and satisfy applicable source and relinking obligations. This engineering note is not legal advice.

## Device validation

A successful CI build proves only that the source compiles for the configured Android toolchain. It does not prove compatibility with a particular phone, GPU driver, Wine prefix, or Windows application. Physical-device tests remain required.
