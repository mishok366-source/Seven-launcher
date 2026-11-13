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
}

for label, passed in checks.items():
    print(("PASS" if passed else "FAIL") + ": " + label)

if not all(checks.values()):
    raise SystemExit(1)
