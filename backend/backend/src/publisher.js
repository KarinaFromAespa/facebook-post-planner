const store = require('./store');
const facebook = require('./facebook');
const media = require('./media');

async function publishOne(post) {
  store.upsertPost({ ...post, status: 'publishing', updatedAt: new Date().toISOString() });
  try {
    const result = await facebook.publishPost(post, media.mediaFor(post));
    if (!result.ok) throw new Error(result.reason);
    const done = {
      ...post, status: 'published', facebookPostId: result.facebookPostId,
      failureReason: null, updatedAt: new Date().toISOString(),
    };
    store.upsertPost(done);
    media.deleteMedia(post);
    return done;
  } catch (err) {
    const failed = {
      ...post, status: 'failed', failureReason: err.message,
      updatedAt: new Date().toISOString(),
    };
    store.upsertPost(failed);
    return failed;
  }
}

function duePosts() {
  if (store.isQueuePaused()) return [];
  const now = Date.now();
  return store.allPosts().filter(
    (p) => p.status === 'scheduled' && p.scheduledAt && Date.parse(p.scheduledAt) <= now,
  );
}

async function runOnce() {
  for (const post of duePosts()) {
    await publishOne(post);
  }
}

function start() {
  setInterval(() => { runOnce().catch(console.error); }, 60 * 1000);
  runOnce().catch(console.error);
}

module.exports = { start, runOnce, publishOne };
