/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  selectComponentDetailsViolationsSlice,
  selectComponentViolations,
  selectSelectedViolationDetail,
} from '../../../../main/frontend/componentDetails/ViolationsTableTile/PolicyViolationsSelectors';

describe('policyViolationsSelectors', () => {
  const mainViolation = {
    policyId: 'policy2',
    policyViolationId: 'violation2',
    policyName: 'Security-High',
    policyThreatLevel: 10,
    waived: false,
    grandfathered: false,
    policyThreatCategory: 'SECURITY',
    constraints: [
      {
        constraintId: 'contraintIdViolation2',
        constraintName: 'High risk CVSS score',
        constraintOperator: 'AND',
        conditions: [
          {
            conditionType: 'SecurityVulnerabilitySeverity',
            conditionSummary: 'Security Vulnerability Severity >= 7',
            conditionReason: 'Found security vulnerability CVE-0001-0002 with severity >= 7 (severity = 8.6)',
            conditionTriggerReference: { value: 'CVE-0001-0002', type: 'SECURITY_VULNERABILITY_REFID' },
          },
          {
            conditionType: 'SecurityVulnerabilitySeverity',
            conditionSummary: 'Security Vulnerability Severity < 9',
            conditionReason: 'Found security vulnerability CVE-0001-0002 with severity < 9 (severity = 8.6)',
            conditionTriggerReference: { value: 'CVE-0001-0002', type: 'SECURITY_VULNERABILITY_REFID' },
          },
        ],
      },
    ],
  };
  const mockViolations = [{ policyViolationId: 'violation1' }, mainViolation];
  const mockState = {
    router: {
      currentParams: {
        hash: 'some-component-hash',
      },
    },
    applicationReport: {
      metadata: {
        application: {
          name: 'The App',
          organization: {
            name: 'The Org',
          },
        },
        reportTime: 1623135382098,
        reportTitle: 'Title of Report',
      },
      selectedReport: {
        displayedEntries: [{ hash: 'some-component-hash', derivedComponentName: 'a-name-componentname : v1.0.0' }],
      },
    },
    componentDetailsPolicyViolations: {
      violations: mockViolations,
      waivers: ['waiver1'],
      loading: false,
      loadError: 'error during last load',
      selectedPolicyViolationId: 'violation2',
      violationType: null,
    },
  };

  describe('selectComponentDetailsViolationsSlice', () => {
    it('selects the componentDetailsViolations slice of the state', () => {
      const expectedSelection = {
        violations: mockViolations,
        waivers: ['waiver1'],
        loading: false,
        loadError: 'error during last load',
        selectedPolicyViolationId: 'violation2',
        violationType: null,
      };

      const actualSelection = selectComponentDetailsViolationsSlice(mockState);
      expect(actualSelection).toEqual(expectedSelection);
    });
  });

  describe('selectComponentViolations', () => {
    it('returns the violations currently contained in the componentDetailsViolations slice', () => {
      const expectedSelection = mockViolations;

      const actualSelection = selectComponentViolations(mockState);
      expect(actualSelection).toEqual(expectedSelection);
    });

    it('returns the filtered violations currently contained in the componentDetailsViolations slice', () => {
      const expectedSelection = mockViolations.filter((violation) => violation.policyThreatCategory === 'SECURITY');

      const actualSelection = selectComponentViolations({
        ...mockState,
        componentDetailsPolicyViolations: {
          ...mockState.componentDetailsPolicyViolations,
          violationType: 'SECURITY',
        },
      });
      expect(actualSelection).toEqual(expectedSelection);
    });
  });

  describe('selectSelectedViolationDetail', () => {
    it('returns null when there is no selectedPolicyViolationId in the componentDetailsViolations slice', () => {
      const stateWithNoSelectedViolation = {
        ...mockState,
        componentDetailsPolicyViolations: {
          ...mockState.componentDetailsPolicyViolations,
          selectedPolicyViolationId: null,
        },
      };
      const actualSelection = selectSelectedViolationDetail(stateWithNoSelectedViolation);
      expect(actualSelection).toBeNull();
    });

    it('returns null when there is no match for the selected PolicyViolationId in the componentDetailsViolations slice', () => {
      const stateWithNoSelectedViolation = {
        ...mockState,
        componentDetailsPolicyViolations: {
          ...mockState.componentDetailsPolicyViolations,
          selectedPolicyViolationId: 'randomViolation',
        },
      };
      const actualSelection = selectSelectedViolationDetail(stateWithNoSelectedViolation);
      expect(actualSelection).toBeNull();
    });

    it('returns the expected structure of a violationDetails for the selectedPolicyViolationId  in the componentDetailsViolations slice', () => {
      const expectedSelection = {
        policyViolationId: 'violation2',
        policyName: 'Security-High',
        derivedComponentName: 'a-name-componentname : v1.0.0',
        violationVulnerabilityId: 'CVE-0001-0002',
        threatLevel: 10,
        constraintViolations: [
          {
            constraintName: 'High risk CVSS score',
            reasons: [
              { reason: 'Found security vulnerability CVE-0001-0002 with severity >= 7 (severity = 8.6)' },
              { reason: 'Found security vulnerability CVE-0001-0002 with severity < 9 (severity = 8.6)' },
            ],
          },
        ],
      };
      const actualSelection = selectSelectedViolationDetail(mockState);
      expect(actualSelection).toEqual(expectedSelection);
    });
  });
});
