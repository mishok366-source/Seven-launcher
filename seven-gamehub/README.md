# SevenX

SevenX is the game manager for Windows games on Android. It owns the game library, EXE/MSI/SETUP imports, game shortcuts, launch history, and per-game profiles.

## Two-part architecture

### SevenX — game manager
- Persistent library of imported Windows games and installers.
- Import validation, shortcuts, favorites, and launch history.
- Installer entries are kept separate from installed-game shortcuts.
- Game/library settings only. Low-level Wine and graphics controls stay in SevenX Desktop.

### SevenX Desktop — Windows environment
- A separately built Winlator-based runtime with SevenX branding and a redesigned dark-purple interface.
- **Рабочий стол** starts a Wine desktop session in a selected container.
- **Приложения** opens the Windows shortcut/application list.
- Runtime controls include containers, Wine, Box64, graphics drivers, DXVK/VKD3D, audio, resolution, and input.

SevenX passes imported files to the runtime using Android content URIs. The runtime stages a copy in the selected container; the original Android file remains untouched.

## Compatibility target — 0.11.0

The target is a practical **6–8 GiB installed footprint for a broad, commonly useful compatibility setup**, not a mandatory download and not a guarantee that every Windows program works. Keep the base runtime lean and offer larger dependencies as optional, on-demand packs. Users may install more than the target when they choose.

Planned pack groups and their intended scope are specified in [COMPATIBILITY_PACKS_0.11.0.md](COMPATIBILITY_PACKS_0.11.0.md). In particular:
- Core Wine/Box64 runtime and the existing graphics components.
- Common VC++ runtime dependencies.
- .NET/Mono support with framework versions selected per application.
- WebView/browser dependencies only through a verified installation path.
- Optional gaming and legacy compatibility components.

**Important:** the pack plan is a target, not a claim that VC++, Microsoft .NET Framework, or WebView2 installers are already integrated. Each pack must have a verified source, version, checksum, dependencies, install/uninstall behavior, and device test before it is exposed as installable.

## Game Manager 0.13.0

- Import and validate PE EXE files and Windows Installer MSI databases.
- MSI packages are launched through `msiexec /i` inside the selected Wine container.
- MSI and EXE installer entries remain in the library unless the runtime confirms the launch callback.
- Search the imported game library by file name.
- Mark entries as favorites; favorite state is stored with the library entry.
- View launch history, launch count and last confirmed launch time. History is updated only when the SevenX Desktop bridge confirms process creation.
- Removing an entry from the library does not delete the original Android file.

## Build outputs

The CI workflow builds two APKs:
- `sevenx-debug` — SevenX game manager.
- `winlator-sevenx-runtime-debug` — SevenX Desktop runtime.

CI compilation does not prove Windows application compatibility on a physical device. Results depend on CPU architecture, Android version, Vulkan/OpenGL drivers, memory, Wine/Box64, and application-specific requirements.
