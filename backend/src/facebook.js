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
  let total = null;
  let monetized = false;
  try {
    const pageData = await graph(`/${page.id}`, {
      params: { fields: 'monetization_approximate_earnings,content_monetization_earnings' },
      token: page.token,
    });
    const earnings = pageData.monetization_approximate_earnings ?? pageData.content_monetization_earnings;
    if (earnings != null) { monetized = true; total = Number(earnings); }
  } catch { /* fall through as not monetized */ }
  const posts = [];
  for (const post of publishedPosts) {
    if (!post.facebookPostId) continue;
    try {
      const data = await graph(`/${post.facebookPostId}`, {
        params: { fields: 'likes.summary(true),comments.summary(true),shares,insights.metric(post_impressions)' },
        token: page.token,
      });
      posts.push({
        postId: post.id,
        likes: data.likes?.summary?.total_count ?? 0,
        comments: data.comments?.summary?.total_count ?? 0,
        shares: data.shares?.count ?? 0,
        views: data.insights?.data?.[0]?.values?.[0]?.value ?? 0,
        earningsUsd: null,
      });
    } catch { /* skip posts whose insights fail; do not invent numbers */ }
  }
  posts.sort((a, b) => (b.likes + b.comments + b.shares) - (a.likes + a.comments + a.shares));
  return { monetized, monetizationCriteria: monetized ? null : 'Page not enrolled in Content Monetization.', totalEarningsUsd: total, posts };
}

module.exports = { configured, publishPost, insights, exchangeLongLivedToken };
