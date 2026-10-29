import { AccessToken } from "livekit-server-sdk";

// No CORS headers — this is a mobile app backend, not a browser API.
// Browser-based requests will be blocked by default.

// Simple in-memory rate limiter (best-effort; Cloudflare Rate Limiting rules
// are recommended for production). Limits token minting to 10/minute per IP.
const rateLimitMap = new Map();
const RATE_LIMIT_WINDOW_MS = 60_000;
const RATE_LIMIT_MAX = 10;

function checkRateLimit(ip) {
  const now = Date.now();
  const entry = rateLimitMap.get(ip);
  if (!entry || now - entry.windowStart > RATE_LIMIT_WINDOW_MS) {
    rateLimitMap.set(ip, { windowStart: now, count: 1 });
    return true;
  }
  if (entry.count >= RATE_LIMIT_MAX) {
    return false;
  }
  entry.count++;
  return true;
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204 });
    }

    if (request.method === "GET" && url.pathname === "/") {
      return json({ ok: true, service: "mute-point-token" });
    }

    if (request.method === "POST" && url.pathname === "/token") {
      const ip = request.headers.get("CF-Connecting-IP") || "unknown";
      if (!checkRateLimit(ip)) {
        return json({ error: "rate limited" }, 429);
      }
      return handleToken(request, env);
    }

    return json({ error: "not found" }, 404);
  },
};

async function handleToken(request, env) {
  const roomCode = request.headers.get("X-Room-Code");
  if (!roomCode || roomCode !== env.ROOM_CODE) {
    return json({ error: "forbidden" }, 403);
  }

  let body;
  try {
    body = await request.json();
  } catch {
    return json({ error: "invalid json" }, 400);
  }

  const room = typeof body.room === "string" && body.room.length > 0 ? body.room : null;
  const identity =
    typeof body.identity === "string" && body.identity.length > 0 ? body.identity : null;
  if (!room || !identity) {
    return json({ error: "room and identity are required" }, 400);
  }
  if (!/^[A-Za-z0-9_-]{1,64}$/.test(room) || !/^[A-Za-z0-9_-]{1,64}$/.test(identity)) {
    return json({ error: "room and identity must be alphanumeric" }, 400);
  }

  const at = new AccessToken(env.LIVEKIT_API_KEY, env.LIVEKIT_API_SECRET, {
    identity,
    ttl: "15m",
  });
  at.addGrant({
    room,
    roomJoin: true,
    canPublish: true,
    canSubscribe: true,
    canPublishData: true,
  });

  const token = await at.toJwt();
  return json({ token, url: env.LIVEKIT_URL, room });
}

function json(payload, status = 200) {
  return new Response(JSON.stringify(payload), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
