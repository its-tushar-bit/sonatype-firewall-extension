/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {createReducerFromActionMap} from '../util/reduxUtil';
import {
  FIREWALL_LOAD_CONFIGURATION_FAILED,
  FIREWALL_LOAD_CONFIGURATION_FULFILLED,
  FIREWALL_LOAD_STATUS_FAILED,
  FIREWALL_LOAD_STATUS_FULFILLED,
  FIREWALL_LOAD_STATUS_REQUESTED,
  FIREWALL_SAVE_CONFIGURATION_FULFILLED,
  FIREWALL_SET_SHOW_CONFIGURATION_MODAL,
  FIREWALL_RELEASE_QUARANTINE_SUMMARY_FAILED,
  FIREWALL_RELEASE_QUARANTINE_SUMMARY_FULFILLED,
  FIREWALL_RELEASE_QUARANTINE_SUMMARY_REQUESTED,
  FIREWALL_QUARANTINE_SUMMARY_REQUESTED,
  FIREWALL_QUARANTINE_SUMMARY_FULFILLED,
  FIREWALL_QUARANTINE_SUMMARY_FAILED
} from './firewallActions';
import {__, always, lensPath, over, merge} from 'ramda';
import {pathSet, propSet} from '../util/jsUtil';

const initialState = Object.freeze({
  viewState: Object.freeze({
    loadedStatus: false,
    loadStatusError: null,
    isShowConfigurationModal: false
  }),
  statusState: Object.freeze({
    isEnabled: false
  }),
  autoUnquarantineState: Object.freeze({
    viewState: Object.freeze({
      loadedConfiguration: false,
      loadConfigurationError: null,
      loadedReleaseQuarantineSummary: false,
      loadReleaseQuarantineSummaryError: null,
      autoReleaseQuarantineCountMTD: '-',
      enabledPolicyConditionTypesCount: 0,
      totalPolicyConditionTypesCount: 1
    })
  }),
  configurationState: Object.freeze({
    autoUnquarantineEnabled: false
  }),
  quarantineSummaryState: Object.freeze({
    viewState: Object.freeze({
      loadedQuarantineSummary: false,
      loadQuarantineSummaryError: null,
      quarantineEnabled: false,
      quarantineEnabledRepositoryCount: null,
      repositoryCount: null,
      totalComponentCount: null,
      quarantinedComponentCount: null
    })
  })
});

const loadStatusFulfilled = (payload, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
    loadedStatus: true,
    loadStatusError: null
  },
  statusState: {
    ...state.statusState,
    isEnabled: payload.experimentalFeatures.firewallAutoUnquarantine
  }
});

const loadStatusFailed = (payload, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
    loadedStatus: true,
    loadStatusError: payload
  }
});

const loadedReleaseQuarantineSummaryFulfilled = (payload, state) => ({
  ...state,
  autoUnquarantineState: {
    ...state.autoUnquarantineState,
    viewState: {
      ...state.autoUnquarantineState.viewState,
      loadedReleaseQuarantineSummary: true,
      loadReleaseQuarantineSummaryError: null,
      autoReleaseQuarantineCountMTD: payload.autoReleaseQuarantineCountMTD.toString()
    }
  }
});

const loadedReleaseQuarantineSummaryFailed = (payload, state) => ({
  ...state,
  autoUnquarantineState: {
    ...state.autoUnquarantineState,
    viewState: {
      ...state.autoUnquarantineState.viewState,
      loadedReleaseQuarantineSummary: true,
      loadReleaseQuarantineSummaryError: payload
    }
  }
});

const setShowConfigurationModal = (payload, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
    isShowConfigurationModal: payload
  }
});

const saveConfigurationFulfilled = (payload, state) => ({
  ...state,
  autoUnquarantineState: pathSet(['viewState', 'enabledPolicyConditionTypesCount'],
      numberOfenabledPolicyConditionTypesCount(payload), state.autoUnquarantineState),
  configurationState: propSet('autoUnquarantineEnabled', payload.autoUnquarantineEnabled, state.configurationState)
});

const loadConfigurationFulfilled = (payload, state) => ({
  ...state,
  autoUnquarantineState: {
    ...state.autoUnquarantineState,
    viewState: {
      ...state.autoUnquarantineState.viewState,
      loadedConfiguration: true,
      loadConfigurationError: null,
      enabledPolicyConditionTypesCount: numberOfenabledPolicyConditionTypesCount(payload),
      totalPolicyConditionTypesCount: 1
    }
  },
  configurationState: payload
});

const loadConfigurationFailed = (payload, state) =>
  over(lensPath(['autoUnquarantineState', 'viewState']), merge(__, {
    loadedConfiguration: true,
    loadConfigurationError: payload
  }), state);

function numberOfenabledPolicyConditionTypesCount(payload) {
  return payload.autoUnquarantineEnabled ? 1 : 0;
}

const quarantineSummaryRequested = (payload, state) => ({
  ...state,
  quarantineSummaryState: {
    ...state.quarantineSummaryState,
    viewState: {
      ...state.quarantineSummaryState.viewState,
      loadedQuarantineSummary: false,
      loadQuarantineSummaryError: null
    }
  }
});

const quarantineSummaryFulfilled = (payload, state) => ({
  ...state,
  quarantineSummaryState: {
    ...state.quarantineSummaryState,
    viewState: {
      ...state.quarantineSummaryState.viewState,
      loadedQuarantineSummary: true,
      quarantineEnabled: payload.quarantineEnabled,
      quarantineEnabledRepositoryCount: payload.quarantineEnabledRepositoryCount,
      repositoryCount: payload.repositoryCount,
      totalComponentCount: payload.totalComponentCount,
      quarantinedComponentCount: payload.quarantinedComponentCount
    }
  }
});

const quarantineSummaryFailed = (payload, state) => ({
  ...state,
  quarantineSummaryState: {
    ...state.quarantineSummaryState,
    viewState: {
      ...state.quarantineSummaryState.viewState,
      loadedQuarantineSummary: true,
      loadQuarantineSummaryError: payload
    }
  }
});

const reducerActionMap = {
  [FIREWALL_LOAD_STATUS_FAILED]: loadStatusFailed,
  [FIREWALL_LOAD_STATUS_FULFILLED]: loadStatusFulfilled,
  [FIREWALL_LOAD_STATUS_REQUESTED]: always(initialState),
  [FIREWALL_SET_SHOW_CONFIGURATION_MODAL]: setShowConfigurationModal,
  [FIREWALL_LOAD_CONFIGURATION_FULFILLED]: loadConfigurationFulfilled,
  [FIREWALL_LOAD_CONFIGURATION_FAILED]: loadConfigurationFailed,
  [FIREWALL_SAVE_CONFIGURATION_FULFILLED]: saveConfigurationFulfilled,
  [FIREWALL_RELEASE_QUARANTINE_SUMMARY_FAILED]: loadedReleaseQuarantineSummaryFailed,
  [FIREWALL_RELEASE_QUARANTINE_SUMMARY_FULFILLED]: loadedReleaseQuarantineSummaryFulfilled,
  [FIREWALL_RELEASE_QUARANTINE_SUMMARY_REQUESTED]: always(initialState),
  [FIREWALL_SAVE_CONFIGURATION_FULFILLED]: saveConfigurationFulfilled,
  [FIREWALL_QUARANTINE_SUMMARY_REQUESTED]: quarantineSummaryRequested,
  [FIREWALL_QUARANTINE_SUMMARY_FULFILLED]: quarantineSummaryFulfilled,
  [FIREWALL_QUARANTINE_SUMMARY_FAILED]: quarantineSummaryFailed
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
