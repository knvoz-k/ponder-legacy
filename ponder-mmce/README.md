# MMCE optional integration sources

The Cleanroom edition compiles this directory's Java adapter and tests into the
main Ponder Legacy build. **Do not install a separate Ponder-MMCE addon.** MMCE
2.3.2 remains an optional, independently installed dependency; its classes are
never bundled into Ponder.

See the current [integration guide](../docs/MMCE.md) for Java, ZenScript, JSON,
blueprint mapping and compatibility limits. Static and dynamic structures,
preview NBT, named groups and stable fingerprints are supported. Tutorials must
be authored explicitly; examples are never installed automatically.

From the repository root, using Java 25:

```bash
./tools/dev.sh build reobfMmceHarnessJar
PONDER_CLEANROOM_VERSION=0.4.0-alpha python3 tools/verify-cleanroom.py client --mmce present
PONDER_CLEANROOM_VERSION=0.4.0-alpha python3 tools/verify-cleanroom.py server --mmce present
```

Repeat with `--mmce absent` / `--mmce incompatible` and with
`PONDER_CLEANROOM_VERSION=0.4.0` for the full matrix. Results, package hashes and
screenshots are recorded in [runtime verification](../docs/CLEANROOM-VERIFICATION.md).
The fixtures in `examples/smoke/` are copied only into isolated verification instances.

The old standalone `build.gradle`, `PonderMMCEMod` and `verification/` harnesses
are retained from the Forge baseline for reference. They are not Gradle
subprojects or runtime entrypoints in this Cleanroom build; the current harness
sources live in `src/mmceHarness/` at the repository root.
