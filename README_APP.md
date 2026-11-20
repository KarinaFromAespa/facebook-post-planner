# Facebook Post Planner — Android app (Kotlin + Jetpack Compose)

V1, locked 2026-11-20. This is the `app/` side of the monorepo
`KarinaFromAespa/facebook-post-planner`. Copy this whole `app/` folder
(plus the root `settings.gradle.kts`, `build.gradle.kts`,
`gradle.properties`, `gradle/libs.versions.toml`) into
`D:\Test\facebook-post-planner\`, next to `backend/`.

## What it does
- **Publish tab (home):** Today list, hourly slots 08:00–18:00, one card
  per scheduled item (time / caption / media collage, status chip, 3-dot
  behaviour via Publish now / Delete / Retry). Multiple items can share
  a slot, each with its own status/failure. Pause / Resume queue
  (Buffer-style, nothing deleted).
- **Create tab:** composer for feed post / reel (single MP4) / story
  (photo in v1), text + photos, hashtags. Schedule to an hour, save
  as draft (no time), or Publish Now (bypasses the queue entirely).
- **Insights tab:** earnings-first. Total approx. earnings + per-post
  earnings ranking + engagement. If the Page is not enrolled in Content
  Monetization, the whole earnings section shows one 'Not monetized'
  message with the criteria — never per-post $0. Engagement / best
  performer ranking still shows.
- No Community tab, no 'Created by …' line, no recycle/repost,
  no home-screen widgets. Bottom nav: Create / Publish / Insights only.

## How it works
- Local-first: queue and drafts live in Room (`planner.db`) so the
  Today list works offline. Every create / publish-now / pause also
  calls the Node backend, which owns the 1-minute publisher, Oracle
  Object Storage (media deleted after confirmed publish), and Facebook
  publishing. If the backend is unreachable, the local copy is kept
  and the error is shown — nothing is silently dropped.
- Backend URL: `planner.backend.url` in `gradle.properties`
  (default `http://129.225.82.58:3000`), changeable at runtime via
  `SettingsStore` (DataStore). Short-notice 2–10 min ahead works via
  the app's own queue + the backend's 1-minute publisher, not
  Facebook native scheduling.
- Crop: v1 relies on the system photo picker / editor before the URI
  reaches the composer — no custom cropper is shipped.

## Build (on your PC, Android Studio in D:\Programs\Android)
1. Open `D:\Test\facebook-post-planner` in Android Studio.
2. Sync Gradle, then Run on a device / emulator (minSdk 26).
3. Start the backend first (`backend/` — Node), or the app will show
   'Backend unreachable' and work from the local queue only.

## Files
- `app/build.gradle.kts` — Compose BOM, Room, Retrofit, Coil, DataStore, WorkManager
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/facebookpostplanner/MainActivity.kt` — nav (Create / Publish / Insights)
- `app/src/main/java/com/facebookpostplanner/data/local/` — Room: `Post`, `PostDao`, `AppDatabase`, `InsightEntity`
- `app/src/main/java/com/facebookpostplanner/data/remote/` — Retrofit `PlannerApi`, `ApiFactory` + `SettingsStore`
- `app/src/main/java/com/facebookpostplanner/data/PlannerRepository.kt` — local-first queue/drafts/publish/pause/insights
- `app/src/main/java/com/facebookpostplanner/ui/publish/` — Today list + hourly slots
- `app/src/main/java/com/facebookpostplanner/ui/create/` — composer
- `app/src/main/java/com/facebookpostplanner/ui/insights/` — earnings-first insights
- `app/src/main/java/com/facebookpostplanner/ui/components/PostCard.kt`
- `app/src/main/java/com/facebookpostplanner/ui/theme/Theme.kt`

## Not yet wired (needs your side)
- Facebook Page token / developer setup — the backend needs it before
  any real publish works. The app's Publish Now will report the
  backend's error until then.
- Oracle Object Storage + Autonomous DB credentials for the backend.
- The backend itself (`backend/`) is a separate step — this delivery
  is the Android app code only.
