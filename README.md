# Facebook Post Planner

Buffer-style queue-and-publish app for Facebook Pages. Private repo.

## Layout
- `app/` - Android app (Kotlin + Jetpack Compose)
- `backend/` - Node backend (Oracle Autonomous DB + Oracle Object Storage, 1-min publisher)

## V1 spec (locked 2026-11-20)
- Publish tab as home: Today list, hourly slots, time / caption / media collage card, + FAB. Bottom nav: Create / Publish / Insights only.
- Composer: feed post / reel (single MP4) / story (photo in v1), text + photos, crop, hashtags.
- Unlimited queue, no cap. Multiple items can share one time slot, each with its own status/failure.
- Batch schedule in the morning (~8:00) for the day to ~18:00, roughly hourly. Short-notice 2-10 min ahead via own queue + 1-min publisher (not Facebook native scheduling).
- Drafts are a separate state. Publish Now bypasses the queue entirely. Pause/Resume queue kept (Buffer-style).
- No Community tab. No 'Created by ...' line. No recycle/repost. No home-screen widgets.
- Insights: earnings-first. Total approx. earnings + per-post earnings ranking + engagement (likes/comments/shares/views). If not enrolled in Content Monetization, show a whole-section 'Not monetized' message with criteria, not per-post $0.
- Storage: Oracle Object Storage for scheduled media, deleted after confirmed publish. Scheduling horizon 1-2 days.
