const fs = require('fs');
const path = require('path');

const DATA_FILE = path.join(__dirname, '..', 'data', 'planner.json');

function emptyState() {
  return { posts: [], queuePaused: false };
}

function load() {
  try {
    return JSON.parse(fs.readFileSync(DATA_FILE, 'utf8'));
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
};
