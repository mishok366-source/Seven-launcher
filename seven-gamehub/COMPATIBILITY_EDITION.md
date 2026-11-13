# SevenX Desktop Compatibility Edition

## Goal

Build the fullest practical Windows-application runtime for ARM64 Android while keeping the **installed optional compatibility payload at or below 5 GiB**. This is a budget, not a claim that every Windows application can run.

The base SevenX Desktop APK remains a separate, installable Android application. Compatibility components are installed into a selected Wine prefix/container and can be updated independently.

## Runtime architecture

- Android ARM64 host with the pinned Winlator/XServer integration.
- Wine WoW64 where supported by the selected upstream revision; retain the upstream-supported 32-bit path rather than assuming every prefix can mix architectures.
- Box64 for x86_64 guest binaries. Box86 is included only if the selected Android/Winlator build actually provides and supports it; do not advertise it based on a package name alone.
- Graphics backends remain selectable per container: DXVK, VKD3D-Proton, and WineD3D, subject to device Vulkan/OpenGL driver support.
- Audio, input, display, fonts, and common Windows runtime dependencies are installed as managed components, not copied blindly into every prefix.
- Windows desktop and application shortcuts continue to use the real Wine/XServer execution path.

## 5 GiB installed-payload budget

| Component group | Target budget |
| --- | ---: |
| Wine runtime and required host libraries | 1.6 GiB |
| x86/x64 translation components | 0.8 GiB |
| DXVK, VKD3D-Proton, WineD3D support files | 0.7 GiB |
| Common VC++ runtimes, fonts, audio and multimedia support | 0.7 GiB |
| .NET/Mono compatibility components | 0.8 GiB |
| Installer metadata, tools, safety margin | 0.4 GiB |
| **Total ceiling** | **5.0 GiB** |

These are planning allocations. CI must measure the actual compressed download and installed sizes before a release; if upstream binaries exceed the budget, publish the measured result and prioritize components instead of silently removing files.

## Component policy

1. Prefer official upstream releases and reproducible pinned versions.
2. Preserve each component's license and notices; do not bundle proprietary Windows DLLs or redistribute Microsoft installers without checking their redistribution terms.
3. Install optional dependencies into the selected prefix using a component manager. Never overwrite Wine core files indiscriminately.
4. Record component name, version, source URL, checksum, license, installed size, target architecture, and install/uninstall action in the component manifest.
5. Make installation transactional: verify download checksum, stage files, apply changes, and roll back on failure.
6. Detect available disk space before installation and show the user the exact download and installed sizes.
7. Do not claim WebView2 support unless its installer and representative WebView2 applications pass on the actual Wine build and device.

## Compatibility targets (must be tested, not assumed)

- Classic Win32 x86 and x64 applications.
- MSI and EXE installers.
- Visual C++ runtime families, installed side-by-side where required.
- .NET Framework through the Wine-supported Mono/.NET approach, and modern .NET only where the specific application/runtime combination is verified.
- Electron/Tauri applications only when their embedded or system web runtime is compatible.
- Direct3D 9/10/11 through DXVK where Vulkan works; Direct3D 12 through VKD3D-Proton only on supported drivers/devices; WineD3D as a fallback.
- Fonts, audio, clipboard, file dialogs, and desktop shortcuts.

Windows kernel drivers, anti-cheat systems, kernel services, UWP dependencies, and some DRM/security software are outside the compatibility promise.

## Release gates

- [ ] Build the ARM64 Android runtime from pinned source revisions.
- [ ] Produce a machine-readable component manifest with versions, checksums, licenses, and sizes.
- [ ] Verify the installed payload does not exceed 5 GiB.
- [ ] Verify install, update, uninstall, and rollback for every component.
- [ ] Test x86 and x64 Win32 smoke-test applications.
- [ ] Test MSI and EXE installation.
- [ ] Test VC++ and .NET sample applications.
- [ ] Test DXVK/VKD3D/WineD3D selection and record device/driver results.
- [ ] Test WebView2 separately; mark unsupported if it fails.
- [ ] Test on physical ARM64 Android hardware. CI success alone is not runtime validation.

## Current status

This document defines the implementation target. It does not mean the listed dependencies are already bundled or that all compatibility tests have passed. The current runtime remains the existing pinned Winlator build until each component is integrated and verified.
