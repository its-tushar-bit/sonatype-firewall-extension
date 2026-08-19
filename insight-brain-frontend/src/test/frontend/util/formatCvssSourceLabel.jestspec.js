/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { formatCvssSourceLabel } from 'MainRoot/util/vulnerabilityUtils';

describe('formatCvssSourceLabel()', () => {
  describe('when sourceLabel is provided', () => {
    it('returns the sourceLabel directly when provided', () => {
      expect(formatCvssSourceLabel('cve_cvss_2', 'Custom Label')).toBe('Custom Label');
      expect(formatCvssSourceLabel('sonatype_cvss_3', 'Vendor Score')).toBe('Vendor Score');
    });
  });

  describe('when sourceLabel is absent', () => {
    // Label text matches the long-standing convention already asserted across the
    // sbomManager and waiver specs (e.g. 'CVE CVSS 3', 'CVE CVSS 2.0') so the same
    // score renders identically in the legacy IQ UI and the NOSC UI.
    it.each([
      ['cve_cvss_2', 'CVE CVSS 2.0'],
      ['cve_cvss_3', 'CVE CVSS 3'],
      ['cve_cvss_31', 'CVE CVSS 3.1'],
      ['cve_cvss_4', 'CVE CVSS 4'],
      ['sonatype_cvss_2', 'Sonatype CVSS 2.0'],
      ['sonatype_cvss_3', 'Sonatype CVSS 3'],
      ['sonatype_cvss_31', 'Sonatype CVSS 3.1'],
      ['sonatype_cvss_4', 'Sonatype CVSS 4'],
      ['sonatype_cve_cvss_2', 'Sonatype CVE CVSS 2.0'],
      ['sonatype_cve_cvss_3', 'Sonatype CVE CVSS 3'],
      ['sonatype_cve_cvss_31', 'Sonatype CVE CVSS 3.1'],
      ['sonatype_cve_cvss_4', 'Sonatype CVE CVSS 4'],
      ['severity', 'Severity'],
    ])('maps %s to %s', (source, expected) => {
      expect(formatCvssSourceLabel(source)).toBe(expected);
    });

    it('matches keys case-insensitively', () => {
      expect(formatCvssSourceLabel('CVE_CVSS_2')).toBe('CVE CVSS 2.0');
      expect(formatCvssSourceLabel('Sonatype_CVE_CVSS_31')).toBe('Sonatype CVE CVSS 3.1');
    });

    it('returns undefined when the source is not in the map', () => {
      expect(formatCvssSourceLabel('unknown_source')).toBe(undefined);
    });
  });

  describe('edge cases', () => {
    it('returns undefined when both source and sourceLabel are undefined', () => {
      expect(formatCvssSourceLabel(undefined, undefined)).toBe(undefined);
    });

    it('returns undefined when source is null and sourceLabel is absent', () => {
      expect(formatCvssSourceLabel(null)).toBe(undefined);
    });

    it('returns undefined when source is empty and sourceLabel is absent', () => {
      expect(formatCvssSourceLabel('')).toBe(undefined);
    });

    it('falls back to map when sourceLabel is empty string', () => {
      expect(formatCvssSourceLabel('cve_cvss_2', '')).toBe('CVE CVSS 2.0');
    });

    it('falls back to map when sourceLabel is null', () => {
      expect(formatCvssSourceLabel('cve_cvss_2', null)).toBe('CVE CVSS 2.0');
    });

    it('handles null source with provided sourceLabel', () => {
      expect(formatCvssSourceLabel(null, 'Custom Label')).toBe('Custom Label');
    });

    it('does not leak an unmapped internal key introduced by the backend', () => {
      expect(formatCvssSourceLabel('future_cvss_5')).toBe(undefined);
    });
  });
});
