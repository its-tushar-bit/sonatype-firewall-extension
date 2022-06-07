/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, {
  SUBMIT_MASK_SAVING_CONFIGURATION_MESSAGE,
  SUBMIT_MASK_TESTING_CONFIGURATION_MESSAGE,
} from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryConfigurationModalSlice';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import {
  getInitialState,
  getMinimalValidFormState,
  getPayload,
} from 'TestRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryConfigurationModalTestData';

describe('artifactoryRepositoryConfigurationModalSliceReducer', () => {
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

  describe('artifactoryRepositoryConfigurationModal/setBaseUrl action', () => {
    it('sets the `baseUrl` to the payload and updates computed props', () => {
      const state = Object.freeze({
        ...getInitialState(),
        saveConfigurationError: 'someError',
        testConfigurationSuccessful: true,
        testConfigurationError: 'someError',
      });

      const newState = reducer(state, {
        type: 'artifactoryRepositoryConfigurationModal/setBaseUrl',
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

  describe('artifactoryRepositoryConfigurationModal/setAnonymous action', () => {
    it('sets `isAnonymous` to the payload and updates computed props', () => {
      const state = Object.freeze({
        ...getInitialState(),
        saveConfigurationError: 'someError',
        testConfigurationSuccessful: true,
        testConfigurationError: 'someError',
      });

      const newState = reducer(state, {
        type: 'artifactoryRepositoryConfigurationModal/setAnonymous',
        payload: false,
      });

      expect(newState.isAnonymous).toBeFalsy();
    });
  });

  describe('artifactoryRepositoryConfigurationModal/setUsername action', () => {
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
        type: 'artifactoryRepositoryConfigurationModal/setUsername',
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

  describe('artifactoryRepositoryConfigurationModal/setPassword action', () => {
    it('sets the `password` to the payload and updates computed props', () => {
      const state = {
        ...getInitialState(),
        formState: { ...getMinimalValidFormState(), isAnonymous: false },
        saveConfigurationError: 'someError',
        testConfigurationSuccessful: true,
        testConfigurationError: 'someError',
      };

      const newState = reducer(state, {
        type: 'artifactoryRepositoryConfigurationModal/setPassword',
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

  describe('artifactoryRepositoryConfigurationModal/reset action', () => {
    it('reverts to the initial state if there is no server data', () => {
      const state = {
        formState: {
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
        type: 'artifactoryRepositoryConfigurationModal/reset',
      });

      expect(newState.formState).toEqual(getInitialState().formState);
      expect(newState.saveConfigurationError).toBeNull();
      expect(newState.testConfigurationSuccessful).toBeFalsy();
      expect(newState.testConfigurationError).toBeNull();
    });
  });

  describe('artifactoryRepositoryConfigurationModal/resetSubmitMask action', () => {
    it('sets the `submitMaskState` to `null`', () => {
      const state = {
        submitMaskState: true,
      };

      const newState = reducer(state, {
        type: 'artifactoryRepositoryConfigurationModal/resetSubmitMask',
      });

      expect(newState.submitMaskState).toBeNull();
    });
  });

  describe('artifactoryRepositoryConfigurationModal/loadConfiguration/pending action', () => {
    it('sets the initial state with `loading` to true and `loadConfigurationError` to null', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'artifactoryRepositoryConfigurationModal/loadConfiguration/pending',
        meta: {
          arg: 'someArtifactoryConnectionId',
        },
      });
      expect(newState).toEqual({
        loading: true,
        loadConfigurationError: null,
        showModal: true,
        artifactoryConnectionId: 'someArtifactoryConnectionId',
      });
    });
  });

  describe('artifactoryRepositoryConfigurationModal/loadConfiguration/fulfilled action', () => {
    it('sets `loading` to false, `serverData` to the payload, and `formState` to represent the payload', () => {
      const state = {};
      const payload = getPayload(false);

      const newState = reducer(state, {
        type: 'artifactoryRepositoryConfigurationModal/loadConfiguration/fulfilled',
        payload: payload,
      });

      expect(newState.loading).toBeFalsy();
      expect(newState.serverData).toEqual(payload);
      expect(newState.formState).toEqual({
        baseUrlState: nxTextInputStateHelpers.initialState(payload.baseUrl),
        isAnonymous: payload.isAnonymous,
        usernameState: nxTextInputStateHelpers.initialState(payload.username),
        passwordState: nxTextInputStateHelpers.initialState(payload.password),
      });
    });
  });

  describe('artifactoryRepositoryConfigurationModal/loadConfiguration/rejected action', () => {
    it('sets `loading` to false and `loadConfigurationError` to the payload http error message', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'artifactoryRepositoryConfigurationModal/loadConfiguration/rejected',
        payload: 'someError',
      });

      expect(newState.loading).toBeFalsy();
      expect(newState.loadConfigurationError).toBe('someError');
    });
  });

  describe('artifactoryRepositoryConfigurationModal/saveConfiguration/pending action', () => {
    it('sets `submitMaskState` to false, the `submitMaskMessage`, and `saveConfigurationError` to null', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'artifactoryRepositoryConfigurationModal/saveConfiguration/pending',
      });

      expect(newState.submitMaskState).toBeFalsy();
      expect(newState.submitMaskMessage).toBe(SUBMIT_MASK_SAVING_CONFIGURATION_MESSAGE);
      expect(newState.saveConfigurationError).toBeNull();
    });
  });

  describe('artifactoryRepositoryConfigurationModal/saveConfiguration/fulfilled action', () => {
    it('sets `submitMaskState` to true', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'artifactoryRepositoryConfigurationModal/saveConfiguration/fulfilled',
      });

      expect(newState).toEqual({ submitMaskState: true });
    });
  });

  describe('artifactoryRepositoryConfigurationModal/saveConfiguration/rejected action', () => {
    it('sets `submitMaskState` to null and `saveConfigurationError` to the payload http error message', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'artifactoryRepositoryConfigurationModal/saveConfiguration/rejected',
        payload: 'someError',
      });

      expect(newState.submitMaskState).toBeNull();
      expect(newState.saveConfigurationError).toBe('someError');
    });
  });

  describe('artifactoryRepositoryConfigurationModal/testConfiguration/pending action', () => {
    it('sets `submitMaskState` to false, `submitMaskMessage`, and `testConfigurationError` to null', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'artifactoryRepositoryConfigurationModal/testConfiguration/pending',
      });

      expect(newState.submitMaskState).toBeFalsy();
      expect(newState.submitMaskMessage).toBe(SUBMIT_MASK_TESTING_CONFIGURATION_MESSAGE);
      expect(newState.testConfigurationError).toBeNull();
    });
  });

  describe('artifactoryRepositoryConfigurationModal/testConfiguration/fulfilled action', () => {
    it('sets `submitMaskState` to true and `testConfigurationSuccessful` to true if the payload has status code 200', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'artifactoryRepositoryConfigurationModal/testConfiguration/fulfilled',
        payload: { code: 200, message: 'OK' },
      });

      expect(newState.submitMaskState).toBeTruthy();
      expect(newState.testConfigurationSuccessful).toBeTruthy();
    });

    it('sets `submitMaskState` to null, `testConfigurationSuccessful` to false, and `testConfigurationError` if the payload does not have status code 200', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'artifactoryRepositoryConfigurationModal/testConfiguration/fulfilled',
        payload: { code: 401, message: 'Unauthorized' },
      });

      expect(newState.submitMaskState).toBeNull();
      expect(newState.testConfigurationSuccessful).toBeFalsy();
      expect(newState.testConfigurationError).toBe('401 Unauthorized');
    });
  });

  describe('artifactoryRepositoryConfigurationModal/testConfiguration/rejected action', () => {
    it('sets `submitMaskState` to null, `testConfigurationSuccessful` to false, and `testConfigurationError` to the payload http error message', () => {
      const state = {};

      let newState = reducer(state, {
        type: 'artifactoryRepositoryConfigurationModal/testConfiguration/rejected',
        payload: 'someError',
      });

      expect(newState.submitMaskState).toBeNull();
      expect(newState.testConfigurationSuccessful).toBeFalsy();
      expect(newState.testConfigurationError).toBe('someError');

      newState = reducer(state, {
        type: 'artifactoryRepositoryConfigurationModal/testConfiguration/rejected',
        payload: { status: '401', message: 'Unauthorized' },
      });

      expect(newState.submitMaskState).toBeNull();
      expect(newState.testConfigurationSuccessful).toBeFalsy();
      expect(newState.testConfigurationError).toBe('Error 401');
    });
  });
});
