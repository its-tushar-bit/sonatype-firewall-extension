/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, { initialConfiguration } from 'MainRoot/configuration/roiConfiguration/roiConfigurationPageSlice';

describe('roiConfigurationPageSlice', () => {
  describe('roiConfigurationPage/loadConfiguration', () => {
    it('/pending', () => {
      const state = Object.freeze({
        loading: true,
        error: null,
        configuration: { ...initialConfiguration },
      });

      const newState = reducer(state, {
        type: 'roiConfigurationPage/loadConfiguration/pending',
      });

      expect(newState.loading).toBe(true);
      expect(newState.error).toBe(null);
    });

    it('/rejected', () => {
      const state = Object.freeze({
        loading: true,
        error: null,
        configuration: { ...initialConfiguration },
      });

      const newState = reducer(state, {
        type: 'roiConfigurationPage/loadConfiguration/rejected',
        payload: 'Something went wrong.',
      });

      expect(newState.loading).toBe(false);
      expect(newState.error).toBe('Something went wrong.');
    });

    it('/fulfilled', () => {
      const state = Object.freeze({
        loading: true,
        error: null,
        configuration: { ...initialConfiguration },
      });

      const newState = reducer(state, {
        type: 'roiConfigurationPage/loadConfiguration/fulfilled',
        payload: {
          baselineDaysToResolveViolationMinimum: 100,
          baselineDaysToResolveViolation: 150,
          dailyRiskCostOfUnfixedViolationMinimum: 1000,
          dailyRiskCostOfUnfixedViolation: 1234.56,
          malwareAttacksPreventedMinimum: 1000,
          malwareAttacksPrevented: 1111.11,
          namespaceAttacksPreventedMinimum: 1000,
          namespaceAttacksPrevented: 2222.22,
          safeComponentsAutoSelectedMinimum: 1000,
          safeComponentsAutoSelected: 3333.33,
        },
      });

      expect(newState.loading).toBe(false);
      expect(newState.error).toBe(null);
      expect(newState.configuration).toEqual({
        baselineDaysToResolveViolation: 150,
        dailyRiskCostOfUnfixedViolation: 1234.56,
        malwareAttacksPrevented: 1111.11,
        namespaceAttacksPrevented: 2222.22,
        safeComponentsAutoSelected: 3333.33,
      });
    });
  });
});
