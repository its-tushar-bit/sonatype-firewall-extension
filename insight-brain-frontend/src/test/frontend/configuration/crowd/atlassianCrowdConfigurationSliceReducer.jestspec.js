/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reducer from 'MainRoot/configuration/crowd/atlassianCrowdConfigurationSlice';
import { FAKE_PASSWORD } from 'MainRoot/configuration/crowd/util';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';

describe('AtlassianCrowdConfigurationReducer', () => {
  let initialState;
  const formState = {
    serverUrl: { trimmedValue: 'http://localhost:8070' },
    applicationName: { trimmedValue: 'Sonatype' },
    applicationPassword: { trimmedValue: 'admin123' },
  };
  const crowdConfiguration = {
    serverUrl: 'http://localhost:8070',
    applicationName: 'Sonatype',
    applicationPassword: 'admin123',
  };

  beforeEach(() => {
    const dummyAction = { type: 'DUMMY_ACTION' };
    initialState = reducer(undefined, dummyAction);
  });

  describe('initial state', () => {
    it('has default field values', function () {
      expect(initialState.loading).toBeFalsy();
      expect(initialState.loadError).toBeNull();
      expect(initialState.updateError).toBeNull();
      expect(initialState.deleteError).toBeNull();
      expect(initialState.testError).toBeNull();
      expect(initialState.testSuccessMessage).toBeNull();
      expect(initialState.isDirty).toBeFalsy();
      expect(initialState.submitMaskState).toBeNull();
      expect(initialState.submitMaskMessage).toBeNull();
      expect(initialState.deleteMaskState).toBeNull();
      expect(initialState.formState.serverUrl.value).toBe('');
      expect(initialState.formState.applicationName.value).toBe('');
      expect(initialState.formState.applicationPassword.value).toBe('');
      expect(initialState.serverData).toBeNull();
      expect(initialState.mustReenterPassword).toBeFalsy();
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

  describe('atlassianCrowdConfiguration/load/pending', () => {
    it('returns initial state', function () {
      const action = { type: 'atlassianCrowdConfiguration/load/pending' };
      const newState = reducer(undefined, action);

      expect(newState).toEqual({ ...initialState, loading: true });
    });
  });

  describe('atlassianCrowdConfiguration/load/fulfilled', () => {
    it('sets the form state as expected with server data', function () {
      const oldState = {};
      const newState = reducer(oldState, {
        type: 'atlassianCrowdConfiguration/load/fulfilled',
        payload: crowdConfiguration,
      });

      expect(newState).toEqual({
        ...initialState,
        formState: {
          serverUrl: nxTextInputStateHelpers.initialState(crowdConfiguration.serverUrl),
          applicationName: nxTextInputStateHelpers.initialState(crowdConfiguration.applicationName),
          applicationPassword: nxTextInputStateHelpers.initialState(FAKE_PASSWORD),
        },
        serverData: crowdConfiguration,
      });
    });

    it('sets the form state as expected without server data', function () {
      const oldState = {};
      const newState = reducer(oldState, {
        type: 'atlassianCrowdConfiguration/load/fulfilled',
        payload: null,
      });

      expect(newState).toEqual(initialState);
    });
  });

  describe('atlassianCrowdConfiguration/load/rejected', () => {
    let errorMessage, action, oldState, newState;

    beforeEach(() => {
      errorMessage = 'error on load';
      oldState = {
        ...initialState,
        updateError: 'Update error',
        deleteError: 'Delete error',
        testError: 'Test error',
      };
      action = { type: 'atlassianCrowdConfiguration/load/rejected', payload: errorMessage };
      newState = reducer(oldState, action);
    });

    it('sets loading', () => {
      expect(newState.loading).toBeFalsy();
    });

    it('sets loadError', function () {
      expect(newState.loadError).toBe(errorMessage);
    });

    it('clears other errors', function () {
      expect(newState.updateError).toBeNull();
      expect(newState.deleteError).toBeNull();
      expect(newState.testError).toBeNull();
    });
  });

  describe('atlassianCrowdConfiguration/update/pending', () => {
    it('sets errors to null, submitMaskState, and submitMaskMessage', () => {
      const action = { type: 'atlassianCrowdConfiguration/update/pending' };
      const newState = reducer({}, action);

      expect(newState).toEqual({
        loadError: null,
        updateError: null,
        deleteError: null,
        testError: null,
        testSuccessMessage: null,
        submitMaskState: false,
        submitMaskMessage: 'Saving…',
      });
    });
  });

  describe('atlassianCrowdConfiguration/update/fulfilled', () => {
    let newState;
    beforeEach(() => {
      const oldState = {
        ...initialState,
        loadError: 'error',
        updateError: 'error',
        isDirty: true,
        submitMaskState: false,
        formState: formState,
      };
      newState = reducer(oldState, { type: 'atlassianCrowdConfiguration/update/fulfilled' });
    });

    it('sets submitMaskState', () => {
      expect(newState.submitMaskState).toBeTruthy();
    });

    it('sets isDirty', () => {
      expect(newState.isDirty).toBeFalsy();
    });

    it('sets serverData', () => {
      expect(newState.serverData).toEqual({
        serverUrl: 'http://localhost:8070',
        applicationName: 'Sonatype',
      });
    });
  });

  describe('atlassianCrowdConfiguration/update/rejected', () => {
    let errorMessage, action, newState;
    beforeEach(() => {
      errorMessage = 'error on update';
      action = { type: 'atlassianCrowdConfiguration/update/rejected', payload: { response: { data: errorMessage } } };
      const oldState = { ...initialState, submitMaskState: true };
      newState = reducer(oldState, action);
    });

    it('sets submitMaskState', () => {
      expect(newState.submitMaskState).toBe(null);
    });

    it('sets errorMessage', () => {
      expect(newState.updateError).toBe(errorMessage);
    });
  });

  describe('atlassianCrowdConfiguration/submitMaskTimerDone', () => {
    it('sets submitMaskState', () => {
      const action = { type: 'atlassianCrowdConfiguration/submitMaskTimerDone' };
      const newState = reducer({ submitMaskState: true }, action);
      expect(newState.submitMaskState).toBeNull();
    });
  });

  describe('atlassianCrowdConfiguration/deleteMaskTimerDone', () => {
    it('sets deleteMaskState', () => {
      const action = { type: 'atlassianCrowdConfiguration/deleteMaskTimerDone' };
      const newState = reducer({ deleteMaskState: true }, action);
      expect(newState.deleteMaskState).toBeNull();
    });
  });

  describe('atlassianCrowdConfiguration/delete/pending', () => {
    it('clears errors and the test message and sets the submitMaskState and submitMaskMessage', () => {
      const action = { type: 'atlassianCrowdConfiguration/delete/pending' };
      const newState = reducer({}, action);

      expect(newState).toEqual({
        loadError: null,
        updateError: null,
        deleteError: null,
        testError: null,
        testSuccessMessage: null,
        deleteMaskState: false,
        submitMaskMessage: 'Deleting…',
      });
    });
  });

  describe('atlassianCrowdConfiguration/delete/fulfilled', () => {
    it('sets the initial state and submitMaskState to true', () => {
      const newState = reducer({}, { type: 'atlassianCrowdConfiguration/delete/fulfilled' });
      expect(newState).toEqual({
        ...initialState,
        showModal: true,
        deleteMaskState: true,
      });
    });
  });

  describe('atlassianCrowdConfiguration/delete/rejected', () => {
    it('sets submitMaskState to null and the deleteError', () => {
      const errorMessage = 'error on delete';
      const newState = reducer({}, { type: 'atlassianCrowdConfiguration/delete/rejected', payload: errorMessage });
      expect(newState).toEqual({
        deleteMaskState: null,
        deleteError: errorMessage,
      });
    });
  });

  describe('atlassianCrowdConfiguration/test/pending', () => {
    it('sets submitMaskMessage', () => {
      const action = { type: 'atlassianCrowdConfiguration/test/pending' };
      const newState = reducer({}, action);

      expect(newState).toEqual({
        loadError: null,
        updateError: null,
        deleteError: null,
        testError: null,
        testSuccessMessage: null,
        submitMaskState: false,
        submitMaskMessage: 'Testing…',
      });
    });
  });

  describe('atlassianCrowdConfiguration/test/fulfilled', () => {
    let newState;
    describe('on test successful', () => {
      beforeEach(() => {
        const action = { type: 'atlassianCrowdConfiguration/test/fulfilled', payload: { code: 200 } };
        newState = reducer(initialState, action);
      });

      it('sets saveMaskState', () => {
        expect(newState.submitMaskState).toBeNull();
      });

      it('sets testSuccessMessage', () => {
        expect(newState.testSuccessMessage).toBe('Success!');
      });
    });

    describe('on test failed', () => {
      beforeEach(() => {
        const action = {
          type: 'atlassianCrowdConfiguration/test/fulfilled',
          payload: { code: 400, message: 'test failed' },
        };
        newState = reducer(initialState, action);
      });

      it('sets saveMaskState', () => {
        expect(newState.submitMaskState).toBeNull();
      });

      it('sets testError', () => {
        expect(newState.testError).toEqual('test failed');
      });
    });
  });

  describe('atlassianCrowdConfiguration/test/rejected', () => {
    let errorMessage, action, newState;
    beforeEach(() => {
      errorMessage = 'test connection failed';
      action = { type: 'atlassianCrowdConfiguration/test/rejected', payload: errorMessage };
      newState = reducer(initialState, action);
    });

    it('sets saveMaskState', () => {
      expect(newState.submitMaskState).toBeNull();
    });

    it('sets testError', () => {
      expect(newState.testError).toEqual(errorMessage);
    });
  });

  describe('atlassianCrowdConfiguration/resetTestAlertMessages', () => {
    it('clears alert messages', () => {
      const action = { type: 'atlassianCrowdConfiguration/resetTestAlertMessages' };
      const oldState = {
        ...initialState,
        testError: 'test connection failed',
        testSuccessMessage: 'success!',
      };
      const newState = reducer(oldState, action);

      expect(newState.testError).toBeNull();
      expect(newState.testSuccessMessage).toBeNull();
    });
  });

  describe('atlassianCrowdConfiguration/resetForm', () => {
    let clearedForm = {
      serverUrl: { value: '', isPristine: true, trimmedValue: '', validationErrors: null },
      applicationName: { value: '', isPristine: true, trimmedValue: '', validationErrors: null },
      applicationPassword: { value: '', isPristine: true, trimmedValue: '', validationErrors: null },
    };
    let action;
    beforeEach(() => {
      action = { type: 'atlassianCrowdConfiguration/resetForm' };
    });

    it('resets the form', () => {
      const oldState = { ...initialState, formState: crowdConfiguration };
      const newState = reducer(oldState, action);

      expect(newState.formState).toEqual(clearedForm);
      expect(newState.isDirty).toBeFalsy();
    });

    it('keeps serverData in formState', () => {
      let dirtyForm = {
        serverUrl: { value: 'someUrl', isPristine: true, trimmedValue: '', validationErrors: null },
        applicationName: { value: 'someAppName', isPristine: true, trimmedValue: '', validationErrors: null },
        applicationPassword: { value: 'somePassword', isPristine: true, trimmedValue: '', validationErrors: null },
      };
      const oldState = { ...initialState, serverData: crowdConfiguration, formState: dirtyForm };
      const newState = reducer(oldState, action);

      expect(newState.formState.serverUrl.value).toEqual(crowdConfiguration.serverUrl);
      expect(newState.formState.applicationName.value).toEqual(crowdConfiguration.applicationName);
      expect(newState.isDirty).toBeFalsy();
    });
  });

  describe('atlassianCrowdConfiguration/setInputValueServerUrl', () => {
    const state = Object.freeze({
      formState: {
        serverUrl: { value: '' },
        applicationName: { value: '' },
        applicationPassword: { value: '' },
      },
      isValid: true,
    });
    it('updates the formState when no serverData', () => {
      const dirtyValue = 'someUrl';
      const action = { type: 'atlassianCrowdConfiguration/setInputValueServerUrl', payload: dirtyValue };
      const newState = reducer(state, action);

      expect(newState.formState.serverUrl.value).toBe(dirtyValue);
      expect(newState.isDirty).toBeTruthy();
    });

    it('updates the formState when serverData', () => {
      const dirtyField = 'someAppName';
      const action = { type: 'atlassianCrowdConfiguration/setInputValueApplicationName', payload: dirtyField };
      const oldState = { ...state, serverData: crowdConfiguration };
      const newState = reducer(oldState, action);

      expect(newState.formState.applicationName.value).toBe(dirtyField);
      expect(newState.isDirty).toBeTruthy();
    });
  });

  describe('atlassianCrowdConfiguration/setShowModal', () => {
    it('sets showModal and clears the delete error', () => {
      const action = { type: 'atlassianCrowdConfiguration/setShowModal', payload: 'any' };
      const newState = reducer({}, action);
      expect(newState).toEqual({
        showModal: 'any',
        deleteError: null,
      });
    });
  });
});
