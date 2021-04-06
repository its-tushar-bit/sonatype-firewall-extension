/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reduce, {INTEGRITY_RATING_POLICY_TYPE_ID} from
  '../../../../main/frontend/firewall/config/firewallConfigurationModalReducer';

describe('firewallConfigurationModalReducer', function() {
  let otherObject;

  beforeEach(function() {
    otherObject = {value: 'test value'};
  });

  describe('initial state', function() {
    it('is used if no state is provided', function() {
      const action = {type: 'UNKNOWN'};
      const newState = reduce(undefined, action);
      expect(newState).not.toBeUndefined();
    });

    it('has default fields', function() {
      const action = {type: 'UNKNOWN'};
      const newState = reduce(undefined, action);

      //viewState
      expect(newState.viewState.submitMaskSuccessState).toBeNull();
      expect(newState.viewState.saveConfigurationError).toBeNull();
      expect(newState.viewState.isDirty).toBe(false);

      //serverState
      expect(newState.serverState).toEqual(
          {conditionTypes: [{'id': INTEGRITY_RATING_POLICY_TYPE_ID, 'autoReleaseQuarantineEnabled': false}]});

      //formState
      expect(newState.formState).toEqual(
          {conditionTypes: [{'id': INTEGRITY_RATING_POLICY_TYPE_ID, 'autoReleaseQuarantineEnabled': false}]});
    });
  });

  describe('unknown action', function() {
    it('returns original state', function() {
      const state = Object.freeze({foo: 'bar'});
      const action = {
        type: 'UNKNOWN'
      };
      const newState = reduce(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('FIREWALL_SAVE_CONFIGURATION_REQUESTED action', function() {
    it('updates the state', function() {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          submitMaskSuccessState: true,
          saveConfigurationError: 'error'
        },
        serverState: {
          other: otherObject
        },
        formState: {
          other: otherObject
        }
      });
      const newState = reduce(state, {
        type: 'FIREWALL_SAVE_CONFIGURATION_REQUESTED'
      });
      // viewState
      expect(newState.viewState.submitMaskSuccessState).toBe(false);
      expect(newState.viewState.saveConfigurationError).toBeNull();
      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toEqual(otherObject);
      expect(newState.serverState.other).toEqual(otherObject);
      expect(newState.formState.other).toEqual(otherObject);
    });
  });

  describe('FIREWALL_SAVE_CONFIGURATION_FULFILLED action', function() {
    it('updates the state', function() {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          submitMaskSuccessState: false,
          saveConfigurationError: 'error'
        },
        serverState: {
          other: otherObject
        },
        formState: {
          other: otherObject
        }
      });
      const newState = reduce(state, {
        type: 'FIREWALL_SAVE_CONFIGURATION_FULFILLED',
        payload: {}
      });
      // viewState
      expect(newState.viewState.submitMaskSuccessState).toBe(true);
      expect(newState.viewState.saveConfigurationError).toBeNull();
      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toEqual(otherObject);
      expect(newState.serverState.other).toEqual(otherObject);
      expect(newState.formState.other).toEqual(otherObject);
    });
  });

  describe('FIREWALL_SAVE_CONFIGURATION_FAILED action', function() {
    it('updates the state and sets the saveConfigurationError to the payload', function() {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          submitMaskSuccessState: true,
          saveConfigurationError: null
        },
        serverState: {
          other: otherObject
        },
        formState: {
          other: otherObject
        }
      });
      const newState = reduce(state, {
        type: 'FIREWALL_SAVE_CONFIGURATION_FAILED',
        payload: 'error'
      });
      // viewState
      expect(newState.viewState.submitMaskSuccessState).toBe(null);
      expect(newState.viewState.saveConfigurationError).toEqual('error');
      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toEqual(otherObject);
      expect(newState.serverState.other).toEqual(otherObject);
      expect(newState.formState.other).toEqual(otherObject);
    });
  });

  describe('FIREWALL_LOAD_CONFIGURATION_REQUESTED action', function() {
    it('updates to the initial state', function() {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject
        },
        serverState: {
          other: otherObject
        },
        formState: {
          other: otherObject
        }
      });
      const newState = reduce(state, {
        type: 'FIREWALL_LOAD_CONFIGURATION_REQUESTED'
      });
      //viewState
      expect(newState.viewState.submitMaskSuccessState).toBeNull();
      expect(newState.viewState.saveConfigurationError).toBeNull();
      //serverState
      expect(newState.serverState).toEqual(
          {conditionTypes: [{'id': INTEGRITY_RATING_POLICY_TYPE_ID, 'autoReleaseQuarantineEnabled': false}]});
      //formState
      expect(newState.formState).toEqual(
          {conditionTypes: [{'id': INTEGRITY_RATING_POLICY_TYPE_ID, 'autoReleaseQuarantineEnabled': false}]});
      //others to be undefined
      expect(newState.other).toBeUndefined();
      expect(newState.viewState.other).toBeUndefined();
      expect(newState.serverState.other).toBeUndefined();
      expect(newState.formState.other).toBeUndefined();
    });
  });

  describe('FIREWALL_LOAD_CONFIGURATION_FULFILLED action', function() {
    it('updates the state and sets the autoUnquarantineEnabled to the payload', function() {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          isDirty: true
        },
        serverState: {
          other: otherObject,
          conditionTypes: [{'id': INTEGRITY_RATING_POLICY_TYPE_ID, 'autoReleaseQuarantineEnabled': false}]
        },
        formState: {
          other: otherObject,
          conditionTypes: [{'id': INTEGRITY_RATING_POLICY_TYPE_ID, 'autoReleaseQuarantineEnabled': false}]
        }
      });
      const newState = reduce(state, {
        type: 'FIREWALL_LOAD_CONFIGURATION_FULFILLED',
        payload: [{'id': INTEGRITY_RATING_POLICY_TYPE_ID, 'autoReleaseQuarantineEnabled': true}]
      });
      //viewState
      expect(newState.viewState.isDirty).toBe(false);
      //serverState
      expect(newState.serverState.conditionTypes[0].autoReleaseQuarantineEnabled).toBe(true);
      //formState
      expect(newState.formState.conditionTypes[0].autoReleaseQuarantineEnabled).toBe(true);
      // other properties are not modified for state and viewState
      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toEqual(otherObject);
      // other properties are undefined for serverState and formState
      expect(newState.serverState.other).toBeUndefined();
      expect(newState.formState.other).toBeUndefined();
    });
  });

  describe('FIREWALL_TOGGLE_AUTO_UNQUARANTINE_ENABLED action', function() {
    it('updates the state and toggles the autoUnquarantineEnabled for the given condition type', function() {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject
        },
        serverState: {
          other: otherObject
        },
        formState: {
          conditionTypes: [
            {'id': INTEGRITY_RATING_POLICY_TYPE_ID, 'autoReleaseQuarantineEnabled': false},
            {'id': 'testId', 'autoReleaseQuarantineEnabled': false}
          ]
        }
      });
      const newState = reduce(state, {
        type: 'FIREWALL_TOGGLE_AUTO_UNQUARANTINE_ENABLED',
        payload: 'testId'
      });
      //formState
      expect(newState.formState).toEqual({
        conditionTypes: [
          {'id': INTEGRITY_RATING_POLICY_TYPE_ID, 'autoReleaseQuarantineEnabled': false},
          {'id': 'testId', 'autoReleaseQuarantineEnabled': true}
        ]
      });
      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toEqual(otherObject);
      expect(newState.serverState.other).toEqual(otherObject);
    });

    it('updates sets the isDirty flag to true if changed', function() {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          isDirty: false
        },
        serverState: {
          conditionTypes: [
            {'id': INTEGRITY_RATING_POLICY_TYPE_ID, 'autoReleaseQuarantineEnabled': false},
            {'id': 'testId', 'autoReleaseQuarantineEnabled': false}
          ]
        },
        formState: {
          conditionTypes: [
            {'id': INTEGRITY_RATING_POLICY_TYPE_ID, 'autoReleaseQuarantineEnabled': false},
            {'id': 'testId', 'autoReleaseQuarantineEnabled': false}
          ]
        }
      });
      const newState = reduce(state, {
        type: 'FIREWALL_TOGGLE_AUTO_UNQUARANTINE_ENABLED',
        payload: 'testId'
      });
      //viewState
      expect(newState.viewState.isDirty).toBe(true);
      //formState
      expect(newState.formState).toEqual({
        conditionTypes: [
          {'id': INTEGRITY_RATING_POLICY_TYPE_ID, 'autoReleaseQuarantineEnabled': false},
          {'id': 'testId', 'autoReleaseQuarantineEnabled': true}
        ]
      });
      //serverState
      expect(newState.serverState).toEqual({
        conditionTypes: [
          {'id': INTEGRITY_RATING_POLICY_TYPE_ID, 'autoReleaseQuarantineEnabled': false},
          {'id': 'testId', 'autoReleaseQuarantineEnabled': false}
        ]
      });
      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toEqual(otherObject);
    });

    it('updates sets the isDirty flag to false if not changed', function() {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          isDirty: true
        },
        serverState: {
          conditionTypes: [
            {'id': INTEGRITY_RATING_POLICY_TYPE_ID, 'autoReleaseQuarantineEnabled': false},
            {'id': 'testId', 'autoReleaseQuarantineEnabled': true}
          ]
        },
        formState: {
          conditionTypes: [
            {'id': INTEGRITY_RATING_POLICY_TYPE_ID, 'autoReleaseQuarantineEnabled': false},
            {'id': 'testId', 'autoReleaseQuarantineEnabled': false}
          ]
        }
      });
      const newState = reduce(state, {
        type: 'FIREWALL_TOGGLE_AUTO_UNQUARANTINE_ENABLED',
        payload: 'testId'
      });
      //viewState
      expect(newState.viewState.isDirty).toBe(false);
      //formState
      expect(newState.formState).toEqual({
        conditionTypes: [
          {'id': INTEGRITY_RATING_POLICY_TYPE_ID, 'autoReleaseQuarantineEnabled': false},
          {'id': 'testId', 'autoReleaseQuarantineEnabled': true}
        ]
      });
      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toEqual(otherObject);
    });
  });

  describe('FIREWALL_CONFIGURATION_SAVE_MASK_TIMER_DONE action', function() {
    it('updates the state', function() {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          submitMaskSuccessState: true,
          saveConfigurationError: 'error'
        },
        serverState: {
          other: otherObject
        },
        formState: {
          other: otherObject
        }
      });
      const newState = reduce(state, {
        type: 'FIREWALL_CONFIGURATION_SAVE_MASK_TIMER_DONE'
      });
      //viewState
      expect(newState.viewState.submitMaskSuccessState).toBeNull();
      expect(newState.viewState.saveConfigurationError).toBeNull();
      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toEqual(otherObject);
      expect(newState.serverState.other).toEqual(otherObject);
      expect(newState.formState.other).toEqual(otherObject);
    });
  });
});
