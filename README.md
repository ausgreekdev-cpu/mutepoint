# Mute Point

A safety-oriented Android app that helps people practice assertive communication and monitor conversations. Built with stealth design principles — the app appears as "My Notes & Ideas" on the home screen.

## Features

- **PIN Vault** — App-level PIN protection with panic gesture (triple-tap) and emergency exit
- **Safe Rooms** — LiveKit-powered audio rooms with baton-style turn-taking
- **Voice Activity Detection** — ONNX-based VAD for interruption detection
- **Reality Journal** — Encrypted journaling with SQLCipher + Android Keystore
- **Insights** — Session analytics with interruption tracking
- **Practice Cards** — Assertive communication practice exercises

## Security

- All data encrypted at rest (SQLCipher + Android Keystore)
- PIN brute-force protection with exponential backoff
- `FLAG_SECURE` prevents screenshots/screen recording
- Stealth launcher icon and app name
- No data leaves the device except LiveKit audio (when in a Safe Room)

## Build

### Prerequisites
- Android Studio (latest stable)
- JDK 17
- Android SDK (API 26+)
- `local.properties` with LiveKit configuration (see below)

### Setup
1. Clone the repo
2. Create `local.properties` in the project root:
   ```
   sdk.dir=/path/to/android-sdk
   LIVEKIT_URL=wss://your-livekit-url
   LIVEKIT_TOKEN_URL=https://your-token-server/token
   LIVEKIT_ROOM_NAME=buddy
   LIVEKIT_ROOM_CODE=your-room-code
   ```
3. Create `keystore.properties` from `keystore.properties.example` (for release builds)
4. Open in Android Studio and build

### Token Server
See `tools/token-server/README.md` for Cloudflare Worker deployment instructions.

## Architecture

```
app/
  core/          # Security, audio, DI, services
  data/          # Repositories, DAOs, LiveKit integration
  domain/        # Models, use cases, repositories
  presentation/  # Compose UI, ViewModels, navigation
tools/
  token-server/  # Cloudflare Worker (LiveKit token minting)
  dashboard/     # Analytics dashboard (Python)
```

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **DI:** Hilt
- **Database:** Room + SQLCipher
- **Audio:** ONNX Runtime (VAD), LiveKit Android SDK
- **Networking:** HttpURLConnection (token fetch)
- **Build:** Gradle (Kotlin DSL)

## Testing

```bash
# Unit tests
./gradlew testDebugUnitTest

# Instrumented tests (requires emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lintDebug

# Static analysis
./gradlew detekt
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

Private — not for redistribution.
