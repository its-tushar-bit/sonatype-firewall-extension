/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Coerce a raw dual-thumb (or single-thumb) slider payload into an in-bounds, ascending
 * {@code [min, max]} pair. Optional {@code round} keeps decimal rails (CVSS/EPSS) stable.
 */
export function normalizeRange(
  next: readonly number[],
  min: number,
  max: number,
  round?: (n: number) => number,
): [number, number] {
  const clamp = (n: number): number => {
    const safe = Number.isFinite(n) ? n : min;
    return Math.min(max, Math.max(min, safe));
  };
  const a = clamp(next[0]);
  const b = clamp(next.length > 1 ? next[1] : next[0]);
  const lo = Math.min(a, b);
  const hi = Math.max(a, b);
  if (!round) {
    return [lo, hi];
  }
  return [round(lo), round(hi)];
}
