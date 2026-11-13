# SevenX Desktop 0.11.0 — Compatibility Packs

## Product target

Aim for a practical **6–8 GiB installed footprint** for a commonly useful Windows compatibility setup. This is a planning target, not a guaranteed size, a download limit, or a promise of universal application support. Optional packs may take the installation beyond 8 GiB when the user explicitly chooses them.

Keep the initial runtime compact. Show both download size and estimated expanded size before installation. Do not download every pack during first launch.

## Pack catalogue

| Pack | Intended contents | Default |
|---|---|---|
| Core Runtime | Pinned Wine build, Box64, essential system libraries and container bootstrap | Required |
| Graphics | Existing DXVK, VKD3D-Proton, WineD3D and device-compatible Turnip options | Optional / device-dependent |
| VC++ Runtime | Verified Microsoft Visual C++ Redistributable versions commonly requested by applications | Optional |
| .NET | Wine Mono and separately verified .NET Framework/runtime versions | Optional |
| Web Compatibility | WebView2 only if a reproducible Wine/Box64 installation and application test pass; otherwise clearly marked unavailable | Optional / gated |
| Legacy Compatibility | Older runtime libraries and compatibility helpers with explicit version/dependency metadata | Optional |
| Gaming Extras | Additional graphics/audio components and game-specific dependencies | Optional |

This catalogue describes the intended product structure. It does not assert that all listed packs are already downloadable or working.

## Dependency and install model

1. Resolve the selected application's detected or user-selected requirements.
2. Display required and recommended packs, with a reason for each recommendation.
3. Show download size, estimated expanded size, target container and required free space.
4. Fetch only from a trusted, documented source.
5. Verify SHA-256 before extraction or execution.
6. Install into the selected container unless a component is explicitly proven safe to share.
7. Record pack ID, version, checksum, source, install time and container ID.
8. Offer repair/update/removal only when the pack has a defined safe uninstall procedure.

Use atomic temporary downloads and staging directories. A failed download or checksum mismatch must never be recorded as installed. Never silently overwrite a user-modified Windows runtime file.

## Microsoft runtime policy

- VC++: use official redistributable packages where licensing and download terms permit. Keep x86 and x64 variants distinct; ARM64 host does not mean x86 guest applications can use an ARM64 redistributable.
- .NET: distinguish Wine Mono from Microsoft's .NET Framework and modern .NET Desktop Runtime. They are not interchangeable. Install only versions supported by a documented application test.
- WebView2: do not promise general support. Verify the installer behavior, WebView2 loader/runtime discovery and at least one real application under the pinned Wine/Box64 build before changing its status to supported.

Do not bundle proprietary installers or redistribute Microsoft components unless the applicable license and distribution terms explicitly allow it. When redistribution is not allowed, provide a user-initiated download/install flow from the official source.

## Compatibility labels

Use truthful per-pack states:
- Planned
- Available for download
- Installed
- Update available
- Unsupported on this device
- Failed (with reason)

Application compatibility must be reported separately from pack installation. A successful pack install does not prove the application launches.

## Release gates

- [ ] Pack manifest schema and stable IDs.
- [ ] Trusted source and license review for every downloadable package.
- [ ] SHA-256 verification and interrupted-download recovery.
- [ ] Accurate download/expanded-size and free-space checks.
- [ ] Per-container install state and dependency resolution.
- [ ] Safe repair/update/removal with user data preserved.
- [ ] Smoke-test matrix for common EXE installers, VC++ dependent apps, .NET apps, and any claimed WebView2 app.
- [ ] Physical ARM64 device tests with recorded CPU/GPU, Android, Wine and Box64 versions.
- [ ] Recalculate the 6–8 GiB target from measured installed sizes before publishing a release claim.

## Explicit limitations

The 6–8 GiB target cannot overcome Wine/Box64 architecture gaps, missing Android driver features, unsupported Windows APIs, kernel drivers, anti-cheat systems, or application-specific restrictions. USB passthrough and Windows kernel-driver support are separate platform features and are not implied by this pack system.
