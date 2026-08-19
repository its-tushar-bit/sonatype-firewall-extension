/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import moment from 'moment';
import {
  waiverMatcherStrategy,
  useFirewallWaiverExpirations,
  isCustomExpiryTimeValid,
  isCustomExpiryTimeSelected,
  isNeverExpiryTimeSelected,
  isExpireWhenRemediationAvailableSelected,
  getExpiryTime,
  getExpirationDaysMessage,
  formatCustomDate,
  formatFirewallComponentForWaiver,
  buildFirewallBulkWaiverPayload,
  validateFirewallBulkWaiverConfig,
  displayFirewallRepositoryScope,
  normalizeFirewallOwnerType,
} from 'MainRoot/firewall/bulkWaive/firewallWaiverUtils';

describe('firewallWaiverUtils', () => {
  describe('waiverMatcherStrategy', () => {
    it('should have correct matcher strategy constants', () => {
      expect(waiverMatcherStrategy.ALL_COMPONENTS).toBe('ALL_COMPONENTS');
      expect(waiverMatcherStrategy.ALL_VERSIONS).toBe('ALL_VERSIONS');
      expect(waiverMatcherStrategy.EXACT_COMPONENT).toBe('EXACT_COMPONENT');
    });
  });

  describe('useFirewallWaiverExpirations', () => {
    it('should return standard expiration options', () => {
      const expirations = useFirewallWaiverExpirations(false);

      expect(expirations).toContainEqual({ name: 'Never', value: 'never' });
      expect(expirations).toContainEqual({ name: '7 Days', value: '7' });
      expect(expirations).toContainEqual({ name: '30 Days', value: '30' });
      expect(expirations).toContainEqual({ name: 'Custom', value: 'custom' });
    });

    it('should include remediation option when enabled', () => {
      const expirations = useFirewallWaiverExpirations(true);

      expect(expirations).toContainEqual({
        name: 'When Remediation Available',
        value: 'remediationAvailable',
      });
    });

    it('should not include remediation option when disabled', () => {
      const expirations = useFirewallWaiverExpirations(false);

      const hasRemediation = expirations.some((e) => e.value === 'remediationAvailable');
      expect(hasRemediation).toBe(false);
    });
  });

  describe('isCustomExpiryTimeValid', () => {
    it('should return false for empty value', () => {
      expect(isCustomExpiryTimeValid('')).toBe(false);
      expect(isCustomExpiryTimeValid(null)).toBe(false);
      expect(isCustomExpiryTimeValid(undefined)).toBe(false);
    });

    it('should return false for past dates', () => {
      const pastDate = '2020-01-01';
      expect(isCustomExpiryTimeValid(pastDate)).toBe(false);
    });

    it('should return false for today', () => {
      const today = moment().format('YYYY-MM-DD');
      expect(isCustomExpiryTimeValid(today)).toBe(false);
    });

    it('should return true for future dates', () => {
      const futureDateStr = moment().add(1, 'days').format('YYYY-MM-DD');

      expect(isCustomExpiryTimeValid(futureDateStr)).toBe(true);
    });
  });

  describe('isCustomExpiryTimeSelected', () => {
    it('should return true when custom is selected', () => {
      expect(isCustomExpiryTimeSelected('custom')).toBe(true);
    });

    it('should return false for other values', () => {
      expect(isCustomExpiryTimeSelected('7')).toBe(false);
      expect(isCustomExpiryTimeSelected('30')).toBe(false);
      expect(isCustomExpiryTimeSelected('never')).toBe(false);
      expect(isCustomExpiryTimeSelected(null)).toBe(false);
    });
  });

  describe('isNeverExpiryTimeSelected', () => {
    it('should return true for never', () => {
      expect(isNeverExpiryTimeSelected('never')).toBe(true);
    });

    it('should return true for null', () => {
      expect(isNeverExpiryTimeSelected(null)).toBe(true);
    });

    it('should return false for other values', () => {
      expect(isNeverExpiryTimeSelected('7')).toBe(false);
      expect(isNeverExpiryTimeSelected('custom')).toBe(false);
    });
  });

  describe('isExpireWhenRemediationAvailableSelected', () => {
    it('should return true for remediationAvailable', () => {
      expect(isExpireWhenRemediationAvailableSelected('remediationAvailable')).toBe(true);
    });

    it('should return false for other values', () => {
      expect(isExpireWhenRemediationAvailableSelected('7')).toBe(false);
      expect(isExpireWhenRemediationAvailableSelected('never')).toBe(false);
    });
  });

  describe('getExpiryTime', () => {
    it('should return null for no expiration', () => {
      expect(getExpiryTime(null)).toBeNull();
      expect(getExpiryTime(undefined)).toBeNull();
      expect(getExpiryTime(0)).toBeNull();
    });

    it('should return future date for valid expiration days', () => {
      const result = getExpiryTime(30);
      expect(result).toBeDefined();
      expect(typeof result).toBe('string');

      // Verify it's a future date
      const futureDate = new Date(result);
      expect(futureDate > new Date()).toBe(true);
    });
  });

  describe('getExpirationDaysMessage', () => {
    it('should return message for custom expiry with valid date', () => {
      const futureDateStr = moment().add(45, 'days').format('YYYY-MM-DD');

      const customExpiryTime = { value: futureDateStr };
      const message = getExpirationDaysMessage('custom', customExpiryTime);

      expect(message).toContain('This waiver will expire in');
      expect(message).toContain('45 days');
    });

    it('should return empty string for custom expiry without valid date', () => {
      const customExpiryTime = { value: '' };
      const message = getExpirationDaysMessage('custom', customExpiryTime);

      expect(message).toBe('');
    });

    it('should return message for standard expiration days', () => {
      const message = getExpirationDaysMessage('30', null);

      expect(message).toBe('This waiver will expire in 30 days');
    });

    it('should return empty string for never', () => {
      const message = getExpirationDaysMessage('never', null);

      expect(message).toBe('');
    });

    it('should return remediation message', () => {
      const message = getExpirationDaysMessage('remediationAvailable', null);

      expect(message).toBe('This waiver will expire when an upgrade that fixes the violation is available');
    });
  });

  describe('formatCustomDate', () => {
    it('should format valid date', () => {
      const date = '2027-12-31';
      const formatted = formatCustomDate(date);

      expect(formatted).toBe('2027-12-31');
    });

    it('should return empty string for invalid date', () => {
      // Suppress moment.js deprecation warnings for invalid date tests
      const consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});

      expect(formatCustomDate('')).toBe('');
      expect(formatCustomDate(null)).toBe('');
      expect(formatCustomDate('invalid-date')).toBe('');

      consoleWarnSpy.mockRestore();
    });
  });

  describe('formatFirewallComponentForWaiver', () => {
    it('should format component data correctly', () => {
      const component = {
        policyName: 'Security Policy',
        policyId: 'policy-1',
        policyViolationId: 'v1',
        threatLevel: 10,
        pathname: '/path/to/component',
        displayName: 'Component A',
        componentIdentifier: {
          format: 'maven',
          coordinates: { groupId: 'com.example', artifactId: 'component-a' },
        },
        matchState: 'exact',
        constraints: [
          {
            constraintId: 'c1',
            constraintName: 'Security Constraint',
            conditions: [{ conditionReason: 'Critical vulnerability' }],
          },
        ],
      };

      const formatted = formatFirewallComponentForWaiver(component);

      expect(formatted.policyName).toBe('Security Policy');
      expect(formatted.policyId).toBe('policy-1');
      expect(formatted.policyViolationId).toBe('v1');
      expect(formatted.policyThreatLevel).toBe(10);
      expect(formatted.pathname).toBe('/path/to/component');
      expect(formatted.displayName).toBe('Component A');
      expect(formatted.componentIdentifier).toEqual(component.componentIdentifier);
      expect(formatted.matchState).toBe('exact');
      expect(formatted.constraintViolations).toHaveLength(1);
      expect(formatted.constraintViolations[0].constraintId).toBe('c1');
    });

    it('should use coordinates as displayName fallback', () => {
      const component = {
        policyName: 'Policy',
        policyId: 'p1',
        policyViolationId: 'v1',
        threatLevel: 5,
        pathname: '/path',
        displayName: null,
        componentIdentifier: {
          format: 'maven',
          coordinates: { groupId: 'com.example', artifactId: 'component-b' },
        },
        matchState: 'similar',
        constraints: [],
      };

      const formatted = formatFirewallComponentForWaiver(component);
      expect(formatted.displayName).toEqual(component.componentIdentifier.coordinates);
    });
  });

  describe('buildFirewallBulkWaiverPayload', () => {
    it('should build payload with standard expiration', () => {
      const params = {
        selectedViolations: [
          {
            policyName: 'Policy',
            policyId: 'p1',
            policyViolationId: 'v1',
            threatLevel: 7,
            pathname: '/path',
            displayName: 'Component',
            componentIdentifier: { format: 'maven' },
            matchState: 'exact',
            constraints: [],
          },
        ],
        waiverReasonId: 'reason-1',
        expiryTime: '30',
        customExpiryTime: { value: '' },
        comments: 'Test comments',
        componentMatcherStrategy: 'ALL_VERSIONS',
        selectedWaiverScope: { id: 'scope-1', label: 'Repository' },
      };

      const payload = buildFirewallBulkWaiverPayload(params);

      expect(payload.violations).toHaveLength(1);
      expect(payload.waiverReasonId).toBe('reason-1');
      expect(payload.expiryTime).toBeDefined();
      expect(payload.comment).toBe('Test comments');
      expect(payload.componentMatcherStrategy).toBe('ALL_VERSIONS');
      expect(payload.scopeOwnerId).toBe('scope-1');
      expect(payload.scopeOwnerType).toBe('Repository');
    });

    it('should build payload with custom expiration', () => {
      const futureDateStr = moment().add(45, 'days').format('YYYY-MM-DD');

      const params = {
        selectedViolations: [],
        waiverReasonId: 'reason-1',
        expiryTime: 'custom',
        customExpiryTime: { value: futureDateStr },
        comments: '',
        componentMatcherStrategy: 'EXACT_COMPONENT',
        selectedWaiverScope: { id: 'scope-1', label: 'Repository' },
      };

      const payload = buildFirewallBulkWaiverPayload(params);

      expect(payload.expiryTime).toBe(futureDateStr);
    });

    it('should build payload with never expiration', () => {
      const params = {
        selectedViolations: [],
        waiverReasonId: 'reason-1',
        expiryTime: 'never',
        customExpiryTime: { value: '' },
        comments: '',
        componentMatcherStrategy: 'ALL_VERSIONS',
        selectedWaiverScope: { id: 'scope-1', label: 'Repository' },
      };

      const payload = buildFirewallBulkWaiverPayload(params);

      expect(payload.expiryTime).toBeNull();
    });
  });

  describe('validateFirewallBulkWaiverConfig', () => {
    const validConfig = {
      waiverReasonId: 'reason-1',
      expiryTime: '30',
      customExpiryTime: { value: '' },
      componentMatcherStrategy: 'ALL_VERSIONS',
      selectedWaiverScope: { id: 'scope-1' },
      selectedViolations: [{ policyViolationId: 'v1' }],
    };

    it('should validate valid configuration', () => {
      const result = validateFirewallBulkWaiverConfig(validConfig);

      expect(result.isValid).toBe(true);
      expect(result.errors).toHaveLength(0);
    });

    it('should require waiver reason', () => {
      const config = { ...validConfig, waiverReasonId: null };
      const result = validateFirewallBulkWaiverConfig(config);

      expect(result.isValid).toBe(false);
      expect(result.errors).toContain('Waiver reason is required');
    });

    it('should require expiration time', () => {
      const config = { ...validConfig, expiryTime: null };
      const result = validateFirewallBulkWaiverConfig(config);

      expect(result.isValid).toBe(false);
      expect(result.errors).toContain('Expiration time is required');
    });

    it('should validate custom expiry date is in future', () => {
      const config = {
        ...validConfig,
        expiryTime: 'custom',
        customExpiryTime: { value: '2020-01-01' },
      };
      const result = validateFirewallBulkWaiverConfig(config);

      expect(result.isValid).toBe(false);
      expect(result.errors).toContain('Custom expiry date must be in the future');
    });

    it('should require component matcher strategy', () => {
      const config = { ...validConfig, componentMatcherStrategy: null };
      const result = validateFirewallBulkWaiverConfig(config);

      expect(result.isValid).toBe(false);
      expect(result.errors).toContain('Component matcher strategy is required');
    });

    it('should require waiver scope', () => {
      const config = { ...validConfig, selectedWaiverScope: null };
      const result = validateFirewallBulkWaiverConfig(config);

      expect(result.isValid).toBe(false);
      expect(result.errors).toContain('Waiver scope is required');
    });

    it('should require at least one violation', () => {
      const config = { ...validConfig, selectedViolations: [] };
      const result = validateFirewallBulkWaiverConfig(config);

      expect(result.isValid).toBe(false);
      expect(result.errors).toContain('At least one violation must be selected');
    });

    it('should return multiple errors for multiple missing fields', () => {
      const config = {
        waiverReasonId: null,
        expiryTime: null,
        customExpiryTime: { value: '' },
        componentMatcherStrategy: null,
        selectedWaiverScope: null,
        selectedViolations: [],
      };
      const result = validateFirewallBulkWaiverConfig(config);

      expect(result.isValid).toBe(false);
      expect(result.errors.length).toBeGreaterThan(1);
    });
  });

  describe('displayFirewallRepositoryScope', () => {
    it('should format repository container scope', () => {
      const scope = { label: 'Repository_container', name: 'Container Name' };
      const result = displayFirewallRepositoryScope(scope);

      expect(result).toBe('Container Name');
    });

    it('should format repository scope', () => {
      const scope = { label: 'repository', name: 'Repo Name' };
      const result = displayFirewallRepositoryScope(scope);

      expect(result).toBe('Repository - Repo Name');
    });

    it('should format repository manager scope', () => {
      const scope = { label: 'repository_manager', name: 'Manager Name' };
      const result = displayFirewallRepositoryScope(scope);

      expect(result).toBe('Repository Manager - Manager Name');
    });

    it('should format unknown scope type', () => {
      const scope = { label: 'custom_scope', name: 'Custom Name' };
      const result = displayFirewallRepositoryScope(scope);

      expect(result).toBe('custom_scope - Custom Name');
    });

    it('should return empty string for null scope', () => {
      const result = displayFirewallRepositoryScope(null);

      expect(result).toBe('');
    });
  });

  describe('normalizeFirewallOwnerType', () => {
    it('maps root_organization to organization', () => {
      expect(normalizeFirewallOwnerType('root_organization')).toBe('organization');
    });

    it('maps ROOT_ORGANIZATION (uppercase) to organization', () => {
      expect(normalizeFirewallOwnerType('ROOT_ORGANIZATION')).toBe('organization');
    });

    it('lowercases organization', () => {
      expect(normalizeFirewallOwnerType('Organization')).toBe('organization');
    });

    it('lowercases repository_container', () => {
      expect(normalizeFirewallOwnerType('Repository_Container')).toBe('repository_container');
    });

    it('returns null for null input', () => {
      expect(normalizeFirewallOwnerType(null)).toBeNull();
    });

    it('returns undefined for undefined input', () => {
      expect(normalizeFirewallOwnerType(undefined)).toBeUndefined();
    });

    it('passes through other owner types lowercased', () => {
      expect(normalizeFirewallOwnerType('repository')).toBe('repository');
    });
  });
});
