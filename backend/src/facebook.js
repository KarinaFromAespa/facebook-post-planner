const GRAPH = 'https://graph.facebook.com/v23.0';
const store = require('./store');

function envConfigured() {
  return Boolean(process.env.FB_PAGE_TOKEN && process.env.FB_PAGE_ID);
}

// Resolve the Page + token for a post: the post's connected Page first,
// then the env-configured Page.
function pageFor(post) {
  if (post && post.pageId) {
    const page = store.getPage(post.pageId);
    if (page && page.accessToken) return { id: page.id, token: page.accessToken };
  }
  if (envConfigured()) return { id: process.env.FB_PAGE_ID, token: process.env.FB_PAGE_TOKEN };
  return null;
}

function configured() {
  return store.allPages().length > 0 || envConfigured();
}

// Graph call. GET puts params in the query string; POST sends them as a
// form-encoded body (what the Graph API expects for publishing). The old
// code did fetch(url) for everything, so every publish went out as a GET.
async function graph(path, { params = {}, method = 'GET', token } = {}) {
  const url = new URL(GRAPH + path);
  let body = null;
  if (method === 'GET') {
    for (const [k, v] of Object.entries(params)) url.searchParams.set(k, v);
  } else {
    body = new URLSearchParams(params);
  }
  if (token) url.searchParams.set('access_token', token);
  const res = await fetch(url, {
    method,
    ...(body ? { body, headers: { 'Content-Type': 'application/x-www-form-urlencoded' } } : {}),
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data?.error?.message || `Facebook API ${res.status}`);
  return data;
}

async function publishPost(post, mediaFiles) {
  const page = pageFor(post);
  if (!page) {
    return { ok: false, reason: 'No Facebook Page connected (connect a Page from the website, or set FB_PAGE_TOKEN and FB_PAGE_ID)' };
  }
  if (post.type === 'story') return publishStory(post, mediaFiles, page);
  const params = { message: post.caption };
  if (mediaFiles.length === 0) {
    const body = await graph(`/${page.id}/feed`, { params, method: 'POST', token: page.token });
    return { ok: true, facebookPostId: body.id };
  }
  if (post.type === 'reel' || mediaFiles[0].mimetype.startsWith('video/')) {
    const body = await graph(`/${page.id}/videos`, {
      params: { ...params, file_url: mediaFiles[0].url, description: post.caption },
      method: 'POST', token: page.token,
    });
    return { ok: true, facebookPostId: body.id };
  }
  const attached = [];
  for (const f of mediaFiles) {
    const body = await graph(`/${page.id}/photos`, {
      params: { url: f.url, published: 'false' }, method: 'POST', token: page.token,
    });
    attached.push({ media_fbid: body.id });
  }
  const body = await graph(`/${page.id}/feed`, {
    params: { ...params, attached_media: JSON.stringify(attached) },
    method: 'POST', token: page.token,
  });
  return { ok: true, facebookPostId: body.id };
}

async function publishStory(post, mediaFiles, page) {
  if (mediaFiles.length === 0 || !mediaFiles[0].mimetype.startsWith('image/')) {
    return { ok: false, reason: 'Story needs one photo in v1' };
  }
  const body = await graph(`/${page.id}/stories`, {
    params: { photo_url: mediaFiles[0].url },
    method: 'POST', token: page.token,
  });
  return { ok: true, facebookPostId: body.id };
}

// Exchange a short-lived Page token for a long-lived one (60 days). Needs
// FB_APP_ID + FB_APP_SECRET as backend env vars; without them the token is
// stored as-is and the caller is told it is short-lived.
async function exchangeLongLivedToken(shortToken) {
  const appId = process.env.FB_APP_ID;
  const appSecret = process.env.FB_APP_SECRET;
  if (!appId || !appSecret) return { token: shortToken, longLived: false };
  const data = await graph('/oauth/access_token', {
    params: {
      grant_type: 'fb_exchange_token',
      client_id: appId,
      client_secret: appSecret,
      fb_exchange_token: shortToken,
    },
  });
  return { token: data.access_token, longLived: true };
}

// Read one day's earnings value from an insights metric. The two
// monetization metrics report on different scales:
// - content_monetization_earnings: micro-units, the value is an object
//   { amount, currency } — amount / 1e6 = dollars.
// - monetization_approximate_earnings: a bare number, already in dollars.
function earningsAmount(value) {
  if (typeof value === 'number') return value;
  if (value && typeof value.amount === 'number') return value.amount / 1e6;
  if (value && typeof value.value === 'number') return value.value / 1e6;
  return 0;
}

async function insights(publishedPosts) {
  if (!configured()) {
    return {
      monetized: false,
      monetizationCriteria: 'Not enrolled in Content Monetization. Check Meta Business Suite > Monetization > View Page eligibility. Earnings show here once the Page is enrolled; engagement ranking still shows below.',
      totalEarningsUsd: null,
      posts: [],
    };
  }
  const first = store.allPages()[0];
  const page = first
    ? { id: first.id, token: first.accessToken }
    : { id: process.env.FB_PAGE_ID, token: process.env.FB_PAGE_TOKEN };

  // Page monetization total (last 30 days). These are insights METRICS
  // (Graph v23.0) on the /insights edge — not Page fields. The old code
  // asked for them as Page fields, which do not exist, so the call always
  // failed and every page read as "not monetized". Reading them needs the
  // read_insights permission on the Page token; without it the edge returns
  // empty data instead of an error.
  let total = null;
  let monetized = false;
  const since = Math.floor(Date.now() / 1000) - 30 * 24 * 3600;
  const until = Math.floor(Date.now() / 1000);
  for (const metric of ['content_monetization_earnings', 'monetization_approximate_earnings']) {
    try {
      const res = await graph(`/${page.id}/insights`, {
        params: { metric, period: 'day', since, until },
        token: page.token,
      });
      const days = res.data?.[0]?.values || [];
      if (days.length > 0) monetized = true;
      for (const d of days) {
        total = (total ?? 0) + earningsAmount(d.value);
      }
    } catch { /* try the other metric */ }
  }
  if (monetized) total = total ?? 0;

  // Per-post details. The page's real recent posts (so Insights has
  // something even before anything is published through the planner) plus
  // any posts the planner published. Each field is best-effort so one
  // blocked read does not drop the whole post.
  const posts = [];
  const seen = new Set();
  const addPost = async (facebookPostId, caption) => {
    if (!facebookPostId || seen.has(facebookPostId)) return;
    seen.add(facebookPostId);
    const row = { postId: facebookPostId, caption: caption || '', likes: 0, comments: 0, shares: 0, views: 0, earningsUsd: null };
    try {
      const d = await graph(`/${facebookPostId}`, { params: { fields: 'shares' }, token: page.token });
      row.shares = d.shares?.count ?? 0;
    } catch {}
    try {
      const d = await graph(`/${facebookPostId}`, { params: { fields: 'likes.summary(true)' }, token: page.token });
      row.likes = d.likes?.summary?.total_count ?? 0;
    } catch {}
    try {
      const d = await graph(`/${facebookPostId}`, { params: { fields: 'comments.summary(true)' }, token: page.token });
      row.comments = d.comments?.summary?.total_count ?? 0;
    } catch {}
    try {
      const res = await graph(`/${facebookPostId}/insights`, {
        params: { metric: 'content_monetization_earnings', period: 'lifetime' },
        token: page.token,
      });
      const v = res.data?.[0]?.values?.[0]?.value;
      if (v != null) row.earningsUsd = earningsAmount(v);
    } catch {}
    posts.push(row);
  };
  for (const post of publishedPosts) {
    await addPost(post.facebookPostId, post.caption || '');
  }
  try {
    const feed = await graph(`/${page.id}/posts`, {
      params: { fields: 'id,message,created_time', limit: 10 },
      token: page.token,
    });
    for (const p of feed.data || []) await addPost(p.id, p.message || '');
  } catch { /* no recent posts to show */ }
  posts.sort((a, b) => (b.likes + b.comments + b.shares) - (a.likes + a.comments + a.shares));
  return {
    monetized,
    monetizationCriteria: monetized
      ? null
      : 'No Content Monetization earnings found in the last 30 days. If the Page is enrolled, check that the Page token has the read_insights permission (re-connect the Page after adding it) and see Meta Business Suite > Monetization > View Page eligibility.',
    totalEarningsUsd: total,
    posts,
  };
}

module.exports = { configured, publishPost, insights, exchangeLongLivedToken };
