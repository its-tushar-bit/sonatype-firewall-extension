/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reduce, { REDUCER_NAME } from 'MainRoot/firewallOnboarding/firewallOnboardingSlice';
import { steps } from 'MainRoot/firewallOnboarding/firewallOnboardingUtils';

const LOAD_UNCONFIGURED_REPOMANAGER_REQUESTED = `${REDUCER_NAME}/loadUnconfiguredRepoManagers/pending`;
const LOAD_UNCONFIGURED_REPOMANAGER_FULFILLED = `${REDUCER_NAME}/loadUnconfiguredRepoManagers/fulfilled`;
const LOAD_UNCONFIGURED_REPOMANAGER_FAILED = `${REDUCER_NAME}/loadUnconfiguredRepoManagers/rejected`;

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

  describe(LOAD_UNCONFIGURED_REPOMANAGER_REQUESTED, () => {
    it('sets loading to true and clears error', () => {
      const state = Object.freeze({
        unconfiguredRepoManagers: {
          repoManagers: [],
          loading: false,
          loadError: 'error',
        },
      });

      const newState = reduce(state, {
        type: LOAD_UNCONFIGURED_REPOMANAGER_REQUESTED,
      });

      expect(newState.unconfiguredRepoManagers.loading).toBe(true);
      expect(newState.unconfiguredRepoManagers.loadError).toBe(null);
    });
  });

  describe(LOAD_UNCONFIGURED_REPOMANAGER_FAILED, () => {
    it('clears loading state and sets a load error', () => {
      const state = Object.freeze({
        unconfiguredRepoManagers: {
          repoManagers: [],
          loading: true,
          loadError: null,
        },
      });

      const newState = reduce(state, {
        type: LOAD_UNCONFIGURED_REPOMANAGER_FAILED,
        payload: 'test error',
      });

      expect(newState.unconfiguredRepoManagers.loading).toBe(false);
      expect(newState.unconfiguredRepoManagers.loadError).toBe('test error');
    });
  });

  describe(LOAD_UNCONFIGURED_REPOMANAGER_FULFILLED, () => {
    it('sets loading to false, clears load error, and sets unconfigured repo managers repoManagers', () => {
      const state = Object.freeze({
        unconfiguredRepoManagers: {
          repoManagers: [],
          loading: true,
          loadError: 'previous error',
        },
      });

      const repoManagers = [1, 2, 3];

      const newState = reduce(state, {
        type: LOAD_UNCONFIGURED_REPOMANAGER_FULFILLED,
        payload: repoManagers,
      });

      expect(newState.unconfiguredRepoManagers.loading).toBe(false);
      expect(newState.unconfiguredRepoManagers.loadError).toBe(null);
      expect(newState.unconfiguredRepoManagers.repoManagers).toBe(repoManagers);
    });
  });
});
