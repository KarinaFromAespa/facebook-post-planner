const fs = require('fs');
const path = require('path');

const DATA_FILE = path.join(__dirname, '..', 'data', 'planner.json');

function emptyState() {
  return { posts: [], queuePaused: false, pages: [] };
}

function load() {
  try {
    const state = JSON.parse(fs.readFileSync(DATA_FILE, 'utf8'));
    if (!Array.isArray(state.posts)) state.posts = [];
    if (typeof state.queuePaused !== 'boolean') state.queuePaused = false;
    if (!Array.isArray(state.pages)) state.pages = [];
    return state;
  } catch {
    return emptyState();
  }
}

function save(state) {
  fs.mkdirSync(path.dirname(DATA_FILE), { recursive: true });
  const tmp = DATA_FILE + '.tmp';
  fs.writeFileSync(tmp, JSON.stringify(state, null, 2));
  fs.renameSync(tmp, DATA_FILE);
}

let state = load();

module.exports = {
  allPosts: () => state.posts,
  getPost: (id) => state.posts.find((p) => p.id === id),
  upsertPost: (post) => {
    const i = state.posts.findIndex((p) => p.id === post.id);
    if (i >= 0) state.posts[i] = post; else state.posts.push(post);
    save(state);
    return post;
  },
  deletePost: (id) => {
    state.posts = state.posts.filter((p) => p.id !== id);
    save(state);
  },
  isQueuePaused: () => state.queuePaused,
  setQueuePaused: (paused) => {
    state.queuePaused = paused;
    save(state);
    return paused;
  },
  // Connected Pages (from POST /api/pages/connect). Tokens live here so the
  // publisher can post as the Page the user picked; never log or expose them.
  allPages: () => state.pages,
  getPage: (id) => state.pages.find((p) => p.id === id),
  upsertPage: (page) => {
    const i = state.pages.findIndex((p) => p.id === page.id);
    if (i >= 0) state.pages[i] = page; else state.pages.push(page);
    save(state);
    return page;
  },
  deletePage: (id) => {
    state.pages = state.pages.filter((p) => p.id !== id);
    save(state);
  },
  // When a Page is disconnected, its scheduled posts can never publish
  // (no token). Move them to draft so they leave the queue instead of
  // sitting there as scheduled forever; the user can re-schedule them
  // from Drafts if the Page is re-connected. The planned time is kept.
  draftScheduledPostsForPage: (pageId) => {
    let changed = false;
    for (const p of state.posts) {
      if (p.pageId === pageId && p.status === 'scheduled') {
        p.status = 'draft';
        changed = true;
      }
    }
    if (changed) save(state);
  },
};
