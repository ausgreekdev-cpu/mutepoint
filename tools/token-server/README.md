# Mute Point token server (Cloudflare Worker)

Mints short-lived LiveKit join tokens so the API secret never ships in the APK.
Entry is gated by a shared `ROOM_CODE` (header `X-Room-Code`).

## Deploy (one-time, ~2 minutes)

```bash
cd tools/token-server
npm install
npx wrangler login          # browser opens; use any Cloudflare account (free tier OK)

npx wrangler secret put LIVEKIT_API_KEY      # paste your LiveKit API key
npx wrangler secret put LIVEKIT_API_SECRET   # paste your LiveKit API secret
npx wrangler secret put LIVEKIT_URL          # paste your LiveKit WebSocket URL (wss://...)
npx wrangler secret put ROOM_CODE            # paste a strong random room code (see below)

npx wrangler deploy
```

`wrangler deploy` prints your URL, e.g. `https://mute-point-token.<account>.workers.dev`.

## Smoke test

```bash
curl https://<your-url>.workers.dev/
# {"ok":true,"service":"mute-point-token"}

curl -X POST https://<your-url>.workers.dev/token \
  -H 'Content-Type: application/json' \
  -H 'X-Room-Code: <ROOM_CODE>' \
  -d '{"room":"buddy","identity":"smoke-test"}'
# {"token":"eyJ…","url":"wss://…","room":"buddy"}
```

Wrong/missing room code → `403 {"error":"forbidden"}`.

## After deploy

Put the worker URL in the app's `local.properties`:

```
LIVEKIT_TOKEN_URL=https://<your-url>.workers.dev/token
```

## Rotate the API secret

If the secret was ever pasted in chat/logs: LiveKit Cloud → Settings → Keys →
regenerate, then `npx wrangler secret put LIVEKIT_API_SECRET` with the new value.
