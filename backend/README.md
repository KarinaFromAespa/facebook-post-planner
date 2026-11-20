# Facebook Post Planner — backend (Node)

Queue-and-publish backend for the Android app and the website. Matches the
app's `PlannerApi` exactly: `GET /api/queue`, `GET /api/drafts`,
`POST /api/posts`, `POST /api/posts/:id/media`,
`POST /api/posts/:id/publish-now`, `GET /api/queue/state`,
`POST /api/queue/pause`, `POST /api/queue/resume`, `GET /api/insights`,
`GET /api/pages`, `POST /api/pages/connect`, `DELETE /api/pages/:id`,
plus `GET /health`.

## Run

```sh
npm install
npm start
```

No env vars are needed to run it. With none set it uses a local JSON
store (`data/planner.json`, gitignored) and local `uploads/`, and
publishing returns a clear `failed` with "Facebook Page token not
configured" — it never invents a published post id or earnings.

## How it works

- **Queue:** posts are `draft` / `scheduled` / `publishing` /
  `published` / `failed`, matching the app's `PostStatus`.
  Multiple items can share one time slot; each publishes independently,
  so one failure never blocks the others.
- **1-minute publisher:** every 60 seconds it picks up `scheduled`
  posts with `scheduledAt <= now`, unless the queue is paused.
  Short-notice 2–10 min ahead works through this, not Facebook native
  scheduling (which needs 10+ minutes).
- **Publish Now:** `POST /api/posts/:id/publish-now` publishes
  immediately, bypassing the queue entirely.
- **Pause / Resume:** `POST /api/queue/pause` holds all scheduled
  posts without deleting them; `resume` releases them.
- **Media:** uploaded files go to `uploads/` by default. A file is
  deleted only after its post is confirmed published; on failure it is
  kept for retry. Facebook fetches media by URL, so `PUBLIC_BASE_URL`
  must be the backend's public address (e.g.
  `http://129.225.82.58:25565`) — without it the URLs are relative and
  only work through the website's `/uploads` proxy. Oracle Object
  Storage is the production target:
  set `ORACLE_OBJECT_STORAGE_NAMESPACE`, `ORACLE_OBJECT_STORAGE_BUCKET`
  and `ORACLE_OBJECT_STORAGE_REGION` — the media module is the one
  place to swap the local store for Object Storage, and a 7-day
  lifecycle rule on the bucket is the safety net for orphaned files.
  Scheduling horizon is 1–2 days.
- **Connected Pages:** `POST /api/pages/connect` stores a Page
  (`pageId`, `pageName`, Page access token) in `data/planner.json`.
  A post's `pageId` picks which Page it publishes as; without one it
  falls back to the env-configured Page. Short-lived tokens are
  exchanged for long-lived ones (60 days) when `FB_APP_ID` +
  `FB_APP_SECRET` are set as backend env vars. `GET /api/pages`
  lists connected Pages (tokens never exposed).
- **Insights:** earnings-first. With `FB_PAGE_TOKEN` and `FB_PAGE_ID`
  set it reads `monetization_approximate_earnings` /
  `content_monetization_earnings` from Graph API v23 and per-post
  likes / comments / shares / views, ranked by engagement. Without a
  token, or when the Page is not enrolled, it returns
  `monetized: false` with a whole-section criteria message — never
  per-post $0, and never invented earnings.
- **Facebook publishing:** a connected Page (via
  `POST /api/pages/connect`) or `FB_PAGE_TOKEN` (long-lived Page token
  with `pages_manage_posts`) and `FB_PAGE_ID` as env vars, never in
  the APK or the repo. Publishing uses real POSTs to the Graph API;
  a post is only marked published when Facebook returns a post ID —
  anything else is a failure and the media is kept for retry. Feed
  post = text or multi-photo, reel = single MP4, story = one photo
  in v1.

## Oracle Autonomous DB

The local JSON store is the v1 default so the backend runs anywhere
with no credentials. The store is isolated in `src/store.js` — swap
that one module for an Oracle Autonomous DB adapter (env
`ORACLE_DB_USER`, `ORACLE_DB_PASSWORD`, `ORACLE_DB_CONNECT_STRING`)
when the DB is provisioned. The 1-minute publisher keeps the
Autonomous DB awake by querying it on every tick.

## Deploy (Oracle Always Free VM, 1 GB)

Node is light enough for the 1 GB VM. Run it as a systemd service
or under `pm2`, with `PORT=25565` and the env vars above. The app's
default backend URL is `http://129.225.82.58:25565`.
