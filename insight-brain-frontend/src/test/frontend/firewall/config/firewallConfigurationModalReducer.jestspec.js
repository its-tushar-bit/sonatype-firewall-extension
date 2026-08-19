/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reduce, {
  INTEGRITY_RATING_POLICY_TYPE_ID,
} from '../../../../main/frontend/firewall/config/firewallConfigurationModalReducer';

describe('firewallConfigurationModalReducer', function () {
  const defaultState = Object.freeze({
    viewState: Object.freeze({
      submitMaskSuccessState: null,
      saveConfigurationError: null,
      isDirty: false,
    }),
    serverState: Object.freeze({
      conditionTypes: [
        {
          id: INTEGRITY_RATING_POLICY_TYPE_ID,
          autoReleaseQuarantineEnabled: false,
        },
      ],
    }),
    formState: Object.freeze({
      conditionTypes: [
        {
          id: INTEGRITY_RATING_POLICY_TYPE_ID,
          autoReleaseQuarantineEnabled: false,
        },
      ],
    }),
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

      expect(newState).toEqual(defaultState);
    });
  });

  describe('unknown action', function () {
    it('returns original state', function () {
      const state = Object.freeze({ foo: 'bar' });
      const action = { type: 'UNKNOWN' };
      const newState = reduce(state, action);

      expect(newState).toBe(state);
    });
  });

  describe('FIREWALL_SAVE_CONFIGURATION_REQUESTED action', function () {
    let minimumState = { viewState: {} };

    it('updates the state', function () {
      expect(reduce(minimumState, { type: 'FIREWALL_SAVE_CONFIGURATION_REQUESTED' })).toEqual({
        ...minimumState,
        viewState: {
          ...minimumState.viewState,
          submitMaskSuccessState: false,
          saveConfigurationError: null,
        },
      });
    });
  });

  describe('FIREWALL_SAVE_CONFIGURATION_FULFILLED action', function () {
    let minimumState = {};

    it('updates the state', function () {
      expect(reduce(minimumState, { type: 'FIREWALL_SAVE_CONFIGURATION_FULFILLED', payload: {} })).toEqual({
        ...minimumState,
        viewState: {
          ...minimumState.viewState,
          submitMaskSuccessState: true,
        },
      });
    });
  });

  describe('FIREWALL_SAVE_CONFIGURATION_FAILED action', function () {
    let minimumState = {};

    it('updates the state and sets the saveConfigurationError to the payload', function () {
      expect(reduce(minimumState, { type: 'FIREWALL_SAVE_CONFIGURATION_FAILED', payload: 'error!' })).toEqual({
        ...minimumState,
        viewState: {
          ...minimumState.viewState,
          submitMaskSuccessState: null,
          saveConfigurationError: 'error!',
        },
      });
    });
  });

  describe('FIREWALL_LOAD_CONFIGURATION_REQUESTED action', function () {
    let minimumState = {};

    it('updates to the initial state', function () {
      expect(reduce(minimumState, { type: 'FIREWALL_LOAD_CONFIGURATION_REQUESTED' })).toEqual({
        ...defaultState,
      });
    });
  });

  describe('FIREWALL_LOAD_CONFIGURATION_FULFILLED action', function () {
    let minimumState = {};

    it('updates the state and sets the autoUnquarantineEnabled to the payload', function () {
      let payload = [
        {
          id: INTEGRITY_RATING_POLICY_TYPE_ID,
          autoReleaseQuarantineEnabled: false,
        },
      ];

      expect(reduce(minimumState, { type: 'FIREWALL_LOAD_CONFIGURATION_FULFILLED', payload: payload })).toEqual({
        ...minimumState,
        viewState: {
          ...minimumState.viewState,
          isDirty: false,
        },
        serverState: {
          ...minimumState.serverState,
          conditionTypes: payload,
        },
        formState: {
          ...minimumState.formState,
          conditionTypes: payload,
        },
      });
    });
  });

  describe('FIREWALL_TOGGLE_AUTO_UNQUARANTINE_ENABLED action', function () {
    it('updates the isDirty flag and toggles autoUnquarantineEnabled for the given condition type', function () {
      expect(
        reduce(defaultState, {
          type: 'FIREWALL_TOGGLE_AUTO_UNQUARANTINE_ENABLED',
          payload: INTEGRITY_RATING_POLICY_TYPE_ID,
        })
      ).toEqual({
        ...defaultState,
        viewState: {
          ...defaultState.viewState,
          isDirty: true,
        },
        formState: {
          ...defaultState.formState,
          conditionTypes: [
            {
              id: INTEGRITY_RATING_POLICY_TYPE_ID,
              autoReleaseQuarantineEnabled: true,
            },
          ],
        },
      });
    });

    it('sets the isDirty flag to false if not changed', function () {
      let payload = INTEGRITY_RATING_POLICY_TYPE_ID,
        newState = reduce(defaultState, { type: 'FIREWALL_TOGGLE_AUTO_UNQUARANTINE_ENABLED', payload: payload });

      expect(newState.viewState.isDirty).toBe(true);

      expect(reduce(newState, { type: 'FIREWALL_TOGGLE_AUTO_UNQUARANTINE_ENABLED', payload: payload })).toEqual({
        ...defaultState,
        formState: {
          ...defaultState.formState,
          conditionTypes: [
            {
              id: INTEGRITY_RATING_POLICY_TYPE_ID,
              autoReleaseQuarantineEnabled: false,
            },
          ],
        },
      });
    });
  });

  describe('FIREWALL_CONFIGURATION_SAVE_MASK_TIMER_DONE action', function () {
    let minimumState = {};

    it('updates the state', function () {
      let newState = reduce(minimumState, {
        type: 'FIREWALL_SAVE_CONFIGURATION_FAILED',
        payload: 'error!',
      });

      expect(reduce(newState, { type: 'FIREWALL_CONFIGURATION_SAVE_MASK_TIMER_DONE' })).toEqual({
        ...minimumState,
        viewState: {
          ...minimumState.viewState,
          submitMaskSuccessState: null,
          saveConfigurationError: null,
        },
      });
    });
  });
});
