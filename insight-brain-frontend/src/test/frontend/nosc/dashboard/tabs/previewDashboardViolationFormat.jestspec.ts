/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { formatAgeFromMs } from 'MainRoot/nosc/dashboard/tabs/previewDashboardViolationFormat';

describe('formatAgeFromMs', () => {
  const now = Date.UTC(2026, 5, 18, 12, 0, 0);

  it('returns an em dash when the timestamp is missing', () => {
    expect(formatAgeFromMs(undefined, now)).toBe('—');
    expect(formatAgeFromMs(NaN, now)).toBe('—');
  });

  it('treats a genuine epoch-0 timestamp as a real (ancient) date, not "missing"', () => {
    // ts=0 is 1970-01-01, decades before `now` — it must format as an age, not "—".
    const result = formatAgeFromMs(0, now);
    expect(result).not.toBe('—');
    expect(result).toMatch(/^\d+y$/);
  });

  it('returns today for timestamps less than one day old', () => {
    expect(formatAgeFromMs(now - 6 * 60 * 60 * 1000, now)).toBe('today');
  });

  it('returns day counts under thirty days', () => {
    expect(formatAgeFromMs(now - 5 * 24 * 60 * 60 * 1000, now)).toBe('5d');
  });

  it('returns month counts under twelve months', () => {
    expect(formatAgeFromMs(now - 90 * 24 * 60 * 60 * 1000, now)).toBe('3mo');
  });

  it('returns year counts for older violations', () => {
    expect(formatAgeFromMs(now - 400 * 24 * 60 * 60 * 1000, now)).toBe('1y');
  });
});
