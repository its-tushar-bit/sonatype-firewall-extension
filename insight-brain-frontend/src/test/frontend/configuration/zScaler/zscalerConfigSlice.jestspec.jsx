/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';

import reducer, { initialState as initialConfigState } from 'MainRoot/configuration/zscaler/zscalerConfigSlice';

const { initialState, userInput } = nxTextInputStateHelpers;

describe('zscalerConfigSlice', () => {
  let initialFormState, payload;

  beforeEach(function () {
    initialFormState = {
      username: initialState(''),
      password: initialState(''),
      hostname: initialState(''),
      apiKey: initialState(''),
      eula: {
        value: false,
        isPristine: true,
        validationErrors: 'This field is required',
        disabled: false,
      },
      configuredFormatState: {
        formats: new Set(),
        isPristine: true,
        validationErrors: 'At least one format must be selected',
      },
    };
    payload = {
      username: 'user',
      password: 'asdf',
      hostname: 'https://zsapi.zscalertwo.net',
      apiKey: 'foo',
    };
  });

  describe('zscalerConfig/load', () => {
    it('pending', () => {
      const state = Object.freeze({
        loading: false,
      });

      const newState = reducer(state, {
        type: 'zscalerConfig/load/pending',
      });

      expect(newState.loading).toBe(true);
    });

    it('rejected', () => {
      const state = Object.freeze({
        loading: true,
        loadError: null,
      });

      const newState = reducer(state, {
        type: 'zscalerConfig/load/rejected',
        payload: 'Something went wrong.',
      });

      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBe('Something went wrong.');
    });

    it('fulfilled', () => {
      const state = Object.freeze({
        loading: true,
        serverData: null,
        formState: initialFormState,
      });

      const newState = reducer(state, {
        type: 'zscalerConfig/load/fulfilled',
        payload: payload,
      });

      expect(newState.loading).toBe(false);
      expect(newState.serverData).toEqual(payload);
      expect(newState.formState.username.value).toBe('user');
      expect(newState.formState.password.value).toBe('\0\0\0\0\0'); // fake password will be set
      expect(newState.formState.hostname.value).toBe('https://zsapi.zscalertwo.net');
      expect(newState.formState.apiKey.value).toBe('foo');
    });
  });

  describe('zscalerConfig/save', () => {
    it('pending', () => {
      const state = Object.freeze({
        submitMaskState: null,
        submitMaskMessage: null,
      });

      const newState = reducer(state, {
        type: 'zscalerConfig/save/pending',
      });

      expect(newState.submitMaskState).toBe(false);
      expect(newState.submitMaskMessage).toBe('Saving');
    });

    it('rejected', () => {
      const state = Object.freeze({
        loading: true,
        submitMaskState: false,
        saveError: null,
      });

      const newState = reducer(state, {
        type: 'zscalerConfig/save/rejected',
        payload: 'Something went wrong.',
      });

      expect(newState.loading).toBe(false);
      expect(newState.submitMaskState).toBe(null);
      expect(newState.saveError).toBe('Something went wrong.');
    });

    it('fulfilled', () => {
      const state = Object.freeze({
        loading: true,
        serverData: null,
        submitMaskState: false,
        formState: initialFormState,
      });

      const newState = reducer(state, {
        type: 'zscalerConfig/save/fulfilled',
        payload: payload,
      });

      expect(newState.loading).toBe(false);
      expect(newState.submitMaskState).toBe(true);
      expect(newState.serverData).toEqual(payload);
      expect(newState.formState.username.value).toBe('user');
      expect(newState.formState.password.value).toBe('\0\0\0\0\0'); // fake password will be set
      expect(newState.formState.hostname.value).toBe('https://zsapi.zscalertwo.net');
      expect(newState.formState.apiKey.value).toBe('foo');
    });
  });

  describe('zscalerConfig/delete', () => {
    it('pending', () => {
      const state = Object.freeze({
        submitMaskState: null,
        submitMaskMessage: null,
      });

      const newState = reducer(state, {
        type: 'zscalerConfig/delete/pending',
      });

      expect(newState.submitMaskState).toBe(false);
      expect(newState.submitMaskMessage).toBe('Deleting');
    });

    it('rejected', () => {
      const state = Object.freeze({
        loading: true,
        submitMaskState: false,
        deleteError: null,
      });

      const newState = reducer(state, {
        type: 'zscalerConfig/delete/rejected',
        payload: 'Something went wrong.',
      });

      expect(newState.loading).toBe(false);
      expect(newState.submitMaskState).toBe(null);
      expect(newState.deleteError).toBe('Something went wrong.');
    });

    it('fulfilled', () => {
      const state = Object.freeze({
        submitMaskState: false,
        showDeleteModal: true,
      });

      const newState = reducer(state, {
        type: 'zscalerConfig/delete/fulfilled',
      });

      expect(newState.submitMaskState).toBe(true);
      expect(newState.showDeleteModal).toEqual(false);
    });
  });

  describe('zscalerConfig/testConfig', () => {
    it('pending', () => {
      const state = Object.freeze({
        submitMaskState: null,
        submitMaskMessage: null,
      });

      const newState = reducer(state, {
        type: 'zscalerConfig/testConfig/pending',
      });

      expect(newState.submitMaskState).toBe(false);
      expect(newState.submitMaskMessage).toBe('Testing configuration...');
    });

    it('rejected', () => {
      const state = Object.freeze({
        submitMaskState: false,
        testConfigError: null,
        testConfigSuccess: true,
      });

      const errorPayload = {
        response: {
          status: 400,
          data: {
            message: 'Insufficient ZScaler permissions',
          },
        },
      };

      const newState = reducer(state, {
        type: 'zscalerConfig/testConfig/rejected',
        payload: errorPayload,
      });

      expect(newState.submitMaskState).toBe(null);
      expect(newState.testConfigError).toBeTruthy();
      expect(typeof newState.testConfigError).toBe('string');
      expect(newState.testConfigSuccess).toBe(false);
    });

    it('fulfilled', () => {
      const state = Object.freeze({
        submitMaskState: false,
        testConfigError: 'Some error message',
        testConfigSuccess: false,
      });

      const newState = reducer(state, {
        type: 'zscalerConfig/testConfig/fulfilled',
      });

      expect(newState.submitMaskState).toBe(true);
      expect(newState.testConfigError).toBeNull();
      expect(newState.testConfigSuccess).toEqual(true);
    });
  });

  describe('zscalerConfig/resetForm', () => {
    it('returns the initial state if there is no serverData', () => {
      const state = Object.freeze({
        formState: initialFormState,
      });

      const newState = reducer(state, {
        type: 'zscalerConfig/resetForm',
      });

      expect(newState).toEqual(initialConfigState);
    });

    it('resets the form state to the serverData', () => {
      const state = Object.freeze({
        formState: {
          username: userInput(() => 'foo', 'user-1'),
          password: userInput(() => 'foo', 'asdf'),
          hostname: userInput(() => 'foo', 'zsapi.user-1.net'),
          apiKey: userInput(() => 'foo', 'bar'),
        },
        serverData: payload,
      });

      const newState = reducer(state, {
        type: 'zscalerConfig/resetForm',
      });

      expect(newState.formState.username.value).toBe('user');
      expect(newState.formState.password.value).toBe('\0\0\0\0\0'); // fake password will be set
      expect(newState.formState.hostname.value).toBe('https://zsapi.zscalertwo.net');
      expect(newState.formState.apiKey.value).toBe('foo');
    });
  });

  describe('zscalerConfig/setUsername', () => {
    it('sets the username and a validation error when the payload is empty string', () => {
      const state = Object.freeze({
        formState: initialFormState,
      });

      const newState = reducer(state, {
        type: 'zscalerConfig/setUsername',
        payload: '',
      });

      expect(newState.formState.username.value).toBe('');
      expect(newState.formState.username.isPristine).toBe(false);
      expect(newState.formState.username.validationErrors).toContain('Must be non-empty');
    });

    it('sets the username and no validation error when the payload is present', () => {
      const state = Object.freeze({
        formState: initialFormState,
      });

      const newState = reducer(state, {
        type: 'zscalerConfig/setUsername',
        payload: 'user',
      });

      expect(newState.formState.username.value).toBe('user');
      expect(newState.formState.username.isPristine).toBe(false);
      expect(newState.formState.username.validationErrors).toBeFalsy();
    });
  });

  describe('zscalerConfig/setPassword', () => {
    it('sets the password and a validation error when the payload is empty string', () => {
      const state = Object.freeze({
        formState: initialFormState,
      });

      const newState = reducer(state, {
        type: 'zscalerConfig/setPassword',
        payload: '',
      });

      expect(newState.formState.password.value).toBe('');
      expect(newState.formState.password.isPristine).toBe(false);
      expect(newState.formState.password.validationErrors).toContain('Must be non-empty');
    });

    it('sets the password and no validation error when the payload is present', () => {
      const state = Object.freeze({
        formState: initialFormState,
      });

      const newState = reducer(state, {
        type: 'zscalerConfig/setPassword',
        payload: 'asdf',
      });

      expect(newState.formState.password.value).toBe('asdf');
      expect(newState.formState.password.isPristine).toBe(false);
      expect(newState.formState.password.validationErrors).toBeFalsy();
    });
  });

  describe('zscalerConfig/setHostname', () => {
    it('sets the hostname and a validation error when the payload is empty string', () => {
      const state = Object.freeze({
        formState: initialFormState,
      });

      const newState = reducer(state, {
        type: 'zscalerConfig/setHostname',
        payload: '',
      });

      expect(newState.formState.hostname.value).toBe('');
      expect(newState.formState.hostname.isPristine).toBe(false);
      expect(newState.formState.hostname.validationErrors).toContain('URL is required');
    });

    it('sets the hostname and no validation error when the payload is present', () => {
      const state = Object.freeze({
        formState: initialFormState,
      });

      const newState = reducer(state, {
        type: 'zscalerConfig/setHostname',
        payload: 'https://zsapi.zscalertwo.net',
      });

      expect(newState.formState.hostname.value).toBe('https://zsapi.zscalertwo.net');
      expect(newState.formState.hostname.isPristine).toBe(false);
      expect(newState.formState.hostname.validationErrors).toBe(null);
    });
  });

  describe('zscalerConfig/setApiKey', () => {
    it('sets the apiKey and a validation error when the payload is empty string', () => {
      const state = Object.freeze({
        formState: initialFormState,
      });

      const newState = reducer(state, {
        type: 'zscalerConfig/setApiKey',
        payload: '',
      });

      expect(newState.formState.apiKey.value).toBe('');
      expect(newState.formState.apiKey.isPristine).toBe(false);
      expect(newState.formState.apiKey.validationErrors).toContain('Must be non-empty');
    });

    it('sets the apiKey and no validation error when the payload is present', () => {
      const state = Object.freeze({
        formState: initialFormState,
      });

      const newState = reducer(state, {
        type: 'zscalerConfig/setApiKey',
        payload: 'boo',
      });

      expect(newState.formState.apiKey.value).toBe('boo');
      expect(newState.formState.apiKey.isPristine).toBe(false);
      expect(newState.formState.apiKey.validationErrors).toBeFalsy();
    });
  });

  describe('zscalerConfig/setEulaCheckbox', () => {
    it('updates to the intended state.', () => {
      const state = Object.freeze({
        formState: initialFormState,
      });

      const newState = reducer(state, {
        type: 'zscalerConfig/setEulaCheckbox',
        payload: true,
      });

      expect(newState.formState.eula.value).toBe(true);
      expect(newState.formState.eula.isPristine).toBe(false);
      expect(newState.formState.eula.validationErrors).toBe(null);
      expect(newState.formState.eula.disabled).toBe(false);
    });
  });

  describe('zscalerConfig/setConfiguredFormats', () => {
    it('sets the ConfiguredFormatState and a validation error when the payload is empty', () => {
      const state = Object.freeze({
        formState: initialFormState,
      });

      const newState = reducer(state, {
        type: 'zscalerConfig/setConfiguredFormats',
        payload: new Set([]),
      });

      expect(newState.formState.configuredFormatState.formats).toEqual(new Set());
      expect(newState.formState.configuredFormatState.isPristine).toBe(false);
      expect(newState.formState.configuredFormatState.validationErrors).toContain(
        'At least one format must be selected'
      );
    });

    it('sets the ConfiguredFormatState and no validation error when the payload is present', () => {
      const state = Object.freeze({
        formState: initialFormState,
      });

      const newState = reducer(state, {
        type: 'zscalerConfig/setConfiguredFormats',
        payload: new Set(['foo', 'bar']),
      });

      expect(newState.formState.configuredFormatState.formats).toEqual(new Set(['foo', 'bar']));
      expect(newState.formState.configuredFormatState.isPristine).toBe(false);
      expect(newState.formState.configuredFormatState.validationErrors).toBeFalsy();
    });
  });

  describe('zscalerConfig/setShowDeleteModal', () => {
    it('sets the showDeleteModal state', () => {
      const state = Object.freeze({
        showDeleteModal: false,
      });

      const newState = reducer(state, {
        type: 'zscalerConfig/setShowDeleteModal',
        payload: true,
      });

      expect(newState.showDeleteModal).toBe(true);
    });
  });
});
