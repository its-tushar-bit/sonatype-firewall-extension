/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Formats a violation's first-occurrence timestamp as a compact age label.
 *
 * Intentionally NOT delegating to `terseAgo` (CommonServices.js): the two already share the same
 * 30-day-month / 360-day-year truncation model, so the `${n}d` / `${n}mo` / `${n}y` buckets agree. The
 * deliberate differences are (1) this collapses anything under a day to `today` (the dashboard column does not
 * surface hour/minute granularity), and (2) it takes an injectable `nowMs` so the rendering is deterministic
 * under test, whereas `terseAgo` reads the wall clock directly (`new Date()`) and cannot be clock-injected
 * without fake system timers.
 */
export function formatAgeFromMs(
  ts: number | undefined,
  nowMs: number = Date.now(),
): string {
  // Only a missing/non-numeric timestamp is unknown ("—"). A genuine epoch-0
  // timestamp is a real (if ancient) date and must not collapse to "—".
  if (ts == null || Number.isNaN(ts)) return '—';
  const elapsedMs = nowMs - ts;
  // A future timestamp (clock skew between server and client) has no sensible
  // age; collapse it to "today" rather than rendering a negative bucket.
  if (elapsedMs < 0) return 'today';
  const days = Math.floor(elapsedMs / (1000 * 60 * 60 * 24));
  if (days < 1) return 'today';
  if (days < 30) return `${days}d`;
  const months = Math.floor(days / 30);
  if (months < 12) return `${months}mo`;
  const years = Math.floor(months / 12);
  return `${years}y`;
}
