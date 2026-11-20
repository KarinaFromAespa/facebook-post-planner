const fs = require('fs');
const path = require('path');
const multer = require('multer');

const UPLOAD_DIR = path.join(__dirname, '..', 'uploads');
fs.mkdirSync(UPLOAD_DIR, { recursive: true });

const upload = multer({
  dest: UPLOAD_DIR,
  limits: { fileSize: 200 * 1024 * 1024 },
});

function mediaFor(post) {
  return (post.mediaFiles || []).map((f) => ({
    ...f,
    url: f.url || `/uploads/${path.basename(f.path)}`,
  }));
}

function deleteMedia(post) {
  for (const f of post.mediaFiles || []) {
    try { fs.unlinkSync(f.path); } catch { /* already gone */ }
  }
}

module.exports = { upload, mediaFor, deleteMedia };
