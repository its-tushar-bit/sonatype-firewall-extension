/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  selectFirewallOnboardingSlice,
  selectCurrentStep,
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
});
