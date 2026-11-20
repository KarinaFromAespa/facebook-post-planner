const crypto = require('crypto');
const express = require('express');
const path = require('path');
const store = require('./src/store');
const media = require('./src/media');
const publisher = require('./src/publisher');
const facebook = require('./src/facebook');

const app = express();
app.use(express.json({ limit: '1mb' }));
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

const PORT = Number(process.env.PORT || 3000);
const TYPES = new Set(['post', 'reel', 'story']);
const STATUSES = new Set(['draft', 'scheduled', 'publishing', 'published', 'failed']);

function toDto(p) {
  return {
    id: p.id, type: p.type, caption: p.caption, hashtags: p.hashtags || null,
    scheduledAt: p.scheduledAt || null, status: p.status,
    failureReason: p.failureReason || null, facebookPostId: p.facebookPostId || null,
  };
}

app.get('/health', (req, res) => res.json({
  ok: true, queuePaused: store.isQueuePaused(),
  facebookConfigured: facebook.configured(),
  counts: { queued: store.allPosts().filter((p) => p.status === 'scheduled').length },
}));

app.get('/', (req, res) => res.json({ name: 'facebook-post-planner-backend', version: '1.0.0' }));

app.get('/api/queue', (req, res) => res.json(
  store.allPosts().filter((p) => p.status === 'scheduled')
    .sort((a, b) => Date.parse(a.scheduledAt || 0) - Date.parse(b.scheduledAt || 0)).map(toDto),
));

app.get('/api/drafts', (req, res) => res.json(
  store.allPosts().filter((p) => p.status === 'draft').map(toDto),
));

app.post('/api/posts', (req, res) => {
  const { type, caption, hashtags, scheduledAt, status, publishNow } = req.body || {};
  if (!TYPES.has(type)) return res.status(400).json({ error: 'type must be post, reel or story' });
  if (typeof caption !== 'string' || !caption.trim() || caption.length > 1024) {
    return res.status(400).json({ error: 'caption is required, max 1024 chars' });
  }
  if (scheduledAt && Number.isNaN(Date.parse(scheduledAt))) {
    return res.status(400).json({ error: 'scheduledAt must be ISO-8601' });
  }
  const post = {
    id: crypto.randomUUID(), type, caption: caption.trim(),
    hashtags: hashtags || null, scheduledAt: scheduledAt || null,
    status: publishNow ? 'publishing' : (STATUSES.has(status) ? status : (scheduledAt ? 'scheduled' : 'draft')),
    failureReason: null, facebookPostId: null, mediaFiles: [],
    createdAt: new Date().toISOString(), updatedAt: new Date().toISOString(),
  };
  store.upsertPost(post);
  if (publishNow) {
    publisher.publishOne(post).then((done) => res.json(toDto(done))).catch((e) => res.status(500).json({ error: e.message }));
  } else {
    res.status(201).json(toDto(post));
  }
});

app.get('/api/posts/:id', (req, res) => {
  const post = store.getPost(req.params.id);
  if (!post) return res.status(404).json({ error: 'not found' });
  res.json(toDto(post));
});

app.delete('/api/posts/:id', (req, res) => {
  const post = store.getPost(req.params.id);
  if (!post) return res.status(404).json({ error: 'not found' });
  media.deleteMedia(post);
  store.deletePost(req.params.id);
  res.json({ ok: true });
});

app.post('/api/posts/:id/media', media.upload.single('file'), (req, res) => {
  const post = store.getPost(req.params.id);
  if (!post) return res.status(404).json({ error: 'not found' });
  if (!req.file) return res.status(400).json({ error: 'file is required' });
  post.mediaFiles = [...(post.mediaFiles || []), {
    path: req.file.path, filename: req.file.originalname, mimetype: req.file.mimetype,
  }];
  post.updatedAt = new Date().toISOString();
  store.upsertPost(post);
  res.json(toDto(post));
});

app.post('/api/posts/:id/publish-now', async (req, res) => {
  const post = store.getPost(req.params.id);
  if (!post) return res.status(404).json({ error: 'not found' });
  const done = await publisher.publishOne(post);
  res.json(toDto(done));
});

app.get('/api/queue/state', (req, res) => res.json({ paused: store.isQueuePaused() }));
app.post('/api/queue/pause', (req, res) => res.json({ paused: store.setQueuePaused(true) }));
app.post('/api/queue/resume', (req, res) => res.json({ paused: store.setQueuePaused(false) }));

app.get('/api/insights', async (req, res) => {
  const published = store.allPosts().filter((p) => p.status === 'published');
  res.json(await facebook.insights(published));
});

app.listen(PORT, () => {
  console.log(`facebook-post-planner-backend listening on :${PORT}`);
  console.log(`facebook configured: ${facebook.configured()}`);
  publisher.start();
});
