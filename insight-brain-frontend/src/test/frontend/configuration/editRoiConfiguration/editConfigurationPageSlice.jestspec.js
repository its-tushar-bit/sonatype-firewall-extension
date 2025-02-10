/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, {
  generateDefaultNumericState,
} from 'MainRoot/configuration/editRoiConfiguration/editRoiConfigurationPageSlice';

describe('editRoiConfigurationPageSlice', () => {
  describe('editRoiConfigurationPage/updateConfigurationValue', () => {
    it('should not allow update when is not enabled', () => {
      const state = Object.freeze({
        configuration: {
          developerHourlyRate: generateDefaultNumericState(false, 100, 100),
        },
      });

      const state0 = reducer(state, {
        type: 'editRoiConfigurationPage/updateConfigurationValue',
        payload: {
          key: 'developerHourlyRate',
          value: '999',
        },
      });

      expect(state0.configuration.developerHourlyRate.input.value).toBe('100');
    });

    it('should update and validate currency values correctly', () => {
      const state = Object.freeze({
        configuration: {
          developerHourlyRate: generateDefaultNumericState(true, 100, 100),
        },
      });

      const state0 = reducer(state, {
        type: 'editRoiConfigurationPage/updateConfigurationValue',
        payload: {
          key: 'developerHourlyRate',
          value: '',
        },
      });

      expect(state0.configuration.developerHourlyRate.input.value).toBe('');
      expect(state0.configuration.developerHourlyRate.input.validationErrors).toBe('Must be non-empty.');

      const state1 = reducer(state, {
        type: 'editRoiConfigurationPage/updateConfigurationValue',
        payload: {
          key: 'developerHourlyRate',
          value: ' a b c ',
        },
      });

      expect(state1.configuration.developerHourlyRate.input.value).toBe(' a b c ');
      expect(state1.configuration.developerHourlyRate.input.validationErrors).toBe('Must be a valid numeric format.');

      const state2 = reducer(state, {
        type: 'editRoiConfigurationPage/updateConfigurationValue',
        payload: {
          key: 'developerHourlyRate',
          value: '1a123',
        },
      });

      expect(state2.configuration.developerHourlyRate.input.value).toBe('1a123');
      expect(state2.configuration.developerHourlyRate.input.validationErrors).toBe('Must be a valid numeric format.');

      const state3 = reducer(state, {
        type: 'editRoiConfigurationPage/updateConfigurationValue',
        payload: {
          key: 'developerHourlyRate',
          value: '99.99',
        },
      });

      expect(state3.configuration.developerHourlyRate.input.value).toBe('99.99');
      expect(state3.configuration.developerHourlyRate.input.validationErrors).toBe(
        'Must be greater than or equal to 100.'
      );

      const state4 = reducer(state, {
        type: 'editRoiConfigurationPage/updateConfigurationValue',
        payload: {
          key: 'developerHourlyRate',
          value: '1,000,000.99',
        },
      });

      expect(state4.configuration.developerHourlyRate.input.value).toBe('1,000,000.99');
      expect(state4.configuration.developerHourlyRate.input.validationErrors).toBe(null);
    });

    it('should update and validate integer values correctly', () => {
      const state = Object.freeze({
        configuration: {
          fixRate: generateDefaultNumericState(true, 100, 100),
        },
      });

      const state0 = reducer(state, {
        type: 'editRoiConfigurationPage/updateConfigurationValue',
        payload: {
          key: 'fixRate',
          value: '',
        },
      });

      expect(state0.configuration.fixRate.input.value).toBe('');
      expect(state0.configuration.fixRate.input.validationErrors).toBe('Must be non-empty.');

      const state1 = reducer(state, {
        type: 'editRoiConfigurationPage/updateConfigurationValue',
        payload: {
          key: 'fixRate',
          value: ' a b c ',
        },
      });

      expect(state1.configuration.fixRate.input.value).toBe(' a b c ');
      expect(state1.configuration.fixRate.input.validationErrors).toBe('Must be a valid positive integer.');

      const state2 = reducer(state, {
        type: 'editRoiConfigurationPage/updateConfigurationValue',
        payload: {
          key: 'fixRate',
          value: '100.99',
        },
      });

      expect(state2.configuration.fixRate.input.value).toBe('100.99');
      expect(state2.configuration.fixRate.input.validationErrors).toBe('Must be a valid positive integer.');

      const state3 = reducer(state, {
        type: 'editRoiConfigurationPage/updateConfigurationValue',
        payload: {
          key: 'fixRate',
          value: '10',
        },
      });

      expect(state3.configuration.fixRate.input.value).toBe('10');
      expect(state3.configuration.fixRate.input.validationErrors).toBe('Must be greater than or equal to 100.');

      const state4 = reducer(state, {
        type: 'editRoiConfigurationPage/updateConfigurationValue',
        payload: {
          key: 'fixRate',
          value: '1,000,000',
        },
      });

      expect(state4.configuration.fixRate.input.value).toBe('1,000,000');
      expect(state4.configuration.fixRate.input.validationErrors).toBe(null);
    });
  });

  describe('editRoiConfigurationPage/toggleConfigurationBooleanValue', () => {
    it('should toggle configuration boolean value correctly', () => {
      const state = Object.freeze({
        configuration: {
          waivedViolations: false,
        },
      });

      const state0 = reducer(state, {
        type: 'editRoiConfigurationPage/toggleConfigurationBooleanValue',
        payload: {
          key: 'waivedViolations',
        },
      });

      expect(state0.configuration.waivedViolations).toBe(true);
    });
  });

  describe('editRoiConfigurationPage/toggleSecurityViolationEnabled', () => {
    it('should toggle security violation enabled boolean value correctly', () => {
      const state = Object.freeze({
        configuration: {
          securityViolation: {
            high: generateDefaultNumericState(false, 0, 0),
          },
        },
      });

      const state0 = reducer(state, {
        type: 'editRoiConfigurationPage/toggleSecurityViolationEnabled',
        payload: {
          key: 'high',
        },
      });

      expect(state0.configuration.securityViolation.high.enabled).toBe(true);
    });
  });

  describe('editRoiConfigurationPage/updateSecurityViolationValue', () => {
    it('should not allow update when is not enabled', () => {
      const state = Object.freeze({
        configuration: {
          securityViolation: {
            critical: generateDefaultNumericState(false, 100, 100),
          },
        },
      });

      const state0 = reducer(state, {
        type: 'editRoiConfigurationPage/updateSecurityViolationValue',
        payload: {
          key: 'critical',
          value: '999',
        },
      });

      expect(state0.configuration.securityViolation.critical.input.value).toBe('100');
    });

    it('should update and validate currency values correctly', () => {
      const state = Object.freeze({
        configuration: {
          securityViolation: {
            critical: generateDefaultNumericState(true, 100, 100),
          },
        },
      });

      const state0 = reducer(state, {
        type: 'editRoiConfigurationPage/updateSecurityViolationValue',
        payload: {
          key: 'critical',
          value: '',
        },
      });

      expect(state0.configuration.securityViolation.critical.input.value).toBe('');
      expect(state0.configuration.securityViolation.critical.input.validationErrors).toBe('Must be non-empty.');

      const state1 = reducer(state, {
        type: 'editRoiConfigurationPage/updateSecurityViolationValue',
        payload: {
          key: 'critical',
          value: ' a b c ',
        },
      });

      expect(state1.configuration.securityViolation.critical.input.value).toBe(' a b c ');
      expect(state1.configuration.securityViolation.critical.input.validationErrors).toBe(
        'Must be a valid numeric format.'
      );

      const state2 = reducer(state, {
        type: 'editRoiConfigurationPage/updateSecurityViolationValue',
        payload: {
          key: 'critical',
          value: '99.99',
        },
      });

      expect(state2.configuration.securityViolation.critical.input.value).toBe('99.99');
      expect(state2.configuration.securityViolation.critical.input.validationErrors).toBe(
        'Must be greater than or equal to 100.'
      );

      const state3 = reducer(state, {
        type: 'editRoiConfigurationPage/updateSecurityViolationValue',
        payload: {
          key: 'critical',
          value: '1,000,000.99',
        },
      });

      expect(state3.configuration.securityViolation.critical.input.value).toBe('1,000,000.99');
      expect(state3.configuration.securityViolation.critical.input.validationErrors).toBe(null);
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
      });

      const newState = reducer(state, {
        type: 'editRoiConfigurationPage/loadConfiguration/fulfilled',
        payload: {},
      });

      expect(newState.loading).toBe(false);
      expect(newState.error).toBe(null);
      // TODO: test mapping of payload to configuration state.
    });
  });
});
