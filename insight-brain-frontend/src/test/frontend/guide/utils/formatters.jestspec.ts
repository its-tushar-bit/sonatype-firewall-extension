/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { formatEpssScore } from 'GuideRoot/utils/formatters';

describe('formatEpssScore', () => {
  it('formats normal EPSS scores with 3 decimal places', () => {
    expect(formatEpssScore(0.975)).toBe('97.500%');
    expect(formatEpssScore(0.5)).toBe('50.000%');
    expect(formatEpssScore(0.123456)).toBe('12.346%');
  });

  it('handles zero EPSS score', () => {
    expect(formatEpssScore(0)).toBe('0%');
  });

  it('returns "Not available" for null', () => {
    expect(formatEpssScore(null)).toBe('Not available');
  });

  it('returns "Not available" for undefined', () => {
    expect(formatEpssScore(undefined)).toBe('Not available');
  });

  it('handles very small EPSS scores', () => {
    expect(formatEpssScore(0.00001)).toBe('0.001%');
    expect(formatEpssScore(0.000001)).toBe('0.000%');
  });

  it('handles maximum EPSS score', () => {
    expect(formatEpssScore(1)).toBe('100.000%');
  });
});
