/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createReducerFromActionMap } from '../util/reduxUtil';
import {
  FIREWALL_AUTO_UNQUARANTINE_DATA_REQUESTED,
  FIREWALL_AUTO_UNQUARANTINE_GRID_SET_PAGE,
  FIREWALL_AUTO_UNQUARANTINE_GRID_SET_SORTING,
  FIREWALL_CIP_MODAL_CLOSED,
  FIREWALL_CIP_MODAL_SHOW,
  FIREWALL_LOAD_CONFIGURATION_FAILED,
  FIREWALL_LOAD_CONFIGURATION_FULFILLED,
  FIREWALL_LOAD_CONFIGURATION_REQUESTED,
  FIREWALL_LOAD_DATA_REQUESTED,
  FIREWALL_POLICIES_FAILED,
  FIREWALL_POLICIES_FULFILLED,
  FIREWALL_POLICIES_REQUESTED,
  FIREWALL_QUARANTINE_GRID_SET_FILTER,
  FIREWALL_QUARANTINE_GRID_SET_LAST_UPDATED,
  FIREWALL_QUARANTINE_GRID_SET_PAGE,
  FIREWALL_QUARANTINE_GRID_SET_SORTING,
  FIREWALL_QUARANTINE_LIST_FAILED,
  FIREWALL_QUARANTINE_LIST_FULFILLED,
  FIREWALL_QUARANTINE_LIST_REQUESTED,
  FIREWALL_QUARANTINE_SUMMARY_FAILED,
  FIREWALL_QUARANTINE_SUMMARY_FULFILLED,
  FIREWALL_QUARANTINE_SUMMARY_REQUESTED,
  FIREWALL_RELEASE_QUARANTINE_LIST_FAILED,
  FIREWALL_RELEASE_QUARANTINE_LIST_FULFILLED,
  FIREWALL_RELEASE_QUARANTINE_LIST_REQUESTED,
  FIREWALL_RELEASE_QUARANTINE_SUMMARY_FAILED,
  FIREWALL_RELEASE_QUARANTINE_SUMMARY_FULFILLED,
  FIREWALL_RELEASE_QUARANTINE_SUMMARY_REQUESTED,
  FIREWALL_SAVE_CONFIGURATION_FULFILLED,
  FIREWALL_SELECT_COMPONENT,
  FIREWALL_SET_SHOW_CONFIGURATION_MODAL,
  FIREWALL_COMPONENT_DETAILS_REQUESTED,
  FIREWALL_COMPONENT_DETAILS_FULFILLED,
  FIREWALL_COMPONENT_DETAILS_FAILED,
} from './firewallActions';
import { __, always, assoc, curry, dissoc, lensPath, lensProp, merge, over, prop } from 'ramda';
import { pathSet } from '../util/jsUtil';

const initialState = Object.freeze({
  cip: Object.freeze({
    showCipModal: false,
    selectedComponent: null,
    selectedComponentIndex: null,
    displayedEntries: [],
  }),
  cdp: Object.freeze({
    isLoadingComponentDetails: false,
    componentDetails: null,
    componentDetailsError: null,
  }),
  viewState: Object.freeze({
    isShowConfigurationModal: false,
    loadError: null,
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
      releaseQuarantineList: [],
      releaseQuarantinePageCount: 0,
      pageSize: 12,
      currentPage: null,
      sortDir: null,
      sortField: null,
    }),
  }),
  policiesState: Object.freeze({
    loadedPolicies: false,
    policies: [],
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
  quarantineGridState: Object.freeze({
    loadQuarantineGridError: null,
    loadedQuarantineList: false,
    quarantineList: [],
    quarantinePageCount: 0,
    pageSize: 12,
    currentPage: null,
    sortDir: null,
    sortField: null,
    filterPolicy: undefined,
    lastUpdated: null,
  }),
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
      loadAutoUnquarantineGridError: null,
      releaseQuarantineList: [],
    }),
    state
  );

const renameKey = curry((oldKey, newKey, obj) => assoc(newKey, prop(oldKey, obj), dissoc(oldKey, obj)));

const loadReleaseQuarantineListFulfilled = (payload, state) =>
  over(
    lensPath(['autoUnquarantineState', 'autoUnquarantineGridState']),
    merge(__, {
      loadedReleaseQuarantineList: true,
      releaseQuarantineList: payload.results.map((result) => renameKey('displayName', 'componentDisplayText', result)),
      releaseQuarantinePageCount: payload.pageCount,
      currentPage: payload.pageCount === 0 ? null : payload.page - 1,
    }),
    state
  );

const loadReleaseQuarantineListFailed = (payload, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
    loadError: state.viewState.loadError || payload,
  },
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
    lensProp('policiesState'),
    merge(__, {
      loadedPolicies: false,
      policies: [],
    }),
    state
  );

const loadPoliciesFulfilled = (payload, state) =>
  over(
    lensPath(['policiesState']),
    merge(__, {
      loadedPolicies: true,
      policies: payload.policies.filter((policy) => policy.ownerId === 'ROOT_ORGANIZATION_ID'),
    }),
    state
  );

const loadPoliciesFailed = (payload, state) => ({
  ...state,
  policiesState: {
    loadedPolicies: true,
    policies: [],
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
      loadedReleaseQuarantineList: false,
      releaseQuarantineList: [],
      releaseQuarantinePageCount: 0,
      currentPage: null,
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

const loadQuarantineListRequested = (_, state) =>
  over(
    lensPath(['quarantineGridState']),
    merge(__, {
      loadedQuarantineList: false,
      loadQuarantineGridError: null,
    }),
    state
  );

const loadQuarantineListFulfilled = (payload, state) =>
  over(
    lensPath(['quarantineGridState']),
    merge(__, {
      loadedQuarantineList: true,
      quarantineList: payload.results.map((result) => renameKey('displayName', 'componentDisplayText', result)),
      quarantinePageCount: payload.pageCount,
      currentPage: payload.pageCount === 0 ? null : payload.page - 1,
    }),
    state
  );

const loadQuarantineListFailed = (payload, state) => ({
  ...state,
  quarantineGridState: {
    ...state.quarantineGridState,
    loadQuarantineGridError: payload,
    loadedQuarantineList: true,
    quarantineList: [],
  },
});

const setQuarantineGridPage = (payload, state) =>
  over(
    lensPath(['quarantineGridState']),
    merge(__, {
      currentPage: payload.currentPage,
    }),
    state
  );

const setQuarantineGridSorting = (payload, state) =>
  over(
    lensPath(['quarantineGridState']),
    merge(__, {
      sortDir: payload.sortDir,
      sortField: payload.sortField,
      currentPage: null,
      loadedQuarantineList: false,
      quarantineList: [],
      quarantinePageCount: 0,
    }),
    state
  );

const setQuarantineGridPolicyFilter = (payload, state) =>
  over(
    lensPath(['quarantineGridState']),
    merge(__, {
      filterPolicy: payload.policy,
      currentPage: null,
      loadedQuarantineList: false,
      quarantineList: [],
      quarantinePageCount: 0,
    }),
    state
  );

const setQuarantineGridLastUpdated = (payload, state) =>
  over(
    lensPath(['quarantineGridState']),
    merge(__, {
      lastUpdated: payload.lastUpdated,
    }),
    state
  );

function setSelectedComponent(payload, state) {
  return {
    ...state,
    cip: {
      ...state.cip,
      selectedComponent: payload.component,
      selectedComponentIndex: payload.componentIndex,
      displayedEntries: payload.components,
    },
  };
}

function cipModalClosed(_, state) {
  return {
    ...state,
    cip: {
      showCipModal: false,
      selectedComponent: null,
      selectedComponentIndex: null,
      displayedEntries: [],
    },
  };
}

function cipModalShow(_, state) {
  return {
    ...state,
    cip: {
      ...state.cip,
      showCipModal: true,
    },
  };
}

function loadComponentDetailsRequested(_, state) {
  return {
    ...state,
    cdp: {
      ...state.cdp,
      isLoadingComponentDetails: true,
    },
  };
}

function loadComponentDetailsFulfilled(payload, state) {
  return {
    ...state,
    cdp: {
      ...state.cdp,
      isLoadingComponentDetails: false,
      componentDetails: { ...state.cdp.componentDetails, ...payload },
      componentDetailsError: null,
    },
  };
}

function loadComponentDetailsFailed(payload, state) {
  return {
    ...state,
    cdp: {
      ...state.cdp,
      isLoadingComponentDetails: false,
      componentDetailsError: payload,
    },
  };
}

const reducerActionMap = {
  [FIREWALL_LOAD_DATA_REQUESTED]: always(initialState),
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
  [FIREWALL_AUTO_UNQUARANTINE_DATA_REQUESTED]: always(initialState),
  [FIREWALL_AUTO_UNQUARANTINE_GRID_SET_PAGE]: setAutoUnquarantineGridPage,
  [FIREWALL_AUTO_UNQUARANTINE_GRID_SET_SORTING]: setAutoUnquarantineGridSorting,
  [FIREWALL_QUARANTINE_LIST_REQUESTED]: loadQuarantineListRequested,
  [FIREWALL_QUARANTINE_LIST_FAILED]: loadQuarantineListFailed,
  [FIREWALL_QUARANTINE_LIST_FULFILLED]: loadQuarantineListFulfilled,
  [FIREWALL_QUARANTINE_GRID_SET_PAGE]: setQuarantineGridPage,
  [FIREWALL_QUARANTINE_GRID_SET_SORTING]: setQuarantineGridSorting,
  [FIREWALL_QUARANTINE_GRID_SET_FILTER]: setQuarantineGridPolicyFilter,
  [FIREWALL_QUARANTINE_GRID_SET_LAST_UPDATED]: setQuarantineGridLastUpdated,
  [FIREWALL_POLICIES_REQUESTED]: loadPoliciesRequested,
  [FIREWALL_POLICIES_FAILED]: loadPoliciesFailed,
  [FIREWALL_POLICIES_FULFILLED]: loadPoliciesFulfilled,
  [FIREWALL_QUARANTINE_SUMMARY_REQUESTED]: quarantineSummaryRequested,
  [FIREWALL_QUARANTINE_SUMMARY_FULFILLED]: quarantineSummaryFulfilled,
  [FIREWALL_QUARANTINE_SUMMARY_FAILED]: quarantineSummaryFailed,
  [FIREWALL_SELECT_COMPONENT]: setSelectedComponent,
  [FIREWALL_CIP_MODAL_CLOSED]: cipModalClosed,
  [FIREWALL_CIP_MODAL_SHOW]: cipModalShow,
  [FIREWALL_COMPONENT_DETAILS_REQUESTED]: loadComponentDetailsRequested,
  [FIREWALL_COMPONENT_DETAILS_FULFILLED]: loadComponentDetailsFulfilled,
  [FIREWALL_COMPONENT_DETAILS_FAILED]: loadComponentDetailsFailed,
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
