# CampusBusTracker Backend

Two endpoints, matching what the Android app expects:

- `POST /api/scan` — body: `{ "bus_code": "...", "qr_code_value": "..." }`
- `GET /api/status` — returns: `{ "buses": [ { "bus_id", "bus_code", "checkpoint_name", "scanned_at" } ] }`

## Deploy steps (see chat for full walkthrough)
1. Push this folder to a GitHub repo (or `vercel` CLI directly from this folder).
2. Import into Vercel.
3. In the Vercel dashboard: Storage tab → Create Database → KV (Redis) → Connect to this project.
   This automatically adds the required environment variables.
4. Deploy. Copy the resulting URL into `ApiClient.kt` in the Android project.
