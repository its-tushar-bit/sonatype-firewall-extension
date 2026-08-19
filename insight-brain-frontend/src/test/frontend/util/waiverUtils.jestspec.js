/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  displayWaiverScope,
  convertToWaiverViolationFormat,
  formatWaiverDetails,
  waiverMatcherStrategy,
  isCustomExpiryTimeValid,
  getExpirationDaysMessage,
  isWaiverExpired,
  getWaiverDaysRemaining,
  useWaiverExpirations,
} from 'MainRoot/util/waiverUtils';
import { WAIVER_CREATE_TIME, WAIVER_EXPIRATION_TIME } from 'TestRoot/SpecUtil';
import moment from 'moment';

describe('waiverUtils', function () {
  describe('dislayWaiverScope', () => {
    it('returns a readable label if the scopeOwnerType is `root_organization`', () => {
      const waiver = {
        scopeOwnerType: 'root_organization',
      };
      const result = displayWaiverScope(waiver);
      expect(result).toEqual('Root Organization');
    });

    it('returns a readable label with name if the scopeOwnerType is `organization`', () => {
      const waiver = {
        scopeOwnerType: 'organization',
        scopeOwnerName: 'a org',
      };
      const result = displayWaiverScope(waiver);
      expect(result).toEqual('Organization - a org');
    });

    it('returns a readable label with name if the scopeOwnerType is `application`', () => {
      const waiver = {
        scopeOwnerType: 'application',
        scopeOwnerName: 'App X',
      };
      const result = displayWaiverScope(waiver);
      expect(result).toEqual('Application - App X');
    });

    it('returns a readable label with name if the scopeOwnerType is `repository`', () => {
      const waiver = {
        scopeOwnerType: 'repository',
        scopeOwnerName: 'maven-central',
      };
      const result = displayWaiverScope(waiver);
      expect(result).toEqual('Repository - maven-central');
    });

    it('returns a readable label with name if the scopeOwnerType is `repository_container`', () => {
      const waiver = {
        scopeOwnerType: 'repository_container',
        scopeOwnerName: 'Repository Managers',
      };
      const result = displayWaiverScope(waiver);
      expect(result).toEqual('Repository Managers');
    });

    it('returns null if the scopeOwnerType is not valid', () => {
      let waiver = {
        scopeOwnerType: 'weird',
      };
      let result = displayWaiverScope(waiver);
      expect(result).toBeNull();

      waiver.scopeOwnerType = undefined;
      result = displayWaiverScope(waiver);
      expect(result).toBeNull();

      waiver.scopeOwnerType = null;
      result = displayWaiverScope(waiver);
      expect(result).toBeNull();
    });
  });

  describe('formatWaiverDetails', () => {
    const componentDisplayName = {
      parts: [
        {
          field: 'Group',
          value: 'test-group',
        },
        {
          value: ':',
        },
        {
          field: 'Artifact',
          value: 'test-artifact',
        },
        {
          value: ':',
        },
        {
          field: 'Version',
          value: '1.2.3',
        },
      ],
    };

    it('returns a falsy object for a falsy waiver object', () => {
      expect(formatWaiverDetails(null)).toEqual({});
    });

    it('returns the correctly formatted data from the waiver object', function () {
      const waiverDetails = {
        comment: 'a comment',
        constraintFacts: [
          { constraintName: 'test constraint', conditionFacts: [{ reason: 'reason 1' }, { reason: 'reason 2' }] },
        ],
        createTime: WAIVER_CREATE_TIME,
        creatorName: 'test creator',
        expiryTime: WAIVER_EXPIRATION_TIME,
        policyName: 'test policy',
        policyWaiverId: '1234testid',
        scopeOwnerId: 'ROOT_ORGANIZATION',
        scopeOwnerName: 'root org',
        scopeOwnerType: 'root_organization',
        vulnerabilityId: 'CVE-2013-7285',
        associatedPackageUrl: 'a/package/url',
        componentIdentifier: null,
        matcherStrategy: waiverMatcherStrategy.EXACT_COMPONENT,
        displayName: componentDisplayName,
        componentUpgradeAvailable: null,
        reasonText: 'reason 1',
      };

      expect(formatWaiverDetails(waiverDetails)).toEqual({
        policyName: 'test policy',
        constraintName: 'test constraint',
        reasons: ['reason 1', 'reason 2'],
        waiverScope: 'Root Organization',
        expiration: WAIVER_EXPIRATION_TIME,
        comment: 'a comment',
        creatorName: 'test creator',
        vulnerabilityId: 'CVE-2013-7285',
        dateCreated: WAIVER_CREATE_TIME,
        component: {
          associatedPackageUrl: 'a/package/url',
          componentIdentifier: null,
          displayName: componentDisplayName,
          matcherStrategy: waiverMatcherStrategy.EXACT_COMPONENT,
        },
        componentUpgradeAvailable: null,
        reasonText: 'reason 1',
      });
    });

    it('returns the correctly formatted data from the waiver object with no reason', function () {
      const waiverDetails = {
        comment: 'a comment',
        constraintFacts: [
          { constraintName: 'test constraint', conditionFacts: [{ reason: 'reason 1' }, { reason: 'reason 2' }] },
        ],
        createTime: WAIVER_CREATE_TIME,
        creatorName: 'test creator',
        expiryTime: WAIVER_EXPIRATION_TIME,
        policyName: 'test policy',
        policyWaiverId: '1234testid',
        scopeOwnerId: 'ROOT_ORGANIZATION',
        scopeOwnerName: 'root org',
        scopeOwnerType: 'root_organization',
        vulnerabilityId: 'CVE-2013-7285',
        associatedPackageUrl: 'a/package/url',
        componentIdentifier: null,
        matcherStrategy: waiverMatcherStrategy.EXACT_COMPONENT,
        displayName: componentDisplayName,
        componentUpgradeAvailable: null,
        reasonText: null,
      };

      expect(formatWaiverDetails(waiverDetails)).toEqual({
        policyName: 'test policy',
        constraintName: 'test constraint',
        reasons: ['reason 1', 'reason 2'],
        waiverScope: 'Root Organization',
        expiration: WAIVER_EXPIRATION_TIME,
        comment: 'a comment',
        creatorName: 'test creator',
        vulnerabilityId: 'CVE-2013-7285',
        dateCreated: WAIVER_CREATE_TIME,
        component: {
          associatedPackageUrl: 'a/package/url',
          componentIdentifier: null,
          displayName: componentDisplayName,
          matcherStrategy: waiverMatcherStrategy.EXACT_COMPONENT,
        },
        componentUpgradeAvailable: null,
        reasonText: '--',
      });
    });

    it('formats the expiration correctly for waivers with no expiration date', () => {
      const waiverDetails = {
        comment: 'a comment',
        constraintFacts: [
          { constraintName: 'test constraint', conditionFacts: [{ reason: 'reason 1' }, { reason: 'reason 2' }] },
        ],
        createTime: WAIVER_CREATE_TIME,
        creatorName: 'test creator',
        policyName: 'test policy',
        policyWaiverId: '1234testid',
        scopeOwnerId: 'ROOT_ORGANIZATION',
        scopeOwnerName: 'root org',
        scopeOwnerType: 'root_organization',
        vulnerabilityId: 'CVE-2013-7285',
        associatedPackageUrl: 'a/package/url',
        componentIdentifier: null,
        matcherStrategy: waiverMatcherStrategy.EXACT_COMPONENT,
        displayName: componentDisplayName,
        componentUpgradeAvailable: null,
        reasonText: 'reason 1',
      };

      expect(formatWaiverDetails(waiverDetails)).toEqual({
        policyName: 'test policy',
        constraintName: 'test constraint',
        reasons: ['reason 1', 'reason 2'],
        waiverScope: 'Root Organization',
        expiration: 'Does not expire',
        comment: 'a comment',
        creatorName: 'test creator',
        vulnerabilityId: 'CVE-2013-7285',
        dateCreated: WAIVER_CREATE_TIME,
        component: {
          associatedPackageUrl: 'a/package/url',
          componentIdentifier: null,
          displayName: componentDisplayName,
          matcherStrategy: waiverMatcherStrategy.EXACT_COMPONENT,
        },
        componentUpgradeAvailable: null,
        reasonText: 'reason 1',
      });
    });
  });

  describe('convertToWaiverViolationFormat', () => {
    let incomingData, convertData;
    beforeEach(function () {
      incomingData = {
        policyViolationId: 'e0ecf0a629d341e88179f8d40f4675ee',
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'ant',
            classifier: '',
            extension: 'jar',
            groupId: 'ant',
            version: '1.6',
          },
        },
        componentDisplayName: {
          parts: [
            {
              field: 'Group',
              value: 'ant',
            },
            {
              value: ' : ',
            },
            {
              field: 'Artifact',
              value: 'ant',
            },
            {
              value: ' : ',
            },
            {
              field: 'Version',
              value: '1.6',
            },
          ],
          name: 'ant',
        },
        hash: '7a3c2521ae0c6f53e044',
        policyId: 'd98fb873ed1f48e5b00316d8acddbc0f',
        policyName: 'Security-Medium',
        policyOwner: {
          ownerId: 'ROOT_ORGANIZATION_ID',
          ownerName: 'Root Organization',
          ownerType: 'organization',
        },
        policyThreatLevel: 7,
        policyThreatCategory: 'SECURITY',
        constraints: [
          {
            constraintId: 'c6436a5a051046b1ba2aa94e9fd82a51',
            constraintName: 'Medium risk CVSS score',
            constraintOperator: 'AND',
            conditions: [
              {
                conditionType: 'SecurityVulnerabilitySeverity',
                conditionSummary: 'Security Vulnerability Severity >= 4',
                conditionReason: 'Found security vulnerability CVE-2012-2098 with severity >= 4 (severity = 5.0)',
                conditionTriggerReference: {
                  value: 'CVE-2012-2098',
                  type: 'SECURITY_VULNERABILITY_REFID',
                },
              },
              {
                conditionType: 'SecurityVulnerabilitySeverity',
                conditionSummary: 'Security Vulnerability Severity < 7',
                conditionReason: 'Found security vulnerability CVE-2012-2098 with severity < 7 (severity = 5.0)',
                conditionTriggerReference: {
                  value: 'CVE-2012-2098',
                  type: 'SECURITY_VULNERABILITY_REFID',
                },
              },
            ],
          },
        ],
        constraintFactsJson:
          '[{"constraintId":"c6436a5a051046b1ba2aa94e9fd82a51","constraintName":"Medium risk CVSS score","operatorName":"AND","conditionFacts":[{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":0,"summary":"Security Vulnerability Severity >= 4","reason":"Found security vulnerability CVE-2012-2098 with severity >= 4 (severity = 5.0)","reference":{"value":"CVE-2012-2098","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":0,\\"trigger\\":{\\"refId\\":\\"CVE-2012-2098\\",\\"severity\\":5.0}}"},{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":1,"summary":"Security Vulnerability Severity < 7","reason":"Found security vulnerability CVE-2012-2098 with severity < 7 (severity = 5.0)","reference":{"value":"CVE-2012-2098","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":1,\\"trigger\\":{\\"refId\\":\\"CVE-2012-2098\\",\\"severity\\":5.0}}"}]}]',
        policyActionTypeId: null,
        lastReported: '2022-10-10T16:01:37.586+03:00',
      };

      convertData = {
        policyViolationId: 'e0ecf0a629d341e88179f8d40f4675ee',
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'ant',
            classifier: '',
            extension: 'jar',
            groupId: 'ant',
            version: '1.6',
          },
        },
        componentDisplayName: {
          parts: [
            {
              field: 'Group',
              value: 'ant',
            },
            {
              value: ' : ',
            },
            {
              field: 'Artifact',
              value: 'ant',
            },
            {
              value: ' : ',
            },
            {
              field: 'Version',
              value: '1.6',
            },
          ],
          name: 'ant',
        },
        hash: '7a3c2521ae0c6f53e044',
        policyId: 'd98fb873ed1f48e5b00316d8acddbc0f',
        policyName: 'Security-Medium',
        policyOwner: {
          ownerId: 'ROOT_ORGANIZATION_ID',
          ownerName: 'Root Organization',
          ownerType: 'organization',
        },
        policyThreatLevel: 7,
        policyThreatCategory: 'SECURITY',
        constraints: [
          {
            constraintId: 'c6436a5a051046b1ba2aa94e9fd82a51',
            constraintName: 'Medium risk CVSS score',
            constraintOperator: 'AND',
            conditions: [
              {
                conditionType: 'SecurityVulnerabilitySeverity',
                conditionSummary: 'Security Vulnerability Severity >= 4',
                conditionReason: 'Found security vulnerability CVE-2012-2098 with severity >= 4 (severity = 5.0)',
                conditionTriggerReference: {
                  value: 'CVE-2012-2098',
                  type: 'SECURITY_VULNERABILITY_REFID',
                },
              },
              {
                conditionType: 'SecurityVulnerabilitySeverity',
                conditionSummary: 'Security Vulnerability Severity < 7',
                conditionReason: 'Found security vulnerability CVE-2012-2098 with severity < 7 (severity = 5.0)',
                conditionTriggerReference: {
                  value: 'CVE-2012-2098',
                  type: 'SECURITY_VULNERABILITY_REFID',
                },
              },
            ],
          },
        ],
        constraintFactsJson:
          '[{"constraintId":"c6436a5a051046b1ba2aa94e9fd82a51","constraintName":"Medium risk CVSS score","operatorName":"AND","conditionFacts":[{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":0,"summary":"Security Vulnerability Severity >= 4","reason":"Found security vulnerability CVE-2012-2098 with severity >= 4 (severity = 5.0)","reference":{"value":"CVE-2012-2098","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":0,\\"trigger\\":{\\"refId\\":\\"CVE-2012-2098\\",\\"severity\\":5.0}}"},{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":1,"summary":"Security Vulnerability Severity < 7","reason":"Found security vulnerability CVE-2012-2098 with severity < 7 (severity = 5.0)","reference":{"value":"CVE-2012-2098","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":1,\\"trigger\\":{\\"refId\\":\\"CVE-2012-2098\\",\\"severity\\":5.0}}"}]}]',
        policyActionTypeId: null,
        lastReported: '2022-10-10T16:01:37.586+03:00',
        threatLevel: 7,
        constraintViolations: [
          {
            constraintId: 'c6436a5a051046b1ba2aa94e9fd82a51',
            constraintName: 'Medium risk CVSS score',
            reasons: [
              {
                reason: 'Found security vulnerability CVE-2012-2098 with severity >= 4 (severity = 5.0)',
                reference: null,
              },
            ],
          },
        ],
        applicationPublicId: '',
        applicationName: '',
        organizationName: '',
        openTime: '2022-10-10T16:01:37.586+03:00',
        fixTime: null,
        displayName: {
          parts: [
            {
              field: 'Group',
              value: 'ant',
            },
            {
              value: ' : ',
            },
            {
              field: 'Artifact',
              value: 'ant',
            },
            {
              value: ' : ',
            },
            {
              field: 'Version',
              value: '1.6',
            },
          ],
          name: 'ant',
        },
        filename: null,
        stageData: {},
        waived: false,
      };
    });

    it('convert incoming data for waiver violation structure', () => {
      const convertResult = convertToWaiverViolationFormat(incomingData);
      expect(convertResult).toEqual(convertData);
    });
  });

  describe('isCustomExpiryTimeValid', () => {
    it('should return false if the value is null or undefined', () => {
      expect(isCustomExpiryTimeValid(null)).toBe(false);
      expect(isCustomExpiryTimeValid(undefined)).toBe(false);
    });

    it('should return false if the value is an invalid date', () => {
      expect(isCustomExpiryTimeValid('invalid-date')).toBe(false);
    });

    it('should return false if the value is a past date', () => {
      const pastDate = new Date();
      pastDate.setDate(pastDate.getDate() - 1);
      expect(isCustomExpiryTimeValid(pastDate.toISOString())).toBe(false);
    });

    it('should return true if the value is a future date', () => {
      const futureDate = new Date();
      futureDate.setDate(futureDate.getDate() + 1);
      expect(isCustomExpiryTimeValid(futureDate.toISOString())).toBe(true);
    });
  });

  describe('isWaiverExpired', () => {
    beforeEach(() => {
      jest.useFakeTimers({ now: new Date(2026, 4, 12) }); // 2026-05-12
    });

    afterEach(() => {
      jest.useRealTimers();
    });

    it('returns false for null expiryTime', () => {
      expect(isWaiverExpired(null)).toBe(false);
    });

    it('returns false for undefined expiryTime', () => {
      expect(isWaiverExpired(undefined)).toBe(false);
    });

    it('returns false when expiryTime is today', () => {
      expect(isWaiverExpired(new Date(2026, 4, 12).getTime())).toBe(false);
    });

    it('returns false when expiryTime is tomorrow', () => {
      expect(isWaiverExpired(new Date(2026, 4, 13).getTime())).toBe(false);
    });

    it('returns true when expiryTime is yesterday', () => {
      expect(isWaiverExpired(new Date(2026, 4, 11).getTime())).toBe(true);
    });

    it('returns true when expiryTime is well in the past', () => {
      expect(isWaiverExpired(new Date(2025, 0, 1).getTime())).toBe(true);
    });
  });

  describe('getWaiverDaysRemaining', () => {
    beforeEach(() => {
      jest.useFakeTimers({ now: new Date(2026, 4, 12) }); // 2026-05-12
    });

    afterEach(() => {
      jest.useRealTimers();
    });

    it('returns null for null expiryTime', () => {
      expect(getWaiverDaysRemaining(null, false, false)).toBeNull();
    });

    it('returns null for undefined expiryTime', () => {
      expect(getWaiverDaysRemaining(undefined, false, false)).toBeNull();
    });

    it('returns null for auto waivers', () => {
      expect(getWaiverDaysRemaining(new Date(2026, 4, 20).getTime(), true, false)).toBeNull();
    });

    it('returns null for remediation-available waivers', () => {
      expect(getWaiverDaysRemaining(new Date(2026, 4, 20).getTime(), false, true)).toBeNull();
    });

    it('returns 0 when expiryTime is today', () => {
      expect(getWaiverDaysRemaining(new Date(2026, 4, 12).getTime(), false, false)).toBe(0);
    });

    it('returns 1 when expiryTime is tomorrow', () => {
      expect(getWaiverDaysRemaining(new Date(2026, 4, 13).getTime(), false, false)).toBe(1);
    });

    it('returns -1 when expiryTime is yesterday', () => {
      expect(getWaiverDaysRemaining(new Date(2026, 4, 11).getTime(), false, false)).toBe(-1);
    });

    it('returns 30 when expiryTime is 30 days from now', () => {
      expect(getWaiverDaysRemaining(new Date(2026, 5, 11).getTime(), false, false)).toBe(30);
    });
  });

  describe('getExpirationDaysMessage', () => {
    it('should return the correct message for expiry time', () => {
      expect(getExpirationDaysMessage('30', null)).toBe('This waiver will expire in 30 days');
    });

    it('should return the correct message for "never" expiry time', () => {
      expect(getExpirationDaysMessage('never', null)).toBe('');
    });

    it('should return the correct message when remediationAvailable', () => {
      expect(getExpirationDaysMessage('remediationAvailable', null)).toBe(
        'This waiver will expire when an upgrade that fixes the violation is available'
      );
    });

    it('should return the correct message for custom expiry time', () => {
      // Use fake timers to avoid DST boundary issues (e.g. spring-forward losing an hour)
      jest.useFakeTimers({ now: new Date(2026, 0, 15) });
      try {
        const customExpiryTime = { value: moment().add(5, 'days').format('YYYY-MM-DD') };
        expect(getExpirationDaysMessage('custom', customExpiryTime)).toBe('This waiver will expire in 5 days');
      } finally {
        jest.useRealTimers();
      }
    });
  });

  describe('useWaiverExpirations', () => {
    const hasRemediationAvailable = (options) => options.some((o) => o.value === 'remediationAvailable');

    it('includes the "When Remediation Available" option when the feature is enabled', () => {
      expect(hasRemediationAvailable(useWaiverExpirations(true))).toBe(true);
    });

    it('excludes it when the feature is disabled and no loaded value needs it', () => {
      expect(hasRemediationAvailable(useWaiverExpirations(false))).toBe(false);
      expect(hasRemediationAvailable(useWaiverExpirations(false, null))).toBe(false);
      expect(hasRemediationAvailable(useWaiverExpirations(false, 'custom'))).toBe(false);
      expect(hasRemediationAvailable(useWaiverExpirations(false, 'never'))).toBe(false);
    });

    it('still includes it when the feature is disabled but the loaded value is remediationAvailable', () => {
      // Without this branch, an uncontrolled <select defaultValue="remediationAvailable">
      // falls back to option index 0 ("Never") while Redux state and the submitted payload
      // still say 'remediationAvailable' — a silent UI/payload disagreement.
      expect(hasRemediationAvailable(useWaiverExpirations(false, 'remediationAvailable'))).toBe(true);
    });
  });
});
