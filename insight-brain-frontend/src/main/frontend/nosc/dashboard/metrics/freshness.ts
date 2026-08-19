/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Format a "freshness" label from `lastUpdatedAt` (CLM-40905).
 *
 * Returns `null` when the timestamp is missing/invalid so callers degrade
 * gracefully (render nothing) rather than showing a misleading time. `now`
 * is injected so tests can control the clock without fake timers.
 */
export function formatUpdatedAgo(
  lastUpdatedAt: number | null | undefined,
  now: number = Date.now(),
): string | null {
  if (typeof lastUpdatedAt !== 'number' || !Number.isFinite(lastUpdatedAt)) {
    return null;
  }

  const diffMs = now - lastUpdatedAt;
  // Clamp future / clock-skew timestamps to "just now" instead of "in -3s".
  const seconds = Math.max(0, Math.floor(diffMs / 1000));

  if (seconds < 5) return 'Updated just now';
  if (seconds < 60) return `Updated ${seconds}s ago`;

  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `Updated ${minutes}m ago`;

  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `Updated ${hours}h ago`;

  const days = Math.floor(hours / 24);
  return `Updated ${days}d ago`;
}
