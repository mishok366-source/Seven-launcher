# SevenX 0.13.0 — Windows application compatibility

## Implemented in this increment

- SevenX accepts Windows PE `.exe` programs and Windows Installer `.msi` databases.
- EXE files are checked for a valid PE header before import.
- MSI files are checked for the OLE Compound File signature before import.
- MSI launch requests are translated to `msiexec.exe /i "<package>"` inside the selected Wine container.
- Newly created containers enable the bundled Visual C++ 2005 and 2010 native component sets by default; existing containers keep their saved settings.
- The original Android installer file is preserved. Removing an installer entry from the SevenX library does not delete the source file.
- The runtime still uses the selected Winlator container, Wine prefix, Box64 and the container's graphics/audio configuration.

## Installing application dependencies manually

Until managed compatibility packs are implemented, users can import a legitimate Windows dependency installer as an EXE or MSI and launch it in the same container as the application that needs it. Examples include vendor redistributables and Wine Mono MSI packages.

Important:
- Install dependencies into the same Wine container/prefix as the target application.
- Use the package's official vendor source and choose the correct guest architecture (x86 or x64).
- A successful installer launch does not prove that the dependency installed correctly or that the target application will work.
- Do not assume that Microsoft .NET Framework, modern .NET Desktop Runtime, Wine Mono and WebView2 are interchangeable.
- Do not redistribute proprietary Microsoft installers or DLLs unless their license explicitly permits it.

## Compatibility work still required before a broad-support release

1. Managed VC++ packs: side-by-side x86/x64 versions, trusted official sources, version metadata, checksums, container-scoped install/repair/remove.
2. .NET: separate Wine Mono and Microsoft .NET Framework workflows; verify versions individually. Modern .NET remains a separate target.
3. WebView2: only expose as supported after the runtime installer, loader discovery and real applications pass on the pinned Wine/Box64 build.
4. Application profiles: select a tested Wine version, Box64 preset, DXVK/VKD3D/WineD3D and environment overrides per app without changing other containers.
5. Diagnostics: capture architecture, missing DLL/API clues, Wine/Box64 logs, GPU/driver and container configuration.
6. Physical-device test matrix: 32-bit and 64-bit EXE, MSI, VC++ apps, .NET apps, Electron/WebView apps, DirectX 9/11/12 and OpenGL.

## Release truth

This update adds MSI installation support. It does **not** make every Windows application compatible. Kernel drivers, anti-cheat, unsupported CPU instructions, missing Windows APIs, DRM, UWP dependencies and GPU-driver limitations remain outside the compatibility guarantee. CI compilation is not a substitute for physical-device tests.
