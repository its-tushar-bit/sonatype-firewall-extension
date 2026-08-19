/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  selectFirewallOnboardingSlice,
  selectCurrentStep,
  selectShowWelcomeScreen,
  selectUnconfiguredRepoManagersList,
  selectUnconfiguredRepoManager,
  selectRepositories,
  selectRepositoriesList,
  selectSupportedFormats,
  selectRepositoriesByType,
  selectTotalEnabledRepositoriesByTypeAndProp,
  selectProtectionRules,
} from 'MainRoot/firewallOnboarding/firewallOnboardingSelectors';

describe('FirewallOnboardingSelectors', () => {
  describe('selectFirewallOnboardingSlice', () => {
    it('selects the firewall onboarding slice of the state', () => {
      const firewallOnboarding = { currentStep: null };
      const state = { firewallOnboarding };

      const actualSelection = selectFirewallOnboardingSlice(state);

      expect(actualSelection).toBe(firewallOnboarding);
    });
  });

  describe('selectCurrentStep', () => {
    it('is composed from the following selector', () => {
      expect(selectCurrentStep.dependencies).toEqual([selectFirewallOnboardingSlice]);
    });

    it('selects the current step from firewall onboarding slice', () => {
      const slice = { currentStep: { id: 'select', name: 'Select' } };

      const actualSelection = selectCurrentStep.resultFunc(slice);

      expect(actualSelection).toBe(slice.currentStep);
    });
  });

  describe('selectShowWelcomeScreen', () => {
    it('selects showWelcomeScreen from firewall onboarding slice', () => {
      const slice = { showWelcomeScreen: true };

      const actualSelection = selectShowWelcomeScreen.resultFunc(slice);

      expect(actualSelection).toBe(slice.showWelcomeScreen);
    });
  });

  describe('selectUnconfiguredRepoManagersList', () => {
    it('selects the unconfiguredRepoManagers from firewall onboarding slice', () => {
      const slice = { unconfiguredRepoManagers: { repoManagers: [], loading: false, loadError: null } };

      const actualSelection = selectUnconfiguredRepoManagersList.resultFunc(slice);

      expect(actualSelection).toBe(slice.unconfiguredRepoManagers);
    });
  });

  describe('selectUnconfiguredRepoManager', () => {
    it('selects the first unconfigured repositories manager from firewall onboarding slice', () => {
      const slice = {
        repoManagers: [
          {
            id: 'id1',
            instanceId: 'instanceId1',
            userAgent: 'Nexus/3.44.0-SNAPSHOT (OSS; Mac OS X; 10.16; x86_64; 1.8.0_322)',
            configured: false,
            configureTime: null,
          },
          {
            id: 'id2',
            instanceId: 'instanceId2',
            userAgent: 'MyRepoManager/3.44.0-SNAPSHOT',
            configured: false,
            configureTime: null,
          },
        ],
        loading: false,
        loadError: null,
      };

      const actualSelection = selectUnconfiguredRepoManager.resultFunc(slice);

      expect(actualSelection).toBe(slice.repoManagers[0]);
    });

    it('selects null when no unconfigured repositories managers present', () => {
      const slice = {
        repoManagers: undefined,
        loading: false,
        loadError: null,
      };

      const actualSelection = selectUnconfiguredRepoManager.resultFunc(slice);

      expect(actualSelection).toBe(null);
    });
  });

  describe('selectRepositories', () => {
    it('selects the repositories from firewall onboarding slice', () => {
      const slice = {
        repositories: {
          loading: false,
          loadError: null,
          list: [
            {
              id: 'id',
              repositoryManagerId: 'repoManagerId',
              publicId: 'publicId',
              repositoryType: 'proxy',
              auditEnabled: true,
              quarantineEnabled: true,
              policyCompliantComponentSelectionEnabled: false,
              namespaceConfusionProtectionEnabled: false,
              format: 'maven',
            },
          ],
        },
      };

      const actualSelection = selectRepositories.resultFunc(slice);

      expect(actualSelection).toBe(slice.repositories);
    });
  });

  describe('selectRepositoriesList', () => {
    it('selects the repositories list from firewall onboarding slice', () => {
      const slice = {
        repositories: {
          loading: false,
          loadError: null,
          list: [
            {
              id: 'id',
              repositoryManagerId: 'repoManagerId',
              publicId: 'publicId',
              repositoryType: 'proxy',
              auditEnabled: true,
              quarantineEnabled: true,
              policyCompliantComponentSelectionEnabled: false,
              namespaceConfusionProtectionEnabled: false,
              format: 'maven',
            },
          ],
        },
      };

      const actualSelection = selectRepositoriesList.resultFunc(slice.repositories);

      expect(actualSelection).toBe(slice.repositories.list);
    });
  });

  describe('selectSupportedFormats', () => {
    it('selects the supported format list from firewall onboarding slice', () => {
      const slice = {
        supportedFormats: ['a', 'b', 'c'],
      };

      const actualSelection = selectSupportedFormats.resultFunc(slice);

      expect(actualSelection).toEqual(slice.supportedFormats);
    });
  });

  describe('selectTotalEnabledRepositoriesByTypeAndProp', () => {
    it('should return the correct count of enabled repositories by type and prop', () => {
      const repositories = [
        { id: '1', repositoryType: 'type1', format: 'npm', quarantineEnabled: true },
        { id: '2', repositoryType: 'type1', format: 'npm', quarantineEnabled: false },
        { id: '3', repositoryType: 'type2', format: 'npm', quarantineEnabled: true },
        { id: '4', repositoryType: 'type2', format: 'npm', quarantineEnabled: true },
        { id: '5', repositoryType: 'type2', format: 'npm', quarantineEnabled: false },
        { id: '5', repositoryType: 'type2', format: 'unsupportedFormat', quarantineEnabled: true },
      ];
      const supportedFormats = ['npm'];
      const type = 'type1';
      const propName = 'quarantineEnabled';

      const result = selectTotalEnabledRepositoriesByTypeAndProp.resultFunc(
        repositories,
        supportedFormats,
        type,
        propName
      );

      expect(result).toBe(1);
    });

    it('should return 0 if no repositories match the type and prop', () => {
      const repositories = [
        { id: '1', repositoryType: 'type1', format: 'npm', quarantineEnabled: false },
        { id: '2', repositoryType: 'type1', format: 'npm', quarantineEnabled: false },
        { id: '3', repositoryType: 'type2', format: 'npm', quarantineEnabled: false },
        { id: '4', repositoryType: 'type2', format: 'npm', quarantineEnabled: false },
      ];
      const supportedFormats = ['npm'];

      const type = 'type3';
      const propName = 'quarantineEnabled';

      const result = selectTotalEnabledRepositoriesByTypeAndProp.resultFunc(
        repositories,
        supportedFormats,
        type,
        propName
      );

      expect(result).toBe(0);
    });

    it('should default to propName "quarantineEnabled" if not provided', () => {
      const repositories = [
        { id: '1', repositoryType: 'type1', format: 'npm', quarantineEnabled: true },
        { id: '2', repositoryType: 'type1', format: 'npm', quarantineEnabled: true },
        { id: '3', repositoryType: 'type2', format: 'npm', quarantineEnabled: false },
        { id: '4', repositoryType: 'type2', format: 'npm', quarantineEnabled: false },
      ];
      const supportedFormats = ['npm'];
      const type = 'type1';

      const result = selectTotalEnabledRepositoriesByTypeAndProp.resultFunc(repositories, supportedFormats, type);

      expect(result).toBe(2);
    });
  });

  describe('selectRepositoriesByType', () => {
    it('should return the repositories of the specified type', () => {
      const repositories = [
        { id: '1', repositoryType: 'proxy', format: 'format1' },
        { id: '2', repositoryType: 'proxy', format: 'format2' },
        { id: '3', repositoryType: 'proxy', format: 'format3' },
        { id: '4', repositoryType: 'proxy', format: 'format1' },
        { id: '5', repositoryType: 'type1', format: 'format2' },
      ];
      const formats = ['format1', 'format2', 'format3'];
      const repositoryType = 'proxy';

      const result = selectRepositoriesByType.resultFunc(repositories, formats, repositoryType);

      const expectedRepositories = [
        {
          format: 'format1',
          repositories: [
            { id: '1', repositoryType: 'proxy', format: 'format1' },
            { id: '4', repositoryType: 'proxy', format: 'format1' },
          ],
        },
        {
          format: 'format2',
          repositories: [{ id: '2', repositoryType: 'proxy', format: 'format2' }],
        },
        {
          format: 'format3',
          repositories: [{ id: '3', repositoryType: 'proxy', format: 'format3' }],
        },
      ];

      expect(result).toEqual(expectedRepositories);
    });

    it('should return an empty array if no repositories match the type', () => {
      const repositories = [
        { id: '1', repositoryType: 'proxy', format: 'format1' },
        { id: '2', repositoryType: 'proxy', format: 'format2' },
        { id: '3', repositoryType: 'proxy', format: 'format3' },
      ];
      const formats = ['format1', 'format2', 'format3'];
      const repositoryType = 'mytype';

      const result = selectRepositoriesByType.resultFunc(repositories, formats, repositoryType);

      expect(result).toEqual([]);
    });
  });

  describe('selectProtectionRules', () => {
    it('is composed from the following selector', () => {
      expect(selectProtectionRules.dependencies).toEqual([selectFirewallOnboardingSlice]);
    });

    it('selects protection rules from firewall onboarding slice', () => {
      const slice = {
        protectionRules: {
          supplyChainAttacksProtectionEnabled: false,
          namespaceConfusionProtectionEnabled: false,
          configuring: false,
          configureError: null,
        },
      };

      const actualSelection = selectProtectionRules.resultFunc(slice);

      expect(actualSelection).toEqual({
        supplyChainAttacksProtectionEnabled: false,
        namespaceConfusionProtectionEnabled: false,
      });
    });
  });
});
