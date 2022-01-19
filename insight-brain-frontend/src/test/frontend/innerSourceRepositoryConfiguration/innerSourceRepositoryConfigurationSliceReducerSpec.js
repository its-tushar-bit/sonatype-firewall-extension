/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, {
  SUBMIT_MASK_DELETING_CONFIGURATION_MESSAGE,
  SUBMIT_MASK_SAVING_CONFIGURATION_MESSAGE,
  SUBMIT_MASK_TESTING_CONFIGURATION_MESSAGE,
} from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryConfigurationSlice';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import {
  getInitialState,
  getMinimalValidFormState,
  getPayload,
} from 'TestRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryConfigurationTestData';

describe('innerSourceRepositoryConfigurationSliceReducer', () => {
  describe('initial state', () => {
    it('returns the initial state given an undefined state', function () {
      const state = undefined;

      const newState = reducer(state, {});

      expect(newState).toEqual(getInitialState());
    });
  });

  describe('unknown action', () => {
    it('returns original state', function () {
      const state = Object.freeze({ foo: 'bar' });
      const action = {
        type: 'UNKNOWN',
      };

      const newState = reducer(state, action);

      expect(newState).toBe(state);
    });
  });

  describe('innerSourceRepositoryConfiguration/setFormat action', () => {
    it('sets the `format` to the payload and updates computed props', () => {
      const state = Object.freeze({
        ...getInitialState(),
        saveConfigurationError: 'someError',
        testConfigurationSuccessful: true,
        testConfigurationError: 'someError',
      });

      const newState = reducer(state, {
        type: 'innerSourceRepositoryConfiguration/setFormat',
        payload: 'maven',
      });

      expect(newState.formState.format).toBe('maven');
      expect(newState.saveConfigurationError).toBeNull();
      expect(newState.testConfigurationSuccessful).toBeFalsy();
      expect(newState.testConfigurationError).toBeNull();
    });
  });

  describe('innerSourceRepositoryConfiguration/setBaseUrl action', () => {
    it('sets the `baseUrl` to the payload and updates computed props', () => {
      const state = Object.freeze({
        ...getInitialState(),
        saveConfigurationError: 'someError',
        testConfigurationSuccessful: true,
        testConfigurationError: 'someError',
      });

      const newState = reducer(state, {
        type: 'innerSourceRepositoryConfiguration/setBaseUrl',
        payload: 'someBaseUrl',
      });

      expect(newState.formState.baseUrlState).toEqual({
        value: 'someBaseUrl',
        trimmedValue: 'someBaseUrl',
        isPristine: false,
        validationErrors: null,
      });
      expect(newState.saveConfigurationError).toBeNull();
      expect(newState.testConfigurationSuccessful).toBeFalsy();
      expect(newState.testConfigurationError).toBeNull();
    });
  });

  describe('innerSourceRepositoryConfiguration/setAnonymous action', () => {
    it('sets `isAnonymous` to the payload and updates computed props', () => {
      const state = Object.freeze({
        ...getInitialState(),
        saveConfigurationError: 'someError',
        testConfigurationSuccessful: true,
        testConfigurationError: 'someError',
      });

      const newState = reducer(state, {
        type: 'innerSourceRepositoryConfiguration/setAnonymous',
        payload: false,
      });

      expect(newState.isAnonymous).toBeFalsy();
    });
  });

  describe('innerSourceRepositoryConfiguration/setUsername action', () => {
    it('sets the `username` to the payload and updates computed props', () => {
      const state = {
        ...getInitialState(),
        formState: {
          ...getMinimalValidFormState(),
          isAnonymous: false,
          saveConfigurationError: 'someError',
          testConfigurationSuccessful: true,
          testConfigurationError: 'someError',
        },
      };

      const newState = reducer(state, {
        type: 'innerSourceRepositoryConfiguration/setUsername',
        payload: 'someUsername',
      });

      expect(newState.formState.usernameState).toEqual({
        value: 'someUsername',
        trimmedValue: 'someUsername',
        isPristine: false,
        validationErrors: null,
      });
      expect(newState.saveConfigurationError).toBeNull();
      expect(newState.testConfigurationSuccessful).toBeFalsy();
      expect(newState.testConfigurationError).toBeNull();
    });
  });

  describe('innerSourceRepositoryConfiguration/setPassword action', () => {
    it('sets the `password` to the payload and updates computed props', () => {
      const state = {
        ...getInitialState(),
        formState: { ...getMinimalValidFormState(), isAnonymous: false },
        saveConfigurationError: 'someError',
        testConfigurationSuccessful: true,
        testConfigurationError: 'someError',
      };

      const newState = reducer(state, {
        type: 'innerSourceRepositoryConfiguration/setPassword',
        payload: 'somePassword',
      });

      expect(newState.formState.passwordState).toEqual({
        value: 'somePassword',
        trimmedValue: 'somePassword',
        isPristine: false,
        validationErrors: null,
      });
      expect(newState.saveConfigurationError).toBeNull();
      expect(newState.testConfigurationSuccessful).toBeFalsy();
      expect(newState.testConfigurationError).toBeNull();
    });
  });

  describe('innerSourceRepositoryConfiguration/cancel action', () => {
    it('reverts to the initial state if there is no server data', () => {
      const state = {
        formState: {
          format: 'maven',
          baseUrlState: nxTextInputStateHelpers.initialState('someBaseUrl'),
          isAnonymous: false,
          usernameState: nxTextInputStateHelpers.initialState('someUsername'),
          passwordState: nxTextInputStateHelpers.initialState('somePassword'),
        },
        saveConfigurationError: 'someError',
        testConfigurationSuccessful: true,
        testConfigurationError: 'someError',
      };

      const newState = reducer(state, {
        type: 'innerSourceRepositoryConfiguration/cancel',
      });

      expect(newState.formState).toEqual(getInitialState().formState);
      expect(newState.saveConfigurationError).toBeNull();
      expect(newState.testConfigurationSuccessful).toBeFalsy();
      expect(newState.testConfigurationError).toBeNull();
    });

    it('reverts to the server data if it exists', () => {
      const state = {
        serverData: {
          format: 'npm',
          baseUrl: 'someOtherBaseUrl',
        },
        formState: {
          format: 'maven',
          baseUrlState: nxTextInputStateHelpers.initialState('someBaseUrl'),
          isAnonymous: false,
          usernameState: nxTextInputStateHelpers.initialState('someUsername'),
          passwordState: nxTextInputStateHelpers.initialState('somePassword'),
        },
      };

      const newState = reducer(state, {
        type: 'innerSourceRepositoryConfiguration/cancel',
      });

      expect(newState.formState).toEqual({
        format: 'npm',
        baseUrlState: nxTextInputStateHelpers.initialState('someOtherBaseUrl'),
        isAnonymous: true,
        usernameState: nxTextInputStateHelpers.initialState(''),
        passwordState: nxTextInputStateHelpers.initialState(''),
      });
    });
  });

  describe('innerSourceRepositoryConfiguration/submitMaskTimerDone action', () => {
    it('sets the `submitMaskState` to `null` and `deleteSubmitMaskState` to `null`', () => {
      const state = {
        submitMaskState: true,
        deleteSubmitMaskState: true,
      };

      const newState = reducer(state, {
        type: 'innerSourceRepositoryConfiguration/submitMaskTimerDone',
      });

      expect(newState.submitMaskState).toBeNull();
      expect(newState.deleteSubmitMaskState).toBeNull();
    });
  });

  describe('innerSourceRepositoryConfiguration/setShowDeleteModal', () => {
    it('sets `showDeleteModal` to the payload and `deleteConfigurationError` to `null`', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'innerSourceRepositoryConfiguration/setShowDeleteModal',
        payload: true,
      });

      expect(newState.showDeleteModal).toBeTruthy();
      expect(newState.deleteConfigurationError).toBeNull();
    });
  });

  describe('innerSourceRepositoryConfiguration/loadConfiguration/pending action', () => {
    it('sets the initial state with `loading` to true and `loadConfigurationError` to null', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'innerSourceRepositoryConfiguration/loadConfiguration/pending',
      });

      expect(newState).toEqual({ ...getInitialState(), loading: true });
    });
  });

  describe('innerSourceRepositoryConfiguration/loadConfiguration/fulfilled action', () => {
    it('sets `loading` to false, `serverData` to the payload, and `formState` to represent the payload', () => {
      const state = {};
      const payload = getPayload(false);

      const newState = reducer(state, {
        type: 'innerSourceRepositoryConfiguration/loadConfiguration/fulfilled',
        payload: payload,
      });

      expect(newState.loading).toBeFalsy();
      expect(newState.serverData).toEqual(payload);
      expect(newState.formState).toEqual({
        format: payload.format,
        baseUrlState: nxTextInputStateHelpers.initialState(payload.baseUrl),
        isAnonymous: payload.isAnonymous,
        usernameState: nxTextInputStateHelpers.initialState(payload.username),
        passwordState: nxTextInputStateHelpers.initialState(payload.password),
      });
    });
  });

  describe('innerSourceRepositoryConfiguration/loadConfiguration/rejected action', () => {
    it('sets `loading` to false and `loadConfigurationError` to the payload http error message', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'innerSourceRepositoryConfiguration/loadConfiguration/rejected',
        payload: 'someError',
      });

      expect(newState.loading).toBeFalsy();
      expect(newState.loadConfigurationError).toBe('someError');
    });
  });

  describe('innerSourceRepositoryConfiguration/saveConfiguration/pending action', () => {
    it('sets `submitMaskState` to false, the `submitMaskMessage`, and `saveConfigurationError` to null', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'innerSourceRepositoryConfiguration/saveConfiguration/pending',
      });

      expect(newState.submitMaskState).toBeFalsy();
      expect(newState.submitMaskMessage).toBe(SUBMIT_MASK_SAVING_CONFIGURATION_MESSAGE);
      expect(newState.saveConfigurationError).toBeNull();
    });
  });

  describe('innerSourceRepositoryConfiguration/saveConfiguration/fulfilled action', () => {
    it('sets `submitMaskState` to true and the `serverData` to the payload', () => {
      const state = {
        formState: getInitialState().formState,
      };
      const payload = getPayload(false);

      const newState = reducer(state, {
        type: 'innerSourceRepositoryConfiguration/saveConfiguration/fulfilled',
        payload: payload,
      });

      expect(newState.submitMaskState).toBeTruthy();
      expect(newState.serverData).toEqual(payload);
    });
  });

  describe('innerSourceRepositoryConfiguration/saveConfiguration/rejected action', () => {
    it('sets `submitMaskState` to null and `saveConfigurationError` to the payload http error message', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'innerSourceRepositoryConfiguration/saveConfiguration/rejected',
        payload: 'someError',
      });

      expect(newState.submitMaskState).toBeNull();
      expect(newState.saveConfigurationError).toBe('someError');
    });
  });

  describe('innerSourceRepositoryConfiguration/testConfiguration/pending action', () => {
    it('sets `submitMaskState` to false, `submitMaskMessage`, and `testConfigurationError` to null', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'innerSourceRepositoryConfiguration/testConfiguration/pending',
      });

      expect(newState.submitMaskState).toBeFalsy();
      expect(newState.submitMaskMessage).toBe(SUBMIT_MASK_TESTING_CONFIGURATION_MESSAGE);
      expect(newState.testConfigurationError).toBeNull();
    });
  });

  describe('innerSourceRepositoryConfiguration/testConfiguration/fulfilled action', () => {
    it('sets `submitMaskState` to true and `testConfigurationSuccessful` to true if the payload has status code 200', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'innerSourceRepositoryConfiguration/testConfiguration/fulfilled',
        payload: { code: 200, message: 'OK' },
      });

      expect(newState.submitMaskState).toBeTruthy();
      expect(newState.testConfigurationSuccessful).toBeTruthy();
    });

    it('sets `submitMaskState` to null, `testConfigurationSuccessful` to false, and `testConfigurationError` if the payload does not have status code 200', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'innerSourceRepositoryConfiguration/testConfiguration/fulfilled',
        payload: { code: 401, message: 'Unauthorized' },
      });

      expect(newState.submitMaskState).toBeNull();
      expect(newState.testConfigurationSuccessful).toBeFalsy();
      expect(newState.testConfigurationError).toBe('401 Unauthorized');
    });
  });

  describe('innerSourceRepositoryConfiguration/testConfiguration/rejected action', () => {
    it('sets `submitMaskState` to null, `testConfigurationSuccessful` to false, and `testConfigurationError` to the payload http error message', () => {
      const state = {};

      let newState = reducer(state, {
        type: 'innerSourceRepositoryConfiguration/testConfiguration/rejected',
        payload: 'someError',
      });

      expect(newState.submitMaskState).toBeNull();
      expect(newState.testConfigurationSuccessful).toBeFalsy();
      expect(newState.testConfigurationError).toBe('someError');

      newState = reducer(state, {
        type: 'innerSourceRepositoryConfiguration/testConfiguration/rejected',
        payload: { status: '401', message: 'Unauthorized' },
      });

      expect(newState.submitMaskState).toBeNull();
      expect(newState.testConfigurationSuccessful).toBeFalsy();
      expect(newState.testConfigurationError).toBe('Error 401');
    });
  });

  describe('innerSourceRepositoryConfiguration/deleteConfiguration/pending action', () => {
    it('sets `deleteSubmitMaskState` to false, `submitMaskMessage`, and `deleteConfigurationError` to null', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'innerSourceRepositoryConfiguration/deleteConfiguration/pending',
      });

      expect(newState.deleteSubmitMaskState).toBeFalsy();
      expect(newState.submitMaskMessage).toBe(SUBMIT_MASK_DELETING_CONFIGURATION_MESSAGE);
      expect(newState.deleteConfigurationError).toBeNull();
    });
  });

  describe('innerSourceRepositoryConfiguration/deleteConfiguration/fulfilled action', () => {
    it('sets the initial state and `deleteSubmitMaskState` to true', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'innerSourceRepositoryConfiguration/deleteConfiguration/fulfilled',
      });

      expect(newState).toEqual({ ...getInitialState(), deleteSubmitMaskState: true });
    });
  });

  describe('innerSourceRepositoryConfiguration/deleteConfiguration/rejected action', () => {
    it('sets `deleteSubmitMaskState` to null and `deleteConfigurationError` to the payload http error message', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'innerSourceRepositoryConfiguration/deleteConfiguration/rejected',
        payload: 'someError',
      });

      expect(newState.deleteSubmitMaskState).toBeNull();
      expect(newState.deleteConfigurationError).toBe('someError');
    });
  });
});
