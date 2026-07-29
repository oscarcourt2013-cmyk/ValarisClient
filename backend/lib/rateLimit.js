/**
 * Simple in-memory rate limiter (IP + route).
 */
const buckets = new Map();

function key(ip, route) {
  return `${ip}|${route}`;
}

/**
 * @param {string} ip
 * @param {string} route
 * @param {{ limit: number, windowMs: number }} opts
 * @returns {{ ok: boolean, retryAfterMs?: number }}
 */
function check(ip, route, opts = { limit: 60, windowMs: 60_000 }) {
  const k = key(ip || 'unknown', route);
  const now = Date.now();
  let b = buckets.get(k);
  if (!b || now - b.start >= opts.windowMs) {
    b = { start: now, count: 0 };
    buckets.set(k, b);
  }
  b.count += 1;
  if (b.count > opts.limit) {
    return { ok: false, retryAfterMs: opts.windowMs - (now - b.start) };
  }
  return { ok: true };
}

/** Cleanup old buckets occasionally */
setInterval(() => {
  const now = Date.now();
  for (const [k, b] of buckets) {
    if (now - b.start > 5 * 60_000) buckets.delete(k);
  }
}, 60_000).unref?.();

module.exports = { check };
