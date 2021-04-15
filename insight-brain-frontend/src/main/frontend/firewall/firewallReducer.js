/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createReducerFromActionMap } from '../util/reduxUtil';
import {
  FIREWALL_LOAD_CONFIGURATION_FAILED,
  FIREWALL_LOAD_CONFIGURATION_FULFILLED,
  FIREWALL_LOAD_STATUS_FAILED,
  FIREWALL_LOAD_STATUS_FULFILLED,
  FIREWALL_SAVE_CONFIGURATION_FULFILLED,
  FIREWALL_SET_SHOW_CONFIGURATION_MODAL,
  FIREWALL_RELEASE_QUARANTINE_SUMMARY_FAILED,
  FIREWALL_RELEASE_QUARANTINE_SUMMARY_FULFILLED,
  FIREWALL_RELEASE_QUARANTINE_LIST_REQUESTED,
  FIREWALL_RELEASE_QUARANTINE_LIST_FAILED,
  FIREWALL_RELEASE_QUARANTINE_LIST_FULFILLED,
  FIREWALL_AUTO_UNQUARANTINE_GRID_SET_PAGE,
  FIREWALL_AUTO_UNQUARANTINE_GRID_SET_SORTING,
  FIREWALL_AUTO_UNQUARANTINE_GRID_SET_FILTER,
  FIREWALL_POLICIES_REQUESTED,
  FIREWALL_POLICIES_FULFILLED,
  FIREWALL_POLICIES_FAILED,
  FIREWALL_LOAD_DATA_REQUESTED,
  FIREWALL_LOAD_CONFIGURATION_REQUESTED,
  FIREWALL_LOAD_STATUS_REQUESTED,
  FIREWALL_RELEASE_QUARANTINE_SUMMARY_REQUESTED,
  FIREWALL_QUARANTINE_SUMMARY_REQUESTED,
  FIREWALL_QUARANTINE_SUMMARY_FULFILLED,
  FIREWALL_QUARANTINE_SUMMARY_FAILED,
} from './firewallActions';
import { __, always, lensPath, over, merge } from 'ramda';
import { pathSet } from '../util/jsUtil';

const initialState = Object.freeze({
  viewState: Object.freeze({
    loadedStatus: false,
    loadStatusError: null,
    isShowConfigurationModal: false,
    loadError: null,
  }),
  statusState: Object.freeze({
    isEnabled: false,
  }),
  autoUnquarantineState: Object.freeze({
    viewState: Object.freeze({
      loadedConfiguration: false,
      loadConfigurationError: null,
      loadedReleaseQuarantineSummary: false,
      loadReleaseQuarantineSummaryError: null,
      autoReleaseQuarantineCountMTD: '-',
      autoReleaseQuarantineCountYTD: '-',
      enabledPolicyConditionTypesCount: 0,
      totalPolicyConditionTypesCount: 0,
    }),
    autoUnquarantineGridState: Object.freeze({
      loadedReleaseQuarantineList: false,
      loadAutoUnquarantineGridError: null,
      loadedPolicies: false,
      releaseQuarantineList: [],
      releaseQuarantinePageCount: 0,
      pageSize: 12,
      currentPage: null,
      sortDir: null,
      sortField: null,
      filterPolicyId: '',
      policies: [],
    }),
  }),
  configurationState: Object.freeze({
    autoUnquarantineEnabled: false,
  }),
  quarantineSummaryState: Object.freeze({
    viewState: Object.freeze({
      loadedQuarantineSummary: false,
      loadQuarantineSummaryError: null,
      quarantineEnabled: false,
      quarantineEnabledRepositoryCount: 0,
      repositoryCount: 0,
      totalComponentCount: 0,
      quarantinedComponentCount: 0,
    }),
  }),
});

const loadStatusRequested = (_, state) =>
  over(
    lensPath(['viewState']),
    merge(__, {
      loadedStatus: false,
      loadStatusError: null,
    }),
    state
  );

const loadStatusFulfilled = (payload, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
    loadedStatus: true,
    loadStatusError: null,
  },
  statusState: {
    ...state.statusState,
    isEnabled: payload.experimentalFeatures.firewallAutoUnquarantine,
  },
});

const loadStatusFailed = (payload, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
    loadedStatus: true,
    loadStatusError: payload,
    loadError: state.viewState.loadError || payload,
  },
});

const loadReleaseQuarantineSummaryRequested = (_, state) =>
  over(
    lensPath(['autoUnquarantineState', 'viewState']),
    merge(__, {
      loadedReleaseQuarantineSummary: false,
      loadReleaseQuarantineSummaryError: null,
    }),
    state
  );

const loadReleaseQuarantineSummaryFulfilled = (payload, state) => ({
  ...state,
  autoUnquarantineState: {
    ...state.autoUnquarantineState,
    viewState: {
      ...state.autoUnquarantineState.viewState,
      loadedReleaseQuarantineSummary: true,
      loadReleaseQuarantineSummaryError: null,
      autoReleaseQuarantineCountMTD: payload.autoReleaseQuarantineCountMTD.toString(),
      autoReleaseQuarantineCountYTD: payload.autoReleaseQuarantineCountYTD.toString(),
    },
  },
});

const loadReleaseQuarantineSummaryFailed = (payload, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
    loadError: state.viewState.loadError || payload,
  },
  autoUnquarantineState: {
    ...state.autoUnquarantineState,
    viewState: {
      ...state.autoUnquarantineState.viewState,
      loadedReleaseQuarantineSummary: true,
      loadReleaseQuarantineSummaryError: payload,
    },
  },
});

const loadReleaseQuarantineListRequested = (_, state) =>
  over(
    lensPath(['autoUnquarantineState', 'autoUnquarantineGridState']),
    merge(__, {
      loadedReleaseQuarantineList: false,
    }),
    state
  );

const loadReleaseQuarantineListFulfilled = (payload, state) =>
  over(
    lensPath(['autoUnquarantineState', 'autoUnquarantineGridState']),
    merge(__, {
      loadedReleaseQuarantineList: true,
      releaseQuarantineList: payload.results,
      releaseQuarantinePageCount: payload.pageCount,
      currentPage: payload.pageCount === 0 ? null : payload.page - 1,
    }),
    state
  );

const loadReleaseQuarantineListFailed = (payload, state) => ({
  ...state,
  autoUnquarantineState: {
    ...state.autoUnquarantineState,
    autoUnquarantineGridState: {
      ...state.autoUnquarantineState.autoUnquarantineGridState,
      loadAutoUnquarantineGridError: payload,
      loadedReleaseQuarantineList: true,
      releaseQuarantineList: [],
    },
  },
});

const loadPoliciesRequested = (_, state) =>
  over(
    lensPath(['autoUnquarantineState', 'autoUnquarantineGridState']),
    merge(__, {
      loadedPolicies: false,
    }),
    state
  );

const loadPoliciesFulfilled = (payload, state) =>
  over(
    lensPath(['autoUnquarantineState', 'autoUnquarantineGridState']),
    merge(__, {
      loadedPolicies: true,
      policies: payload.policies.filter((policy) => policy.ownerId === 'ROOT_ORGANIZATION_ID'),
    }),
    state
  );

const loadPoliciesFailed = (payload, state) => ({
  ...state,
  autoUnquarantineState: {
    ...state.autoUnquarantineState,
    autoUnquarantineGridState: {
      ...state.autoUnquarantineState.autoUnquarantineGridState,
      loadedPolicies: true,
      policies: [],
    },
  },
});

const setAutoUnquarantineGridPage = (payload, state) =>
  over(
    lensPath(['autoUnquarantineState', 'autoUnquarantineGridState']),
    merge(__, {
      currentPage: payload.currentPage,
    }),
    state
  );

const setAutoUnquarantineGridSorting = (payload, state) =>
  over(
    lensPath(['autoUnquarantineState', 'autoUnquarantineGridState']),
    merge(__, {
      sortDir: payload.sortDir,
      sortField: payload.sortField,
    }),
    state
  );

const setAutoUnquarantineGridPolicyFilter = (payload, state) =>
  over(
    lensPath(['autoUnquarantineState', 'autoUnquarantineGridState']),
    merge(__, {
      filterPolicyId: payload.policyId,
    }),
    state
  );

const setShowConfigurationModal = (payload, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
    isShowConfigurationModal: payload,
  },
});

const saveConfigurationFulfilled = (payload, state) => ({
  ...state,
  autoUnquarantineState: {
    ...state.autoUnquarantineState,
    viewState: {
      ...state.autoUnquarantineState.viewState,
      enabledPolicyConditionTypesCount: numberOfEnabledPolicyConditionTypesCount(payload),
      totalPolicyConditionTypesCount: payload.length,
    },
  },
  configurationState: {
    autoUnquarantineEnabled: numberOfEnabledPolicyConditionTypesCount(payload) > 0,
  },
});

const loadConfigurationRequested = (_, state) =>
  over(
    lensPath(['autoUnquarantineState', 'viewState']),
    merge(__, {
      loadedConfiguration: false,
      loadConfigurationError: null,
    }),
    state
  );

const loadConfigurationFulfilled = (payload, state) => ({
  ...state,
  autoUnquarantineState: {
    ...state.autoUnquarantineState,
    viewState: {
      ...state.autoUnquarantineState.viewState,
      loadedConfiguration: true,
      loadConfigurationError: null,
      enabledPolicyConditionTypesCount: numberOfEnabledPolicyConditionTypesCount(payload),
      totalPolicyConditionTypesCount: payload.length,
    },
  },
  configurationState: {
    autoUnquarantineEnabled: numberOfEnabledPolicyConditionTypesCount(payload) > 0,
  },
});

const loadConfigurationFailed = (payload, state) => {
  const newState = over(
    lensPath(['autoUnquarantineState', 'viewState']),
    merge(__, {
      loadedConfiguration: true,
      loadConfigurationError: payload,
    }),
    state
  );
  return pathSet(['viewState', 'loadError'], newState.viewState.loadError || payload, newState);
};

function numberOfEnabledPolicyConditionTypesCount(payload) {
  return payload.filter(function (conditionType) {
    return conditionType.autoReleaseQuarantineEnabled === true;
  }).length;
}

const quarantineSummaryRequested = (payload, state) => ({
  ...state,
  quarantineSummaryState: {
    ...state.quarantineSummaryState,
    viewState: {
      ...state.quarantineSummaryState.viewState,
      loadedQuarantineSummary: false,
      loadQuarantineSummaryError: null,
    },
  },
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
      quarantinedComponentCount: payload.quarantinedComponentCount,
    },
  },
});

const quarantineSummaryFailed = (payload, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
    loadError: state.viewState.loadError || payload,
  },
  quarantineSummaryState: {
    ...state.quarantineSummaryState,
    viewState: {
      ...state.quarantineSummaryState.viewState,
      loadedQuarantineSummary: true,
      loadQuarantineSummaryError: payload,
    },
  },
});

const reducerActionMap = {
  [FIREWALL_LOAD_DATA_REQUESTED]: always(initialState),
  [FIREWALL_LOAD_STATUS_REQUESTED]: loadStatusRequested,
  [FIREWALL_LOAD_STATUS_FAILED]: loadStatusFailed,
  [FIREWALL_LOAD_STATUS_FULFILLED]: loadStatusFulfilled,
  [FIREWALL_SET_SHOW_CONFIGURATION_MODAL]: setShowConfigurationModal,
  [FIREWALL_LOAD_CONFIGURATION_REQUESTED]: loadConfigurationRequested,
  [FIREWALL_LOAD_CONFIGURATION_FULFILLED]: loadConfigurationFulfilled,
  [FIREWALL_LOAD_CONFIGURATION_FAILED]: loadConfigurationFailed,
  [FIREWALL_SAVE_CONFIGURATION_FULFILLED]: saveConfigurationFulfilled,
  [FIREWALL_RELEASE_QUARANTINE_SUMMARY_REQUESTED]: loadReleaseQuarantineSummaryRequested,
  [FIREWALL_RELEASE_QUARANTINE_SUMMARY_FAILED]: loadReleaseQuarantineSummaryFailed,
  [FIREWALL_RELEASE_QUARANTINE_SUMMARY_FULFILLED]: loadReleaseQuarantineSummaryFulfilled,
  [FIREWALL_RELEASE_QUARANTINE_LIST_REQUESTED]: loadReleaseQuarantineListRequested,
  [FIREWALL_RELEASE_QUARANTINE_LIST_FAILED]: loadReleaseQuarantineListFailed,
  [FIREWALL_RELEASE_QUARANTINE_LIST_FULFILLED]: loadReleaseQuarantineListFulfilled,
  [FIREWALL_AUTO_UNQUARANTINE_GRID_SET_PAGE]: setAutoUnquarantineGridPage,
  [FIREWALL_AUTO_UNQUARANTINE_GRID_SET_SORTING]: setAutoUnquarantineGridSorting,
  [FIREWALL_AUTO_UNQUARANTINE_GRID_SET_FILTER]: setAutoUnquarantineGridPolicyFilter,
  [FIREWALL_POLICIES_REQUESTED]: loadPoliciesRequested,
  [FIREWALL_POLICIES_FAILED]: loadPoliciesFailed,
  [FIREWALL_POLICIES_FULFILLED]: loadPoliciesFulfilled,
  [FIREWALL_QUARANTINE_SUMMARY_REQUESTED]: quarantineSummaryRequested,
  [FIREWALL_QUARANTINE_SUMMARY_FULFILLED]: quarantineSummaryFulfilled,
  [FIREWALL_QUARANTINE_SUMMARY_FAILED]: quarantineSummaryFailed,
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
