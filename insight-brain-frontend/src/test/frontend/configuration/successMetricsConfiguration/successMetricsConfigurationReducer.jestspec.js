/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reduce from '../../../../main/frontend/configuration/successMetricsConfiguration/successMetricsConfigurationReducer';

describe('successMetricsConfigurationReducer', function () {
  let otherObject;

  beforeEach(function () {
    otherObject = { value: 'test value' };
  });

  describe('initial state', function () {
    it('is used if no state is provided', function () {
      const action = { type: 'UNKNOWN' };
      const newState = reduce(undefined, action);

      expect(newState).not.toBeUndefined();
    });

    it('has default fields', function () {
      const action = { type: 'UNKNOWN' };
      const newState = reduce(undefined, action);

      expect(newState.formState.enabled).toBe(false);

      expect(newState.viewState.loading).toBe(true);
      expect(newState.viewState.loadError).toBeNull();
      expect(newState.viewState.updateError).toBeNull();
      expect(newState.viewState.submitMaskState).toBeNull();
      expect(newState.viewState.isDirty).toBe(false);

      expect(newState.serverData).toBeNull();
    });

    describe('unknown action', function () {
      it('returns original state', function () {
        const state = Object.freeze({ serverData: { enadled: false } });
        const action = {
          type: 'UNKNOWN',
        };
        const newState = reduce(state, action);

        expect(newState).toBe(state);
      });
    });
  });

  describe('SUCCESS_METRICS_CONFIGURATION_UPDATE_REQUESTED action', function () {
    it('sets submitMaskState to false', function () {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          submitMaskState: null,
        },
      });

      const newState = reduce(state, {
        type: 'SUCCESS_METRICS_CONFIGURATION_UPDATE_REQUESTED',
      });

      expect(newState.viewState.submitMaskState).toBe(false);

      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toBe(otherObject);
    });
  });

  describe('SUCCESS_METRICS_CONFIGURATION_UPDATE_FULFILLED action', function () {
    it('sets submitMaskState to true', function () {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          submitMaskState: false,
        },
      });

      const newState = reduce(state, {
        type: 'SUCCESS_METRICS_CONFIGURATION_UPDATE_FULFILLED',
      });

      expect(newState.viewState.submitMaskState).toBe(true);

      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toBe(otherObject);
    });
  });

  describe('SUCCESS_METRICS_CONFIGURATION_UPDATE_FAILED action', function () {
    it('sets submitMaskState to null', function () {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          submitMaskState: true,
        },
        formState: {},
        serverData: {
          enabled: false,
        },
      });

      const newState = reduce(state, {
        type: 'SUCCESS_METRICS_CONFIGURATION_UPDATE_FAILED',
        payload: { status: 403 },
      });

      expect(newState.viewState.submitMaskState).toBeNull();

      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toBe(otherObject);
    });

    it('sets updateError to the payload', function () {
      const state = Object.freeze({
        other: otherObject,
        viewState: {},
        formState: {},
        serverData: {
          enabled: false,
        },
      });

      const newState = reduce(state, {
        type: 'SUCCESS_METRICS_CONFIGURATION_UPDATE_FAILED',
        payload: 'Error 403',
      });

      expect(newState.viewState.updateError).toEqual('Error 403');

      expect(newState.other).toBe(otherObject);
    });

    it('does not reset the form state', function () {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          submitMaskState: true,
        },
        formState: {
          enabled: true,
        },
      });

      const newState = reduce(state, {
        type: 'SUCCESS_METRICS_CONFIGURATION_UPDATE_FAILED',
        payload: { status: 403 },
      });

      expect(newState.formState.enabled).toBe(true);

      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toBe(otherObject);
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

      const newState = reduce(state, {
        type: 'SUCCESS_METRICS_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE',
      });

      expect(newState.viewState.submitMaskState).toBeNull();

      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toBe(otherObject);
    });
  });

  describe('SUCCESS_METRICS_CONFIGURATION_LOAD_REQUESTED action', function () {
    it('updates to the initial state', function () {
      const state = Object.freeze({
        other: otherObject,
      });
      const newState = reduce(state, {
        type: 'SUCCESS_METRICS_CONFIGURATION_LOAD_REQUESTED',
      });

      expect(newState.formState.enabled).toBe(false);

      expect(newState.viewState.loading).toBe(true);
      expect(newState.viewState.loadError).toBeNull();
      expect(newState.viewState.updateError).toBeNull();
      expect(newState.viewState.submitMaskState).toBeNull();
      expect(newState.viewState.isDirty).toBe(false);

      expect(newState.serverData).toBeNull();
    });
  });

  describe('SUCCESS_METRICS_CONFIGURATION_LOAD_FULFILLED action', function () {
    it('updates the state and sets the error to null', function () {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          loading: true,
          loadError: 'Error 403',
          updateError: 'Error 403',
        },
        formState: {},
        serverData: {},
      });
      const payload = {
        enabled: true,
      };
      const newState = reduce(state, {
        type: 'SUCCESS_METRICS_CONFIGURATION_LOAD_FULFILLED',
        payload: payload,
      });

      expect(newState.viewState.loading).toBe(false);
      expect(newState.viewState.loadError).toBeNull();
      expect(newState.viewState.updateError).toBeNull();
      expect(newState.formState).toBe(payload);
      expect(newState.serverData).toBe(payload);

      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toEqual(otherObject);
    });
  });

  describe('SUCCESS_METRICS_CONFIGURATION_LOAD_FAILED action', function () {
    it('updates the state and sets the loadError to the payload', function () {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          loading: true,
          loadError: null,
        },
      });
      const newState = reduce(state, {
        type: 'SUCCESS_METRICS_CONFIGURATION_LOAD_FAILED',
        payload: 'Error 403',
      });

      expect(newState.viewState.loading).toBe(false);
      expect(newState.viewState.loadError).toEqual('Error 403');

      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toEqual(otherObject);
    });
  });

  describe('SUCCESS_METRICS_CONFIGURATION_RESET_FORM action', function () {
    it('updates the state and resets formState to initial', function () {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          isDirty: true,
          updateError: 'some error',
        },
        formState: {
          enabled: false,
        },
        serverData: {
          enabled: true,
        },
      });
      const newState = reduce(state, {
        type: 'SUCCESS_METRICS_CONFIGURATION_RESET_FORM',
      });

      expect(newState.viewState.isDirty).toBe(false);
      expect(newState.viewState.updateError).toBe(null);
      expect(newState.formState.enabled).toBe(true);

      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toEqual(otherObject);
    });
  });

  describe('SUCCESS_METRICS_CONFIGURATION_TOGGLE_ENABLED action', function () {
    it('updates the state and toggles enabled formState value', function () {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          isDirty: false,
        },
        formState: {
          enabled: true,
        },
        serverData: {
          enabled: true,
        },
      });
      const newState = reduce(state, {
        type: 'SUCCESS_METRICS_CONFIGURATION_TOGGLE_ENABLED',
      });

      expect(newState.viewState.isDirty).toBe(true);
      expect(newState.formState.enabled).toBe(false);

      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toEqual(otherObject);
    });
  });
});
