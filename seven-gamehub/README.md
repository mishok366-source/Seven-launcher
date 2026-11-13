# SevenX

SevenX is the game manager for Windows games on Android. It owns the game library, EXE/MSI imports, shortcuts, launch history, a per-game compatibility toggle, and a compatibility center with official dependency sources.

## Two-part architecture

### SevenX — game manager
- Persistent library of imported Windows applications and installers.
- EXE PE-header and MSI database-signature validation.
- Favorites, shortcuts, launch history and per-game profiles.
- Compatibility Center links to official VC++, Wine Mono, .NET and WebView2 sources.
- Imported installer files are kept on Android; removing a library entry does not delete the source file.

### SevenX Desktop — Winlator runtime
- Built from the pinned Winlator source and keeps its original interface and built-in workflows.
- Adds the SevenX launch bridge, automatic reuse of the active Wine container, bundled VC++ prefix preparation and a desktop-session entry.
- Keeps native Winlator controls for containers, Wine, Box64, graphics drivers, DXVK/VKD3D, audio, resolution and input.
- Branding and launcher-icon changes are deliberately deferred until after the 0.13.0 release.

SevenX passes imported files to the runtime using Android content URIs. The runtime automatically reuses the active Wine container, stages a copy with a temporary file and a final rename, and preserves the original Android file.

## Compatibility update — 0.13.0

- Imports validated Windows PE EXE and MSI installer files.
- Routes MSI packages through `msiexec.exe /i` in the automatically selected active Wine container.
- Automatically enables bundled VC++ 2005/2010 components in the target Wine prefix before launch, without asking the user to open their installers.\n- Adds a per-game Compatibility profile that disables ESYNC/FSYNC for apps that need a more conservative synchronization configuration. Standard profile inherits the container's settings.
- Adds a Compatibility Center with official download links for VC++ v14 x86/x64, VC++ 2013, legacy VC++ documentation, Wine Mono, Microsoft .NET Framework, modern .NET and WebView2.
- Additional downloaded installers can still be imported through the EXE/MSI flow. Wine Mono, Microsoft .NET Framework, modern .NET and WebView2 are not yet automatically installed; WebView2 remains experimental under Wine/Box64.
- Exposes device ABI, Vulkan feature flags and RAM diagnostics.

The Compatibility Center is still a source catalogue for runtimes beyond the bundled VC++ components, not a universal background package manager. SevenX does not claim every Windows application will work: unsupported APIs, drivers, anti-cheat, DRM and CPU/GPU limitations remain.

See [COMPATIBILITY_0.13.0.md](COMPATIBILITY_0.13.0.md) for exact scope, official sources, known limitations and release gates.

## Compatibility target

The earlier 6–8 GiB installed-footprint target remains a planning target, not a download cap or guarantee. Larger optional dependencies can exceed it. Never download every component automatically during first launch.

The native Winlator component workflows remain the source of truth for Box64, Turnip, DXVK, VKD3D-Proton, WineD3D, SoundFont and WinComponents. SevenX does not inject a duplicate custom component screen into Winlator.

## Build outputs

The CI workflow builds two APKs:
- `sevenx-debug` — SevenX Game Manager.
- `winlator-sevenx-runtime-debug` — SevenX-enabled Winlator runtime.

CI compilation proves only that the Android projects compile. It does not prove a Windows application works on a physical device. Results depend on CPU architecture, Android version, GPU/Vulkan/OpenGL drivers, memory, Wine/Box64 and application-specific requirements.

## Release gates

- Both APK build jobs pass for the final PR commit.
- The EXE/MSI hand-off and installer staging are checked on a physical ARM64 device.
- VC++ dependent applications, Wine Mono/.NET applications and graphics paths are tested individually.
- A compatibility claim is made only for an application/version/device combination that has been tested.
