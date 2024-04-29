/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  selectComponentDetailsViolationsSlice,
  selectComponentViolations,
  selectIsPolicyViolationsLoading,
  selectSelectedComponentPolicyViolation,
  selectSelectedViolationId,
} from 'MainRoot/componentDetails/ViolationsTableTile/PolicyViolationsSelectors';

describe('policyViolationsSelectors', () => {
  const mainViolation = {
    policyId: 'policy2',
    policyViolationId: 'violation2',
    policyName: 'Security-High',
    policyThreatLevel: 10,
    waived: false,
    legacyViolation: false,
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

  describe('selectIsPolicyViolationsLoading', () => {
    it('selects the `loading` prop from the state', () => {
      const state = {
        ...mockState,
        componentDetailsPolicyViolations: {
          ...mockState.componentDetailsPolicyViolations,
          loading: true,
        },
      };
      const actual = selectIsPolicyViolationsLoading(state);
      expect(actual).toEqual(true);

      const state2 = {
        ...mockState,
        componentDetailsPolicyViolations: {
          ...mockState.componentDetailsPolicyViolations,
          loading: false,
        },
      };
      const actual2 = selectIsPolicyViolationsLoading(state2);
      expect(actual2).toEqual(false);
    });
  });

  describe('selectSelectedComponentPolicyViolation', () => {
    const violations = [
      {
        policyName: 'Some policy name',
        threatLevel: 10,
        policyViolationId: 'violationId',
      },
    ];
    const transitiveViolations = [
      {
        policyName: 'Some transitive policy name',
        threatLevel: 2,
        policyViolationId: 'violationId',
      },
    ];
    const selectedPolicyViolationId = 'violationId';

    it('selects the violation that has been selected from violations slice', () => {
      const result = selectSelectedComponentPolicyViolation.resultFunc(
        selectedPolicyViolationId,
        violations,
        transitiveViolations,
        false
      );
      expect(result).toEqual({
        policyName: 'Some policy name',
        threatLevel: 10,
        policyViolationId: 'violationId',
      });
    });

    it('selects the violation that has been selected from transitive violations slice', () => {
      const result = selectSelectedComponentPolicyViolation.resultFunc(
        selectedPolicyViolationId,
        violations,
        transitiveViolations,
        true
      );
      expect(result).toEqual({
        policyName: 'Some transitive policy name',
        threatLevel: 2,
        policyViolationId: 'violationId',
      });
    });

    it('returns undefined when there is no violation selected', () => {
      const result = selectSelectedComponentPolicyViolation.resultFunc(null, violations);
      expect(result).toBeUndefined();
    });
  });

  describe('selectSelectedViolationId', () => {
    it('selects the violation ID from the routerCurrentParams as violationId', () => {
      const result = selectSelectedViolationId.resultFunc(
        {
          selectedPolicyViolationId: 'selectedPolicyViolationId',
        },
        {
          violationId: 'violationId',
          id: 'id',
        }
      );
      expect(result).toEqual('violationId');
    });

    it('selects the violation ID from the routerCurrentParams as id', () => {
      const result = selectSelectedViolationId.resultFunc(
        {
          selectedPolicyViolationId: 'selectedPolicyViolationId',
        },
        {
          id: 'id',
        }
      );
      expect(result).toEqual('id');
    });

    it('selects the violation ID from the componentDetailsPolicyViolations as selectedPolicyViolationId', () => {
      const result = selectSelectedViolationId.resultFunc(
        {
          selectedPolicyViolationId: 'selectedPolicyViolationId',
        },
        {}
      );
      expect(result).toEqual('selectedPolicyViolationId');
    });
  });
});
