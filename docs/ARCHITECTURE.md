# Architecture — Mute Point

## Overview

Mute Point is an Android app with a Cloudflare Worker backend. It follows Clean Architecture with three layers: presentation, domain, and data.

## Module Structure

```
app/
  core/          # Cross-cutting concerns
    security/    # PIN vault, SQLCipher passphrase, relock policy
    audio/       # Audio recording, VAD engine
    di/          # Hilt modules
    service/     # Foreground monitoring service
    ui/          # Theme, colors, typography
  data/          # Data layer
    local/       # Room database, DAOs, entities
    livekit/     # LiveKit room management, baton sync
    repository/  # Repository implementations
    mapper/      # Entity ↔ domain mappers
  domain/        # Business logic
    model/       # Domain models
    repository/  # Repository interfaces
    usecase/     # Use cases (BatonSyncReducer, InterruptionDetector, etc.)
  presentation/  # UI layer
    security/    # Vault container, panic gesture, emergency exit
    journal/     # Reality journal screens + ViewModel
    practice/    # Practice cards screens + ViewModel
    saferoom/    # Safe room screens + ViewModel
    analytics/   # Insights screens + ViewModels
    settings/    # Settings screen
    support/     # Support resources screen
```

## Key Design Decisions

### Stealth Design
The app presents itself as "My Notes & Ideas" with a decoy launcher icon. The real functionality is behind a PIN gate. This is a safety feature for users in controlling relationships.

### Encryption
- Database: SQLCipher (AES-256) with passphrase wrapped by Android Keystore
- PIN: Salted SHA-256 (UI gating only; actual data protection is via SQLCipher + Keystore)
- Keystore key: AES-256-GCM, hardware-backed where available

### Audio Processing
- Voice Activity Detection uses Silero VAD (ONNX model) running on-device
- Interruption detection compares speaker vs. other dB levels
- No audio is recorded to disk; only metrics (dB, duration) are stored

### LiveKit Integration
- Token server is a Cloudflare Worker that mints short-lived JWTs
- Room access gated by shared room code (header `X-Room-Code`)
- Baton (turn-taking) state synced via LiveKit data channel
- Anonymous identity = random UUID (no hardware IDs)

### Navigation
- Jetpack Navigation 3 with per-tab back stacks
- 5 tabs: Journal, Practice, Safe Rooms, Insights, Support
- Settings accessible from Support tab

## Data Flow

```
UI (Compose) → ViewModel → UseCase → Repository → DAO → Room/SQLCipher
                                    ↓
                              LiveKit Room Manager → Cloudflare Worker → LiveKit Cloud
```

## Security Boundaries

| Boundary | Protection |
|---|---|
| App launch | PIN gate (VaultContainer) |
| Data at rest | SQLCipher + Android Keystore |
| Data in transit | TLS (WSS/HTTPS) |
| Screenshots | FLAG_SECURE |
| Backups | Disabled (allowBackup=false) |
| Debug builds | Simulated receiver (DUMP permission) |
