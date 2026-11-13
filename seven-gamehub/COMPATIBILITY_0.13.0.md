# SevenX 0.13.0 — Windows application compatibility

- Прямые загрузки VC++ v14 и VC++ 2013 доступны в Центре совместимости.
- Центр совместимости предлагает пакетную загрузку 11 официальных установщиков: VC++ v14 и 2013 (x86/x64), Wine Mono 11.3 (x86/ARM64), .NET Desktop Runtime 10.0.15 (x86/x64/ARM64), .NET Framework 4.8 offline и WebView2 Evergreen Bootstrapper. Кнопка ставит все файлы в очередь Android DownloadManager; это только скачивание, не автоматическая установка. Совместимость отдельных .NET/WebView2 установщиков с Wine не гарантируется.

## Implemented in this increment

- SevenX accepts Windows PE `.exe` programs and Windows Installer `.msi` databases.
- EXE files are checked for a valid PE header; MSI files are checked for the OLE Compound File signature.
- On first launch, if no Wine prefix exists, the bridge asks Winlator's `ContainerManager` to create a default container automatically; it then activates and uses that prefix. Existing installations continue using the active container.
- The bridge automatically reuses Winlator's active Wine container; it no longer asks the user to pick a container on every launch. If none is active, it selects and activates the first available container.
- EXE/MSI files are copied into the active container using a temporary file and an atomic rename, with free-space checks and partial-copy cleanup.
- SevenX automatically enables the bundled VC++ 2005 and 2010 native WinComponents in the target prefix before launch. Winlator applies the changes through its normal startup component pipeline; newly created containers also default to these components.
- PE import analysis can also request the pinned runtime's native DirectShow and DirectPlay components when the executable statically imports their DLLs. These options are enabled only for the affected container and only when the import table provides evidence; dynamic loading is not visible to this scan.
- The original Android installer file is preserved. Removing an installer entry from the SevenX library does not delete the source file.
- Per-game launch profiles are saved in the library. The Compatibility profile disables Wine ESYNC/FSYNC for applications that misbehave with synchronization features; Standard inherits the selected container settings.
- The Game Manager includes a Compatibility Center with official vendor sources. VC++ 2013 and v14 x86/x64 packages can be downloaded directly through Android DownloadManager into SevenX app storage; the user then imports and explicitly launches the installer in Winlator. The bundled VC++ 2005/2010 components are prepared automatically; .NET, Wine Mono and WebView2 remain official-page/manual workflows.
- Device diagnostics expose Android ABI, Vulkan feature flags and RAM information.

## Automatic static dependency inspection\n\nWhen an EXE is imported, SevenX reads its PE header and static import table without executing it. The library records the detected guest architecture and hints for common VC++ runtimes, CLR/.NET, DirectX, XInput/DirectInput and the WebView2 loader. These are diagnostic hints, not an installation decision: delay-loaded DLLs, dynamic LoadLibrary calls, MSI custom actions and dependencies loaded by child processes are not visible in the static import table. MSI requirements can only be assessed after installation. SevenX does not silently download or execute unverified redistributables.\n\n## Compatibility Center sources

- Microsoft Visual C++ latest supported redistributables (x86/x64): https://learn.microsoft.com/en-us/cpp/windows/latest-supported-vc-redist/
- Microsoft Visual C++ v14 x86: https://aka.ms/vc14/vc_redist.x86.exe
- Microsoft Visual C++ v14 x64: https://aka.ms/vc14/vc_redist.x64.exe
- Microsoft Visual C++ 2013 x86: https://aka.ms/highdpimfc2013x86enu
- Microsoft Visual C++ 2013 x64: https://aka.ms/highdpimfc2013x64enu
- Wine Mono releases: https://github.com/wine-mono/wine-mono/releases
- Microsoft .NET Framework downloads: https://dotnet.microsoft.com/en-us/download/dotnet-framework
- Modern .NET downloads: https://dotnet.microsoft.com/en-us/download/dotnet
- Microsoft Edge WebView2: https://developer.microsoft.com/en-us/microsoft-edge/webview2/

Use the architecture required by the Windows guest application. On an ARM64 Android host, x86/x64 guest requirements do not become ARM64 guest requirements. VC++ 2013 and earlier remain side-by-side runtimes; the v14 runtime covers Visual Studio 2015 and later. See Microsoft's documentation for version and licensing details.

## Runtime and graphics components

SevenX Desktop keeps the pinned Winlator interface and its existing native component workflows. Use its existing controls for Box64, Turnip, DXVK, VKD3D-Proton, WineD3D, SoundFont and WinComponents. No second custom component screen is injected into Winlator.

The Compatibility profile only changes ESYNC/FSYNC environment flags. It does not change Wine version, Box64 preset, graphics driver, DXVK/VKD3D selection or application-specific registry state.

## Wine Mono, .NET and WebView2

Wine Mono, Microsoft .NET Framework and modern .NET Desktop Runtime are different products and are not interchangeable. The current build does not yet automate their download or installation. Static PE import inspection can flag CLR and WebView2 loader references, but cannot infer the exact framework version or prove runtime availability. A successful installer process start does not prove the dependency installed correctly.

WebView2 remains experimental under Wine/Box64. The Compatibility Center links to Microsoft's official download page but does not label WebView2 as supported or guarantee Electron/WebView applications.

## Release truth and remaining validation

- CI must compile both the SevenX Game Manager and the patched Winlator runtime.
- Physical-device tests are required for EXE and MSI installers, VC++ dependent applications, Wine Mono/.NET applications, graphics paths and the per-game profile.
- CI compilation and successful installer process creation are not application compatibility evidence.
- Kernel drivers, anti-cheat, unsupported CPU instructions, missing Windows APIs, DRM, UWP dependencies and GPU-driver limitations remain outside the compatibility guarantee.
- Winlator branding and launcher icon changes remain deferred until after the 0.13.0 release, as planned.


## In-app dependency downloads

The Compatibility Center downloads the Microsoft VC++ 2013 and VC++ v14 x86/x64 redistributables directly with Android DownloadManager. Files are placed in the shared Android Downloads directory and the Android download notification reports completion. The user then imports the downloaded EXE and explicitly starts it through the existing Winlator bridge. SevenX does not silently execute downloaded installers. Other packages remain links to their official vendor pages until stable, versioned, Wine-tested installation paths exist.
