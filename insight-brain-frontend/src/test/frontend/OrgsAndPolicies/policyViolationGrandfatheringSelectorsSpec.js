/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectOrgsAndPoliciesSlice } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import {
  selectPolicyViolationGrandfatheringSlice,
  selectLoadError,
  selectLoading,
  selectPolicyViolationGrandfathering,
  selectPolicyViolationGrandfatheringConfig,
  selectCalculatedEnabled,
  selectGrandfatheringStatusMessage,
} from 'MainRoot/OrgsAndPolicies/policyViolationGrandfatheringSelectors';
import { selectIsRootOrganization } from 'MainRoot/reduxUiRouter/routerSelectors';

describe('policyViolationGrandfatheringSelectors', () => {
  describe('selectPolicyViolationGrandfatheringSlice', () => {
    it('is composed from the following selector', () => {
      expect(selectPolicyViolationGrandfatheringSlice.dependencies).toEqual([selectOrgsAndPoliciesSlice]);
    });

    it('selects PolicyViolationGrandfathering slice', () => {
      const orgsAndPoliciesSlice = {
        policyViolationGrandfathering: 'policyViolationGrandfathering',
      };

      const selected = selectPolicyViolationGrandfatheringSlice.resultFunc(orgsAndPoliciesSlice);
      expect(selected).toBe('policyViolationGrandfathering');
    });
  });

  describe('immediate slice keys', () => {
    const orgsAndPoliciesSlice = {
      loading: 'loading',
      loadError: 'loadError',
      data: 'data',
    };
    describe('selectLoadError', () => {
      it('is composed from the following selector', () => {
        expect(selectLoadError.dependencies).toEqual([selectPolicyViolationGrandfatheringSlice]);
      });

      it('selects loadError from the selectPolicyViolationGrandfatheringSlice', () => {
        const selected = selectLoadError.resultFunc(orgsAndPoliciesSlice);
        expect(selected).toBe('loadError');
      });
    });

    describe('selectLoading', () => {
      it('is composed from the following selector', () => {
        expect(selectLoading.dependencies).toEqual([selectPolicyViolationGrandfatheringSlice]);
      });

      it('selects loading from the selectPolicyViolationGrandfatheringSlice', () => {
        const selected = selectLoading.resultFunc(orgsAndPoliciesSlice);
        expect(selected).toBe('loading');
      });
    });

    describe('selectPolicyViolationGrandfathering', () => {
      it('is composed from the following selector', () => {
        expect(selectPolicyViolationGrandfathering.dependencies).toEqual([selectPolicyViolationGrandfatheringSlice]);
      });

      it('selects data from the selectPolicyViolationGrandfatheringSlice', () => {
        const selected = selectPolicyViolationGrandfathering.resultFunc(orgsAndPoliciesSlice);
        expect(selected).toBe('data');
      });
    });
  });

  describe('selectCalculatedEnabled', () => {
    it('is composed from the following selector', () => {
      expect(selectCalculatedEnabled.dependencies).toEqual([selectPolicyViolationGrandfatheringConfig]);
    });

    it('selects calculatedEnabled from policyViolationGrandfatheringConfig', () => {
      const policyViolationGrandfatheringConfig = {
        calculatedEnabled: true,
      };

      const selected = selectCalculatedEnabled.resultFunc(policyViolationGrandfatheringConfig);
      expect(selected).toBeTrue();
    });
  });

  describe('selectPolicyViolationGrandfatheringConfig', () => {
    const testsToRun = [
      {
        inheritedFromOrganizationName: null,
        allowOverride: true,
        allowChange: false,
        enabled: true,
        isOrg: false,
        expected: {
          inheritedFromOrganizationName: null,
          allowOverride: true,
          allowChange: false,
          enabled: true,
          calculatedEnabled: true,
        },
      },
      {
        inheritedFromOrganizationName: 'name',
        allowOverride: true,
        allowChange: false,
        enabled: false,
        isOrg: false,
        expected: {
          inheritedFromOrganizationName: 'name',
          allowOverride: true,
          allowChange: false,
          enabled: null,
          calculatedEnabled: false,
        },
      },
      {
        inheritedFromOrganizationName: 'name',
        allowOverride: true,
        allowChange: false,
        enabled: null,
        isOrg: false,
        expected: {
          inheritedFromOrganizationName: 'name',
          allowOverride: true,
          allowChange: false,
          enabled: null,
          calculatedEnabled: null,
        },
      },
      {
        inheritedFromOrganizationName: null,
        allowOverride: true,
        allowChange: false,
        enabled: null,
        isOrg: true,
        expected: {
          inheritedFromOrganizationName: null,
          allowOverride: true,
          allowChange: false,
          enabled: false,
          calculatedEnabled: false,
        },
      },
    ];
    it('is composed from the following selector', () => {
      expect(selectPolicyViolationGrandfatheringConfig.dependencies).toEqual([
        selectPolicyViolationGrandfathering,
        selectIsRootOrganization,
      ]);
    });

    testsToRun.forEach((policyViolationGrandfatheringSlice) => {
      const { inheritedFromOrganizationName, isOrg, expected } = policyViolationGrandfatheringSlice;
      it(`selects PolicyViolationGrandfathering slice with rootOrg = ${isOrg} and inheritedFromOrganizationName = ${inheritedFromOrganizationName}`, () => {
        const selected = selectPolicyViolationGrandfatheringConfig.resultFunc(
          policyViolationGrandfatheringSlice,
          isOrg
        );
        expect(selected).toEqual(expected);
      });
    });
  });

  describe('selectGrandfatheringStatusMessage', () => {
    const testsToRun = [
      {
        inheritedFromOrganizationName: null,
        allowOverride: true,
        allowChange: false,
        enabled: true,
        calculatedEnabled: true,
        expectedMessage: 'Grandfathering is enabled',
      },
      {
        inheritedFromOrganizationName: 'name',
        allowOverride: true,
        allowChange: false,
        enabled: false,
        calculatedEnabled: false,
        expectedMessage: 'Inherit from name (Grandfathering is disabled)',
      },
    ];
    it('is composed from the following selector', () => {
      expect(selectGrandfatheringStatusMessage.dependencies).toEqual([selectPolicyViolationGrandfatheringConfig]);
    });

    testsToRun.forEach((policyViolationGrandfatheringConfig) => {
      const { inheritedFromOrganizationName, isOrg, expectedMessage } = policyViolationGrandfatheringConfig;
      it(`selects PolicyViolationGrandfathering slice with rootOrg = ${isOrg} and inheritedFromOrganizationName = ${inheritedFromOrganizationName}`, () => {
        const selected = selectGrandfatheringStatusMessage.resultFunc(policyViolationGrandfatheringConfig);
        expect(selected).toEqual(expectedMessage);
      });
    });
  });
});
