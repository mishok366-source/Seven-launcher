# SevenX — Native Winlator component workflows

SevenX 0.13.0 does not inject a second component-management screen into Winlator. SevenX Desktop keeps the pinned Winlator interface and its built-in component controls.

## Existing Winlator component types

The pinned runtime's existing `GeneralComponents` implementation provides version selection and installation workflows for:
- Box64 — x86_64 translation layer versions.
- Turnip — Vulkan driver packages; GPU/device dependent.
- DXVK — Direct3D 9/10/11 translation.
- VKD3D — Direct3D 12 translation.
- WineD3D — Wine's OpenGL-based Direct3D implementation.
- SoundFont — MIDI sound banks.

Winlator also exposes native WinComponents controls in container settings, including available Direct3D, DirectSound, DirectMusic, DirectShow, DirectPlay, XAudio and bundled VC++ options.

## SevenX 0.13.0 additions

- Newly created containers enable the bundled VC++ 2005 and 2010 components by default.
- Existing containers retain their saved component settings.
- The Game Manager Compatibility Center links to official vendor downloads for additional runtimes and imports downloaded EXE/MSI installers into the normal selected-container launch flow.
- Per-game Compatibility profile can disable ESYNC/FSYNC without changing the container-wide defaults.

## Not included / not claimed

The Compatibility Center is not a verified background package manager. It does not yet maintain per-container install/repair/uninstall records or pinned package checksums for Microsoft redistributables, Wine Mono, .NET or WebView2. WebView2 remains experimental under Wine/Box64.

Do not label a component or application as supported until its installation and a real application test pass on a documented device/runtime combination.
