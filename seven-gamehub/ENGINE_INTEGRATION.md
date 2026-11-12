# SevenX Windows Engine Integration Notes

Status: architecture preparation only. No Wine, Box64, Mesa, DXVK or Windows process launcher is bundled by this change.

## Current boundary

The app module talks to `com.seven.gamehub.runtime.WindowsRuntimeEngine` through a small API:

- `status(context)` reports whether a compatible runtime is actually available.
- `launch(context, executable)` returns `STARTED` only after the backend has positively confirmed that the Windows process was created.
- `NOT_INSTALLED` and `FAILED` must never trigger installer cleanup.
- The library owns the entry metadata. The runtime must not delete user EXE files.
- Installer cleanup remains a library-only operation after a confirmed start.

The current `WindowsRuntime` implementation is deliberately unavailable. This is not an EXE execution engine.

## Technical reference reviewed

The Winlator project is a relevant Android reference implementation:

- Main project: https://github.com/brunodev85/winlator
- Android application source: https://github.com/brunodev85/winlator-app
- The main repository declares separate `app`, `vortek` and `gladio` Git submodules.
- Its README describes an Android stack based on Wine and Box86/Box64.
- The application source uses Android NDK/CMake native builds, targets ARM64 in its debug configuration, and includes runtime setup/assets and container management.

This confirms that a working implementation is substantially more than adding a dependency or launching another app with an Intent. SevenX needs a supported way to provision runtime assets, create/manage prefixes, invoke the process, display output, route input/audio/graphics, and collect exit/error information.

## Licensing and redistribution gate

The Winlator application repository declares LGPL-2.1. The main repository and its submodules, bundled runtime assets, native dependencies and third-party components must each be reviewed independently before copying, linking, packaging or redistributing anything.

Before importing upstream code or binaries:

1. Pin exact upstream commits and submodule revisions.
2. Inventory every source tree, native library, runtime asset and bundled executable.
3. Record each component's license, notices, source-offer/source-publication duties and redistribution conditions.
4. Check compatibility between the licenses of all bundled components.
5. Keep required notices and corresponding source available with any distributed build.
6. Prefer a clean adapter boundary and separately maintained components over copying the upstream UI/application wholesale.

This note is an engineering checklist, not legal advice. No upstream Winlator source or binary is copied into SevenX by this change.

## Planned implementation order

1. **Runtime discovery:** detect supported ABI and required native capabilities; report unsupported devices clearly.
2. **Asset provisioning:** download/install only verified, redistributable runtime packages with checksums, version pinning, cancellation and storage checks.
3. **Prefix/container lifecycle:** create, persist, repair and remove isolated Windows prefixes without touching imported source EXEs.
4. **First process launch:** launch a small, user-supplied Windows test application and confirm process creation/exit.
5. **Display and input:** provide a usable app surface, touch/mouse/keyboard mapping, orientation and resolution controls.
6. **Graphics/audio:** add compatible graphics and audio backends after basic process execution is stable.
7. **Installer lifecycle:** remove only the SETUP library entry after confirmed process start; retain the original EXE on device.

## Device-first validation

The first target is Android ARM64. Each milestone must be built in CI and tested on a physical device. A successful Gradle build alone does not prove Windows compatibility. Unsupported ABIs, missing assets, failed prefix creation and process launch errors must remain visible and must never be reported as successful execution.
