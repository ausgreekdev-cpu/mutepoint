import { AccessToken } from "livekit-server-sdk";

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "POST, GET, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, X-Room-Code",
};

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (request.method === "OPTIONS") {
      return new Response(null, { headers: CORS_HEADERS });
    }

    if (request.method === "GET" && url.pathname === "/") {
      return json({ ok: true, service: "mute-point-token" });
    }

    if (request.method === "POST" && url.pathname === "/token") {
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
    headers: { ...CORS_HEADERS, "Content-Type": "application/json" },
  });
}
