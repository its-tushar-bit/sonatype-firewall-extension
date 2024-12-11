/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createReducerFromActionMap } from '../util/reduxUtil';
import {
  FIREWALL_SET_SHOW_WELCOME_MODAL,
  FIREWALL_AUTO_UNQUARANTINE_DATA_REQUESTED,
  FIREWALL_AUTO_UNQUARANTINE_GRID_SET_PAGE,
  FIREWALL_AUTO_UNQUARANTINE_GRID_SET_SORTING,
  FIREWALL_LOAD_CONFIGURATION_FAILED,
  FIREWALL_LOAD_CONFIGURATION_FULFILLED,
  FIREWALL_LOAD_CONFIGURATION_REQUESTED,
  FIREWALL_LOAD_DATA_REQUESTED,
  FIREWALL_POLICIES_FAILED,
  FIREWALL_POLICIES_FULFILLED,
  FIREWALL_POLICIES_REQUESTED,
  FIREWALL_QUARANTINE_GRID_SET_POLICY_FILTER,
  FIREWALL_QUARANTINE_GRID_SET_COMPONENT_NAME_FILTER,
  FIREWALL_QUARANTINE_GRID_SET_REPOSITORY_PUBLIC_ID_FILTER,
  FIREWALL_QUARANTINE_GRID_SET_QUARANTINE_TIME_FILTER,
  FIREWALL_QUARANTINE_GRID_SET_LAST_UPDATED,
  FIREWALL_QUARANTINE_GRID_SET_PAGE,
  FIREWALL_QUARANTINE_GRID_SET_SORTING,
  FIREWALL_QUARANTINE_LIST_FAILED,
  FIREWALL_QUARANTINE_LIST_FULFILLED,
  FIREWALL_QUARANTINE_LIST_REQUESTED,
  FIREWALL_LOAD_TILE_METRICS_REQUESTED,
  FIREWALL_LOAD_TILE_METRICS_FAILED,
  FIREWALL_LOAD_TILE_METRICS_FULFILLED,
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
  FIREWALL_LOAD_COMPONENT_POLICY_VIOLATIONS_REQUESTED,
  FIREWALL_LOAD_COMPONENT_POLICY_VIOLATIONS_FULFILLED,
  FIREWALL_LOAD_COMPONENT_POLICY_VIOLATIONS_FAILED,
  FIREWALL_LOAD_COMPONENT_LICENSES_REQUESTED,
  FIREWALL_LOAD_COMPONENT_LICENSES_FULFILLED,
  FIREWALL_LOAD_COMPONENT_LICENSES_FAILED,
  FIREWALL_LOAD_EXISTING_WAIVERS_DATA_REQUESTED,
  FIREWALL_LOAD_EXISTING_WAIVERS_DATA_FULFILLED,
  FIREWALL_LOAD_EXISTING_WAIVERS_DATA_FAILED,
  FIREWALL_SHOW_MANAGE_WAIVER_PAGE,
  FIREWALL_LOAD_VIOLATION_DETAIL_FULFILLED,
  FIREWALL_LOAD_VIOLATION_DETAIL_REQUESTED,
  FIREWALL_LOAD_VIOLATION_DETAIL_FAILED,
  FIREWALL_REEVALUATE_COMPONENT_REQUESTED,
  FIREWALL_REEVALUATE_COMPONENT_FULFILLED,
  FIREWALL_REEVALUATE_COMPONENT_FAILED,
  FIREWALL_POLICIES_WITH_CONDITIONS_FAILED,
  FIREWALL_POLICIES_WITH_CONDITIONS_FULFILLED,
  FIREWALL_POLICIES_WITH_CONDITIONS_REQUESTED,
  FIREWALL_SET_SELECTED_POLICY_ID,
} from './firewallActions';
import { __, always, assoc, curry, dissoc, lensPath, lensProp, merge, over, prop } from 'ramda';
import { pathSet, propSet } from '../util/jsUtil';

export const initialState = Object.freeze({
  selectedPolicyId: null,
  showWelcomeModal: false,
  cip: Object.freeze({
    selectedComponent: null,
    selectedComponentIndex: null,
    displayedEntries: [],
  }),
  componentDetailsPage: Object.freeze({
    isLoadingComponentDetails: false,
    componentDetails: null,
    componentDetailsError: null,
    policyViolations: [],
    isLoadingPolicyViolations: false,
    policyViolationsError: null,
    firewallPolicyName: null,
    firewallThreatLevel: null,
    firewallViolationDetailsError: null,
    firewallViolationDetailsLoading: false,
    componentLicenses: {
      declaredLicenses: [],
      observedLicenses: [],
      effectiveLicenses: [],
      selectableLicenses: [],
      licenseOverride: [],
      allLicenses: [],
    },
    isLoadingComponentLicenses: false,
    componentLicensesError: null,
    policyExistingWaivers: null,
    isLoadExistingWaivers: false,
    existingWaiversError: null,
    showManageWaiverPage: false,
    violationDetails: [],
    hasWaivePermission: false,
  }),
  viewState: Object.freeze({
    isShowConfigurationModal: false,
    loadError: null,
  }),
  tileMetricsState: Object.freeze({
    loadedTileMetrics: false,
    loadTileMetricsError: null,
    componentsAutoReleased: 0,
    componentsQuarantined: 0,
    namespaceAttacksBlocked: 0,
    safeVersionsSelected: 0,
    supplyChainAttacksBlocked: 0,
    waivedComponents: 0,
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
    sortDir: 'desc',
    sortField: 'quarantineTime',
    filterPolicies: [],
    filterComponentName: '',
    filterRepositoryPublicId: '',
    filterQuarantineTime: null,
    lastUpdated: null,
  }),
});

const setShowWelcomeModal = (payload, state) => {
  return {
    ...state,
    showWelcomeModal: payload,
  };
};

const loadPoliciesWithConditionsRequested = (_, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
    loadError: null,
  },
});

const loadPoliciesWithConditionsFulfilled = (_, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
    loadError: null,
  },
});

const loadPoliciesWithConditionsFailed = (payload, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
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
      policies: payload.policies.filter(
        (policy) =>
          policy.ownerId === 'ROOT_ORGANIZATION_ID' ||
          policy.ownerType === 'REPOSITORY_CONTAINER' ||
          policy.ownerType === 'REPOSITORY_MANAGER' ||
          policy.ownerType === 'REPOSITORY'
      ),
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

const loadComponentPolicyViolationsRequested = (_, state) => ({
  ...state,
  componentDetailsPage: {
    ...state.componentDetailsPage,
    isLoadingPolicyViolations: true,
    policyViolationsError: null,
  },
});

const loadComponentPolicyViolationsFulfilled = (payload, state) => ({
  ...state,
  componentDetailsPage: {
    ...state.componentDetailsPage,
    policyViolations: payload,
    isLoadingPolicyViolations: false,
    policyViolationsError: null,
  },
});

const loadComponentPolicyViolationsFailed = (error, state) => ({
  ...state,
  componentDetailsPage: {
    ...state.componentDetailsPage,
    isLoadingPolicyViolations: false,
    policyViolationsError: error,
  },
});

const loadComponentLicensesRequested = (_, state) => ({
  ...state,
  componentDetailsPage: {
    ...state.componentDetailsPage,
    isLoadingComponentLicenses: true,
    componentLicensesError: null,
  },
});

const loadComponentLicensesFulfilled = function (payload, state) {
  return {
    ...state,
    componentDetailsPage: {
      ...state.componentDetailsPage,
      componentLicenses: payload,
      isLoadingComponentLicenses: false,
      componentLicensesError: null,
    },
  };
};

const loadComponentLicensesFailed = (error, state) => ({
  ...state,
  componentDetailsPage: {
    ...state.componentDetailsPage,
    isLoadingComponentLicenses: false,
    componentLicensesError: error,
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

const tileMetricsRequested = (_, state) => ({
  ...state,
  tileMetricsState: {
    ...state.tileMetricsState,
    loadedTileMetrics: false,
    loadTileMetricsError: null,
  },
});

const tileMetricsFulfilled = (payload, state) => ({
  ...state,
  tileMetricsState: {
    ...state.tileMetricsState,
    loadedTileMetrics: true,
    componentsAutoReleased: payload.COMPONENTS_AUTO_RELEASED.firewallMetricsValue,
    componentsQuarantined: payload.COMPONENTS_QUARANTINED.firewallMetricsValue,
    namespaceAttacksBlocked: payload.NAMESPACE_ATTACKS_BLOCKED.firewallMetricsValue,
    safeVersionsSelected: payload.SAFE_VERSIONS_SELECTED_AUTOMATICALLY.firewallMetricsValue,
    supplyChainAttacksBlocked: payload.SUPPLY_CHAIN_ATTACKS_BLOCKED.firewallMetricsValue,
    waivedComponents: payload.WAIVED_COMPONENTS.firewallMetricsValue,
  },
});

const tileMetricsFailed = (payload, state) => ({
  ...state,
  tileMetricsState: {
    ...state.tileMetricsState,
    loadedTileMetrics: true,
    loadTileMetricsError: payload,
  },
});

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
      currentPage: null,
      quarantineList: [],
      quarantinePageCount: 0,
    }),
    state
  );

const loadQuarantineListFulfilled = (payload, state) =>
  over(
    lensPath(['quarantineGridState']),
    merge(__, {
      loadedQuarantineList: true,
      currentPage: payload.pageCount === 0 ? null : payload.page - 1,
      quarantineList: payload.results.map((result) => renameKey('displayName', 'componentDisplayText', result)),
      quarantinePageCount: payload.pageCount,
    }),
    state
  );

const loadQuarantineListFailed = (payload, state) => ({
  ...state,
  quarantineGridState: {
    ...state.quarantineGridState,
    loadQuarantineGridError: payload,
    loadedQuarantineList: true,
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
    }),
    state
  );

const setQuarantineGridPolicyFilter = (payload, state) =>
  over(
    lensPath(['quarantineGridState']),
    merge(__, {
      filterPolicies: payload.policies,
    }),
    state
  );

const setQuarantineGridComponentNameFilter = (payload, state) =>
  over(
    lensPath(['quarantineGridState']),
    merge(__, {
      filterComponentName: payload.componentName,
    }),
    state
  );

const setQuarantineGridRepositoryPublicIdFilter = (payload, state) =>
  over(
    lensPath(['quarantineGridState']),
    merge(__, {
      filterRepositoryPublicId: payload.repositoryPublicId,
    }),
    state
  );

const setQuarantineGridQuarantineTimeFilter = (payload, state) =>
  over(
    lensPath(['quarantineGridState']),
    merge(__, {
      filterQuarantineTime: payload.quarantineTime,
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

function loadComponentDetailsRequested(_, state) {
  return {
    ...state,
    componentDetailsPage: {
      ...state.componentDetailsPage,
      isLoadingComponentDetails: true,
    },
  };
}

function loadComponentDetailsFulfilled(payload, state) {
  return {
    ...state,
    componentDetailsPage: {
      ...state.componentDetailsPage,
      isLoadingComponentDetails: false,
      componentDetails: { ...state.componentDetailsPage.componentDetails, ...payload },
      componentDetailsError: null,
    },
  };
}

function loadComponentDetailsFailed(payload, state) {
  return {
    ...state,
    componentDetailsPage: {
      ...state.componentDetailsPage,
      isLoadingComponentDetails: false,
      componentDetailsError: payload,
    },
  };
}

const loadExistingWaiversDataRequested = (_, state) => ({
  ...state,
  componentDetailsPage: {
    ...state.componentDetailsPage,
    policyExistingWaivers: null,
    isLoadExistingWaivers: true,
    existingWaiversError: null,
  },
});

const loadExistingWaiversDataFulfilled = (payload, state) => ({
  ...state,
  componentDetailsPage: {
    ...state.componentDetailsPage,
    policyExistingWaivers: payload,
    isLoadExistingWaivers: false,
    existingWaiversError: null,
  },
});

const loadExistingWaiversDataFailed = (error, state) => ({
  ...state,
  componentDetailsPage: {
    ...state.componentDetailsPage,
    isLoadExistingWaivers: false,
    existingWaiversError: error,
  },
});

const showManageWaiverPage = (payload, state) => ({
  ...state,
  componentDetailsPage: {
    ...state.componentDetailsPage,
    showManageWaiverPage: payload,
  },
});

const loadViolationDetailRequested = (_, state) => {
  return {
    ...state,
    componentDetailsPage: {
      ...state.componentDetailsPage,
      violationDetails: [],
      firewallViolationDetailsLoading: true,
      firewallViolationDetailsError: null,
    },
  };
};

const loadViolationDetailFulfilled = (payload, state) => {
  return {
    ...state,
    componentDetailsPage: {
      ...state.componentDetailsPage,
      violationDetails: payload,
      hasWaivePermission: payload?.hasWaivePermission || false,
      firewallPolicyName: payload.policyName,
      firewallThreatLevel: payload.threatLevel,
      firewallViolationDetailsLoading: false,
      firewallViolationDetailsError: null,
    },
  };
};

const loadViolationDetailFailed = (error, state) => ({
  ...state,
  componentDetailsPage: {
    ...state.componentDetailsPage,
    violationDetails: [],
    firewallViolationDetailsError: error,
    firewallViolationDetailsLoading: false,
  },
});

const reevaluateComponentRequested = (_, state) => ({
  ...state,
  componentDetailsPage: {
    ...state.componentDetailsPage,
    componentReevaluated: false,
    errorReevaluatingComponent: null,
  },
});

const reevaluateComponentFulfilled = (_, state) => ({
  ...state,
  componentDetailsPage: {
    ...state.componentDetailsPage,
    componentReevaluated: true,
    errorReevaluatingComponent: null,
  },
});

const reevaluateComponentFailed = (error, state) => ({
  ...state,
  componentDetailsPage: {
    ...state.componentDetailsPage,
    componentReevaluated: false,
    errorReevaluatingComponent: error,
  },
});

const setFirewallLoadDataRequested = (_, state) => {
  const sortAndFilterConfig = {
    sortDir: state.quarantineGridState.sortDir,
    sortField: state.quarantineGridState.sortField,
    filterPolicies: state.quarantineGridState.filterPolicies,
    filterComponentName: state.quarantineGridState.filterComponentName,
    filterRepositoryPublicId: state.quarantineGridState.filterRepositoryPublicId,
    filterQuarantineTime: state.quarantineGridState.filterQuarantineTime,
  };
  return {
    ...initialState,
    quarantineGridState: {
      ...initialState.quarantineGridState,
      ...sortAndFilterConfig,
    },
  };
};

const reducerActionMap = {
  [FIREWALL_SET_SHOW_WELCOME_MODAL]: setShowWelcomeModal,
  [FIREWALL_LOAD_DATA_REQUESTED]: setFirewallLoadDataRequested,
  [FIREWALL_SET_SHOW_CONFIGURATION_MODAL]: setShowConfigurationModal,
  [FIREWALL_LOAD_CONFIGURATION_REQUESTED]: loadConfigurationRequested,
  [FIREWALL_LOAD_CONFIGURATION_FULFILLED]: loadConfigurationFulfilled,
  [FIREWALL_LOAD_CONFIGURATION_FAILED]: loadConfigurationFailed,
  [FIREWALL_SAVE_CONFIGURATION_FULFILLED]: saveConfigurationFulfilled,
  [FIREWALL_RELEASE_QUARANTINE_SUMMARY_REQUESTED]: loadReleaseQuarantineSummaryRequested,
  [FIREWALL_RELEASE_QUARANTINE_SUMMARY_FAILED]: loadReleaseQuarantineSummaryFailed,
  [FIREWALL_RELEASE_QUARANTINE_SUMMARY_FULFILLED]: loadReleaseQuarantineSummaryFulfilled,
  [FIREWALL_LOAD_TILE_METRICS_REQUESTED]: tileMetricsRequested,
  [FIREWALL_LOAD_TILE_METRICS_FAILED]: tileMetricsFailed,
  [FIREWALL_LOAD_TILE_METRICS_FULFILLED]: tileMetricsFulfilled,
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
  [FIREWALL_QUARANTINE_GRID_SET_POLICY_FILTER]: setQuarantineGridPolicyFilter,
  [FIREWALL_QUARANTINE_GRID_SET_COMPONENT_NAME_FILTER]: setQuarantineGridComponentNameFilter,
  [FIREWALL_QUARANTINE_GRID_SET_REPOSITORY_PUBLIC_ID_FILTER]: setQuarantineGridRepositoryPublicIdFilter,
  [FIREWALL_QUARANTINE_GRID_SET_QUARANTINE_TIME_FILTER]: setQuarantineGridQuarantineTimeFilter,
  [FIREWALL_QUARANTINE_GRID_SET_LAST_UPDATED]: setQuarantineGridLastUpdated,
  [FIREWALL_POLICIES_REQUESTED]: loadPoliciesRequested,
  [FIREWALL_POLICIES_FAILED]: loadPoliciesFailed,
  [FIREWALL_POLICIES_FULFILLED]: loadPoliciesFulfilled,
  [FIREWALL_POLICIES_WITH_CONDITIONS_REQUESTED]: loadPoliciesWithConditionsRequested,
  [FIREWALL_POLICIES_WITH_CONDITIONS_FULFILLED]: loadPoliciesWithConditionsFulfilled,
  [FIREWALL_POLICIES_WITH_CONDITIONS_FAILED]: loadPoliciesWithConditionsFailed,
  [FIREWALL_QUARANTINE_SUMMARY_REQUESTED]: quarantineSummaryRequested,
  [FIREWALL_QUARANTINE_SUMMARY_FULFILLED]: quarantineSummaryFulfilled,
  [FIREWALL_QUARANTINE_SUMMARY_FAILED]: quarantineSummaryFailed,
  [FIREWALL_SELECT_COMPONENT]: setSelectedComponent,
  [FIREWALL_COMPONENT_DETAILS_REQUESTED]: loadComponentDetailsRequested,
  [FIREWALL_COMPONENT_DETAILS_FULFILLED]: loadComponentDetailsFulfilled,
  [FIREWALL_COMPONENT_DETAILS_FAILED]: loadComponentDetailsFailed,
  [FIREWALL_LOAD_COMPONENT_POLICY_VIOLATIONS_REQUESTED]: loadComponentPolicyViolationsRequested,
  [FIREWALL_LOAD_COMPONENT_POLICY_VIOLATIONS_FULFILLED]: loadComponentPolicyViolationsFulfilled,
  [FIREWALL_LOAD_COMPONENT_POLICY_VIOLATIONS_FAILED]: loadComponentPolicyViolationsFailed,
  [FIREWALL_LOAD_COMPONENT_LICENSES_REQUESTED]: loadComponentLicensesRequested,
  [FIREWALL_LOAD_COMPONENT_LICENSES_FULFILLED]: loadComponentLicensesFulfilled,
  [FIREWALL_LOAD_COMPONENT_LICENSES_FAILED]: loadComponentLicensesFailed,
  [FIREWALL_LOAD_EXISTING_WAIVERS_DATA_REQUESTED]: loadExistingWaiversDataRequested,
  [FIREWALL_LOAD_EXISTING_WAIVERS_DATA_FULFILLED]: loadExistingWaiversDataFulfilled,
  [FIREWALL_LOAD_EXISTING_WAIVERS_DATA_FAILED]: loadExistingWaiversDataFailed,
  [FIREWALL_SHOW_MANAGE_WAIVER_PAGE]: showManageWaiverPage,
  [FIREWALL_LOAD_VIOLATION_DETAIL_REQUESTED]: loadViolationDetailRequested,
  [FIREWALL_LOAD_VIOLATION_DETAIL_FULFILLED]: loadViolationDetailFulfilled,
  [FIREWALL_LOAD_VIOLATION_DETAIL_FAILED]: loadViolationDetailFailed,
  [FIREWALL_REEVALUATE_COMPONENT_REQUESTED]: reevaluateComponentRequested,
  [FIREWALL_REEVALUATE_COMPONENT_FULFILLED]: reevaluateComponentFulfilled,
  [FIREWALL_REEVALUATE_COMPONENT_FAILED]: reevaluateComponentFailed,
  [FIREWALL_SET_SELECTED_POLICY_ID]: propSet('selectedPolicyId'),
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
