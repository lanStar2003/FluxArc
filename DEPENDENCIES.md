# Locked development target

FluxArc targets **GT New Horizons 2.8.4**, Minecraft **1.7.10**, Forge **10.13.4.1614**, MCP stable **12** and Java **8** bytecode. It does not target current GTNH master.

| Dependency | Exact version | Evidence |
|---|---|---|
| GT5-Unofficial | 5.09.51.482 | Official 2.8.4 manifest |
| StructureLib | 1.4.23 | Official 2.8.4 manifest |
| GTNHLib | 0.7.10 | Official 2.8.4 manifest |
| ModularUI | 1.2.20 | Official 2.8.4 manifest |
| RetroFuturaGradle | 1.4.1 | Pinned build plugin |
| Gradle | 8.9 | Pinned wrapper |
| JUnit | 4.13.2 | Unit tests only |

The checked-in `gradle/wrapper/gradle-wrapper.jar` is the unmodified official project launcher from `https://raw.githubusercontent.com/gradle/gradle/v8.9.0/gradle/wrapper/gradle-wrapper.jar`, SHA256 `498495120a03b9a6ab5d155f5de3c8f0d986a449153702fb80fc80e134484f17`. It is not the Gradle distribution or a dependency cache; those remain excluded by `.gitignore`.

At runtime the official GT 5.09.51.482 JAR exposes two Forge identities: `gregtech` has version `MC1710`, while `gregtech_nh` has version `5.09.51.482`. FluxArc requires both IDs, pins the build version on `gregtech_nh`, and preserves loading after `gregtech`. Version 0.1.0 incorrectly pinned the artifact version on the legacy ID and was rejected by Forge; 0.1.1 corrects that declaration without widening the supported GT build.

The unmodified pack manifest is checked in as `gtnh-2.8.4-manifest.json`.
Source: https://github.com/GTNewHorizons/DreamAssemblerXXL/blob/master/releases/manifests/2.8.4.json

Forge and MCP versions are also confirmed in the fixed GT5 tag:
https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.51.482/gradle.properties

Gradle dependencies use fixed coordinates and no dynamic versions. Dependencies are compile-only: this jar must be installed into GTNH 2.8.4, and must not bundle or replace pack dependencies. The optional `.reference/maven` directory is an ignored local artifact cache populated from the official fixed-version GitHub releases; normal builds resolve the same coordinates from GTNH Maven.

## Build

Run Gradle with Java 17 and provide an installed Java 8 toolchain:

```text
python3 tools/bootstrap-dependencies.py
./gradlew -Porg.gradle.java.installations.paths=/absolute/path/to/jdk8 clean build
```

The bootstrap script is a reproducible fallback for unreliable GTNH Maven access and is also used by CI. It downloads the four fixed official development release artifacts, the official production GT JAR for dependency-loading regression fixtures, and the official shaded RetroFuturaGradle 1.4.1 release. It verifies recorded SHA256 and ZIP validity and stages minimal Maven metadata. The plugin marker uses POM packaging and points to that exact RFG artifact. Other build dependencies are fetched by Gradle. Existing matching files are verified and reused. No workstation cache or preinstalled third-party jar is required for CI.

On the development workstation, all writes are isolated under the task workspace. Gradle user home is `../.gradle-fluxarc`; the old global dependency cache is read-only via `GRADLE_RO_DEP_CACHE=C:/Users/windows10/.gradle/caches`. Minecraft/Forge transformation inputs were copied to that workspace cache. No existing mod or game instance is needed.

## Relevant fixed-version API sources

Official sources artifact:
https://github.com/GTNewHorizons/GT5-Unofficial/releases/download/5.09.51.482/gregtech-5.09.51.482-sources.jar

- `gregtech/api/interfaces/tileentity/IEnergyConnected.java`: direct GT cable energy connection, `ForgeDirection` signatures, amp-based accepted-energy contract.
- `gregtech/api/interfaces/tileentity/IColoredTileEntity.java`: colour filtering contract.
- `gregtech/api/metatileentity/implementations/MTEHatchEnergy.java`: reference GT energy hatch behavior.

Local API reference jars and extracted sources are under `.reference/` and excluded from release/source packaging.

Compilation, unit tests and reobfuscation are separate evidence from an in-game GTNH test. A successful build must never be presented as proof of client rendering, dedicated-server startup, pack balance or measured frame rate.
