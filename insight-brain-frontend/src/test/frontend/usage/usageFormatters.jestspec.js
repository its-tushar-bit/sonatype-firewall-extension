/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { formatNumber, formatCount, formatPercent } from 'MainRoot/usage/usageFormatters';

describe('usageFormatters.formatNumber', () => {
  it('returns "0" for null and undefined', () => {
    expect(formatNumber(null)).toBe('0');
    expect(formatNumber(undefined)).toBe('0');
  });
  it('formats with thousands separator', () => {
    expect(formatNumber(1234567)).toBe('1,234,567');
  });
  it('preserves negatives', () => {
    expect(formatNumber(-500)).toBe('-500');
  });
});

describe('usageFormatters.formatCount', () => {
  it('returns "0" for 0', () => {
    expect(formatCount(0)).toBe('0');
  });

  it('keeps small integers as-is', () => {
    expect(formatCount(42)).toBe('42');
    expect(formatCount(999)).toBe('999');
  });

  it('uses 1 decimal place rounded down to nearest hundred in the 1k–10k band', () => {
    expect(formatCount(1000)).toBe('1k');
    expect(formatCount(1500)).toBe('1.5k');
    expect(formatCount(1635)).toBe('1.6k');
    expect(formatCount(9900)).toBe('9.9k');
    // Boundary near top of band: must not spill to '10k' since the next band starts at 10000
    expect(formatCount(9950)).toBe('9.9k');
    expect(formatCount(9999)).toBe('9.9k');
  });

  it('uses rounded integer + k in the 10k–1M band', () => {
    expect(formatCount(10000)).toBe('10k');
    expect(formatCount(25000)).toBe('25k');
    expect(formatCount(650000)).toBe('650k');
    // Boundary just below 1M: high-side rounding overshoot is acceptable here
    // (1000k visually equals 1M; the next band kicks in at exactly 1_000_000).
    expect(formatCount(999500)).toBe('1000k');
  });

  it('uses M suffix for >= 1000000', () => {
    expect(formatCount(1000000)).toBe('1M');
    expect(formatCount(1200000)).toBe('1.2M');
    expect(formatCount(25000000)).toBe('25M');
  });
});

describe('usageFormatters.formatPercent', () => {
  it('rounds to nearest integer percent', () => {
    expect(formatPercent(0)).toBe('0%');
    expect(formatPercent(50)).toBe('50%');
    expect(formatPercent(33.333)).toBe('33%');
    expect(formatPercent(99.4)).toBe('99%');
    expect(formatPercent(99.5)).toBe('100%');
    expect(formatPercent(100)).toBe('100%');
  });
});
