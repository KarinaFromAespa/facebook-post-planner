const fs = require('fs');
const path = require('path');
const multer = require('multer');

const UPLOAD_DIR = path.join(__dirname, '..', 'uploads');
fs.mkdirSync(UPLOAD_DIR, { recursive: true });

const upload = multer({
  dest: UPLOAD_DIR,
  limits: { fileSize: 200 * 1024 * 1024 },
});

function publicBaseUrl() {
  return (process.env.PUBLIC_BASE_URL || '').replace(/\/$/, '');
}

function mediaFor(post) {
  const base = publicBaseUrl();
  return (post.mediaFiles || []).map((f) => ({
    ...f,
    // Facebook must be able to fetch the file, so the URL is absolute once
    // PUBLIC_BASE_URL is set (the VM's public address). Without it the
    // relative path still works through the website's /uploads proxy.
    url: f.url || `${base}/uploads/${path.basename(f.path)}`,
  }));
}

function deleteMedia(post) {
  for (const f of post.mediaFiles || []) {
    try { fs.unlinkSync(f.path); } catch { /* already gone */ }
  }
}

module.exports = { upload, mediaFor, deleteMedia };
