#!/usr/bin/env python3
"""Static regression checks for the SevenX 0.13.0 EXE/MSI launch contract."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
bridge = (ROOT / "scripts/apply-sevenx-winlator-bridge.py").read_text(encoding="utf-8")
hardening = (ROOT / "scripts/apply-sevenx-installer-hardening.py").read_text(encoding="utf-8")
manager = (ROOT / "app/src/main/java/com/seven/gamehub/MainActivity.kt").read_text(encoding="utf-8")
manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
gradle = (ROOT / "app/build.gradle").read_text(encoding="utf-8")

checks = {
    "MSI runtime route invokes Windows Installer": 'msiexec.exe' in bridge and '.msi' in bridge,
    "Hardening keeps both EXE and MSI accepted": 'boolean isMsi = lowerName.endsWith(".msi")' in hardening and 'if (!isExe && !isMsi)' in hardening,
    "Game Manager validates MSI database signature": 'hasMsiDatabaseSignature' in manager and '0xD0.toByte()' in manager,
    "Game Manager accepts EXE and MSI": 'isSupportedWindowsInstaller' in manager and 'isExeName(name) || isMsiName(name)' in manager,
    "Android manifest exposes MSI file intents": 'application/x-msi' in manifest and 'application/vnd.ms-msi' in manifest,
    "Game Manager version is 0.13.0": "versionName '0.13.0'" in gradle,
    "Device diagnostics are visible in Settings": 'diagnostics.summary' in manager and 'diagnostics.vulkanVersion' in manager,
    "Compatibility Center exposes official Microsoft VC++ sources": "https://aka.ms/vc14/vc_redist.x86.exe" in manager and "https://aka.ms/vc14/vc_redist.x64.exe" in manager,
    "Compatibility Center exposes Wine Mono and .NET sources": "wine-mono/wine-mono/releases" in manager and "dotnet.microsoft.com/en-us/download/dotnet-framework" in manager,
    "Per-game profiles are persisted": 'launchProfile' in manager and 'onCycleProfile' in manager,
    "Per-game profile is forwarded to Winlator": 'sevenx_profile' in (ROOT / "app/src/main/java/com/seven/gamehub/runtime/WindowsRuntime.kt").read_text(encoding="utf-8") and 'sevenx_profile' in bridge,
    "Compatibility profile disables sync overrides": 'WINEESYNC", "0' in bridge and 'WINEFSYNC", "0' in bridge,
    "First launch automatically creates a default Wine container": 'createContainerAsync(new JSONObject()' in hardening and "Контейнеров нет. Сначала создай контейнер" not in hardening,
    "Bridge automatically reuses the active Wine container": 'activeContainerLink' in hardening and 'containerManager.activateContainer(selectedContainer)' in hardening and 'Выбери Wine-контейнер' not in hardening,
    "Bundled VC++ dependencies are enabled automatically for existing prefixes": 'prepareBundledDependencies(container)' in hardening and '.replace("vcrun2005=0", "vcrun2005=1")' in hardening and '.replace("vcrun2010=0", "vcrun2010=1")' in hardening,
    "Winlator rebranding is deferred": 'SevenX Desktop' not in bridge and 'SevenXComponentsFragment' not in bridge,
}

for label, passed in checks.items():
    print(("PASS" if passed else "FAIL") + ": " + label)

if not all(checks.values()):
    raise SystemExit(1)
