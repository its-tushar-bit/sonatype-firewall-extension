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
      expect(newState.viewState.loadError).toBe(null);

      //statusState
      expect(newState.statusState.isEnabled).toBe(false);

      //autoUnquarantineState.viewState
      expect(newState.autoUnquarantineState.viewState.loadedReleaseQuarantineSummary).toBe(false);
      expect(newState.autoUnquarantineState.viewState.loadReleaseQuarantineSummaryError).toBe(null);
      expect(newState.autoUnquarantineState.viewState.autoReleaseQuarantineCountMTD).toBe('-');
      expect(newState.autoUnquarantineState.viewState.autoReleaseQuarantineCountYTD).toBe('-');
      expect(newState.autoUnquarantineState.viewState.loadedConfiguration).toBe(false);
      expect(newState.autoUnquarantineState.viewState.loadConfigurationError).toBeNull();
      expect(newState.autoUnquarantineState.viewState.enabledPolicyConditionTypesCount).toBe(0);
      expect(newState.autoUnquarantineState.viewState.totalPolicyConditionTypesCount).toBe(0);

      //quarantineSummaryState.viewState
      expect(newState.quarantineSummaryState.viewState.loadedQuarantineSummary).toBe(false);
      expect(newState.quarantineSummaryState.viewState.loadQuarantineSummaryError).toBe(null);
      expect(newState.quarantineSummaryState.viewState.quarantineEnabled).toBe(false);
      expect(newState.quarantineSummaryState.viewState.quarantineEnabledRepositoryCount).toBe(0);
      expect(newState.quarantineSummaryState.viewState.repositoryCount).toBe(0);
      expect(newState.quarantineSummaryState.viewState.totalComponentCount).toBe(0);
      expect(newState.quarantineSummaryState.viewState.quarantinedComponentCount).toBe(0);

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
    it('updates the state', function() {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          loadedStatus: true,
          loadStatusError: 'Error!'
        },
        statusState: otherObject,
        autoUnquarantineState: otherObject,
        configurationState: otherObject
      });
      const newState = reduce(state, {
        type: 'FIREWALL_LOAD_STATUS_REQUESTED'
      });
      // viewState
      expect(newState.viewState.loadedStatus).toBe(false);
      expect(newState.viewState.loadStatusError).toBeNull();
      expect(newState.viewState.other).toBe(otherObject);

      //statusState
      expect(newState.statusState).toBe(otherObject);

      //autoUnquarantineState.viewState
      expect(newState.autoUnquarantineState).toBe(otherObject);

      //configurationState
      expect(newState.configurationState).toBe(otherObject);

      //other
      expect(newState.other).toBe(otherObject);
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
        },
        quarantineSummaryState: {
          other: otherObject,
          viewState: {
            other: otherObject
          }
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
      expect(newState.quarantineSummaryState.other).toBe(otherObject);
    });
  });

  describe('FIREWALL_LOAD_STATUS_FAILED action', function() {
    it('updates the state and sets the loadStatusError to the payload', function() {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          loadedStatus: false,
          loadStatusError: null,
          loadError: null
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
        },
        quarantineSummaryState: {
          other: otherObject,
          viewState: {
            other: otherObject
          }
        }
      });
      const newState = reduce(state, {
        type: 'FIREWALL_LOAD_STATUS_FAILED',
        payload: 'error!'
      });
      expect(newState.viewState.loadedStatus).toBe(true);
      expect(newState.viewState.loadStatusError).toBe('error!');
      expect(newState.viewState.loadStatusError).toBe('error!');
      //configurationState
      expect(newState.statusState.isEnabled).toBe(false);
      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.statusState.other).toEqual(otherObject);
      expect(newState.viewState.other).toEqual(otherObject);
      expect(newState.autoUnquarantineState.other).toEqual(otherObject);
      expect(newState.configurationState.other).toEqual(otherObject);
      expect(newState.quarantineSummaryState.other).toBe(otherObject);
    });

    it('does not update loadError if it exists', function() {
      const state = Object.freeze({
        viewState: {
          loadedStatus: false,
          loadStatusError: null,
          loadError: 'old error!'
        }
      });
      const newState = reduce(state, {
        type: 'FIREWALL_LOAD_STATUS_FAILED',
        payload: 'error!'
      });
      expect(newState.viewState.loadedStatus).toBe(true);
      expect(newState.viewState.loadStatusError).toBe('error!');
      expect(newState.viewState.loadError).toBe('old error!');
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
        },
        quarantineSummaryState: {
          other: otherObject,
          viewState: {
            other: otherObject
          }
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
      expect(newState.quarantineSummaryState.other).toBe(otherObject);
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
        },
        quarantineSummaryState: {
          other: otherObject,
          viewState: {
            other: otherObject
          }
        }
      });
      const newState = reduce(state, {
        type: 'FIREWALL_SAVE_CONFIGURATION_FULFILLED',
        payload: [{id: 'IntegrityRating', autoReleaseQuarantineEnabled: true}]
      });
      // autoUnquarantineState
      expect(newState.autoUnquarantineState.viewState.enabledPolicyConditionTypesCount).toBe(1);

      // configurationState
      expect(newState.configurationState.autoUnquarantineEnabled).toBe(true);

      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toBe(otherObject);
      expect(newState.statusState.other).toBe(otherObject);
      expect(newState.autoUnquarantineState.other).toBe(otherObject);
      expect(newState.autoUnquarantineState.viewState.other).toBe(otherObject);
      expect(newState.configurationState.other).toBeUndefined();
      expect(newState.quarantineSummaryState.other).toBe(otherObject);
    });
  });

  describe('FIREWALL_LOAD_CONFIGURATION_REQUESTED action', function() {
    it('updates the state', function() {
      const state = Object.freeze({
        other: otherObject,
        viewState: otherObject,
        statusState: otherObject,
        autoUnquarantineState: {
          other: otherObject,
          viewState: {
            other: otherObject,
            loadedConfiguration: true,
            loadConfigurationError: 'error!'
          }
        },
        configurationState: otherObject
      });
      const newState = reduce(state, {
        type: 'FIREWALL_LOAD_CONFIGURATION_REQUESTED'
      });
      // autoUnquarantineState.viewState
      expect(newState.autoUnquarantineState.viewState.loadedConfiguration).toBe(false);
      expect(newState.autoUnquarantineState.viewState.loadConfigurationError).toBeNull();
      expect(newState.autoUnquarantineState.viewState.other).toBe(otherObject);
      expect(newState.autoUnquarantineState.other).toBe(otherObject);

      //viewState
      expect(newState.viewState).toBe(otherObject);
      //statusState
      expect(newState.statusState).toBe(otherObject);
      //configurationState
      expect(newState.configurationState).toBe(otherObject);
      //other
      expect(newState.other).toBe(otherObject);
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
            totalPolicyConditionTypesCount: 0
          }
        },
        configurationState: {
          other: otherObject,
          autoUnquarantineEnabled: false
        },
        quarantineSummaryState: {
          other: otherObject,
          viewState: {
            other: otherObject
          }
        }
      });
      const newState = reduce(state, {
        type: 'FIREWALL_LOAD_CONFIGURATION_FULFILLED',
        payload: [{autoReleaseQuarantineEnabled: true}, {autoReleaseQuarantineEnabled: false}]
      });

      // autoUnquarantineState
      expect(newState.autoUnquarantineState.viewState.loadedConfiguration).toBe(true);
      expect(newState.autoUnquarantineState.viewState.loadConfigurationError).toBeNull();
      expect(newState.autoUnquarantineState.viewState.enabledPolicyConditionTypesCount).toBe(1);
      expect(newState.autoUnquarantineState.viewState.totalPolicyConditionTypesCount).toBe(2);

      // configurationState
      expect(newState.configurationState).toEqual({autoUnquarantineEnabled: true});

      // properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toBe(otherObject);
      expect(newState.statusState.other).toBe(otherObject);
      expect(newState.autoUnquarantineState.other).toBe(otherObject);
      expect(newState.autoUnquarantineState.viewState.other).toBe(otherObject);
      expect(newState.quarantineSummaryState.other).toBe(otherObject);
    });
  });

  describe('FIREWALL_LOAD_CONFIGURATION_FAILED action', function() {
    it('updates the state', function() {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          loadError: null
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
        },
        quarantineSummaryState: {
          other: otherObject,
          viewState: {
            other: otherObject
          }
        }
      });
      const newState = reduce(state, {
        type: 'FIREWALL_LOAD_CONFIGURATION_FAILED',
        payload: 'error!'
      });

      // viewState
      expect(newState.viewState.loadError).toBe('error!');

      // autoUnquarantineState
      expect(newState.autoUnquarantineState.viewState.loadedConfiguration).toBe(true);
      expect(newState.autoUnquarantineState.viewState.loadConfigurationError).toBe('error!');

      // properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toBe(otherObject);
      expect(newState.statusState.other).toBe(otherObject);
      expect(newState.autoUnquarantineState.other).toBe(otherObject);
      expect(newState.autoUnquarantineState.viewState.other).toBe(otherObject);
      expect(newState.configurationState.other).toBe(otherObject);
      expect(newState.quarantineSummaryState.other).toBe(otherObject);
    });

    it('does not update loadError if it exists', function() {
      const state = Object.freeze({
        viewState: {
          loadError: 'old error!'
        },
        autoUnquarantineState: {
          viewState: {
            loadedConfiguration: false,
            loadConfigurationError: null
          }
        }
      });
      const newState = reduce(state, {
        type: 'FIREWALL_LOAD_CONFIGURATION_FAILED',
        payload: 'error!'
      });

      // viewState
      expect(newState.viewState.loadError).toBe('old error!');

      // autoUnquarantineState
      expect(newState.autoUnquarantineState.viewState.loadedConfiguration).toBe(true);
      expect(newState.autoUnquarantineState.viewState.loadConfigurationError).toBe('error!');
    });
  });

  describe('FIREWALL_QUARANTINE_SUMMARY_REQUESTED action', function() {
    it('updates to the initial state', function() {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject
        },
        statusState: {
          other: otherObject
        },
        autoUnquarantineState: {
          other: otherObject
        },
        configurationState: {
          other: otherObject
        },
        quarantineSummaryState: {
          other: otherObject,
          viewState: {
            other: otherObject,
            loadedQuarantineSummary: true,
            loadQuarantineSummaryError: 'error!'
          }
        }
      });
      const newState = reduce(state, {
        type: 'FIREWALL_QUARANTINE_SUMMARY_REQUESTED'
      });

      expect(newState.quarantineSummaryState.viewState.loadedQuarantineSummary).toBe(false);
      expect(newState.quarantineSummaryState.viewState.loadQuarantineSummaryError).toBe(null);
      expect(newState.quarantineSummaryState.other).toBe(otherObject);
      expect(newState.quarantineSummaryState.viewState.other).toBe(otherObject);
      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toEqual(otherObject);
      expect(newState.statusState.other).toEqual(otherObject);
      expect(newState.autoUnquarantineState.other).toEqual(otherObject);
      expect(newState.configurationState.other).toEqual(otherObject);
    });
  });

  describe('FIREWALL_QUARANTINE_SUMMARY_FULFILLED action', function() {
    it('updates the state, sets the load error to null and sets enabled flag from payload', function() {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject
        },
        statusState: {
          other: otherObject
        },
        autoUnquarantineState: {
          other: otherObject
        },
        configurationState: {
          other: otherObject
        },
        quarantineSummaryState: {
          other: otherObject,
          viewState: {
            other: otherObject,
            loadedQuarantineSummary: false,
            loadQuarantineSummaryError: null,
            quarantineEnabled: null,
            repositoryCount: null,
            quarantineEnabledRepositoryCount: null,
            totalComponentCount: null,
            quarantinedComponentCount: null
          }
        }
      });
      const payload = {
        quarantineEnabled: true,
        repositoryCount: 5,
        quarantineEnabledRepositoryCount: 2,
        totalComponentCount: 10,
        quarantinedComponentCount: 3
      };
      const newState = reduce(state, {
        type: 'FIREWALL_QUARANTINE_SUMMARY_FULFILLED',
        payload: payload
      });

      expect(newState.quarantineSummaryState.viewState.loadedQuarantineSummary).toBe(true);
      expect(newState.quarantineSummaryState.viewState.quarantineEnabled).toBe(true);
      expect(newState.quarantineSummaryState.viewState.repositoryCount).toBe(5);
      expect(newState.quarantineSummaryState.viewState.quarantineEnabledRepositoryCount).toBe(2);
      expect(newState.quarantineSummaryState.viewState.totalComponentCount).toBe(10);
      expect(newState.quarantineSummaryState.viewState.quarantinedComponentCount).toBe(3);
      expect(newState.quarantineSummaryState.other).toBe(otherObject);
      expect(newState.quarantineSummaryState.viewState.other).toBe(otherObject);
      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toEqual(otherObject);
      expect(newState.statusState.other).toEqual(otherObject);
      expect(newState.autoUnquarantineState.other).toEqual(otherObject);
      expect(newState.configurationState.other).toEqual(otherObject);
    });
  });

  describe('FIREWALL_QUARANTINE_SUMMARY_FAILED action', function() {
    it('updates the state and sets the loadStatusError to the payload', function() {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          loadError: null
        },
        statusState: {
          other: otherObject
        },
        autoUnquarantineState: {
          other: otherObject
        },
        configurationState: {
          other: otherObject
        },
        quarantineSummaryState: {
          other: otherObject,
          viewState: {
            other: otherObject,
            loadedQuarantineSummary: false
          }
        }
      });
      const newState = reduce(state, {
        type: 'FIREWALL_QUARANTINE_SUMMARY_FAILED',
        payload: 'error!'
      });

      // viewState
      expect(newState.viewState.loadError).toBe('error!');

      // newState.quarantineSummaryState
      expect(newState.quarantineSummaryState.viewState.loadedQuarantineSummary).toBe(true);
      expect(newState.quarantineSummaryState.viewState.loadQuarantineSummaryError).toBe('error!');
      expect(newState.quarantineSummaryState.other).toBe(otherObject);
      expect(newState.quarantineSummaryState.viewState.other).toBe(otherObject);
      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toEqual(otherObject);
      expect(newState.statusState.other).toEqual(otherObject);
      expect(newState.autoUnquarantineState.other).toEqual(otherObject);
      expect(newState.configurationState.other).toEqual(otherObject);
    });

    it('does not update loadError if it exists', function() {
      const state = Object.freeze({
        viewState: {
          loadError: 'old error!'
        },
        quarantineSummaryState: {
          viewState: {
            loadedQuarantineSummary: false
          }
        }
      });
      const newState = reduce(state, {
        type: 'FIREWALL_QUARANTINE_SUMMARY_FAILED',
        payload: 'error!'
      });

      // viewState
      expect(newState.viewState.loadError).toBe('old error!');

      // newState.quarantineSummaryState
      expect(newState.quarantineSummaryState.viewState.loadedQuarantineSummary).toBe(true);
      expect(newState.quarantineSummaryState.viewState.loadQuarantineSummaryError).toBe('error!');
    });
  });

  describe('FIREWALL_RELEASE_QUARANTINE_SUMMARY_REQUESTED action', function() {
    it('updates the state', function() {
      const state = Object.freeze({
        other: otherObject,
        viewState: otherObject,
        statusState: otherObject,
        autoUnquarantineState: {
          viewState: {
            other: otherObject,
            loadedReleaseQuarantineSummary: true,
            loadReleaseQuarantineSummaryError: 'Error!'
          },
          other: otherObject
        },
        configurationState: otherObject
      });
      const newState = reduce(state, {
        type: 'FIREWALL_RELEASE_QUARANTINE_SUMMARY_REQUESTED'
      });

      // autoUnquarantineState.viewState
      expect(newState.autoUnquarantineState.viewState.loadedReleaseQuarantineSummary).toBe(false);
      expect(newState.autoUnquarantineState.viewState.loadReleaseQuarantineSummaryError).toBeNull();
      expect(newState.autoUnquarantineState.viewState.other).toBe(otherObject);

      // viewState
      expect(newState.viewState).toBe(otherObject);
      // autoUnquarantineState
      expect(newState.autoUnquarantineState.other).toBe(otherObject);
      // statusState
      expect(newState.statusState).toBe(otherObject);
      // configurationState
      expect(newState.configurationState).toBe(otherObject);
      //other
      expect(newState.other).toBe(otherObject);
    });
  });

  describe('FIREWALL_RELEASE_QUARANTINE_SUMMARY_FULFILLED action', function() {
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
            loadedReleaseQuarantineSummary: false,
            loadReleaseQuarantineSummaryError: null,
            autoReleaseQuarantineCountMTD: '-'
          }
        },
        configurationState: {
          other: otherObject
        }
      });
      const newState = reduce(state, {
        type: 'FIREWALL_RELEASE_QUARANTINE_SUMMARY_FULFILLED',
        payload: {'autoReleaseQuarantineCountMTD': 0, 'autoReleaseQuarantineCountYTD': 1}
      });

      // viewState
      expect(newState.autoUnquarantineState.viewState.autoReleaseQuarantineCountMTD).toBe('0');
      expect(newState.autoUnquarantineState.viewState.autoReleaseQuarantineCountYTD).toBe('1');
      expect(newState.autoUnquarantineState.viewState.loadedReleaseQuarantineSummary).toBe(true);
      expect(newState.autoUnquarantineState.viewState.loadReleaseQuarantineSummaryError).toBeNull();

      // properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toBe(otherObject);
      expect(newState.statusState.other).toBe(otherObject);
      expect(newState.autoUnquarantineState.other).toBe(otherObject);
      expect(newState.autoUnquarantineState.viewState.other).toBe(otherObject);
      expect(newState.configurationState.other).toBe(otherObject);
    });
  });

  describe('FIREWALL_RELEASE_QUARANTINE_SUMMARY_FAILED action', function() {
    it('updates the state', function() {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          loadError: null
        },
        statusState: {
          other: otherObject
        },
        autoUnquarantineState: {
          other: otherObject,
          viewState: {
            other: otherObject,
            loadedReleaseQuarantineSummary: false,
            loadReleaseQuarantineSummaryError: null,
            autoReleaseQuarantineCountMTD: '-'
          }
        },
        configurationState: {
          other: otherObject
        }
      });
      const newState = reduce(state, {
        type: 'FIREWALL_RELEASE_QUARANTINE_SUMMARY_FAILED',
        payload: 'error'
      });

      // viewState
      expect(newState.viewState.loadError).toBe('error');

      // autoUnquarantineState.viewState
      expect(newState.autoUnquarantineState.viewState.autoReleaseQuarantineCountMTD).toBe('-');
      expect(newState.autoUnquarantineState.viewState.loadedReleaseQuarantineSummary).toBe(true);
      expect(newState.autoUnquarantineState.viewState.loadReleaseQuarantineSummaryError).toBe('error');

      // properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toBe(otherObject);
      expect(newState.statusState.other).toBe(otherObject);
      expect(newState.autoUnquarantineState.other).toBe(otherObject);
      expect(newState.autoUnquarantineState.viewState.other).toBe(otherObject);
      expect(newState.configurationState.other).toBe(otherObject);
    });

    it('it does not update loadError if it exists', function() {
      const state = Object.freeze({
        viewState: {
          other: otherObject,
          loadError: 'old error!'
        },
        autoUnquarantineState: {
          viewState: {
            loadedReleaseQuarantineSummary: false,
            loadReleaseQuarantineSummaryError: null
          }
        }
      });
      const newState = reduce(state, {
        type: 'FIREWALL_RELEASE_QUARANTINE_SUMMARY_FAILED',
        payload: 'error'
      });

      // viewState
      expect(newState.viewState.loadError).toBe('old error!');

      // autoUnquarantineState.viewState
      expect(newState.autoUnquarantineState.viewState.loadedReleaseQuarantineSummary).toBe(true);
      expect(newState.autoUnquarantineState.viewState.loadReleaseQuarantineSummaryError).toBe('error');
    });
  });

  describe('FIREWALL_LOAD_DATA_REQUESTED action', function() {
    it('updates to the initial state', function() {
      const state = Object.freeze({
        other: otherObject,
        viewState: otherObject,
        statusState: otherObject,
        autoUnquarantineState: otherObject,
        configurationState: otherObject
      });
      const newState = reduce(state, {
        type: 'FIREWALL_LOAD_DATA_REQUESTED'
      });
      // viewState
      expect(newState.viewState.loadedStatus).toBe(false);
      expect(newState.viewState.loadStatusError).toBeNull();
      expect(newState.viewState.isShowConfigurationModal).toBe(false);
      expect(newState.viewState.loadError).toBe(null);

      //statusState
      expect(newState.statusState.isEnabled).toBe(false);

      //autoUnquarantineState.viewState
      expect(newState.autoUnquarantineState.viewState.loadedReleaseQuarantineSummary).toBe(false);
      expect(newState.autoUnquarantineState.viewState.loadReleaseQuarantineSummaryError).toBe(null);
      expect(newState.autoUnquarantineState.viewState.autoReleaseQuarantineCountMTD).toBe('-');
      expect(newState.autoUnquarantineState.viewState.autoReleaseQuarantineCountYTD).toBe('-');
      expect(newState.autoUnquarantineState.viewState.loadedConfiguration).toBe(false);
      expect(newState.autoUnquarantineState.viewState.loadConfigurationError).toBeNull();
      expect(newState.autoUnquarantineState.viewState.enabledPolicyConditionTypesCount).toBe(0);
      expect(newState.autoUnquarantineState.viewState.totalPolicyConditionTypesCount).toBe(0);

      //configurationState
      expect(newState.configurationState.autoUnquarantineEnabled).toBe(false);

      //quarantineSummaryState.viewState
      expect(newState.quarantineSummaryState.viewState.loadedQuarantineSummary).toBe(false);
      expect(newState.quarantineSummaryState.viewState.loadQuarantineSummaryError).toBe(null);
      expect(newState.quarantineSummaryState.viewState.quarantineEnabled).toBe(false);
      expect(newState.quarantineSummaryState.viewState.quarantineEnabledRepositoryCount).toBe(0);
      expect(newState.quarantineSummaryState.viewState.repositoryCount).toBe(0);
      expect(newState.quarantineSummaryState.viewState.totalComponentCount).toBe(0);
      expect(newState.quarantineSummaryState.viewState.quarantinedComponentCount).toBe(0);

    });
  });
});
