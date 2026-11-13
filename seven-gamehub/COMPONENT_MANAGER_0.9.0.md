# SevenX Desktop — Component Manager

## Existing component controls

The dedicated **Компоненты** screen reuses the pinned Winlator engine's `GeneralComponents` implementation.

Currently exposed upstream component types:
- Box64 — x86_64 translation layer versions.
- Turnip — Vulkan driver packages; GPU/device dependent.
- DXVK — Direct3D 9/10/11 translation.
- VKD3D — Direct3D 12 translation.
- WineD3D — Wine's OpenGL-based Direct3D implementation.
- SoundFont — MIDI sound banks.

The upstream flow provides built-in/downloaded version listing, downloads from its component index, supported local imports, removal of user-installed archives, and component version selection.

## 0.11.0 compatibility direction

The broader compatibility setup targets roughly **6–8 GiB installed**, with optional packs downloaded only when requested. This is a planning target, not a current measured footprint or a hard size limit. There is no artificial cap: users may install additional packs beyond 8 GiB.

The proposed packs and release gates are documented in [COMPATIBILITY_PACKS_0.11.0.md](COMPATIBILITY_PACKS_0.11.0.md).

## Not yet implemented as managed packs

The current component screen does **not** provide verified managed installers for:
- Microsoft Visual C++ Redistributables.
- Microsoft .NET Framework versions.
- Wine Mono as a separately versioned managed pack.
- Microsoft Edge WebView2 Runtime.
- General browser compatibility bundles.

Do not label these as supported or installable until a tested installation route exists. In particular, WebView2 under Wine/Box64 is application- and version-dependent and must not be advertised as guaranteed.

## Required behavior for future managed packs

Every pack must declare:
- Stable pack ID, display name, version and architecture applicability.
- Trusted source URL and license/redistribution notes.
- Download size and estimated expanded size when known.
- SHA-256 checksum verified before installation.
- Dependencies and conflicts.
- Container scope and whether it is safe to share across containers.
- Install, repair, update and uninstall behavior.
- A clear result state and actionable error message.

Never delete user data or the original Android installer during pack cleanup. Protect built-in runtime components. Calculate available storage before extraction and keep failed downloads/installations from appearing as installed.

## Validation

- Verify upstream Box64/Turnip/DXVK/VKD3D/WineD3D/SoundFont list, download, selection and removal flows.
- Verify checksums and interrupted-download recovery for each new managed pack.
- Verify per-container installation and no accidental cross-container contamination.
- Run smoke tests on physical ARM64 devices; record exact device, Android, GPU, Wine and Box64 versions.
- Treat CI success as compile validation only, not application compatibility evidence.
