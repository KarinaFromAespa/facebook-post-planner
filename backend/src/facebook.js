const GRAPH = 'https://graph.facebook.com/v23.0';

function configured() {
  return Boolean(process.env.FB_PAGE_TOKEN && process.env.FB_PAGE_ID);
}

async function graph(path, params = {}) {
  const url = new URL(GRAPH + path);
  for (const [k, v] of Object.entries(params)) url.searchParams.set(k, v);
  url.searchParams.set('access_token', process.env.FB_PAGE_TOKEN);
  const res = await fetch(url);
  const body = await res.json();
  if (!res.ok) throw new Error(body?.error?.message || `Facebook API ${res.status}`);
  return body;
}

async function publishPost(post, mediaFiles) {
  if (!configured()) {
    return { ok: false, reason: 'Facebook Page token not configured (set FB_PAGE_TOKEN and FB_PAGE_ID)' };
  }
  const pageId = process.env.FB_PAGE_ID;
  if (post.type === 'story') {
    return publishStory(post, mediaFiles);
  }
  const params = { message: post.caption };
  if (mediaFiles.length === 0) {
    const body = await graph(`/${pageId}/feed`, params);
    return { ok: true, facebookPostId: body.id };
  }
  if (post.type === 'reel' || mediaFiles[0].mimetype.startsWith('video/')) {
    const body = await graph(`/${pageId}/videos`, {
      ...params, file_url: mediaFiles[0].url, description: post.caption,
    });
    return { ok: true, facebookPostId: body.id };
  }
  const attached = [];
  for (const f of mediaFiles) {
    const body = await graph(`/${pageId}/photos`, { url: f.url, published: 'false' });
    attached.push({ media_fbid: body.id });
  }
  const body = await graph(`/${pageId}/feed`, { ...params, attached_media: JSON.stringify(attached) });
  return { ok: true, facebookPostId: body.id };
}

async function publishStory(post, mediaFiles) {
  const pageId = process.env.FB_PAGE_ID;
  if (mediaFiles.length === 0 || !mediaFiles[0].mimetype.startsWith('image/')) {
    return { ok: false, reason: 'Story needs one photo in v1' };
  }
  const body = await graph(`/${pageId}/stories`, {
    message: post.caption, photo_url: mediaFiles[0].url,
  });
  return { ok: true, facebookPostId: body.id };
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
  const pageId = process.env.FB_PAGE_ID;
  let total = null;
  let monetized = false;
  try {
    const page = await graph(`/${pageId}`, { fields: 'monetization_approximate_earnings,content_monetization_earnings' });
    const earnings = page.monetization_approximate_earnings ?? page.content_monetization_earnings;
    if (earnings != null) { monetized = true; total = Number(earnings); }
  } catch { /* fall through as not monetized */ }
  const posts = [];
  for (const post of publishedPosts) {
    if (!post.facebookPostId) continue;
    try {
      const data = await graph(`/${post.facebookPostId}`, {
        fields: 'likes.summary(true),comments.summary(true),shares,insights.metric(post_impressions)',
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

module.exports = { configured, publishPost, insights };
