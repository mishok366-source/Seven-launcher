#!/usr/bin/env python3
"""Static regression checks for the SevenX 0.13.0 EXE/MSI launch contract."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
bridge = (ROOT / "scripts/apply-sevenx-winlator-bridge.py").read_text(encoding="utf-8")
hardening = (ROOT / "scripts/apply-sevenx-installer-hardening.py").read_text(encoding="utf-8")
manager = (ROOT / "app/src/main/java/com/seven/gamehub/MainActivity.kt").read_text(encoding="utf-8")
manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
gradle = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
installer = (ROOT / "app/src/main/java/com/seven/gamehub/DependencyInstaller.kt").read_text(encoding="utf-8")
runtime_client = (ROOT / "app/src/main/java/com/seven/gamehub/runtime/WindowsRuntime.kt").read_text(encoding="utf-8")
runtime_events = (ROOT / "app/src/main/java/com/seven/gamehub/runtime/RuntimeLaunchEvents.kt").read_text(encoding="utf-8")
runtime_receiver = (ROOT / "app/src/main/java/com/seven/gamehub/runtime/RuntimeLaunchReceiver.kt").read_text(encoding="utf-8")
file_provider_paths = (ROOT / "app/src/main/res/xml/sevenx_file_paths.xml").read_text(encoding="utf-8")

checks = {
    "MSI runtime route invokes Windows Installer": 'msiexec.exe' in bridge and '.msi' in bridge,
    "Hardening keeps both EXE and MSI accepted": 'boolean isMsi = lowerName.endsWith(".msi")' in hardening and 'if (!isExe && !isMsi)' in hardening,
    "Game Manager validates MSI database signature": 'hasMsiDatabaseSignature' in manager and '0xD0.toByte()' in manager,
    "Game Manager accepts EXE and MSI": 'isSupportedWindowsInstaller' in manager and 'isExeName(name) || isMsiName(name)' in manager,
    "Android manifest exposes MSI file intents": 'application/x-msi' in manifest and 'application/vnd.ms-msi' in manifest,
    "Game Manager version is 0.13.0": "versionName '0.13.0'" in gradle,
    "Device diagnostics are visible in Settings": 'diagnostics.summary' in manager and 'diagnostics.vulkanVersion' in manager,
    "Official dependency downloads have Android internet permission": 'android.permission.INTERNET' in manifest,
    "Compatibility Center downloads official VC++ packages with Android DownloadManager": "DownloadManager.Request" in manager and "setDestinationInExternalPublicDir" in manager and "https://aka.ms/vc14/vc_redist.x86.exe" in manager and "https://aka.ms/highdpimfc2013x64enu" in manager,
    "Compatibility Center restricts direct dependency downloads to trusted HTTPS sources": 'uri.scheme == "https"' in manager and '"builds.dotnet.microsoft.com"' in manager and '"/wine-mono/wine-mono/releases/download/"' in manager,
    "Compatibility Center queues the complete 11-file dependency pack": "enqueueAllDependencyDownloads(context)" in manager and "Скачать ВСЁ (11 файлов)" in manager and "sevenXDependencyDownloads" in manager,
    "Bulk dependency pack includes Wine Mono, .NET Desktop, .NET Framework and WebView2": "wine-mono-11.3.0-x86.msi" in manager and "windowsdesktop-runtime-10.0.15-win-x64.exe" in manager and "NDP48-x86-x64-AllOS-ENU.exe" in manager and "MicrosoftEdgeWebview2Setup.exe" in manager,
    "Compatibility Center exposes official Microsoft VC++ sources": "https://aka.ms/vc14/vc_redist.x86.exe" in manager and "https://aka.ms/vc14/vc_redist.x64.exe" in manager,
    "Compatibility Center exposes Wine Mono and .NET sources": "wine-mono/wine-mono/releases" in manager and "dotnet.microsoft.com/en-us/download/dotnet-framework" in manager,
    "Per-game profiles are persisted": 'launchProfile' in manager and 'onCycleProfile' in manager,
    "Per-game profile is forwarded to Winlator": 'sevenx_profile' in (ROOT / "app/src/main/java/com/seven/gamehub/runtime/WindowsRuntime.kt").read_text(encoding="utf-8") and 'sevenx_profile' in bridge,
    "Compatibility profile disables sync overrides": 'WINEESYNC", "0' in bridge and 'WINEFSYNC", "0' in bridge,
    "First launch automatically creates a default Wine container": 'createContainerAsync(new JSONObject()' in hardening and "Контейнеров нет. Сначала создай контейнер" not in hardening,
    "Bridge automatically reuses the active Wine container": 'activeContainerLink' in hardening and 'containerManager.activateContainer(selectedContainer)' in hardening and 'Выбери Wine-контейнер' not in hardening,
    "Bundled VC++ dependencies are enabled automatically for existing prefixes": 'prepareBundledDependencies(container,' in hardening and 'enableWinComponent(current, "vcrun2005")' in hardening and 'enableWinComponent(updated, "vcrun2010")' in hardening,
    "PE dependency analyzer identifies architecture and common runtime DLLs": "WindowsDependencyAnalyzer.analyze" in manager and "mscoree.dll" in (ROOT / "app/src/main/java/com/seven/gamehub/WindowsDependencyAnalyzer.kt").read_text(encoding="utf-8") and "vcruntime" in (ROOT / "app/src/main/java/com/seven/gamehub/WindowsDependencyAnalyzer.kt").read_text(encoding="utf-8"),
    "Detected compatibility hints are persisted and shown in the library": "compatibilityHints" in manager and "WindowsDependencyAnalyzer.analyze(context, uri)" in manager,
    "PE import analysis detects optional DirectShow and DirectPlay components": '"quartz.dll"' in (ROOT / "app/src/main/java/com/seven/gamehub/WindowsDependencyAnalyzer.kt").read_text(encoding="utf-8") and '"dplayx.dll"' in (ROOT / "app/src/main/java/com/seven/gamehub/WindowsDependencyAnalyzer.kt").read_text(encoding="utf-8"),
    "Detected component hints reach the runtime bridge": 'sevenx_compatibility_hints' in (ROOT / "app/src/main/java/com/seven/gamehub/runtime/WindowsRuntime.kt").read_text(encoding="utf-8") and 'sevenx_compatibility_hints' in hardening,
    "Runtime enables optional DirectShow and DirectPlay only when detected": 'enableWinComponent(updated, "directshow")' in hardening and 'enableWinComponent(updated, "directplay")' in hardening,
    "One-tap UI exposes automatic download and installation": "Скачать и установить ВСЁ автоматически" in manager and "onInstallAll" in manager,
    "Automatic installer contains the complete 11-package list": installer.count('Package("') == 11,
    "Automatic installer downloads over HTTPS from an explicit host allowlist": 'current.protocol.equals("https"' in installer and "allowedHosts" in installer and "instanceFollowRedirects = false" in installer,
    "Automatic installer caches packages privately and launches each through Wine": 'compatibility-installers' in installer and "WindowsRuntime.launchAndWait" in installer,
    "Automatic installer completes all downloads before starting the first Wine installer": installer.index("downloadVerifiedHttps(item.url, temporary)") < installer.index("WindowsRuntime.launchAndWait") and "Phase 1: finish every download" in installer and "Phase 2: only after the download queue is finished" in installer,
    "Automatic installer waits for a real process-exit callback": "withTimeout(timeoutMillis)" in runtime_client and "RuntimeLaunchEvents.complete" in runtime_receiver and "sevenx_process_finished" in hardening,
    "Automatic installer waits for confirmed Wine process start before waiting for exit": "operation.started.await()" in runtime_client and "RuntimeLaunchEvents.started" in runtime_receiver and "setStartCallback" in bridge,
    "Winlator startup has a finite timeout distinct from installer completion": "START_TIMEOUT_MILLIS = 90_000L" in runtime_client and "RuntimeStartTimeoutException" in runtime_client and "Winlator не запустил Windows-процесс за 90 секунд" in manager,
    "Failed Winlator process creation completes the pending callback": 'if (pid <= 0)' in bridge and 'sevenx_process_finished' in bridge and 'sevenx_exit_code' in bridge,
    "Only one automatic dependency batch can run at a time": 'AtomicBoolean(false)' in installer and 'compareAndSet(false, true)' in installer and 'isRunning.set(false)' in installer,
    "Installer timeouts stop the queue instead of overlapping Wine sessions": "15 * 60 * 1000L" in runtime_client and "TimeoutCancellationException" in manager and "if (error is CancellationException) throw error" in installer,
    "Downloaded EXE files are checked for a real PE signature": "RandomAccessFile(file, \"r\")" in installer and "signature.contentEquals(byteArrayOf(0x50" in installer,
    "Winlator bridge forwards silent installer arguments": 'sevenx_install_args' in hardening and 'sevenxInstallArgs' in hardening,
    "FileProvider exposes the private dependency installer cache": 'path="compatibility-installers/"' in file_provider_paths,
    "SevenX sessions return to the caller instead of reopening Winlator file manager": '"sevenx_contract_version"' in hardening and '"sevenx_desktop_mode"' in hardening and "ForegroundService.stopSession(this)" in hardening and "wfm.exe in the EXE folder" in hardening,
    "SevenX Desktop branding changes only the app label and launcher icon": 'SevenX Desktop' in bridge and 'sevenx_launcher.xml' in bridge and 'SevenXComponentsFragment' not in bridge,
}

    "Winlator preloader has a finite five-minute startup watchdog": "300_000L" in bridge and "preloaderDialog.isShowing()" in bridge,
    "Wine process start failure closes the display instead of leaving the spinner": "if (pid <= 0)" in bridge and "Wine не смог запустить гостевой процесс" in bridge and "finish();" in bridge,
    "Winlator environment setup exceptions are logged and shown": "Failed to prepare Winlator environment" in bridge and "new AlertDialog.Builder(this)" in bridge,
    "Wine process creation exceptions are logged instead of swallowed": "SevenXProcess" in bridge and "Failed to start guest command" in bridge,

for label, passed in checks.items():
    print(("PASS" if passed else "FAIL") + ": " + label)

if not all(checks.values()):
    raise SystemExit(1)
