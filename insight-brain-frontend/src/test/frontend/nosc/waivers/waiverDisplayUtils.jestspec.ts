/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */
import {
  formatWaiverCalendarDate,
  formatWaiverListExpiry,
  waiverThreatColor,
} from 'MainRoot/nosc/waivers/waiverDisplayUtils';
import { formatDateUtcYYYYMMDD } from 'MainRoot/util/dateUtils';

describe('waiverDisplayUtils', () => {
  it('formatWaiverCalendarDate formats waiver dates as a UTC calendar day', () => {
    const value = '2026-05-01T10:00:00Z';
    expect(formatWaiverCalendarDate(value)).toBe(formatDateUtcYYYYMMDD(value));
  });

  it('formatWaiverCalendarDate returns an em dash for empty input', () => {
    expect(formatWaiverCalendarDate(undefined)).toBe('—');
    expect(formatWaiverCalendarDate(null)).toBe('—');
  });

  it('formatWaiverListExpiry preserves auto and never states', () => {
    expect(formatWaiverListExpiry({ isAutoWaiver: true } as any)).toBe('Auto');
    expect(formatWaiverListExpiry({} as any)).toBe('Never');
  });

  it('waiverThreatColor matches application threatColorFor thresholds', () => {
    expect(waiverThreatColor(9)).toBe('red');
    expect(waiverThreatColor(5)).toBe('orange');
    expect(waiverThreatColor(3)).toBe('yellow');
    expect(waiverThreatColor(1)).toBe('indigo');
    expect(waiverThreatColor(0)).toBe('gray');
  });
});
