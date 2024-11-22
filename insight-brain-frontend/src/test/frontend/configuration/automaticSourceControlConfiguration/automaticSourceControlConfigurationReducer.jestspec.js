/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reduce from '../../../../main/frontend/configuration/automaticSourceControlConfiguration/automaticSourceControlConfigurationReducer';

describe('AutomaticSourceControlConfigurationReducer', () => {
  let otherObject;

  beforeEach(function () {
    otherObject = { value: 'AUX_VALUE' };
  });

  describe('initial state', () => {
    it('should not crash if no state is provided', () => {
      const action = { type: 'TEST_ACTION' };
      const newState = reduce(undefined, action);
      expect(newState).toBeTruthy();
    });

    it('should have the default field values', function () {
      const action = { type: 'TEST_ACTION' };
      const newState = reduce(undefined, action);

      expect(newState.formState.enabled).toBe(false);
      expect(newState.serverData).toBeNull();
      expect(newState.viewState.loading).toBe(true);
      expect(newState.viewState.loadError).toBeNull();
      expect(newState.viewState.submitMaskState).toBeNull();
      expect(newState.viewState.isDirty).toBe(false);
    });
  });

  describe('unknown action', function () {
    it('returns the original state', function () {
      const state = Object.freeze({ foo: 'bar' });
      const action = {
        type: 'UNKNOWN',
      };
      const newState = reduce(state, action);
      expect(newState).toEqual(state);
    });
  });

  describe('AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_FULFILLED action', function () {
    it('sets loading to false', function () {
      const state = Object.freeze({
        otherObject,
        viewState: {
          otherObject,
          loading: true,
        },
      });

      const action = {
        type: 'AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_FULFILLED',
        payload: {
          automaticApplicationsConfiguration: { enabled: false },
          automaticSourceControlConfiguration: {},
          organizations: [],
        },
      };
      const newState = reduce(state, action);
      expect(newState.viewState.loading).toBe(false);

      expect(newState.otherObject).toBe(otherObject);
      expect(newState.viewState.otherObject).toBe(otherObject);
    });
  });

  describe('AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_FULFILLED action with automatic applications', function () {
    it('updates viewState', function () {
      const state = Object.freeze({
        otherObject,
        viewState: {
          otherObject,
          loading: true,
        },
      });

      const action = {
        type: 'AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_FULFILLED',
        payload: {
          automaticApplicationsConfiguration: { enabled: true, parentOrganizationId: '1' },
          automaticSourceControlConfiguration: {},
          organizations: [{ id: '1', name: 'OrganizationOne' }],
          compositeSourceControl: {
            provider: { value: 'provider' },
          },
        },
      };
      const newState = reduce(state, action);
      expect(newState.viewState.loading).toBe(false);
      expect(newState.viewState.scmProvider).toBe('provider');
      expect(newState.viewState.parentOrganization).toEqual({ id: '1', name: 'OrganizationOne' });
      expect(newState.viewState.automaticApplicationsEnabled).toEqual(true);
      expect(newState.otherObject).toBe(otherObject);
      expect(newState.viewState.otherObject).toBe(otherObject);
    });
  });

  describe('AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_FAIL action', function () {
    let state;
    const action = {
      type: 'AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_FAIL',
      payload: 'TEST-MESSAGE',
    };
    let newState;
    beforeEach(function () {
      state = Object.freeze({
        otherObject,
        viewState: {
          otherObject,
          loading: false,
        },
      });
      newState = reduce(state, action);
    });
    it('sets loading to false', function () {
      expect(newState.viewState.loading).toBe(false);

      expect(newState.otherObject).toBe(otherObject);
      expect(newState.viewState.otherObject).toBe(otherObject);
    });

    it('sets the loadError prop', function () {
      expect(newState.viewState.loadError).toBe('TEST-MESSAGE');
    });
  });

  describe('AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_TOGGLE_ENABLED action', function () {
    const action = {
      type: 'AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_TOGGLE_ENABLED',
    };
    let state;
    let newState;

    beforeEach(function () {
      state = Object.freeze({
        otherObject,
        viewState: {
          otherObject,
          isDirty: false,
        },
        formState: {
          enabled: false,
          otherObject,
        },
        serverData: { enabled: false },
      });
      newState = reduce(state, action);
    });

    it('validates mutability', function () {
      expect(newState.otherObject).toBe(otherObject);
      expect(newState.formState.otherObject).toBe(otherObject);
      expect(newState.viewState.otherObject).toBe(otherObject);
    });

    it('toggles the enabled property', function () {
      expect(newState.formState.enabled).toBe(true);
    });

    it('sets isDirty to true', function () {
      expect(newState.viewState.isDirty).toBe(true);
    });

    it('sets isDirty to false', function () {
      newState = reduce(newState, action);
      expect(newState.viewState.isDirty).toBe(false);
    });
  });

  describe('AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_REQUESTED action', function () {
    it('sets submitMaskState to false', function () {
      const state = Object.freeze({
        otherObject,
        viewState: {
          otherObject,
          submitMaskState: null,
        },
      });
      const action = { type: 'AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_REQUESTED' };
      const newState = reduce(state, action);

      expect(newState.viewState.submitMaskState).toBe(false);

      expect(newState.otherObject).toBe(otherObject);
      expect(newState.viewState.otherObject).toBe(otherObject);
    });
  });

  describe('AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_FULFILLED action', function () {
    let state;
    const action = { type: 'AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_FULFILLED' };
    let newState;

    beforeEach(function () {
      state = Object.freeze({
        otherObject,
        formState: { enabled: false },
        viewState: {
          otherObject,
          submitMaskState: true,
          isDirty: true,
          updateError: {},
        },
        serverData: { enabled: true },
      });
      newState = reduce(state, action);
    });

    it('checks mutability', function () {
      expect(newState.otherObject).toBe(otherObject);
      expect(newState.viewState.otherObject).toBe(otherObject);
    });

    it('sets the server data the same value as formData', function () {
      expect(newState.serverData.enabled).toBe(newState.formState.enabled);
    });

    it('resets the view state variables', function () {
      expect(newState.viewState.submitMaskState).toBe(true);
      expect(newState.viewState.isDirty).toBe(false);
      expect(newState.viewState.updateError).toBeNull();
    });
  });

  describe('AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_FAILED action', function () {
    let state;
    const action = {
      type: 'AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_FAILED',
      payload: { error: 'test' },
    };
    let newState;

    beforeEach(function () {
      state = Object.freeze({
        otherObject,
        formState: { otherObject, enabled: false },
        serverData: { otherObject, enabled: true },
        viewState: {
          otherObject,
          updateError: null,
          submitMaskState: true,
        },
      });
      newState = reduce(state, action);
    });

    it('checks the immutability', function () {
      expect(newState.otherObject).toBe(otherObject);
      expect(newState.formState.otherObject).toBe(otherObject);
      expect(newState.serverData.otherObject).toBe(otherObject);
      expect(newState.viewState.otherObject).toBe(otherObject);
    });

    it('sets the updateError prop', function () {
      expect(newState.viewState.updateError).toEqual(action.payload);
    });

    it('sets null to submitMaskState', function () {
      expect(newState.viewState.submitMaskState).toBeNull();
    });
  });

  describe('SUCCESS_METRICS_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE action', function () {
    it('sets submitMaskState to null', function () {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          submitMaskState: true,
        },
      });

      const action = {
        type: 'SUCCESS_METRICS_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE',
      };

      const newState = reduce(state, action);

      expect(newState.viewState.submitMaskState).toBeNull();

      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toBe(otherObject);
    });
  });

  describe('AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_RESET_FORM action', function () {
    let state, action, newState;

    beforeEach(function () {
      state = Object.freeze({
        formState: {
          enabled: true,
          otherObject,
        },
        viewState: {
          isDirty: true,
          updateError: 'some error happended',
          otherObject,
        },
        serverData: {
          enabled: false,
          otherObject,
        },
        otherObject,
      });
      action = { type: 'AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_RESET_FORM' };
      newState = reduce(state, action);
    });

    it('checks immutability', function () {
      expect(newState.otherObject).toBe(otherObject);
      expect(newState.formState.otherObject).toBe(otherObject);
      expect(newState.viewState.otherObject).toBe(otherObject);
      expect(newState.serverData.otherObject).toBe(otherObject);
    });

    it('resets the form value and the state to initial value', function () {
      expect(newState.formState.enabled).toBe(false);
      expect(newState.viewState.isDirty).toBe(false);
      expect(newState.viewState.updateError).toBeNull();
    });
  });

  describe('AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_REQUESTED action', function () {
    it('returns the initial state', function () {
      const state = Object.freeze({
        formState: {
          enabled: false,
        },
        viewState: {
          loading: true,
          loadError: null,
          updateError: null,
          submitMaskState: null,
          isDirty: false,
          parentOrganization: null,
          automaticApplicationsEnabled: false,
          scmProvider: null,
        },
        serverData: null,
      });
      const action = { type: 'AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_REQUESTED' };
      const newState = reduce(state, action);
      expect(newState).toEqual(state);
    });
  });
});
