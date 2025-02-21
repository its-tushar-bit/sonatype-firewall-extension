/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, {
  instantiateNumericState,
} from 'MainRoot/configuration/editRoiConfiguration/editRoiConfigurationPageSlice';

describe('editRoiConfigurationPageSlice', () => {
  describe('editRoiConfigurationPage/updateConfigurationValue', () => {
    it('should update and validate currency values correctly', () => {
      const state = Object.freeze({
        configuration: {
          dailyRiskCostOfUnfixedViolation: instantiateNumericState(100, 100),
        },
      });

      const state0 = reducer(state, {
        type: 'editRoiConfigurationPage/updateConfigurationValue',
        payload: {
          key: 'dailyRiskCostOfUnfixedViolation',
          value: '',
        },
      });

      expect(state0.configuration.dailyRiskCostOfUnfixedViolation.input.value).toBe('');
      expect(state0.configuration.dailyRiskCostOfUnfixedViolation.input.validationErrors).toBe('Must be non-empty.');

      const state1 = reducer(state, {
        type: 'editRoiConfigurationPage/updateConfigurationValue',
        payload: {
          key: 'dailyRiskCostOfUnfixedViolation',
          value: ' a b c ',
        },
      });

      expect(state1.configuration.dailyRiskCostOfUnfixedViolation.input.value).toBe(' a b c ');
      expect(state1.configuration.dailyRiskCostOfUnfixedViolation.input.validationErrors).toBe(
        'Must be a valid numeric format.'
      );

      const state2 = reducer(state, {
        type: 'editRoiConfigurationPage/updateConfigurationValue',
        payload: {
          key: 'dailyRiskCostOfUnfixedViolation',
          value: '1a123',
        },
      });

      expect(state2.configuration.dailyRiskCostOfUnfixedViolation.input.value).toBe('1a123');
      expect(state2.configuration.dailyRiskCostOfUnfixedViolation.input.validationErrors).toBe(
        'Must be a valid numeric format.'
      );

      const state3 = reducer(state, {
        type: 'editRoiConfigurationPage/updateConfigurationValue',
        payload: {
          key: 'dailyRiskCostOfUnfixedViolation',
          value: '99.99',
        },
      });

      expect(state3.configuration.dailyRiskCostOfUnfixedViolation.input.value).toBe('99.99');
      expect(state3.configuration.dailyRiskCostOfUnfixedViolation.input.validationErrors).toBe(
        'Must be greater than or equal to 100.'
      );

      const state4 = reducer(state, {
        type: 'editRoiConfigurationPage/updateConfigurationValue',
        payload: {
          key: 'dailyRiskCostOfUnfixedViolation',
          value: '1,000,000.99',
        },
      });

      expect(state4.configuration.dailyRiskCostOfUnfixedViolation.input.value).toBe('1,000,000.99');
      expect(state4.configuration.dailyRiskCostOfUnfixedViolation.input.validationErrors).toBe(null);
    });

    it('should update and validate integer values correctly', () => {
      const state = Object.freeze({
        configuration: {
          baselineDaysToResolveViolation: instantiateNumericState(100, 100),
        },
      });

      const state0 = reducer(state, {
        type: 'editRoiConfigurationPage/updateConfigurationValue',
        payload: {
          key: 'baselineDaysToResolveViolation',
          value: '',
        },
      });

      expect(state0.configuration.baselineDaysToResolveViolation.input.value).toBe('');
      expect(state0.configuration.baselineDaysToResolveViolation.input.validationErrors).toBe('Must be non-empty.');

      const state1 = reducer(state, {
        type: 'editRoiConfigurationPage/updateConfigurationValue',
        payload: {
          key: 'baselineDaysToResolveViolation',
          value: ' a b c ',
        },
      });

      expect(state1.configuration.baselineDaysToResolveViolation.input.value).toBe(' a b c ');
      expect(state1.configuration.baselineDaysToResolveViolation.input.validationErrors).toBe(
        'Must be a valid positive integer.'
      );

      const state2 = reducer(state, {
        type: 'editRoiConfigurationPage/updateConfigurationValue',
        payload: {
          key: 'baselineDaysToResolveViolation',
          value: '100.99',
        },
      });

      expect(state2.configuration.baselineDaysToResolveViolation.input.value).toBe('100.99');
      expect(state2.configuration.baselineDaysToResolveViolation.input.validationErrors).toBe(
        'Must be a valid positive integer.'
      );

      const state3 = reducer(state, {
        type: 'editRoiConfigurationPage/updateConfigurationValue',
        payload: {
          key: 'baselineDaysToResolveViolation',
          value: '10',
        },
      });

      expect(state3.configuration.baselineDaysToResolveViolation.input.value).toBe('10');
      expect(state3.configuration.baselineDaysToResolveViolation.input.validationErrors).toBe(
        'Must be greater than or equal to 100.'
      );

      const state4 = reducer(state, {
        type: 'editRoiConfigurationPage/updateConfigurationValue',
        payload: {
          key: 'baselineDaysToResolveViolation',
          value: '1,000,000',
        },
      });

      expect(state4.configuration.baselineDaysToResolveViolation.input.value).toBe('1,000,000');
      expect(state4.configuration.baselineDaysToResolveViolation.input.validationErrors).toBe(null);
    });
  });

  describe('editRoiConfigurationPage/setShowRestoreDefaultsModal', () => {
    it('should set showRestoreDefaultsModal', () => {
      const state = Object.freeze({
        showRestoreDefaultsModal: false,
      });

      const newState = reducer(state, {
        type: 'editRoiConfigurationPage/setShowRestoreDefaultsModal',
        payload: true,
      });

      expect(newState.showRestoreDefaultsModal).toBe(true);
    });
  });

  describe('editRoiConfigurationPage/loadConfiguration', () => {
    it('/pending', () => {
      const state = Object.freeze({
        loading: true,
        error: null,
      });

      const newState = reducer(state, {
        type: 'editRoiConfigurationPage/loadConfiguration/pending',
      });

      expect(newState.loading).toBe(true);
      expect(newState.error).toBe(null);
    });

    it('/rejected', () => {
      const state = Object.freeze({
        loading: true,
        error: null,
      });

      const newState = reducer(state, {
        type: 'editRoiConfigurationPage/loadConfiguration/rejected',
        payload: 'Something went wrong.',
      });

      expect(newState.loading).toBe(false);
      expect(newState.error).toBe('Something went wrong.');
    });

    it('/fulfilled', () => {
      const state = Object.freeze({
        loading: true,
        error: null,
        configuration: {
          baselineDaysToResolveViolation: instantiateNumericState(0, 0),
          dailyRiskCostOfUnfixedViolation: instantiateNumericState(0, 0),
          malwareAttacksPrevented: instantiateNumericState(0, 0),
          namespaceAttacksPrevented: instantiateNumericState(0, 0),
          safeComponentsAutoSelected: instantiateNumericState(0, 0),
        },
      });

      const newState = reducer(state, {
        type: 'editRoiConfigurationPage/loadConfiguration/fulfilled',
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

      expect(newState.configuration.baselineDaysToResolveViolation.input.value).toBe('150');
      expect(newState.configuration.baselineDaysToResolveViolation.minimum).toBe(100);

      expect(newState.configuration.dailyRiskCostOfUnfixedViolation.input.value).toBe('1234.56');
      expect(newState.configuration.dailyRiskCostOfUnfixedViolation.minimum).toBe(1000);

      expect(newState.configuration.malwareAttacksPrevented.input.value).toBe('1111.11');
      expect(newState.configuration.malwareAttacksPrevented.minimum).toBe(1000);

      expect(newState.configuration.namespaceAttacksPrevented.input.value).toBe('2222.22');
      expect(newState.configuration.namespaceAttacksPrevented.minimum).toBe(1000);

      expect(newState.configuration.safeComponentsAutoSelected.input.value).toBe('3333.33');
      expect(newState.configuration.safeComponentsAutoSelected.minimum).toBe(1000);
    });
  });

  describe('editRoiConfigurationPage/updateConfiguration', () => {
    it('/pending', () => {
      const state = Object.freeze({
        loading: true,
        error: null,
      });

      const newState = reducer(state, {
        type: 'editRoiConfigurationPage/updateConfiguration/pending',
      });

      expect(newState.loading).toBe(true);
      expect(newState.error).toBe(null);
    });

    it('/rejected', () => {
      const state = Object.freeze({
        loading: true,
        error: null,
      });

      const newState = reducer(state, {
        type: 'editRoiConfigurationPage/updateConfiguration/rejected',
        payload: 'Something went wrong.',
      });

      expect(newState.loading).toBe(false);
      expect(newState.error).toBe('Something went wrong.');
    });

    it('/fulfilled', () => {
      const state = Object.freeze({
        loading: true,
        error: null,
        configuration: {
          baselineDaysToResolveViolation: instantiateNumericState(0, 0),
          dailyRiskCostOfUnfixedViolation: instantiateNumericState(0, 0),
          malwareAttacksPrevented: instantiateNumericState(0, 0),
          namespaceAttacksPrevented: instantiateNumericState(0, 0),
          safeComponentsAutoSelected: instantiateNumericState(0, 0),
        },
      });

      const newState = reducer(state, {
        type: 'editRoiConfigurationPage/updateConfiguration/fulfilled',
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

      expect(newState.configuration.baselineDaysToResolveViolation.input.value).toBe('150');
      expect(newState.configuration.baselineDaysToResolveViolation.minimum).toBe(100);

      expect(newState.configuration.dailyRiskCostOfUnfixedViolation.input.value).toBe('1234.56');
      expect(newState.configuration.dailyRiskCostOfUnfixedViolation.minimum).toBe(1000);

      expect(newState.configuration.malwareAttacksPrevented.input.value).toBe('1111.11');
      expect(newState.configuration.malwareAttacksPrevented.minimum).toBe(1000);

      expect(newState.configuration.namespaceAttacksPrevented.input.value).toBe('2222.22');
      expect(newState.configuration.namespaceAttacksPrevented.minimum).toBe(1000);

      expect(newState.configuration.safeComponentsAutoSelected.input.value).toBe('3333.33');
      expect(newState.configuration.safeComponentsAutoSelected.minimum).toBe(1000);
    });
  });

  describe('editRoiConfigurationPage/restoreDefaults', () => {
    it('/pending', () => {
      const state = Object.freeze({
        loading: true,
        error: null,
      });

      const newState = reducer(state, {
        type: 'editRoiConfigurationPage/restoreDefaults/pending',
      });

      expect(newState.loading).toBe(true);
      expect(newState.error).toBe(null);
    });

    it('/rejected', () => {
      const state = Object.freeze({
        loading: true,
        error: null,
      });

      const newState = reducer(state, {
        type: 'editRoiConfigurationPage/restoreDefaults/rejected',
        payload: 'Something went wrong.',
      });

      expect(newState.loading).toBe(false);
      expect(newState.error).toBe('Something went wrong.');
    });

    it('/fulfilled', () => {
      const state = Object.freeze({
        loading: true,
        error: null,
        configuration: {
          baselineDaysToResolveViolation: instantiateNumericState(0, 0),
          dailyRiskCostOfUnfixedViolation: instantiateNumericState(0, 0),
          malwareAttacksPrevented: instantiateNumericState(0, 0),
          namespaceAttacksPrevented: instantiateNumericState(0, 0),
          safeComponentsAutoSelected: instantiateNumericState(0, 0),
        },
      });

      const newState = reducer(state, {
        type: 'editRoiConfigurationPage/restoreDefaults/fulfilled',
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

      expect(newState.configuration.baselineDaysToResolveViolation.input.value).toBe('150');
      expect(newState.configuration.baselineDaysToResolveViolation.minimum).toBe(100);

      expect(newState.configuration.dailyRiskCostOfUnfixedViolation.input.value).toBe('1234.56');
      expect(newState.configuration.dailyRiskCostOfUnfixedViolation.minimum).toBe(1000);

      expect(newState.configuration.malwareAttacksPrevented.input.value).toBe('1111.11');
      expect(newState.configuration.malwareAttacksPrevented.minimum).toBe(1000);

      expect(newState.configuration.namespaceAttacksPrevented.input.value).toBe('2222.22');
      expect(newState.configuration.namespaceAttacksPrevented.minimum).toBe(1000);

      expect(newState.configuration.safeComponentsAutoSelected.input.value).toBe('3333.33');
      expect(newState.configuration.safeComponentsAutoSelected.minimum).toBe(1000);
    });
  });
});
