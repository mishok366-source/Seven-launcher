# Nivora Launcher 2.1.1 installer compatibility notes

This note records static inspection of the user-supplied `Nivora.Launcher_2.1.1_x64-setup.exe`. The installer was not executed.

## Verified file facts

- File size: 15,089,530 bytes (about 14.4 MiB).
- SHA-256: `5cd0583de91a253a553b66d9a40ea1f90520dbfb0caa7f0f5bed9165337de37b`.
- Container: Windows PE32 GUI executable.
- Bootstrapper machine: Intel 386 / x86 (32-bit).
- Installer technology: Nullsoft Scriptable Install System (NSIS) 3.08.
- The bootstrapper has no CLR runtime header, so the bootstrapper itself is not a managed .NET executable.
- The filename contains `x64`, but that does not establish the architecture of the payload that NSIS extracts.

## Not yet verified

- The compressed NSIS payload has not been fully extracted and inspected.
- WebView2 presence, bundling, and version requirements are therefore unknown.
- The actual installed Nivora application architecture and its native dependencies are unknown.
- No Wine/Winlator launch or physical Android device test has been performed.

## SevenX handling

SevenX accepts Windows PE `.exe` files and now also recognizes the embedded NSIS manifest marker (`Nullsoft Install System`) when classifying an imported EXE as an installer. This is a format hint, not a compatibility guarantee.

The runtime handoff continues to stage a copy inside the selected Wine container. The source file remains untouched. The installer entry should only leave the SevenX library after the runtime reports that the installer process actually started; that library cleanup must not delete the original file.

## Expected limitations

An NSIS setup can launch under Wine while the application it installs still fails later because of missing WebView2, unsupported native dependencies, graphics requirements, or architecture mismatches. SevenX must report the launch handoff accurately and must not claim that Nivora itself is fully compatible until the installed application is tested.

MSI is not implied by this work and remains unsupported unless a separate `msiexec` path is implemented and tested.
