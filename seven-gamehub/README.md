# SevenX

SevenX is the game manager for Windows games on Android. It owns the game library, EXE/MSI imports, shortcuts, launch history, per-game compatibility profiles and the Compatibility Center.

## Two-part architecture

### SevenX — Game Manager
- Persistent library of imported Windows applications and installers.
- EXE PE-header and MSI database-signature validation.
- Favorites, shortcuts, launch history and per-game profiles.
- Compatibility Center with official runtime sources and a one-tap download/install workflow.
- Imported installer files remain on Android; removing a library entry does not delete the source file.

### SevenX Desktop — Winlator runtime
- Built from the pinned Winlator source and keeps its original interface and built-in workflows.
- Adds the SevenX launch bridge, automatic active-container reuse, bundled VC++ prefix preparation and a desktop-session entry.
- Keeps native Winlator controls for containers, Wine, Box64, graphics drivers, DXVK/VKD3D, audio, resolution and input.
- App label and launcher icon are branded SevenX Desktop; the original Winlator interface and runtime workflows are preserved.

SevenX passes imported files to the runtime using Android content URIs. The runtime stages a copy in the selected Wine prefix using a temporary file and final rename, preserving the original Android file.

## Compatibility update — 0.13.0

- Imports validated Windows PE EXE and MSI installer files; MSI packages are routed through `msiexec.exe /i`.
- Automatically creates a default Wine container on first run when none exists, then reuses the active container.
- Automatically enables bundled VC++ 2005/2010 components in the target prefix through Winlator's native startup component pipeline.
- Adds a per-game Compatibility profile that disables ESYNC/FSYNC for applications that need it.
- Adds static PE import inspection for guest architecture and common VC++, CLR/.NET, DirectX, XInput/DirectInput and WebView2 references.
- Adds a one-tap workflow that downloads 11 official packages into SevenX's private cache and launches each installer sequentially in the active Wine prefix. SevenX waits for the Wine process-exit callback, records each exit code, and continues after an individual package failure. If a guest installer does not exit within 15 minutes, the batch stops rather than launching another installer into a still-running Wine session.
- Keeps individual official download links and the legacy Android DownloadManager bulk-download option.

The 11-package batch attempts VC++ v14 and 2013 (x86/x64), Wine Mono 11.3 (x86/ARM64), .NET Desktop Runtime 10.0.15 (x86/x64/ARM64), .NET Framework 4.8 offline and WebView2 Evergreen. It uses silent-install switches and HTTPS redirects restricted to an explicit vendor/CDN host allowlist. Downloaded files are checked for EXE/MSI signatures and staged atomically.

## Compatibility limits

Automatic installation is best-effort, not a guarantee that every package supports Wine. A zero process exit code only confirms that the guest installer process exited successfully; it does not prove the component is present or that an application works. ARM64 guest packages may be incompatible with the x86_64 Wine prefix. Modern .NET, .NET Framework and WebView2 remain experimental under Wine/Box64 and may report failures. The static import scan cannot see delay-loaded or dynamically loaded DLLs, MSI custom actions or child-process dependencies.

This is not universal Windows compatibility. Kernel drivers, anti-cheat, unsupported CPU instructions, missing APIs, DRM, UWP dependencies and GPU-driver limits remain. Physical-device tests are required before claiming compatibility for a specific app/device combination.

See [COMPATIBILITY_0.13.0.md](COMPATIBILITY_0.13.0.md) for package details, limitations and release gates.

## Compatibility target

The earlier 6–8 GiB installed-footprint target remains a planning target, not a download cap or guarantee. The automatic compatibility batch is user-initiated; it does not run during first launch. The batch caches downloaded installers privately until the user clears app data.

The native Winlator component workflows remain the source of truth for Box64, Turnip, DXVK, VKD3D-Proton, WineD3D, SoundFont and WinComponents. SevenX does not inject a duplicate custom component screen into Winlator.

## Build outputs

The CI workflow builds two APKs:
- `sevenx-debug` — SevenX Game Manager.
- `winlator-sevenx-runtime-debug` — SevenX-enabled Winlator runtime.

CI compilation proves only that the Android projects compile. It does not prove a Windows application works on a physical device.

## Release gates

- Both APK build jobs pass for the final PR commit.
- The bridge, sequential installer completion callbacks and installer hand-off pass regression checks.
- EXE/MSI installation and runtime behavior are tested on a physical ARM64 device.
- VC++ dependent applications, Wine Mono/.NET applications and graphics paths are tested individually.
