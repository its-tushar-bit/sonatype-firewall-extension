/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { formatDashboardTabBadge } from 'MainRoot/nosc/dashboard/dashboardTabBadge';

describe('formatDashboardTabBadge', () => {
  it('returns null while results are unloaded', () => {
    expect(formatDashboardTabBadge({ results: null })).toBeNull();
    expect(formatDashboardTabBadge(undefined)).toBeNull();
  });

  it('returns the exact count when the first page is complete', () => {
    expect(formatDashboardTabBadge({ results: [{}, {}], hasNextPage: false })).toBe('2');
    expect(formatDashboardTabBadge({ results: [], hasNextPage: false })).toBeNull();
  });

  it('suffixes + when additional pages exist', () => {
    expect(
      formatDashboardTabBadge({
        results: new Array(100).fill({}),
        hasNextPage: true,
      }),
    ).toBe('100+');
  });

  it('hides an empty page even when hasNextPage is inconsistently true (no "0+")', () => {
    expect(formatDashboardTabBadge({ results: [], hasNextPage: true })).toBeNull();
  });
});
