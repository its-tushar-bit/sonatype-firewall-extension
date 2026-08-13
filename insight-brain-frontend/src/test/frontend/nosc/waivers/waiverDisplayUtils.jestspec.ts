/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */
import {
  describeWaiverExpiry,
  formatWaiverCalendarDate,
  formatWaiverComponentLabel,
  formatWaiverListExpiry,
  formatWaiverScopeLabel,
  normalizeWaiverOwnerTypeForApi,
  waiverThreatColor,
} from 'MainRoot/nosc/waivers/waiverDisplayUtils';
import { formatDateUtcYYYYMMDD } from 'MainRoot/util/dateUtils';

describe('waiverDisplayUtils', () => {
  it('normalizeWaiverOwnerTypeForApi lowercases Ana enum tokens for the v2 path', () => {
    expect(normalizeWaiverOwnerTypeForApi('APPLICATION')).toBe('application');
    expect(normalizeWaiverOwnerTypeForApi('ORGANIZATION')).toBe('organization');
    expect(normalizeWaiverOwnerTypeForApi('application')).toBe('application');
    expect(normalizeWaiverOwnerTypeForApi('ROOT_ORGANIZATION')).toBe('organization');
    expect(normalizeWaiverOwnerTypeForApi('ALL_REPOSITORIES')).toBe('repository_container');
    expect(normalizeWaiverOwnerTypeForApi(null)).toBeNull();
    expect(normalizeWaiverOwnerTypeForApi(undefined)).toBeNull();
  });

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

  describe('describeWaiverExpiry', () => {
    const NOW = Date.parse('2026-07-26T00:00:00Z');

    it('marks a past expiry as expired without a countdown', () => {
      const expiry = describeWaiverExpiry({ expiryTime: '2026-07-02T00:00:00Z' } as any, NOW);
      expect(expiry).toEqual({
        label: formatDateUtcYYYYMMDD('2026-07-02T00:00:00Z'),
        expired: true,
        relative: null,
      });
    });

    it('counts down to a future expiry', () => {
      expect(describeWaiverExpiry({ expiryTime: '2026-07-31T00:00:00Z' } as any, NOW)).toEqual({
        label: formatDateUtcYYYYMMDD('2026-07-31T00:00:00Z'),
        expired: false,
        relative: 'in 5 days',
      });
      expect(describeWaiverExpiry({ expiryTime: '2026-07-27T00:00:00Z' } as any, NOW).relative).toBe(
        'in 1 day',
      );
    });

    it('accepts numeric epoch expiryTime values from the DTO', () => {
      const expiryTime = Date.parse('2026-07-31T00:00:00Z');
      expect(describeWaiverExpiry({ expiryTime } as any, NOW)).toEqual({
        label: formatDateUtcYYYYMMDD(expiryTime),
        expired: false,
        relative: 'in 5 days',
      });
    });

    it('covers the non-date expiry states', () => {
      expect(describeWaiverExpiry({} as any, NOW).label).toBe('Never');
      expect(
        describeWaiverExpiry({ isExpireWhenRemediationAvailable: true } as any, NOW).label,
      ).toBe('When remediation available');
      // The v2 detail payload omits the `is` prefix the dashboard list row uses.
      expect(describeWaiverExpiry({ expireWhenRemediationAvailable: true } as any, NOW).label).toBe(
        'When remediation available',
      );
      expect(describeWaiverExpiry({ isAutoWaiver: true } as any, NOW).label).toBe(
        'Auto (managed by IQ)',
      );
    });
  });

  describe('formatWaiverScopeLabel', () => {
    it('composes owner name and humanized owner type', () => {
      expect(
        formatWaiverScopeLabel({
          scopeOwnerName: 'Apple - Java',
          scopeOwnerType: 'application',
        } as any),
      ).toBe('Apple - Java (Application)');
      expect(
        formatWaiverScopeLabel({
          scopeOwnerName: 'Central',
          scopeOwnerType: 'repository_manager',
        } as any),
      ).toBe('Central (Repository Manager)');
    });

    it('falls back through ownerName when scope fields are absent', () => {
      expect(
        formatWaiverScopeLabel({ ownerName: 'Legacy App', ownerType: 'application' } as any),
      ).toBe('Legacy App (Application)');
      // When only ownerId is available (no scope info), return null per CLM-44263 AC#3
      expect(formatWaiverScopeLabel({ ownerId: 'abc123', ownerType: '' } as any)).toBeNull();
      expect(formatWaiverScopeLabel({} as any)).toBeNull();
    });

    it('falls back to the humanized owner type when no name is available', () => {
      expect(formatWaiverScopeLabel({ ownerType: 'application' } as any)).toBe('Application');
      expect(
        formatWaiverScopeLabel({ ownerType: 'repository_manager', ownerId: 'abc123' } as any),
      ).toBe('Repository Manager');
    });

    it('uses scope field when scopeOwnerName/ownerName are absent (CLM-44263)', () => {
      // Scenario: v2 detail endpoint returns scope field but not scopeOwnerName
      expect(
        formatWaiverScopeLabel({
          scope: 'Application — Config Service',
          ownerId: 'app-123',
          ownerType: 'application',
        } as any),
      ).toBe('Application — Config Service');
    });

    it('prefers scopeOwnerName over scope field', () => {
      expect(
        formatWaiverScopeLabel({
          scopeOwnerName: 'Config Service',
          scopeOwnerType: 'application',
          scope: 'Application — Old Value',
        } as any),
      ).toBe('Config Service (Application)');
    });

    it('prefers ownerName over scope field', () => {
      expect(
        formatWaiverScopeLabel({
          ownerName: 'My App',
          ownerType: 'application',
          scope: 'Application — Old Value',
        } as any),
      ).toBe('My App (Application)');
    });
  });

  describe('formatWaiverComponentLabel', () => {
    const mavenCi = {
      format: 'maven',
      coordinates: { artifactId: 'log4j-core', version: '2.14.1' },
    };

    it('joins displayName.parts when the payload uses the Classic DTO shape', () => {
      expect(
        formatWaiverComponentLabel({
          componentIdentifier: mavenCi,
          displayName: {
            parts: [{ value: 'org.apache.logging.log4j' }, { value: 'log4j-core' }, { value: '2.14.1' }],
          },
        } as any),
      ).toBe('org.apache.logging.log4j:log4j-core:2.14.1');
    });

    it('prefers a flat displayName string over coordinates', () => {
      expect(
        formatWaiverComponentLabel({
          componentIdentifier: mavenCi,
          displayName: 'log4j-core 2.14.1',
        } as any),
      ).toBe('log4j-core 2.14.1');
    });

    it('falls back to coordinates when displayName is absent', () => {
      expect(formatWaiverComponentLabel({ componentIdentifier: mavenCi } as any)).toBe(
        'log4j-core:2.14.1',
      );
      expect(formatWaiverComponentLabel({} as any)).toBe('All Components');
    });
  });

  it('waiverThreatColor matches application threatColorFor thresholds', () => {
    expect(waiverThreatColor(9)).toBe('red');
    expect(waiverThreatColor(5)).toBe('orange');
    expect(waiverThreatColor(3)).toBe('yellow');
    expect(waiverThreatColor(1)).toBe('indigo');
    expect(waiverThreatColor(0)).toBe('gray');
  });
});
