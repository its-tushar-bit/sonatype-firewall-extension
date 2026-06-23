/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { PERIOD_PRESETS, defaultPresetKey, presetToRange, formatRangeLabel } from 'MainRoot/usage/periodPresets';

describe('periodPresets', () => {
  describe('PERIOD_PRESETS', () => {
    it('exposes the canonical 5 presets in display order', () => {
      expect(PERIOD_PRESETS.map((p) => p.key)).toEqual([
        'currentBillingPeriod',
        'lastCalendarMonth',
        'last30Days',
        'last90Days',
        'custom',
      ]);
    });

    it('every preset has a human-readable label', () => {
      PERIOD_PRESETS.forEach((p) => expect(typeof p.label).toBe('string'));
    });
  });

  describe('defaultPresetKey', () => {
    it('is currentBillingPeriod (matches today behavior — absent BE params)', () => {
      expect(defaultPresetKey).toBe('currentBillingPeriod');
    });
  });

  describe('presetToRange', () => {
    // Use ISO string (not Date object) to avoid UTC→ET timezone shift in jest env
    const today = '2026-06-22';

    it('currentBillingPeriod → null/null (defers to backend default)', () => {
      expect(presetToRange('currentBillingPeriod', today)).toEqual({ startDate: null, endDate: null });
    });

    it('lastCalendarMonth → previous calendar month, full window', () => {
      expect(presetToRange('lastCalendarMonth', today)).toEqual({
        startDate: '2026-05-01',
        endDate: '2026-05-31',
      });
    });

    it('last30Days → inclusive 30 days ending today', () => {
      expect(presetToRange('last30Days', today)).toEqual({
        startDate: '2026-05-24',
        endDate: '2026-06-22',
      });
    });

    it('last90Days → inclusive 90 days ending today', () => {
      expect(presetToRange('last90Days', today)).toEqual({
        startDate: '2026-03-25',
        endDate: '2026-06-22',
      });
    });

    it('custom preset key throws (callers must use setPeriodRange directly)', () => {
      expect(() => presetToRange('custom', today)).toThrow();
    });

    it('unknown preset key throws (forward-compat guard)', () => {
      expect(() => presetToRange('not-a-preset', today)).toThrow();
    });
  });

  describe('formatRangeLabel', () => {
    it('returns "Current billing period" when range is null/null', () => {
      expect(formatRangeLabel({ startDate: null, endDate: null })).toBe('Current billing period');
    });

    it('formats same-year range compactly: "Jun 1 - Jun 30, 2026"', () => {
      expect(formatRangeLabel({ startDate: '2026-06-01', endDate: '2026-06-30' })).toBe('Jun 1 - Jun 30, 2026');
    });

    it('formats cross-year range with year on both sides', () => {
      expect(formatRangeLabel({ startDate: '2025-12-15', endDate: '2026-01-15' })).toBe('Dec 15, 2025 - Jan 15, 2026');
    });
  });
});
