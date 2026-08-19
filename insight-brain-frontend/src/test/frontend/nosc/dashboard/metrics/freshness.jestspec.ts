/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { formatUpdatedAgo } from 'MainRoot/nosc/dashboard/metrics/freshness';

describe('formatUpdatedAgo (CLM-40905 AT-F16: freshness)', () => {
  const NOW = 1_700_000_000_000;

  it('returns null for a null/undefined/NaN timestamp (degrade gracefully, no misleading time)', () => {
    expect(formatUpdatedAgo(null, NOW)).toBeNull();
    expect(formatUpdatedAgo(undefined, NOW)).toBeNull();
    expect(formatUpdatedAgo(Number.NaN, NOW)).toBeNull();
  });

  it('formats sub-minute ages in seconds', () => {
    expect(formatUpdatedAgo(NOW - 15_000, NOW)).toBe('Updated 15s ago');
    expect(formatUpdatedAgo(NOW - 59_000, NOW)).toBe('Updated 59s ago');
  });

  it('treats very recent timestamps as "just now"', () => {
    expect(formatUpdatedAgo(NOW - 2_000, NOW)).toBe('Updated just now');
    expect(formatUpdatedAgo(NOW, NOW)).toBe('Updated just now');
  });

  it('formats minutes, hours, and days', () => {
    expect(formatUpdatedAgo(NOW - 5 * 60_000, NOW)).toBe('Updated 5m ago');
    expect(formatUpdatedAgo(NOW - 3 * 3_600_000, NOW)).toBe('Updated 3h ago');
    expect(formatUpdatedAgo(NOW - 2 * 86_400_000, NOW)).toBe('Updated 2d ago');
  });

  it('clamps future timestamps (clock skew) to "just now" instead of a negative age', () => {
    expect(formatUpdatedAgo(NOW + 10_000, NOW)).toBe('Updated just now');
  });
});
