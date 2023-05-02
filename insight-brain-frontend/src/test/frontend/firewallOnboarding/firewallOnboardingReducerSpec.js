/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reduce from 'MainRoot/firewallOnboarding/firewallOnboardingSlice';
import { steps } from 'MainRoot/firewallOnboarding/firewallOnboardingUtils';

describe('FirewallOnboardingReducer', () => {
  describe('unknown action', () => {
    it('returns original state', () => {
      const state = Object.freeze({ foo: 'bar' });
      const newState = reduce(state, { type: 'UNKNOWN' });

      expect(newState).toBe(state);
    });
  });

  describe('continueToNextStep', () => {
    it('updates currentStep', () => {
      const state = Object.freeze({ currentStep: steps[0] });
      const { currentStep } = reduce(state, { type: 'firewallOnboarding/continueToNextStep' });

      expect(currentStep).toEqual(steps[1]);
    });

    describe('when current step does not have next step', () => {
      it('does not update currentStep', () => {
        const state = Object.freeze({ currentStep: steps[1] });
        const { currentStep } = reduce(state, { type: 'firewallOnboarding/continueToNextStep' });

        expect(currentStep).toEqual(steps[1]);
      });
    });
  });

  describe('goBackToPreviousStep', () => {
    it('updates currentStep', () => {
      const state = Object.freeze({ currentStep: steps[1] });
      const { currentStep } = reduce(state, { type: 'firewallOnboarding/goBackToPreviousStep' });

      expect(currentStep).toEqual(steps[0]);
    });

    describe('when current step does not have previous step', () => {
      it('does not update currentStep', () => {
        const state = Object.freeze({ currentStep: steps[0] });
        const { currentStep } = reduce(state, { type: 'firewallOnboarding/goBackToPreviousStep' });

        expect(currentStep).toEqual(steps[0]);
      });
    });
  });
});
