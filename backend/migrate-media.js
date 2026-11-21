// One-off migration: upload any local media file that has not been copied
// to the Oracle bucket (primary) + Neon S3 (DR) yet, and record its bucket
// object name on the post. Run once on the VM after deploy:
//   node migrate-media.js
// Safe to re-run: files that already have an objectName are skipped.
require('dotenv').config();
const fs = require('fs');
const store = require('./src/store');
const media = require('./src/media');
const mediaStore = require('./src/db/mediaStore');

async function main() {
  await store.init();
  let uploaded = 0;
  let skipped = 0;
  for (const post of store.allPosts()) {
    let changed = false;
    for (const f of post.mediaFiles || []) {
      if (f.objectName) { skipped += 1; continue; }
      if (!f.path || !fs.existsSync(f.path)) continue;
      const entry = await media.addMedia(post, { path: f.path, originalname: f.filename, mimetype: f.mimetype });
      f.objectName = entry.objectName;
      changed = true;
      uploaded += 1;
    }
    if (changed) {
      post.updatedAt = new Date().toISOString();
      store.upsertPost(post);
    }
  }
  console.log(`media migration done. uploaded: ${uploaded}, already in bucket: ${skipped}`);
  process.exit(0);
}

main().catch((e) => { console.error('media migration failed:', e.message); process.exit(1); });
