/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectOrgsAndPoliciesSlice } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import {
  selectLegacyViolationSlice,
  selectLoadError,
  selectLoading,
  selectLegacyViolation,
  selectLegacyViolationConfig,
  selectCalculatedEnabled,
  selectLegacyViolationsStatusMessage,
  selectParentLegacyViolationStatus,
  selectLegacyViolationServerData,
} from 'MainRoot/OrgsAndPolicies/legacyViolationSelectors';
import { selectIsRootOrganization } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectRoot } from '../../../main/frontend/OrgsAndPolicies/legacyViolationSelectors';

describe('legacyViolationSelectors', () => {
  describe('selectLegacyViolationSlice', () => {
    it('is composed from the following selector', () => {
      expect(selectLegacyViolationSlice.dependencies).toEqual([selectOrgsAndPoliciesSlice]);
    });

    it('selects LegacyViolation slice', () => {
      const orgsAndPoliciesSlice = {
        legacyViolations: 'legacyViolation',
      };

      const selected = selectLegacyViolationSlice.resultFunc(orgsAndPoliciesSlice);
      expect(selected).toBe('legacyViolation');
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
        expect(selectLoadError.dependencies).toEqual([selectLegacyViolationSlice]);
      });

      it('selects loadError from the selectLegacyViolationSlice', () => {
        const selected = selectLoadError.resultFunc(orgsAndPoliciesSlice);
        expect(selected).toBe('loadError');
      });
    });

    describe('selectLoading', () => {
      it('is composed from the following selector', () => {
        expect(selectLoading.dependencies).toEqual([selectLegacyViolationSlice]);
      });

      it('selects loading from the selectLegacyViolationSlice', () => {
        const selected = selectLoading.resultFunc(orgsAndPoliciesSlice);
        expect(selected).toBe('loading');
      });
    });

    describe('selectLegacyViolation', () => {
      it('is composed from the following selector', () => {
        expect(selectLegacyViolation.dependencies).toEqual([selectLegacyViolationSlice]);
      });

      it('selects data from the selectLegacyViolationSlice', () => {
        const selected = selectLegacyViolation.resultFunc(orgsAndPoliciesSlice);
        expect(selected).toBe('data');
      });
    });
  });

  describe('selectCalculatedEnabled', () => {
    it('is composed from the following selector', () => {
      expect(selectCalculatedEnabled.dependencies).toEqual([selectLegacyViolationConfig]);
    });

    it('selects calculatedEnabled from legacyViolationConfig', () => {
      const legacyViolationConfig = {
        calculatedEnabled: true,
      };

      const selected = selectCalculatedEnabled.resultFunc(legacyViolationConfig);
      expect(selected).toBe(true);
    });
  });

  describe('selectLegacyViolationConfig', () => {
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
          organizationName: 'myOrg',
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
          organizationName: 'myOrg',
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
          organizationName: 'myOrg',
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
          organizationName: 'myOrg',
        },
      },
    ];
    it('is composed from the following selector', () => {
      expect(selectLegacyViolationConfig.dependencies).toEqual([
        selectLegacyViolation,
        selectIsRootOrganization,
        selectRoot,
      ]);
    });

    testsToRun.forEach((legacyViolationSlice) => {
      const { inheritedFromOrganizationName, isOrg, expected } = legacyViolationSlice;
      it(`selects legacyPolicyViolation slice with rootOrg = ${isOrg} and inheritedFromOrganizationName = ${inheritedFromOrganizationName}`, () => {
        const data = {
          selectedOwner: {
            organizationName: 'myOrg',
          },
        };
        const selected = selectLegacyViolationConfig.resultFunc(legacyViolationSlice, isOrg, data);
        expect(selected).toEqual(expected);
      });
    });
  });

  describe('selectLegacyViolationsStatusMessage', () => {
    const testsToRun = [
      {
        inheritedFromOrganizationName: null,
        allowOverride: true,
        allowChange: false,
        enabled: true,
        calculatedEnabled: true,
        expectedMessage: 'Legacy violations are enabled',
      },
      {
        inheritedFromOrganizationName: 'name',
        allowOverride: true,
        allowChange: false,
        enabled: false,
        calculatedEnabled: false,
        expectedMessage: 'Legacy violations are disabled (Inheriting from name)',
      },
    ];
    it('is composed from the following selector', () => {
      expect(selectLegacyViolationsStatusMessage.dependencies).toEqual([selectLegacyViolationConfig]);
    });

    testsToRun.forEach((legacyViolationConfig) => {
      const { inheritedFromOrganizationName, isOrg, expectedMessage } = legacyViolationConfig;
      it(`selects legacyViolation slice with rootOrg = ${isOrg} and inheritedFromOrganizationName = ${inheritedFromOrganizationName}`, () => {
        const selected = selectLegacyViolationsStatusMessage.resultFunc(legacyViolationConfig);
        expect(selected).toEqual(expectedMessage);
      });
    });
  });

  describe('selectParentLegacyViolationStatus', () => {
    it('is composed from the following selector', () => {
      expect(selectParentLegacyViolationStatus.dependencies).toEqual([selectLegacyViolationServerData]);
    });

    it('returns the string Enabled when enabledInParent is set to true', () => {
      const selectLegacyViolationServerData = { enabledInParent: true };
      const status = selectParentLegacyViolationStatus.resultFunc(selectLegacyViolationServerData);
      expect(status).toBe('Enabled');
    });

    it('returns the string Disabled when enabledInParent is set to false', () => {
      const selectLegacyViolationServerData = { enabledInParent: false };
      const status = selectParentLegacyViolationStatus.resultFunc(selectLegacyViolationServerData);
      expect(status).toBe('Disabled');
    });
  });
});
