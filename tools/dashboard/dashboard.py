#!/usr/bin/env python3
"""Mute Point host control panel.

Single-file stdlib server (no dependencies) that drives and observes the
Android emulator running Mute Point: live screenshot with click-to-tap,
status pills, logcat tail, and named drive actions.

Usage:
    python3 tools/dashboard/dashboard.py [--port 8787]
Open: http://127.0.0.1:8787
"""

import argparse
import json
import os
import shutil
import subprocess
import threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse

ADB_CANDIDATES = [
    os.environ.get("ADB", ""),
    os.path.expanduser("~/android-sdk/platform-tools/adb"),
    shutil.which("adb") or "",
]
ADB = next((p for p in ADB_CANDIDATES if p and os.path.exists(p)), "adb")

APP_ID = "com.mutepoint.app"
MAIN_ACTIVITY = f"{APP_ID}/.presentation.MainActivity"

# Screen coords in device pixels (320x640 AVD). Update here if layout moves.
PIN_FIELD = (160, 342)
SAFE_ROOMS_TAB = (160, 568)
INSIGHTS_TAB = (224, 568)
MONITOR_TOGGLE = (218, 232)
RAISE_HAND = (160, 364)
PASS_BATON = (160, 380)

def _simulate_interruption():
    import random

    duration_ms = random.randint(400, 1500)
    speaking_ms = random.randint(15_000, 40_000)
    # Explicit component: implicit broadcasts to manifest receivers are
    # restricted on modern Android even when the app is foregrounded.
    adb([
        "shell", "am", "broadcast",
        "-n", f"{APP_ID}/{APP_ID}.debug.DebugSimulateReceiver",
        "-a", f"{APP_ID}.action.SIMULATE_INTERRUPTION",
        "--el", "durationMs", str(duration_ms),
        "--ef", "speakerDb", f"-{random.randint(12, 24)}.5",
        "--ef", "otherDb", f"-{random.randint(5, 14)}.0",
        "--el", "speakingMs", str(speaking_ms),
    ])
    return f"simulated {duration_ms}ms interruption (+{speaking_ms}ms speaking)"


def _vault_is_locked():
    """True when the decoy lock screen is showing (vault needs PIN)."""
    adb(["shell", "uiautomator", "dump", "/sdcard/vault_check.xml"])
    _, out = adb(["shell", "cat", "/sdcard/vault_check.xml"])
    return "Quick Note / Search" in out or "My Notes" in out


def _vault_unlock():
    if not _vault_is_locked():
        return "vault already unlocked — skipped"
    adb(["shell", "input", "tap", *map(str, PIN_FIELD)])
    adb(["shell", "input", "text", "7412"])
    adb(["shell", "input", "keyevent", "4"])
    return "typed PIN on lock screen"


NAMED_ACTIONS = {
    "launch": lambda: adb(["shell", "am", "start", "-n", MAIN_ACTIVITY]),
    "stop_app": lambda: adb(["shell", "am", "force-stop", APP_ID]),
    "vault_unlock": _vault_unlock,
    "tab_safe_rooms": lambda: adb(["shell", "input", "tap", *map(str, SAFE_ROOMS_TAB)]),
    "tab_insights": lambda: adb(["shell", "input", "tap", *map(str, INSIGHTS_TAB)]),
    "toggle_monitor": lambda: adb(["shell", "input", "tap", *map(str, MONITOR_TOGGLE)]),
    "simulate_interruption": _simulate_interruption,
    "raise_hand": lambda: adb(["shell", "input", "tap", *map(str, RAISE_HAND)]),
    "pass_baton": lambda: adb(["shell", "input", "tap", *map(str, PASS_BATON)]),
    "wake": lambda: adb(["shell", "input", "keyevent", "224"]),
    "sleep_screen": lambda: adb(["shell", "input", "keyevent", "223"]),
}


def adb(args, binary=False):
    proc = subprocess.run(
        [ADB, *args],
        capture_output=True,
        timeout=15,
    )
    if binary:
        return proc.returncode, proc.stdout
    return proc.returncode, proc.stdout.decode("utf-8", errors="replace")


def device_serial():
    code, out = adb(["devices"])
    if code != 0:
        return None
    for line in out.splitlines()[1:]:
        parts = line.split()
        if len(parts) >= 2 and parts[1] == "device":
            return parts[0]
    return None


def get_screenshot():
    code, data = adb(["exec-out", "screencap", "-p"], binary=True)
    if code != 0 or not data:
        return None
    return data


def get_status():
    serial = device_serial()
    if serial is None:
        return {"device": None, "online": False}

    _, wm = adb(["shell", "wm", "size"])
    size = "320x640"
    for token in wm.split():
        if "x" in token and token.split("x")[0].isdigit():
            size = token.split(":")[-1] if ":" in token else token

    _, focus = adb(["shell", "dumpsys", "activity", "activities"])
    focused_app = ""
    for line in focus.splitlines():
        if "topResumedActivity=" in line or "ResumedActivity:" in line:
            focused_app = line.strip()
            break

    _, ps = adb(["shell", "pidof", APP_ID])
    app_pid = ps.strip() or None

    _, notif = adb(["shell", "dumpsys", "notification"])
    # Only count the monitor FGS: our package + monitor channel + the
    # FOREGROUND_SERVICE flag. (Interruption notif id=42 shares the channel
    # but is not FGS; unrelated system FGS records can mention the channel.)
    def _is_monitor_fgs(block: str) -> bool:
        return (
            "pkg=com.mutepoint.app" in block
            and "mute_point_monitor" in block
            and "FOREGROUND_SERVICE" in block
        )

    monitoring = False
    current_record = []
    for line in notif.splitlines():
        if "NotificationRecord" in line:
            block = "\n".join(current_record)
            if _is_monitor_fgs(block):
                monitoring = True
                break
            current_record = [line]
        else:
            current_record.append(line)
    if not monitoring and _is_monitor_fgs("\n".join(current_record)):
        monitoring = True

    _, batterystats = adb(["shell", "dumpsys", "battery"])
    battery_level = None
    for line in batterystats.splitlines():
        if "level:" in line:
            battery_level = line.split(":")[-1].strip()
            break

    _, screen = adb(["shell", "dumpsys", "power"])
    screen_on = "mWakefulness=Awake" in screen or "mScreenOn=true" in screen

    return {
        "online": True,
        "device": serial,
        "screenSize": size,
        "focusedApp": focused_app,
        "appPid": app_pid,
        "monitoring": monitoring,
        "battery": battery_level,
        "screenOn": screen_on,
    }


def get_logcat(tail=200):
    code, out = adb(["logcat", "-d", "-v", "time", "-t", str(tail)])
    if code != 0:
        return ""
    keep = []
    for line in out.splitlines():
        low = line.lower()
        if any(tag in low for tag in ("mutepoint", "androidruntime", "vadengine",
                                      "monitor", "room", "sqlite", "hilt")):
            keep.append(line)
    return "\n".join(keep[-tail:])


HTML_PAGE = r"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>Mute Point — Control Panel</title>
<style>
  :root { color-scheme: dark; }
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
    background: #0d1117; color: #c9d1d9; min-height: 100vh; padding: 16px;
  }
  h1 { font-size: 18px; margin-bottom: 12px; color: #58a6ff; }
  .layout { display: flex; gap: 16px; align-items: flex-start; flex-wrap: wrap; }
  .left { flex: 0 0 auto; }
  .right { flex: 1 1 420px; min-width: 380px; display: flex; flex-direction: column; gap: 12px; }
  #screen-wrap {
    position: relative; width: 320px; height: 640px; border: 2px solid #30363d;
    border-radius: 8px; overflow: hidden; background: #000; cursor: crosshair;
  }
  #screen { width: 100%; height: 100%; object-fit: fill; display: block; }
  .panel { background: #161b22; border: 1px solid #30363d; border-radius: 8px; padding: 12px; }
  .panel h2 { font-size: 13px; color: #8b949e; margin-bottom: 8px; text-transform: uppercase; letter-spacing: .06em; }
  .pills { display: flex; flex-wrap: wrap; gap: 8px; }
  .pill { padding: 4px 10px; border-radius: 999px; font-size: 12px; border: 1px solid #30363d; }
  .pill.ok { background: #0d2818; color: #3fb950; border-color: #238636; }
  .pill.bad { background: #2d1215; color: #f85149; border-color: #da3633; }
  .pill.neutral { background: #21262d; color: #8b949e; }
  .actions { display: flex; flex-wrap: wrap; gap: 8px; }
  button {
    background: #21262d; color: #c9d1d9; border: 1px solid #30363d; border-radius: 6px;
    padding: 6px 12px; font: inherit; font-size: 12px; cursor: pointer;
  }
  button:hover { background: #30363d; border-color: #8b949e; }
  button.primary { background: #1f6feb; border-color: #1f6feb; color: #fff; }
  button.danger { background: #b62324; border-color: #b62324; color: #fff; }
  #logcat {
    height: 340px; overflow-y: auto; background: #0d1117; border: 1px solid #30363d;
    border-radius: 6px; padding: 8px; font-size: 10.5px; line-height: 1.45; white-space: pre-wrap;
  }
  #tap-info { font-size: 11px; color: #8b949e; margin-top: 6px; }
  .status-line { font-size: 12px; color: #8b949e; margin-top: 6px; word-break: break-all; }
</style>
</head>
<body>
<h1>Mute Point — Control Panel</h1>
<div class="layout">
  <div class="left">
    <div id="screen-wrap">
      <img id="screen" alt="device screenshot">
    </div>
    <div id="tap-info">click screen to tap · polls every 2s</div>
  </div>
  <div class="right">
    <div class="panel">
      <h2>Status</h2>
      <div class="pills" id="pills"><span class="pill neutral">loading…</span></div>
      <div class="status-line" id="focus-line"></div>
    </div>
    <div class="panel">
      <h2>Drive</h2>
      <div class="actions">
        <button class="primary" onclick="act('launch')">Launch app</button>
        <button onclick="act('vault_unlock')">Vault unlock (PIN)</button>
        <button onclick="act('tab_safe_rooms')">Safe Rooms tab</button>
        <button onclick="act('tab_insights')">Insights tab</button>
        <button class="danger" onclick="act('toggle_monitor')">Start/Stop monitoring</button>
        <button class="primary" onclick="act('simulate_interruption')">Simulate interruption</button>
        <button onclick="act('raise_hand')">Raise hand</button>
        <button onclick="act('pass_baton')">Pass baton</button>
        <button onclick="act('wake')">Wake</button>
        <button onclick="act('sleep_screen')">Sleep</button>
        <button class="danger" onclick="act('stop_app')">Force-stop app</button>
      </div>
    </div>
    <div class="panel">
      <h2>Logcat (filtered)</h2>
      <div id="logcat"></div>
    </div>
  </div>
</div>
<script>
const screenImg = document.getElementById('screen');
const pills = document.getElementById('pills');
const focusLine = document.getElementById('focus-line');
const logcatEl = document.getElementById('logcat');
let devW = 320, devH = 640;

async function pollScreen() {
  try {
    const res = await fetch('/api/screenshot', {cache: 'no-store'});
    if (res.ok) {
      const blob = await res.blob();
      screenImg.src = URL.createObjectURL(blob);
    }
  } catch (e) {}
}

async function pollStatus() {
  try {
    const res = await fetch('/api/status');
    const s = await res.json();
    if (!s.online) {
      pills.innerHTML = '<span class="pill bad">no device</span>';
      focusLine.textContent = '';
      return;
    }
    if (s.screenSize) {
      const [w, h] = s.screenSize.split('x').map(Number);
      if (w && h) { devW = w; devH = h; }
    }
    const mk = (label, ok, neutral) =>
      `<span class="pill ${neutral ? 'neutral' : (ok ? 'ok' : 'bad')}">${label}</span>`;
    pills.innerHTML =
      mk('device ' + s.device, true, true) +
      mk('screen ' + s.screenSize, true, true) +
      mk(s.screenOn ? 'awake' : 'asleep', s.screenOn) +
      mk(s.appPid ? 'app pid ' + s.appPid : 'app not running', !!s.appPid) +
      mk(s.monitoring ? 'MONITORING' : 'idle', s.monitoring) +
      mk(s.battery ? 'bat ' + s.battery + '%' : 'bat ?', true, !s.battery);
    focusLine.textContent = s.focusedApp || '';
  } catch (e) {}
}

async function pollLogcat() {
  try {
    const res = await fetch('/api/logcat');
    const text = await res.text();
    logcatEl.textContent = text || '(no matching lines)';
    logcatEl.scrollTop = logcatEl.scrollHeight;
  } catch (e) {}
}

async function act(name) {
  await fetch('/api/action', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({type: 'named', name}),
  });
  setTimeout(pollScreen, 400);
}

document.getElementById('screen-wrap').addEventListener('click', async (ev) => {
  const rect = screenImg.getBoundingClientRect();
  const x = Math.round((ev.clientX - rect.left) / rect.width * devW);
  const y = Math.round((ev.clientY - rect.top) / rect.height * devH);
  document.getElementById('tap-info').textContent = `tap (${x}, ${y}) sent`;
  await fetch('/api/action', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({type: 'tap', x, y}),
  });
  setTimeout(pollScreen, 400);
});

pollScreen(); pollStatus(); pollLogcat();
setInterval(pollScreen, 2000);
setInterval(pollStatus, 2000);
setInterval(pollLogcat, 3000);
</script>
</body>
</html>
"""


class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt, *args):
        pass

    def _send(self, code, content_type, body):
        if isinstance(body, str):
            body = body.encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        path = urlparse(self.path).path
        if path == "/":
            self._send(200, "text/html; charset=utf-8", HTML_PAGE)
        elif path == "/api/screenshot":
            data = get_screenshot()
            if data is None:
                self._send(503, "application/json", json.dumps({"error": "screencap failed"}))
            else:
                self._send(200, "image/png", data)
        elif path == "/api/status":
            self._send(200, "application/json", json.dumps(get_status()))
        elif path == "/api/logcat":
            self._send(200, "text/plain; charset=utf-8", get_logcat())
        else:
            self._send(404, "application/json", json.dumps({"error": "not found"}))

    def do_POST(self):
        path = urlparse(self.path).path
        if path != "/api/action":
            self._send(404, "application/json", json.dumps({"error": "not found"}))
            return
        try:
            length = int(self.headers.get("Content-Length", "0"))
            payload = json.loads(self.rfile.read(length) or b"{}")
            kind = payload.get("type")
            if kind == "named":
                name = payload.get("name")
                fn = NAMED_ACTIONS.get(name)
                if fn is None:
                    self._send(400, "application/json", json.dumps({"error": f"unknown action {name}"}))
                    return
                result = fn()
                self._send(200, "application/json", json.dumps({"ok": True, "result": str(result)}))
            elif kind == "tap":
                x, y = int(payload["x"]), int(payload["y"])
                adb(["shell", "input", "tap", str(x), str(y)])
                self._send(200, "application/json", json.dumps({"ok": True}))
            elif kind == "swipe":
                x1, y1, x2, y2 = (int(payload[k]) for k in ("x1", "y1", "x2", "y2"))
                dur = int(payload.get("durationMs", 400))
                adb(["shell", "input", "swipe", str(x1), str(y1), str(x2), str(y2), str(dur)])
                self._send(200, "application/json", json.dumps({"ok": True}))
            elif kind == "text":
                adb(["shell", "input", "text", str(payload.get("text", ""))])
                self._send(200, "application/json", json.dumps({"ok": True}))
            elif kind == "key":
                adb(["shell", "input", "keyevent", str(payload.get("keyCode", 4))])
                self._send(200, "application/json", json.dumps({"ok": True}))
            else:
                self._send(400, "application/json", json.dumps({"error": f"unknown type {kind}"}))
        except Exception as exc:  # noqa: BLE001 — surface any drive error to the panel
            self._send(500, "application/json", json.dumps({"error": str(exc)}))


def main():
    parser = argparse.ArgumentParser(description="Mute Point control panel")
    parser.add_argument("--port", type=int, default=8787)
    parser.add_argument("--host", default="127.0.0.1")
    args = parser.parse_args()

    server = ThreadingHTTPServer((args.host, args.port), Handler)
    print(f"Mute Point control panel → http://{args.host}:{args.port}")
    print(f"adb: {ADB}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()
