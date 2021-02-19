/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reduce from '../../../main/frontend/firewall/firewallReducer';

describe('firewallReducer', function() {
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

      // viewState
      expect(newState.viewState.loadedStatus).toBe(false);
      expect(newState.viewState.loadStatusError).toBeNull();
      expect(newState.viewState.isShowConfigurationModal).toBe(false);

      //statusState
      expect(newState.statusState.isEnabled).toBe(false);

      //autoUnquarantineState.viewState
      expect(newState.autoUnquarantineState.viewState.loadedConfiguration).toBe(false);
      expect(newState.autoUnquarantineState.viewState.loadConfigurationError).toBeNull();
      expect(newState.autoUnquarantineState.viewState.enabledPolicyConditionTypesCount).toBe(0);
      expect(newState.autoUnquarantineState.viewState.totalPolicyConditionTypesCount).toBe(1);

      //configurationState
      expect(newState.configurationState.autoUnquarantineEnabled).toBe(false);
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

  describe('FIREWALL_LOAD_STATUS_REQUESTED action', function() {
    it('updates to the initial state', function() {
      const state = Object.freeze({
        other: otherObject
      });
      const newState = reduce(state, {
        type: 'FIREWALL_LOAD_STATUS_REQUESTED'
      });
      // viewState
      expect(newState.viewState.loadedStatus).toBe(false);
      expect(newState.viewState.loadStatusError).toBeNull();
      expect(newState.viewState.isShowConfigurationModal).toBe(false);

      //statusState
      expect(newState.statusState.isEnabled).toBe(false);

      //autoUnquarantineState.viewState
      expect(newState.autoUnquarantineState.viewState.loadedConfiguration).toBe(false);
      expect(newState.autoUnquarantineState.viewState.loadConfigurationError).toBeNull();
      expect(newState.autoUnquarantineState.viewState.enabledPolicyConditionTypesCount).toBe(0);
      expect(newState.autoUnquarantineState.viewState.totalPolicyConditionTypesCount).toBe(1);

      //configurationState
      expect(newState.configurationState.autoUnquarantineEnabled).toBe(false);
    });
  });

  describe('FIREWALL_LOAD_STATUS_FULFILLED action', function() {
    it('updates the state, sets the load error to null and sets enabled flag from payload', function() {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          loadedStatus: false,
          loadStatusError: 'error!'
        },
        statusState: {
          isEnabled: false,
          other: otherObject
        },
        autoUnquarantineState: {
          other: otherObject
        },
        configurationState: {
          other: otherObject
        }
      });
      const payload = {
        experimentalFeatures: {firewallAutoUnquarantine: true}
      };
      const newState = reduce(state, {
        type: 'FIREWALL_LOAD_STATUS_FULFILLED',
        payload: payload
      });
      expect(newState.viewState.loadedStatus).toBe(true);
      expect(newState.viewState.loadStatusError).toBeNull();
      //statusState
      expect(newState.statusState.isEnabled).toBe(true);
      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toEqual(otherObject);
      expect(newState.statusState.other).toEqual(otherObject);
      expect(newState.autoUnquarantineState.other).toEqual(otherObject);
      expect(newState.configurationState.other).toEqual(otherObject);
    });
  });

  describe('FIREWALL_LOAD_STATUS_FAILED action', function() {
    it('updates the state and sets the loadStatusError to the payload', function() {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          loadedStatus: false,
          loadStatusError: null
        },
        statusState: {
          isEnabled: false,
          other: otherObject
        },
        autoUnquarantineState: {
          other: otherObject
        },
        configurationState: {
          other: otherObject
        }
      });
      const newState = reduce(state, {
        type: 'FIREWALL_LOAD_STATUS_FAILED',
        payload: 'error!'
      });
      expect(newState.viewState.loadedStatus).toBe(true);
      expect(newState.viewState.loadStatusError).toBe('error!');
      //configurationState
      expect(newState.statusState.isEnabled).toBe(false);
      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.statusState.other).toEqual(otherObject);
      expect(newState.viewState.other).toEqual(otherObject);
      expect(newState.autoUnquarantineState.other).toEqual(otherObject);
      expect(newState.configurationState.other).toEqual(otherObject);
    });
  });

  describe('FIREWALL_SET_SHOW_CONFIGURATION_MODAL action', function() {
    it('updates the state and sets the isShowConfigurationModal to the payload', function() {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          isShowConfigurationModal: false
        },
        statusState: {
          other: otherObject
        },
        autoUnquarantineState: {
          other: otherObject
        },
        configurationState: {
          other: otherObject
        }
      });
      const newState = reduce(state, {
        type: 'FIREWALL_SET_SHOW_CONFIGURATION_MODAL',
        payload: true
      });
      //viewState
      expect(newState.viewState.isShowConfigurationModal).toBe(true);
      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.statusState.other).toEqual(otherObject);
      expect(newState.viewState.other).toEqual(otherObject);
      expect(newState.autoUnquarantineState.other).toEqual(otherObject);
      expect(newState.configurationState.other).toEqual(otherObject);
    });
  });

  describe('FIREWALL_SAVE_CONFIGURATION_FULFILLED action', function() {
    it('updates the state', function() {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject
        },
        statusState: {
          other: otherObject
        },
        autoUnquarantineState: {
          other: otherObject,
          viewState: {
            other: otherObject,
            enabledPolicyConditionTypesCount: 0
          }
        },
        configurationState: {
          other: otherObject,
          autoUnquarantineEnabled: false
        }
      });
      const newState = reduce(state, {
        type: 'FIREWALL_SAVE_CONFIGURATION_FULFILLED',
        payload: {autoUnquarantineEnabled: true}
      });
      // autoUnquarantineState
      expect(newState.autoUnquarantineState.viewState.enabledPolicyConditionTypesCount).toBe(1);

      // configurationState
      expect(newState.configurationState.autoUnquarantineEnabled).toBeTrue();

      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toBe(otherObject);
      expect(newState.statusState.other).toBe(otherObject);
      expect(newState.autoUnquarantineState.other).toBe(otherObject);
      expect(newState.autoUnquarantineState.viewState.other).toBe(otherObject);
      expect(newState.configurationState.other).toEqual(otherObject);
    });
  });

  describe('FIREWALL_LOAD_CONFIGURATION_FULFILLED action', function() {
    it('updates the state', function() {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject
        },
        statusState: {
          other: otherObject
        },
        autoUnquarantineState: {
          other: otherObject,
          viewState: {
            other: otherObject,
            loadedConfiguration: false,
            loadConfigurationError: 'error!',
            enabledPolicyConditionTypesCount: 0,
            totalPolicyConditionTypesCount: 1
          }
        },
        configurationState: null
      });
      const newState = reduce(state, {
        type: 'FIREWALL_LOAD_CONFIGURATION_FULFILLED',
        payload: {autoUnquarantineEnabled: true}
      });

      // autoUnquarantineState
      expect(newState.autoUnquarantineState.viewState.loadedConfiguration).toBeTrue();
      expect(newState.autoUnquarantineState.viewState.loadConfigurationError).toBeNull();
      expect(newState.autoUnquarantineState.viewState.enabledPolicyConditionTypesCount).toBe(1);
      expect(newState.autoUnquarantineState.viewState.totalPolicyConditionTypesCount).toBe(1);

      // configurationState
      expect(newState.configurationState).toEqual({autoUnquarantineEnabled: true});

      // properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toBe(otherObject);
      expect(newState.statusState.other).toBe(otherObject);
      expect(newState.autoUnquarantineState.other).toBe(otherObject);
      expect(newState.autoUnquarantineState.viewState.other).toBe(otherObject);
    });
  });

  describe('FIREWALL_LOAD_CONFIGURATION_FAILED action', function() {
    it('updates the state', function() {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject
        },
        statusState: {
          other: otherObject
        },
        autoUnquarantineState: {
          other: otherObject,
          viewState: {
            other: otherObject,
            loadedConfiguration: false,
            loadConfigurationError: null
          }
        },
        configurationState: {
          other: otherObject
        }
      });
      const newState = reduce(state, {
        type: 'FIREWALL_LOAD_CONFIGURATION_FAILED',
        payload: 'error!'
      });

      // autoUnquarantineState
      expect(newState.autoUnquarantineState.viewState.loadedConfiguration).toBeTrue();
      expect(newState.autoUnquarantineState.viewState.loadConfigurationError).toBe('error!');

      // properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toBe(otherObject);
      expect(newState.statusState.other).toBe(otherObject);
      expect(newState.autoUnquarantineState.other).toBe(otherObject);
      expect(newState.autoUnquarantineState.viewState.other).toBe(otherObject);
      expect(newState.configurationState.other).toBe(otherObject);
    });
  });
});
