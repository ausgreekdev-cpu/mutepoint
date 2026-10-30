# Security Policy

## Supported Versions

Only the latest release is actively supported with security updates.

## Reporting a Vulnerability

Please report security issues privately to the project maintainers. Do not open public GitHub issues for security vulnerabilities.

## Secret Management

### What is committed to this repository

- **Nothing secret.** The repository contains only source code, build configuration, and documentation.
- All secrets are loaded at build/runtime from gitignored files or Cloudflare Worker secrets.

### Required local files (gitignored)

| File | Purpose | How to create |
|---|---|---|
| `local.properties` | LiveKit URL, token endpoint, room name, room code | Copy from a teammate or set up a LiveKit Cloud project |
| `keystore.properties` | Release signing keystore credentials | Copy `keystore.properties.example` and fill in real values |
| `keystore/mutepoint-release.jks` | Release signing key | Generate via Android Studio or `keytool` |

### Cloudflare Worker secrets

Set via `npx wrangler secret put <NAME>`:

- `LIVEKIT_API_KEY`
- `LIVEKIT_API_SECRET`
- `LIVEKIT_URL`
- `ROOM_CODE`

## Secret Rotation

### LiveKit API Key / Secret

1. Log in to [LiveKit Cloud](https://cloud.livekit.io) → Project → Settings → Keys
2. Click **Regenerate** on the key pair
3. Update the worker: `npx wrangler secret put LIVEKIT_SECRET` in `tools/token-server/`
4. The API key is now inactive; the new key is active immediately

### Room Code (shared between app and worker)

1. Generate a new room code: `openssl rand -base64 12`
2. Update `local.properties`: `LIVEKIT_ROOM_CODE=<new_code>`
3. Rebuild the app (the code is embedded at build time via `buildConfigField`)
4. Update the worker: `npx wrangler secret put ROOM_CODE` in `tools/token-server/`
5. Deploy the worker: `npx wrangler deploy`

> ⚠️ Changing the room code invalidates all previously built APKs. Existing app installs must update to the new build before they can connect.

### Signing Keystore

If the release keystore is compromised or lost:

1. Generate a new keystore:
   ```bash
   keytool -genkey -v -keystore keystore/mutepoint-release.jks \
     -alias mutepoint -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Update `keystore.properties` with the new passwords
3. **Note**: Changing the signing key means you cannot update an existing Play Store listing. You must publish as a new app with a different `applicationId`.

### SQLCipher Database Passphrase

The SQLCipher passphrase is wrapped by Android Keystore (AES-256-GCM) and stored encrypted in SharedPreferences. It is unique per install and is **not** shared across devices.

To force a passphrase rotation on a user's device, clear app data (Settings → Apps → Mute Point → Storage → Clear Data).

## Security Architecture

### At Rest

| Data | Protection |
|---|---|
| Journal entries, session analytics, practice cards | SQLCipher (AES-256), passphrase wrapped by Android Keystore |
| PIN hash | SHA-256 with per-install 16-byte salt, stored in SharedPreferences |
| Android Keystore key | AES-256-GCM, hardware-backed where available |

### In Transit

| Channel | Protection |
|---|---|
| LiveKit WebSocket | WSS (TLS) |
| Token server | HTTPS (Cloudflare) |

### PIN Brute-Force Protection

- After 5 consecutive failed attempts, an exponential backoff is enforced (30s, 60s, 120s, up to 5 min)
- Failure counter resets on successful entry
- Lockout state is persisted in SharedPreferences

### PIN Recovery

There is no automated PIN recovery. If a user forgets their PIN:
- The app interface remains locked
- Data is NOT lost — the SQLCipher passphrase is independent of the PIN
- To recover: clear app data (Settings → Apps → Mute Point → Storage → Clear Data)
- This wipes the PIN but preserves the database (passphrase is in Android Keystore, not cleared by data wipe)

> ⚠️ Clearing app data resets the PIN to a new random value but the database remains accessible via the Keystore-wrapped passphrase.

### Stealth / Safety Features

- App icon labeled as "My Notes & Ideas" (decoy launcher)
- `FLAG_SECURE` prevents screenshots and screen recording
- Panic gesture (triple-tap top bar) triggers emergency exit
- Emergency exit stops monitoring, navigates to home screen, and removes the activity from recents
- `allowBackup=false` prevents Google backup of app data

## Known Considerations

- The PIN gates the UI only; the actual data encryption uses an independent SQLCipher passphrase managed by Android Keystore
- LiveKit server-side: ensure the LiveKit project has appropriate room settings (e.g., max participants, token TTL)
- Cloudflare Worker: consider enabling Cloudflare Rate Limiting rules for additional protection beyond the in-worker limiter
