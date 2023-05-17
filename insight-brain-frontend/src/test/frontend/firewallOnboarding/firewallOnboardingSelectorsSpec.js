/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  selectFirewallOnboardingSlice,
  selectCurrentStep,
  selectSelectedRepositories,
  selectUnconfiguredRepoManagersList,
  selectUnconfiguredRepoManager,
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

    it('selects the curren step from firewall onboarding slice', () => {
      const slice = { currentStep: { id: 'select', name: 'Select' } };

      const actualSelection = selectCurrentStep.resultFunc(slice);

      expect(actualSelection).toBe(slice.currentStep);
    });
  });

  describe('selectSelectedRepositories', () => {
    it('selects the selectedRepositories from firewall onboarding slice', () => {
      const slice = { selectedRepositories: [1, 2, 3, 4] };

      const actualSelection = selectSelectedRepositories.resultFunc(slice);

      expect(actualSelection).toBe(slice.selectedRepositories);
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
  });
});
