/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reducer from 'MainRoot/configuration/baseUrl/baseUrlConfigurationSlice';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';

describe('baseUrlConfigurationReducer', () => {
  let initialState;
  const formState = {
    baseUrl: { trimmedValue: 'http://localhost:8070' },
  };
  const baseUrlConfiguration = {
    baseUrl: 'http://localhost:8070',
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
      expect(initialState.isDirty).toBeFalsy();
      expect(initialState.submitMaskState).toBeNull();
      expect(initialState.submitMaskMessage).toBeNull();
      expect(initialState.deleteMaskState).toBeNull();
      expect(initialState.formState.baseUrl.value).toBe('');
      expect(initialState.serverData).toBeNull();
      expect(initialState.showDeleteModal).toBeFalsy();
      expect(initialState.shouldDisplayNotice).toBeFalsy();
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

  describe('baseUrlConfiguration/load/pending', () => {
    it('returns initial state', function () {
      const action = { type: 'baseUrlConfiguration/load/pending' };
      const newState = reducer(undefined, action);

      expect(newState).toEqual({ ...initialState, loading: true });
    });
  });

  describe('baseUrlConfiguration/load/fulfilled', () => {
    it('sets the form state as expected with server data', function () {
      const oldState = {};
      const newState = reducer(oldState, {
        type: 'baseUrlConfiguration/load/fulfilled',
        payload: baseUrlConfiguration,
      });

      expect(newState).toEqual({
        ...initialState,
        shouldDisplayNotice: false,
        formState: {
          baseUrl: nxTextInputStateHelpers.initialState(baseUrlConfiguration.baseUrl),
        },
        serverData: baseUrlConfiguration,
      });
    });

    it('sets the form state as expected without server data', function () {
      const oldState = {};
      const newState = reducer(oldState, {
        type: 'baseUrlConfiguration/load/fulfilled',
        payload: null,
      });

      expect(newState).toEqual({ ...initialState, shouldDisplayNotice: true });
    });
  });

  describe('baseUrlConfiguration/load/rejected', () => {
    let errorMessage, action, oldState, newState;

    beforeEach(() => {
      errorMessage = 'error on load';
      oldState = {
        ...initialState,
        updateError: 'Update error',
        deleteError: 'Delete error',
      };
      action = { type: 'baseUrlConfiguration/load/rejected', payload: errorMessage };
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
    });
  });

  describe('baseUrlConfiguration/update/pending', () => {
    it('sets errors to null, submitMaskState, and submitMaskMessage', () => {
      const action = { type: 'baseUrlConfiguration/update/pending' };
      const newState = reducer({}, action);

      expect(newState).toEqual({
        loadError: null,
        updateError: null,
        deleteError: null,
        submitMaskState: false,
        submitMaskMessage: 'Saving…',
      });
    });
  });

  describe('baseUrlConfiguration/update/fulfilled', () => {
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
      newState = reducer(oldState, { type: 'baseUrlConfiguration/update/fulfilled' });
    });

    it('sets submitMaskState', () => {
      expect(newState.submitMaskState).toBeTruthy();
    });

    it('sets isDirty', () => {
      expect(newState.isDirty).toBeFalsy();
    });

    it('sets serverData', () => {
      expect(newState.serverData).toEqual({
        baseUrl: 'http://localhost:8070',
      });
    });
  });

  describe('baseUrlConfiguration/update/rejected', () => {
    let errorMessage, action, newState;
    beforeEach(() => {
      errorMessage = 'error on update';
      action = { type: 'baseUrlConfiguration/update/rejected', payload: { response: { data: errorMessage } } };
      const oldState = { ...initialState, submitMaskState: true, isDirty: true };
      newState = reducer(oldState, action);
    });

    it('sets submitMaskState', () => {
      expect(newState.submitMaskState).toBe(null);
    });

    it('sets errorMessage', () => {
      expect(newState.updateError).toBe(errorMessage);
    });

    it('keeps isDirty', () => {
      expect(newState.isDirty).toBeTruthy();
    });
  });

  describe('baseUrlConfiguration/submitMaskTimerDone', () => {
    it('sets submitMaskState', () => {
      const action = { type: 'baseUrlConfiguration/submitMaskTimerDone' };
      const newState = reducer({ submitMaskState: true }, action);
      expect(newState.submitMaskState).toBeNull();
    });
  });

  describe('baseUrlConfiguration/deleteMaskTimerDone', () => {
    it('sets deleteMaskState', () => {
      const action = { type: 'baseUrlConfiguration/deleteMaskTimerDone' };
      const newState = reducer({ deleteMaskState: true }, action);
      expect(newState.deleteMaskState).toBeNull();
    });
  });

  describe('baseUrlConfiguration/delete/pending', () => {
    it('clears errors and the test message and sets the submitMaskState and submitMaskMessage', () => {
      const action = { type: 'baseUrlConfiguration/delete/pending' };
      const newState = reducer({}, action);

      expect(newState).toEqual({
        loadError: null,
        updateError: null,
        deleteError: null,
        deleteMaskState: false,
        submitMaskMessage: 'Deleting…',
      });
    });
  });

  describe('baseUrlConfiguration/delete/fulfilled', () => {
    it('sets the initial state and submitMaskState to true', () => {
      const newState = reducer({}, { type: 'baseUrlConfiguration/delete/fulfilled' });
      expect(newState).toEqual({
        ...initialState,
        showDeleteModal: true,
        deleteMaskState: true,
        shouldDisplayNotice: true,
      });
    });
  });

  describe('baseUrlConfiguration/delete/rejected', () => {
    it('sets submitMaskState to null and the deleteError', () => {
      const errorMessage = 'error on delete';
      const newState = reducer({}, { type: 'baseUrlConfiguration/delete/rejected', payload: errorMessage });
      expect(newState).toEqual({
        deleteMaskState: null,
        deleteError: errorMessage,
      });
    });
  });

  describe('baseUrlConfiguration/resetForm', () => {
    let clearedForm = {
      baseUrl: { value: '', isPristine: true, trimmedValue: '', validationErrors: null },
    };
    let action;
    beforeEach(() => {
      action = { type: 'baseUrlConfiguration/resetForm' };
    });

    it('resets the form', () => {
      const oldState = { ...initialState, formState: baseUrlConfiguration };
      const newState = reducer(oldState, action);

      expect(newState.formState).toEqual(clearedForm);
      expect(newState.isDirty).toBeFalsy();
    });

    it('keeps serverData in formState', () => {
      let dirtyForm = {
        baseUrl: { value: 'someUrl', isPristine: true, trimmedValue: '', validationErrors: null },
      };
      const oldState = { ...initialState, serverData: baseUrlConfiguration, formState: dirtyForm };
      const newState = reducer(oldState, action);

      expect(newState.formState.baseUrl.value).toEqual(baseUrlConfiguration.baseUrl);
      expect(newState.isDirty).toBeFalsy();
    });
  });

  describe('baseUrlConfiguration/setInputValueBaseUrl', () => {
    const state = Object.freeze({
      formState: {
        baseUrl: { value: '' },
      },
      isValid: true,
    });
    it('updates the formState when no serverData', () => {
      const dirtyValue = 'someUrl';
      const action = { type: 'baseUrlConfiguration/setInputValueBaseUrl', payload: dirtyValue };
      const newState = reducer(state, action);

      expect(newState.formState.baseUrl.value).toBe(dirtyValue);
      expect(newState.isDirty).toBeTruthy();
    });
  });

  describe('baseUrlConfiguration/setShowDeleteModal', () => {
    it('sets showDeleteModal and clears the delete error', () => {
      const action = { type: 'baseUrlConfiguration/setShowDeleteModal', payload: 'any' };
      const newState = reducer({}, action);
      expect(newState).toEqual({
        showDeleteModal: 'any',
        deleteError: null,
      });
    });
  });
});
