// Neon Postgres (plannerdr) — DR copy of the queue. Same interface as
// src/db/oracle.js. Best-effort: a failed DR write is logged and never
// blocks the primary (Oracle) write or the API response.
const { Pool } = require('pg');

let pool = null;

function configured() {
  return Boolean(process.env.NEON_DATABASE_URL);
}

function getPool() {
  if (!pool) {
    if (!configured()) throw new Error('Neon DB is not configured (NEON_DATABASE_URL)');
    pool = new Pool({ connectionString: process.env.NEON_DATABASE_URL, max: 2 });
  }
  return pool;
}

function toIso(v) {
  if (!v) return null;
  return v instanceof Date ? v.toISOString() : new Date(v).toISOString();
}

function rowToPost(r) {
  return {
    id: r.id, type: r.type, caption: r.caption || '',
    hashtags: r.hashtags || null, scheduledAt: toIso(r.scheduled_at),
    status: r.status, failureReason: r.failure_reason || null,
    facebookPostId: r.facebook_post_id || null, pageId: r.page_id || null,
    mediaFiles: r.media_files || [],
    createdAt: toIso(r.created_at), updatedAt: toIso(r.updated_at),
  };
}

function rowToPage(r) {
  return {
    id: r.id, name: r.name, accessToken: r.access_token,
    pictureUrl: r.picture_url || null, longLived: r.long_lived === true,
    connectedAt: toIso(r.connected_at),
  };
}

async function upsertPost(post) {
  const p = getPool();
  await p.query(
    `INSERT INTO posts (id, type, caption, hashtags, scheduled_at, status,
       failure_reason, facebook_post_id, page_id, media_files, created_at, updated_at)
     VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12)
     ON CONFLICT (id) DO UPDATE SET type=EXCLUDED.type, caption=EXCLUDED.caption,
       hashtags=EXCLUDED.hashtags, scheduled_at=EXCLUDED.scheduled_at,
       status=EXCLUDED.status, failure_reason=EXCLUDED.failure_reason,
       facebook_post_id=EXCLUDED.facebook_post_id, page_id=EXCLUDED.page_id,
       media_files=EXCLUDED.media_files, created_at=EXCLUDED.created_at,
       updated_at=EXCLUDED.updated_at`,
    [post.id, post.type, post.caption || '', post.hashtags || null,
     post.scheduledAt || null, post.status, post.failureReason || null,
     post.facebookPostId || null, post.pageId || null,
     JSON.stringify(post.mediaFiles || []),
     post.createdAt || new Date().toISOString(),
     post.updatedAt || new Date().toISOString()],
  );
}

async function deletePost(id) {
  await getPool().query('DELETE FROM posts WHERE id = $1', [id]);
}

async function upsertPage(page) {
  await getPool().query(
    `INSERT INTO pages (id, name, access_token, picture_url, long_lived, connected_at)
     VALUES ($1,$2,$3,$4,$5,$6)
     ON CONFLICT (id) DO UPDATE SET name=EXCLUDED.name,
       access_token=EXCLUDED.access_token, picture_url=EXCLUDED.picture_url,
       long_lived=EXCLUDED.long_lived, connected_at=EXCLUDED.connected_at`,
    [page.id, page.name, page.accessToken, page.pictureUrl || null,
     page.longLived === true, page.connectedAt || null],
  );
}

async function deletePage(id) {
  await getPool().query('DELETE FROM pages WHERE id = $1', [id]);
}

async function setSetting(key, value) {
  await getPool().query(
    `INSERT INTO settings (key, value) VALUES ($1,$2)
     ON CONFLICT (key) DO UPDATE SET value=EXCLUDED.value`,
    [key, value],
  );
}

// Whole-state read, used on startup when both the local JSON file and the
// Oracle primary are unavailable, so the queue can be rebuilt from DR.
async function getAll() {
  const p = getPool();
  const posts = await p.query('SELECT * FROM posts');
  const pages = await p.query('SELECT * FROM pages');
  const settings = await p.query('SELECT * FROM settings');
  const queuePaused = (settings.rows.find((r) => r.key === 'queuePaused') || {}).value === 'true';
  return {
    posts: posts.rows.map(rowToPost),
    pages: pages.rows.map(rowToPage),
    queuePaused,
  };
}

module.exports = { configured, upsertPost, deletePost, upsertPage, deletePage, setSetting, getAll };
