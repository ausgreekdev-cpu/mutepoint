# Privacy Policy — Mute Point

**Last updated:** October 2026

## Overview

Mute Point is a safety-oriented Android application. Your privacy is fundamental to its design. This policy describes what data the app collects, how it's used, and your rights.

## Data Collection

### What Mute Point collects

| Data | Purpose | Storage |
|---|---|---|
| Journal entries (trigger + reality anchor) | Personal reflection | On-device only (encrypted) |
| Session analytics (speaking time, interruptions) | Self-awareness insights | On-device only (encrypted) |
| Practice card progress | Track practice completion | On-device only (encrypted) |
| PIN hash + salt | App access control | On-device only (SharedPreferences) |
| Anonymous device identity | LiveKit room participation | On-device only (SharedPreferences) |

### What Mute Point does NOT collect

- **No personal identifiers** — no name, email, phone number, or account
- **No location data** — GPS or network location is never accessed
- **No contacts** — the contacts list is never read
- **No analytics/telemetry** — no usage data is sent to any server
- **No advertising** — no ad SDKs or tracking pixels

### Audio data

- Audio is processed **on-device** for voice activity detection (VAD)
- When using Safe Rooms, audio is transmitted via LiveKit (encrypted in transit via WSS/TLS)
- Audio is **never recorded to disk** — only real-time VAD metrics (dB levels, speaking duration) are stored
- The app does not store raw audio recordings

### Network connections

The app makes network connections only to:
1. **Your configured LiveKit server** — for Safe Room audio (only when you join a room)
2. **Your configured token server** — to obtain a short-lived join token (15-minute TTL)

No other network requests are made. No third-party analytics, crash reporting (in release builds), or advertising SDKs are included.

## Data Storage & Security

- All data is stored locally on your device
- Database is encrypted with SQLCipher (AES-256)
- Encryption key is wrapped by Android Keystore (hardware-backed where available)
- `allowBackup=false` — data is not backed up to Google
- `dataExtractionRules=@null` — no data extraction via ADB

## Data Sharing

Mute Point does **not** share any data with third parties. The only data that leaves your device is:
- LiveKit audio (when you actively join a Safe Room)
- Token request to your configured token server (contains room name + anonymous identity)

## Data Retention & Deletion

- Data is retained on-device until you delete it
- You can delete individual journal entries or all data via Settings → Clear Data
- Uninstalling the app removes all data

## Your Rights

- **Access:** All your data is visible within the app
- **Deletion:** Delete entries individually or clear all app data
- **Portability:** Export journal entries and session analytics via the Share button
- **No tracking:** You can verify no network requests are made (use a firewall or proxy)

## Contact

For privacy questions, contact the project maintainers via the repository's issue tracker.

## Changes

This policy may be updated. Changes will be noted in the app's changelog.
