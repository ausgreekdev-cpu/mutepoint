import { AccessToken } from "livekit-server-sdk";

// No CORS headers — this is a mobile app backend, not a browser API.
// Browser-based requests will be blocked by default.

// Rate limiting: uses Cloudflare KV (RATE_LIMIT_KV) when bound for durability
// across isolates; falls back to in-memory (best-effort per isolate).
const rateLimitMap = new Map();
const RATE_LIMIT_WINDOW_MS = 60_000;
const RATE_LIMIT_MAX = 10;

async function checkRateLimit(ip, env) {
  const now = Date.now();

  // Prefer KV if available
  if (env && env.RATE_LIMIT_KV) {
    try {
      const key = `rl:${ip}`;
      const raw = await env.RATE_LIMIT_KV.get(key);
      let entry = null;
      if (raw) {
        try {
          entry = JSON.parse(raw);
        } catch {
          entry = null;
        }
      }
      if (!entry || now - entry.windowStart > RATE_LIMIT_WINDOW_MS) {
        entry = { windowStart: now, count: 1 };
      } else if (entry.count >= RATE_LIMIT_MAX) {
        return false;
      } else {
        entry.count++;
      }
      // KV minimum TTL is 60s
      await env.RATE_LIMIT_KV.put(key, JSON.stringify(entry), { expirationTtl: 120 });
      return true;
    } catch {
      // Fall through to in-memory
    }
  }

  // In-memory fallback
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
      const allowed = await checkRateLimit(ip, env);
      if (!allowed) {
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
