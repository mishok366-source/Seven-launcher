# SevenX Desktop 0.9.0 — Component Manager

## What ships in 0.9.0

A dedicated **Компоненты** entry is added to the SevenX Desktop navigation drawer. It reuses the pinned Winlator engine's existing `GeneralComponents` implementation rather than introducing a second installer.

The screen exposes these existing component types:

- Box64 — x86_64 translation layer versions.
- Turnip — Vulkan driver packages (device/GPU dependent).
- DXVK — Direct3D 9/10/11 translation.
- VKD3D — Direct3D 12 translation.
- WineD3D — Wine's OpenGL-based Direct3D implementation.
- SoundFont — MIDI sound banks.

For each type, the UI uses the upstream component flow for:
- Listing built-in and downloaded versions.
- Downloading available packages from the upstream installable-components index.
- Importing a package from local storage where the upstream type supports it.
- Removing user-installed package archives while protecting built-in versions.
- Selecting the component version from the existing engine settings.

## Boundaries

- This is a manager for component packages already supported by the pinned Winlator source; it does not claim universal Windows compatibility.
- A component being installed does not mean it is selected by a container or compatible with the device.
- Turnip packages are GPU-specific; users must choose a driver compatible with their hardware.
- Wine Mono, Microsoft .NET Framework, VC++ redistributables and WebView2 are not represented as universally installable here. They require their own tested installer/runtime paths.
- The existing upstream downloader and archive extraction remain responsible for package retrieval and installation. SevenX adds the dedicated UI and navigation entry.
- The UI reports the number of downloaded component versions in each category. It does not yet calculate the full expanded disk footprint or enforce the 5 GiB Compatibility Edition ceiling.

## Validation required

- CI compilation of both APKs.
- Verify each component row loads the upstream built-in versions.
- Verify download/import/remove flows per component type.
- Verify protected built-in packages cannot be removed.
- Verify selected component versions are honored by the existing runtime.
- Physical ARM64 device checks, including GPU-specific Turnip validation.
