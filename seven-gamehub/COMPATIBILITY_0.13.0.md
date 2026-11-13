# SevenX 0.13.0 — Windows application compatibility

## Automatic compatibility installation

The Compatibility Center has a user-initiated **Download and install ALL** workflow. It processes 11 official packages sequentially:

1. Visual C++ v14 x86
2. Visual C++ v14 x64
3. Visual C++ 2013 x86
4. Visual C++ 2013 x64
5. Wine Mono 11.3 x86 MSI
6. Wine Mono 11.3 ARM64 MSI
7. .NET Desktop Runtime 10.0.15 x86
8. .NET Desktop Runtime 10.0.15 x64
9. .NET Desktop Runtime 10.0.15 ARM64
10. .NET Framework 4.8 offline
11. WebView2 Evergreen bootstrapper

The workflow downloads packages into SevenX's private app cache over HTTPS. Redirects are followed manually and restricted to an explicit allowlist of vendor/CDN hosts. EXE and MSI signatures are checked, downloads are written to temporary files, and only complete files are renamed into the cache.

Each package is handed to the active Wine prefix through the SevenX-enabled Winlator bridge with its silent-install arguments. MSI files use `msiexec.exe /i`. The Game Manager waits for the actual Wine guest-process exit callback, records the exit code, and continues after individual download or ordinary installation failures. If a guest installer does not exit within 15 minutes, the batch stops rather than starting another installer in a still-running Wine session. The batch is serialized so repeated taps cannot start parallel install sequences.

This is **best-effort installation**, not a promise that all 11 packages can be installed in every Wine prefix. A zero exit code confirms only the guest process exit status; it does not prove that a runtime is registered correctly or that a Windows application works. ARM64 guest installers may be incompatible with the x86_64 Wine prefix. Modern .NET, .NET Framework and WebView2 remain experimental under Wine/Box64.

## Other implemented behavior

- SevenX accepts Windows PE `.exe` programs and Windows Installer `.msi` databases.
- EXE files are checked for a valid PE header; MSI files are checked for the OLE Compound File signature.
- On first launch, if no Wine prefix exists, the bridge asks Winlator's `ContainerManager` to create a default container automatically; it then activates and uses that prefix. Existing installations continue using the active container.
- The bridge automatically reuses Winlator's active Wine container. If none is active, it selects and activates the first available container.
- EXE/MSI files are copied into the active container using a temporary file and atomic rename, with free-space checks and partial-copy cleanup.
- SevenX automatically enables bundled VC++ 2005 and 2010 native WinComponents in the target prefix. Winlator applies the changes through its normal startup component pipeline.
- PE import analysis can request the pinned runtime's native DirectShow and DirectPlay components when the executable statically imports their DLLs. Dynamic loading is not visible to this scan.
- The original Android installer file is preserved. Removing an installer entry from the SevenX library does not delete the source file.
- Per-game launch profiles are saved in the library. The Compatibility profile disables Wine ESYNC/FSYNC; Standard inherits the selected container settings.
- Device diagnostics expose Android ABI, Vulkan feature flags and RAM information.
- SevenX Desktop changes only the Winlator app label and launcher icon; the original Winlator interface and runtime workflows remain intact.

## Automatic static dependency inspection

When an EXE is imported, SevenX reads its PE header and static import table without executing it. The library records the detected guest architecture and hints for common VC++ runtimes, CLR/.NET, DirectX, XInput/DirectInput and the WebView2 loader. These are diagnostic hints, not an installation decision: delay-loaded DLLs, dynamic LoadLibrary calls, MSI custom actions and dependencies loaded by child processes are not visible in the static import table. MSI requirements can only be assessed after installation.

## Official package sources

- Microsoft Visual C++ latest supported redistributables: https://learn.microsoft.com/en-us/cpp/windows/latest-supported-vc-redist/
- Microsoft Visual C++ v14 x86: https://aka.ms/vc14/vc_redist.x86.exe
- Microsoft Visual C++ v14 x64: https://aka.ms/vc14/vc_redist.x64.exe
- Microsoft Visual C++ 2013 x86: https://aka.ms/highdpimfc2013x86enu
- Microsoft Visual C++ 2013 x64: https://aka.ms/highdpimfc2013x64enu
- Wine Mono releases: https://github.com/wine-mono/wine-mono/releases
- Microsoft .NET Framework downloads: https://dotnet.microsoft.com/en-us/download/dotnet-framework
- Modern .NET downloads: https://dotnet.microsoft.com/en-us/download/dotnet
- Microsoft Edge WebView2: https://developer.microsoft.com/en-us/microsoft-edge/webview2/

## Runtime and graphics components

SevenX Desktop keeps the pinned Winlator interface and its existing native component workflows. Use its existing controls for Box64, Turnip, DXVK, VKD3D-Proton, WineD3D, SoundFont and WinComponents. No second custom component screen is injected into Winlator.

The Compatibility profile only changes ESYNC/FSYNC environment flags. It does not change Wine version, Box64 preset, graphics driver, DXVK/VKD3D selection or application-specific registry state.

## Release truth and remaining validation

- CI must compile both the SevenX Game Manager and the patched Winlator runtime for the final PR head.
- Static regression checks cover the 11-package list, secure download path, FileProvider staging, callback correlation, process-exit reporting and installer argument forwarding.
- Physical-device tests are required for EXE/MSI installers, all supported dependency packages, graphics paths and per-game profiles.
- CI compilation and successful installer process exit are not application compatibility evidence.
- Kernel drivers, anti-cheat, unsupported CPU instructions, missing Windows APIs, DRM, UWP dependencies and GPU-driver limitations remain outside the compatibility guarantee.
