-- Neon Postgres (plannerdr) DR schema for the Facebook Post Planner.
-- Already applied; kept here as the reference DDL.
CREATE TABLE IF NOT EXISTS posts (
  id TEXT PRIMARY KEY,
  type TEXT NOT NULL,
  caption TEXT NOT NULL DEFAULT '',
  hashtags TEXT,
  scheduled_at TIMESTAMPTZ,
  status TEXT NOT NULL,
  failure_reason TEXT,
  facebook_post_id TEXT,
  page_id TEXT,
  media_files JSONB NOT NULL DEFAULT '[]',
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE IF NOT EXISTS pages (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  access_token TEXT NOT NULL,
  picture_url TEXT,
  long_lived BOOLEAN NOT NULL DEFAULT FALSE,
  connected_at TIMESTAMPTZ
);
CREATE TABLE IF NOT EXISTS settings (
  key TEXT PRIMARY KEY,
  value TEXT NOT NULL
);
