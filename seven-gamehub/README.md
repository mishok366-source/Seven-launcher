# SevenX

SevenX is the game manager for Windows games on Android. It owns the game library, EXE/SETUP imports, game shortcuts, launch history, and per-game profiles.

## Two-part architecture

### SevenX — game manager
- Persistent library of imported Windows games and installers.
- Import validation, shortcuts, favorites, and launch history.
- Per-game launch profiles are in development: preferred Winlator container, launch arguments, and game-specific overrides.
- Game/library settings only. SevenX does not expose low-level Wine/Box64 engine controls.

### SevenX Desktop — Windows environment
- A separately built Winlator-based runtime with SevenX branding and a redesigned dark-purple interface.
- A dedicated **Рабочий стол** entry starts a Wine desktop session in a selected container.
- The **Приложения** section opens the Windows shortcut/application list.
- Existing Winlator capabilities remain available: file manager, containers, Wine, Box64, graphics and audio configuration, input controls, and display settings.
- Runtime settings belong here: container configuration, Wine/Box64 versions and presets, graphics drivers, DXVK/VKD3D, audio, resolution, and input.

SevenX launches a game through the runtime bridge. The runtime asks for a container when no game-specific container has been selected. It copies the EXE into that container and starts it through Winlator's XServer/Wine path. The original imported file is not deleted.

## Build outputs

The CI workflow builds two APKs:
- `sevenx-debug` — SevenX game manager.
- `winlator-sevenx-runtime-debug` — redesigned SevenX Desktop runtime.

Install both APKs to use the bridge. The runtime requires first-time setup and a configured container. A successful CI build does not prove compatibility on every device or with every Windows application.

## Compatibility and status

The runtime uses Wine and Box64 and depends on the Android device's CPU, Vulkan/OpenGL drivers, memory, and the individual application's requirements. Some Windows applications will not work, even when the runtime installs correctly.

CI compilation is not physical-device validation. Desktop mode, ordinary Windows applications, and game launching must be tested on an ARM64 Android device before describing them as production-ready.
