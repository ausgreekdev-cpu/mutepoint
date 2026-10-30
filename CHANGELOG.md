# Changelog

All notable changes to Mute Point are documented here.

## [1.1.0] - 2026-10-29

### Added
- PIN vault with panic gesture and emergency exit
- Safe Rooms with LiveKit audio and baton turn-taking
- Voice activity detection (ONNX-based VAD)
- Reality journal with SQLCipher encryption
- Session analytics and insights
- Practice cards for assertive communication
- Settings: relock policy, screenshot blocking
- PIN rate limiting with exponential backoff
- Network security config (TLS-only)
- CI/CD: GitHub Actions for build, test, lint, secret scan, worker deploy, release
- Detekt static analysis
- Database migration test

### Security
- Randomized default PIN per-install
- Certificate pinning via Network Security Config
- Empty catch blocks now log warnings
- ProGuard smoke test in CI
- Git history scrubbed of exposed secrets

### Fixed
- Broken `.gitignore` PDF exclusion pattern
- `keystore.properties` and `local.properties` properly gitignored
