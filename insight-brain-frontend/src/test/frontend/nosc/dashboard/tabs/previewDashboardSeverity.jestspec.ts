/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { severityColor, threatColor } from 'MainRoot/nosc/dashboard/tabs/previewDashboardSeverity';

describe('previewDashboardSeverity', () => {
  describe('severityColor', () => {
    it('returns green for zero counts', () => {
      expect(severityColor(0, 'crit')).toBe('green');
      expect(severityColor(undefined, 'total')).toBe('green');
    });

    it('maps each component score kind to its badge color', () => {
      // `total` delegates to the shared threatColorFor helper (red>=8, orange>=4, yellow>=2, indigo==1).
      expect(severityColor(9, 'total')).toBe('red');
      expect(severityColor(5, 'total')).toBe('orange');
      expect(severityColor(3, 'total')).toBe('yellow');
      expect(severityColor(1, 'total')).toBe('indigo');
      // Per-severity columns map to the canonical threat level for their category and route through
      // threatColorFor, so the count magnitude is irrelevant — `low` is `indigo`, NOT a hand-copied gray.
      expect(severityColor(3, 'crit')).toBe('red');
      expect(severityColor(3, 'sev')).toBe('orange');
      expect(severityColor(3, 'mod')).toBe('yellow');
      expect(severityColor(3, 'low')).toBe('indigo');
    });
  });

  describe('threatColor', () => {
    it('maps threat levels to badge colors via the shared threatColorFor helper', () => {
      expect(threatColor(9)).toBe('red');
      expect(threatColor(8)).toBe('red');
      expect(threatColor(7)).toBe('orange');
      expect(threatColor(4)).toBe('orange');
      // Mid tier is yellow, matching the shared threatColorFor used by app-detail + waivers (CLM-40767 review).
      expect(threatColor(3)).toBe('yellow');
      expect(threatColor(2)).toBe('yellow');
      expect(threatColor(1)).toBe('indigo');
    });
  });
});
