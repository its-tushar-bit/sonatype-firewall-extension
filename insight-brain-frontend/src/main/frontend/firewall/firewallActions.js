/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import { noPayloadActionCreator, payloadParamActionCreator } from '../util/reduxUtil';
import { convertToWaiverViolationFormat } from '../util/waiverUtils';
import {
  getComponentDetailsUrl,
  getComponentMultiLicensesUrl,
  getComponentPolicyViolationsUrl,
  getComponentWaivers,
  getFirewallConfigurationUrl,
  getFirewallTileMetricsUrl,
  getFirewallQuarantineListUrl,
  getFirewallQuarantineSummaryUrl,
  getFirewallReleaseQuarantineListUrl,
  getFirewallReleaseQuarantineSummaryUrl,
  getLicenseOverrideUrl,
  getLicensesWithSyntheticFilterUrl,
  getPoliciesUrl,
  getPoliciesWithProprietaryNameConflictAndSecurityVulnerabilityCategoryMaliciousCodeUrl,
  getReevaluateComponentUrl,
  getRepositoryPolicyViolationUrl,
  getVersionGraphUrl,
} from '../util/CLMLocation';
import { Messages } from '../utilAngular/CommonServices';
import { stateGo } from '../reduxUiRouter/routerActions';
import { actions as componentDetailsLicenseDetectionsTileActions } from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LicenseDetectionsTile/licenseDetectionsTileSlice';
import { selectRepositoryId, selectIsStandaloneFirewall } from 'MainRoot/reduxUiRouter/routerSelectors';
import { actions as componentDetailsActions } from 'MainRoot/componentDetails/componentDetailsSlice';
import { selectFirewallComponentDetailsPageRouteParams } from 'MainRoot/firewall/firewallSelectors';
import { checkPermissions } from 'MainRoot/util/authorizationUtil';
import { getShowWelcomeModalFromStore, removeShowWelcomeModalFromStore } from './firewallWelcomeModalStore';
import { loadApplicableWaivers } from 'MainRoot/violation/violationActions';

export const FIREWALL_SET_SHOW_WELCOME_MODAL = 'FIREWALL_SET_SHOW_WELCOME_MODAL';

export const setShowWelcomeModal = payloadParamActionCreator(FIREWALL_SET_SHOW_WELCOME_MODAL);

export const FIREWALL_LOAD_DATA_REQUESTED = 'FIREWALL_LOAD_DATA_REQUESTED';

const loadFirewallDataRequested = noPayloadActionCreator(FIREWALL_LOAD_DATA_REQUESTED);

export const FIREWALL_AUTO_UNQUARANTINE_DATA_REQUESTED = 'FIREWALL_AUTO_UNQUARANTINE_DATA_REQUESTED';

const loadAutoUnquarantineDataRequested = noPayloadActionCreator(FIREWALL_AUTO_UNQUARANTINE_DATA_REQUESTED);

export const FIREWALL_SET_SHOW_CONFIGURATION_MODAL = 'FIREWALL_SET_SHOW_CONFIGURATION_MODAL';

const setShowConfigurationModal = payloadParamActionCreator(FIREWALL_SET_SHOW_CONFIGURATION_MODAL);

export const FIREWALL_LOAD_CONFIGURATION_REQUESTED = 'FIREWALL_LOAD_CONFIGURATION_REQUESTED';
export const FIREWALL_LOAD_CONFIGURATION_FULFILLED = 'FIREWALL_LOAD_CONFIGURATION_FULFILLED';
export const FIREWALL_LOAD_CONFIGURATION_FAILED = 'FIREWALL_LOAD_CONFIGURATION_FAILED';

const loadConfigurationRequested = noPayloadActionCreator(FIREWALL_LOAD_CONFIGURATION_REQUESTED);
const loadConfigurationFulfilled = payloadParamActionCreator(FIREWALL_LOAD_CONFIGURATION_FULFILLED);
const loadConfigurationFailed = payloadParamActionCreator(FIREWALL_LOAD_CONFIGURATION_FAILED);

export const FIREWALL_SAVE_CONFIGURATION_REQUESTED = 'FIREWALL_SAVE_CONFIGURATION_REQUESTED';
export const FIREWALL_SAVE_CONFIGURATION_FAILED = 'FIREWALL_SAVE_CONFIGURATION_FAILED';
export const FIREWALL_SAVE_CONFIGURATION_FULFILLED = 'FIREWALL_SAVE_CONFIGURATION_FULFILLED';

const saveConfigurationRequested = noPayloadActionCreator(FIREWALL_SAVE_CONFIGURATION_REQUESTED);
const saveConfigurationFulfilled = payloadParamActionCreator(FIREWALL_SAVE_CONFIGURATION_FULFILLED);
const saveConfigurationFailed = payloadParamActionCreator(FIREWALL_SAVE_CONFIGURATION_FAILED);

export const FIREWALL_TOGGLE_AUTO_UNQUARANTINE_ENABLED = 'FIREWALL_TOGGLE_AUTO_UNQUARANTINE_ENABLED';
export const FIREWALL_TOGGLE_AUTO_UNQUARANTINE_ALL = 'FIREWALL_TOGGLE_AUTO_UNQUARANTINE_ALL';
export const FIREWALL_CONFIGURATION_SAVE_MASK_TIMER_DONE = 'FIREWALL_CONFIGURATION_SAVE_MASK_TIMER_DONE';

export const toggleAutoUnquarantineEnabled = payloadParamActionCreator(FIREWALL_TOGGLE_AUTO_UNQUARANTINE_ENABLED);
const configurationSaveMaskTimerDone = noPayloadActionCreator(FIREWALL_CONFIGURATION_SAVE_MASK_TIMER_DONE);

export const FIREWALL_RELEASE_QUARANTINE_SUMMARY_REQUESTED = 'FIREWALL_RELEASE_QUARANTINE_SUMMARY_REQUESTED';
export const FIREWALL_RELEASE_QUARANTINE_SUMMARY_FULFILLED = 'FIREWALL_RELEASE_QUARANTINE_SUMMARY_FULFILLED';
export const FIREWALL_RELEASE_QUARANTINE_SUMMARY_FAILED = 'FIREWALL_RELEASE_QUARANTINE_SUMMARY_FAILED';

const loadReleaseQuarantineSummaryRequested = noPayloadActionCreator(FIREWALL_RELEASE_QUARANTINE_SUMMARY_REQUESTED);
const loadReleaseQuarantineSummaryFulfilled = payloadParamActionCreator(FIREWALL_RELEASE_QUARANTINE_SUMMARY_FULFILLED);
const loadReleaseQuarantineSummaryFailed = payloadParamActionCreator(FIREWALL_RELEASE_QUARANTINE_SUMMARY_FAILED);

export const FIREWALL_RELEASE_QUARANTINE_LIST_REQUESTED = 'FIREWALL_RELEASE_QUARANTINE_LIST_REQUESTED';
export const FIREWALL_RELEASE_QUARANTINE_LIST_FULFILLED = 'FIREWALL_RELEASE_QUARANTINE_LIST_FULFILLED';
export const FIREWALL_RELEASE_QUARANTINE_LIST_FAILED = 'FIREWALL_RELEASE_QUARANTINE_LIST_FAILED';

const loadReleaseQuarantineListRequested = noPayloadActionCreator(FIREWALL_RELEASE_QUARANTINE_LIST_REQUESTED);
const loadReleaseQuarantineListFulfilled = payloadParamActionCreator(FIREWALL_RELEASE_QUARANTINE_LIST_FULFILLED);
const loadReleaseQuarantineListFailed = payloadParamActionCreator(FIREWALL_RELEASE_QUARANTINE_LIST_FAILED);

export const FIREWALL_QUARANTINE_LIST_REQUESTED = 'FIREWALL_QUARANTINE_LIST_REQUESTED';
export const FIREWALL_QUARANTINE_LIST_FULFILLED = 'FIREWALL_QUARANTINE_LIST_FULFILLED';
export const FIREWALL_QUARANTINE_LIST_FAILED = 'FIREWALL_QUARANTINE_LIST_FAILED';

const loadQuarantineListRequested = noPayloadActionCreator(FIREWALL_QUARANTINE_LIST_REQUESTED);
const loadQuarantineListFulfilled = payloadParamActionCreator(FIREWALL_QUARANTINE_LIST_FULFILLED);
const loadQuarantineListFailed = payloadParamActionCreator(FIREWALL_QUARANTINE_LIST_FAILED);

export const FIREWALL_POLICIES_REQUESTED = 'FIREWALL_POLICIES_REQUESTED';
export const FIREWALL_POLICIES_FULFILLED = 'FIREWALL_POLICIES_FULFILLED';
export const FIREWALL_POLICIES_FAILED = 'FIREWALL_POLICIES_FAILED';

const loadPoliciesRequested = noPayloadActionCreator(FIREWALL_POLICIES_REQUESTED);
const loadPoliciesFulfilled = payloadParamActionCreator(FIREWALL_POLICIES_FULFILLED);
const loadPoliciesFailed = payloadParamActionCreator(FIREWALL_POLICIES_FAILED);

export const FIREWALL_POLICIES_WITH_CONDITIONS_REQUESTED = 'FIREWALL_POLICIES_WITH_CONDITIONS_REQUESTED';
export const FIREWALL_POLICIES_WITH_CONDITIONS_FULFILLED = 'FIREWALL_POLICIES_WITH_CONDITIONS_FULFILLED';
export const FIREWALL_POLICIES_WITH_CONDITIONS_FAILED = 'FIREWALL_POLICIES_WITH_CONDITIONS_FAILED';

export const loadPoliciesWithConditionsRequested = noPayloadActionCreator(FIREWALL_POLICIES_WITH_CONDITIONS_REQUESTED);
export const loadPoliciesWithConditionsFulfilled = noPayloadActionCreator(FIREWALL_POLICIES_WITH_CONDITIONS_FULFILLED);
export const loadPoliciesWithConditionsFailed = payloadParamActionCreator(FIREWALL_POLICIES_WITH_CONDITIONS_FAILED);

export const FIREWALL_AUTO_UNQUARANTINE_GRID_SET_PAGE = 'FIREWALL_AUTO_UNQUARANTINE_GRID_SET_PAGE';
export const FIREWALL_AUTO_UNQUARANTINE_GRID_SET_SORTING = 'FIREWALL_AUTO_UNQUARANTINE_GRID_SET_SORTING';

export const FIREWALL_QUARANTINE_GRID_SET_PAGE = 'FIREWALL_QUARANTINE_GRID_SET_PAGE';
export const FIREWALL_QUARANTINE_GRID_SET_SORTING = 'FIREWALL_QUARANTINE_GRID_SET_SORTING';
export const FIREWALL_QUARANTINE_GRID_SET_POLICY_FILTER = 'FIREWALL_QUARANTINE_GRID_SET_POLICY_FILTER';
export const FIREWALL_QUARANTINE_GRID_SET_COMPONENT_NAME_FILTER = 'FIREWALL_QUARANTINE_GRID_SET_COMPONENT_NAME_FILTER';
export const FIREWALL_QUARANTINE_GRID_SET_REPOSITORY_PUBLIC_ID_FILTER =
  'FIREWALL_QUARANTINE_GRID_SET_REPOSITORY_PUBLIC_ID_FILTER';
export const FIREWALL_QUARANTINE_GRID_SET_QUARANTINE_TIME_FILTER =
  'FIREWALL_QUARANTINE_GRID_SET_QUARANTINE_TIME_FILTER';
export const FIREWALL_QUARANTINE_GRID_SET_LAST_UPDATED = 'FIREWALL_QUARANTINE_GRID_SET_LAST_UPDATED';

const quarantineGridSetPage = payloadParamActionCreator(FIREWALL_QUARANTINE_GRID_SET_PAGE);
const quarantineGridSetSorting = payloadParamActionCreator(FIREWALL_QUARANTINE_GRID_SET_SORTING);
const quarantineGridSetPolicyFilter = payloadParamActionCreator(FIREWALL_QUARANTINE_GRID_SET_POLICY_FILTER);
const quarantineGridSetComponentNameFilter = payloadParamActionCreator(
  FIREWALL_QUARANTINE_GRID_SET_COMPONENT_NAME_FILTER
);
const quarantineGridSetRepositoryPublicIdFilter = payloadParamActionCreator(
  FIREWALL_QUARANTINE_GRID_SET_REPOSITORY_PUBLIC_ID_FILTER
);
const quarantineGridSetQuarantineTimeFilter = payloadParamActionCreator(
  FIREWALL_QUARANTINE_GRID_SET_QUARANTINE_TIME_FILTER
);

export const FIREWALL_QUARANTINE_SUMMARY_REQUESTED = 'FIREWALL_QUARANTINE_SUMMARY_REQUESTED';
export const FIREWALL_QUARANTINE_SUMMARY_FULFILLED = 'FIREWALL_QUARANTINE_SUMMARY_FULFILLED';
export const FIREWALL_QUARANTINE_SUMMARY_FAILED = 'FIREWALL_QUARANTINE_SUMMARY_FAILED';

const quarantineSummaryRequested = noPayloadActionCreator(FIREWALL_QUARANTINE_SUMMARY_REQUESTED);
const quarantineSummaryFulfilled = payloadParamActionCreator(FIREWALL_QUARANTINE_SUMMARY_FULFILLED);
const quarantineSummaryFailed = payloadParamActionCreator(FIREWALL_QUARANTINE_SUMMARY_FAILED);

export const FIREWALL_COMPONENT_DETAILS_REQUESTED = 'FIREWALL_COMPONENT_DETAILS_REQUESTED';
export const FIREWALL_COMPONENT_DETAILS_FULFILLED = 'FIREWALL_COMPONENT_DETAILS_FULFILLED';
export const FIREWALL_COMPONENT_DETAILS_FAILED = 'FIREWALL_COMPONENT_DETAILS_FAILED';

export const loadComponentDetailsRequested = noPayloadActionCreator(FIREWALL_COMPONENT_DETAILS_REQUESTED);
export const loadComponentDetailsFulfilled = payloadParamActionCreator(FIREWALL_COMPONENT_DETAILS_FULFILLED);
export const loadComponentDetailsFailed = payloadParamActionCreator(FIREWALL_COMPONENT_DETAILS_FAILED);

export const FIREWALL_LOAD_VERSION_EXPLORER_DATA_REQUESTED = 'FIREWALL_LOAD_VERSION_EXPLORER_DATA_REQUESTED';
export const FIREWALL_LOAD_VERSION_EXPLORER_DATA_FULFILLED = 'FIREWALL_LOAD_VERSION_EXPLORER_DATA_FULFILLED';
export const FIREWALL_LOAD_VERSION_EXPLORER_DATA_FAILED = 'FIREWALL_LOAD_VERSION_EXPLORER_DATA_FAILED';

export const loadVersionExplorerDataRequested = noPayloadActionCreator(FIREWALL_LOAD_VERSION_EXPLORER_DATA_REQUESTED);
export const loadVersionExplorerDataFulfilled = payloadParamActionCreator(
  FIREWALL_LOAD_VERSION_EXPLORER_DATA_FULFILLED
);
export const loadVersionExplorerDataFailed = payloadParamActionCreator(FIREWALL_LOAD_VERSION_EXPLORER_DATA_FAILED);

export const FIREWALL_LOAD_COMPONENT_POLICY_VIOLATIONS_REQUESTED =
  'FIREWALL_LOAD_COMPONENT_POLICY_VIOLATIONS_REQUESTED';
export const FIREWALL_LOAD_COMPONENT_POLICY_VIOLATIONS_FULFILLED =
  'FIREWALL_LOAD_COMPONENT_POLICY_VIOLATIONS_FULFILLED';
export const FIREWALL_LOAD_COMPONENT_POLICY_VIOLATIONS_FAILED = 'FIREWALL_LOAD_COMPONENT_POLICY_VIOLATIONS_FAILED';

export const loadComponentPolicyViolationsRequested = noPayloadActionCreator(
  FIREWALL_LOAD_COMPONENT_POLICY_VIOLATIONS_REQUESTED
);
export const loadComponentPolicyViolationsFulfilled = payloadParamActionCreator(
  FIREWALL_LOAD_COMPONENT_POLICY_VIOLATIONS_FULFILLED
);
export const loadComponentPolicyViolationsFailed = payloadParamActionCreator(
  FIREWALL_LOAD_COMPONENT_POLICY_VIOLATIONS_FAILED
);

export const FIREWALL_LOAD_TILE_METRICS_REQUESTED = 'FIREWALL_LOAD_TILE_METRICS_REQUESTED';
export const FIREWALL_LOAD_TILE_METRICS_FULFILLED = 'FIREWALL_LOAD_TILE_METRICS_FULFILLED';
export const FIREWALL_LOAD_TILE_METRICS_FAILED = 'FIREWALL_LOAD_TILE_METRICS_FAILED';

export const loadTileMetricsRequested = noPayloadActionCreator(FIREWALL_LOAD_TILE_METRICS_REQUESTED);
export const loadTileMetricsFulfilled = payloadParamActionCreator(FIREWALL_LOAD_TILE_METRICS_FULFILLED);
export const loadTileMetricsFailed = payloadParamActionCreator(FIREWALL_LOAD_TILE_METRICS_FAILED);

export const FIREWALL_LOAD_COMPONENT_LICENSES_REQUESTED = 'FIREWALL_LOAD_COMPONENT_LICENSES_REQUESTED';
export const FIREWALL_LOAD_COMPONENT_LICENSES_FULFILLED = 'FIREWALL_LOAD_COMPONENT_LICENSES_FULFILLED';
export const FIREWALL_LOAD_COMPONENT_LICENSES_FAILED = 'FIREWALL_LOAD_COMPONENT_LICENSES_FAILED';

export const loadComponentLicensesRequested = noPayloadActionCreator(FIREWALL_LOAD_COMPONENT_LICENSES_REQUESTED);
export const loadComponentLicensesFulfilled = payloadParamActionCreator(FIREWALL_LOAD_COMPONENT_LICENSES_FULFILLED);
export const loadComponentLicensesFailed = payloadParamActionCreator(FIREWALL_LOAD_COMPONENT_LICENSES_FAILED);

export const FIREWALL_LOAD_EXISTING_WAIVERS_DATA_REQUESTED = 'FIREWALL_LOAD_EXISTING_WAIVERS_DATA_REQUESTED';
export const FIREWALL_LOAD_EXISTING_WAIVERS_DATA_FULFILLED = 'FIREWALL_LOAD_EXISTING_WAIVERS_DATA_FULFILLED';
export const FIREWALL_LOAD_EXISTING_WAIVERS_DATA_FAILED = 'FIREWALL_LOAD_EXISTING_WAIVERS_DATA_FAILED';

export const loadExistingWaiversDataRequested = noPayloadActionCreator(FIREWALL_LOAD_EXISTING_WAIVERS_DATA_REQUESTED);
export const loadExistingWaiversDataFulfilled = payloadParamActionCreator(
  FIREWALL_LOAD_EXISTING_WAIVERS_DATA_FULFILLED
);
export const loadExistingWaiversDataFailed = payloadParamActionCreator(FIREWALL_LOAD_EXISTING_WAIVERS_DATA_FAILED);

export const FIREWALL_SHOW_MANAGE_WAIVER_PAGE = 'FIREWALL_SHOW_MANAGE_WAIVER_PAGE';

const isShowManageWaiverPage = payloadParamActionCreator(FIREWALL_SHOW_MANAGE_WAIVER_PAGE);

export const FIREWALL_LOAD_VIOLATION_DETAIL_REQUESTED = 'FIREWALL_LOAD_VIOLATION_DETAIL_REQUESTED';
export const FIREWALL_LOAD_VIOLATION_DETAIL_FULFILLED = 'FIREWALL_LOAD_VIOLATION_DETAIL_FULFILLED';
export const FIREWALL_LOAD_VIOLATION_DETAIL_FAILED = 'FIREWALL_LOAD_VIOLATION_DETAIL_FAILED';

export const loadViolationDetailRequested = noPayloadActionCreator(FIREWALL_LOAD_VIOLATION_DETAIL_REQUESTED);
export const loadViolationDetailFulfilled = payloadParamActionCreator(FIREWALL_LOAD_VIOLATION_DETAIL_FULFILLED);
export const loadViolationDetailFailed = payloadParamActionCreator(FIREWALL_LOAD_VIOLATION_DETAIL_FAILED);

export const FIREWALL_SET_SHOW_WAIVER_MODAL = 'FIREWALL_SET_SHOW_WAIVER_MODAL';

export const FIREWALL_REEVALUATE_COMPONENT_REQUESTED = 'FIREWALL_REEVALUATE_COMPONENT_REQUESTED';
export const FIREWALL_REEVALUATE_COMPONENT_FULFILLED = 'FIREWALL_REEVALUATE_COMPONENT_FULFILLED';
export const FIREWALL_REEVALUATE_COMPONENT_FAILED = 'FIREWALL_REEVALUATE_COMPONENT_FAILED';

export const reevaluateComponentRequested = noPayloadActionCreator(FIREWALL_REEVALUATE_COMPONENT_REQUESTED);
export const reevaluateComponentFulfilled = noPayloadActionCreator(FIREWALL_REEVALUATE_COMPONENT_FULFILLED);
export const reevaluateComponentFailed = noPayloadActionCreator(FIREWALL_REEVALUATE_COMPONENT_FAILED);

export const FIREWALL_SET_SELECTED_POLICY_ID = 'FIREWALL_SET_SELECTED_POLICY_ID';

export const setFirewallSelectedPolicyId = payloadParamActionCreator(FIREWALL_SET_SELECTED_POLICY_ID);

export function initializeWelcomeModal() {
  return (dispatch) => {
    dispatch(setShowWelcomeModal(getShowWelcomeModalFromStore()));
  };
}

export function closeWelcomeModal() {
  return (dispatch) => {
    removeShowWelcomeModalFromStore();
    dispatch(setShowWelcomeModal(false));
  };
}

export function loadFirewallData() {
  return (dispatch) => {
    dispatch(loadFirewallDataRequested());
    dispatch(loadConfiguration());
    dispatch(loadTileMetrics());
    dispatch(loadReleaseQuarantineSummary());
    dispatch(loadQuarantineSummary());
    dispatch(loadQuarantineList());
    dispatch(loadPolicies());
  };
}

export function loadAutoUnquarantineData() {
  return (dispatch) => {
    dispatch(loadAutoUnquarantineDataRequested());
    dispatch(loadConfiguration());
    dispatch(loadReleaseQuarantineSummary());
    dispatch(loadReleaseQuarantineList());
  };
}

export function loadReleaseQuarantineSummary() {
  return function (dispatch) {
    dispatch(loadReleaseQuarantineSummaryRequested());
    return axios
      .get(getFirewallReleaseQuarantineSummaryUrl())
      .then(({ data }) => {
        dispatch(loadReleaseQuarantineSummaryFulfilled(data));
      })
      .catch((error) => {
        dispatch(loadReleaseQuarantineSummaryFailed(Messages.getHttpErrorMessage(error)));
      });
  };
}

export function loadReleaseQuarantineList() {
  return function (dispatch, getState) {
    let gridState = getState().firewall.autoUnquarantineState.autoUnquarantineGridState,
      apiPage = gridState.currentPage ? gridState.currentPage + 1 : 1,
      sortAsc = gridState.sortDir === null ? gridState.sortDir : gridState.sortDir === 'asc';

    dispatch(loadReleaseQuarantineListRequested());
    return axios
      .get(getFirewallReleaseQuarantineListUrl(apiPage, gridState.pageSize, gridState.sortField, sortAsc))
      .then(({ data }) => {
        dispatch(loadReleaseQuarantineListFulfilled(data));
      })
      .catch((error) => {
        dispatch(loadReleaseQuarantineListFailed(Messages.getHttpErrorMessage(error)));
      });
  };
}

export function loadPolicies() {
  return function (dispatch) {
    dispatch(loadPoliciesRequested());
    return axios
      .get(getPoliciesUrl())
      .then(({ data }) => {
        dispatch(loadPoliciesFulfilled(data));
      })
      .catch((error) => {
        dispatch(loadPoliciesFailed(Messages.getHttpErrorMessage(error)));
      });
  };
}

export function setQuarantineGridPolicyFilterWithProprietaryNameConflict() {
  return function (dispatch, getState) {
    dispatch(loadPoliciesWithConditionsRequested());
    const policies = getState().firewall.policiesState.policies;
    return axios
      .get(getPoliciesWithProprietaryNameConflictAndSecurityVulnerabilityCategoryMaliciousCodeUrl())
      .then(({ data: { proprietaryNameConflictPolicies } }) => {
        dispatch(loadPoliciesWithConditionsFulfilled());
        const policyIds = proprietaryNameConflictPolicies.map((p) => p.id);
        const selectedIds = policies.filter((p) => policyIds.includes(p.id)).map((p) => p.id);
        dispatch(setQuarantineGridPolicyFilter(selectedIds));
        dispatch(loadQuarantineList());
      })
      .catch((error) => {
        dispatch(loadPoliciesWithConditionsFailed(Messages.getHttpErrorMessage(error)));
      });
  };
}

export function setQuarantineGridPolicyFilterWithSecurityVulnerabilityCategoryMaliciousCode() {
  return function (dispatch, getState) {
    dispatch(loadPoliciesWithConditionsRequested());
    const policies = getState().firewall.policiesState.policies;
    return axios
      .get(getPoliciesWithProprietaryNameConflictAndSecurityVulnerabilityCategoryMaliciousCodeUrl())
      .then(({ data: { securityVulnerabilityCategoryMaliciousCodePolicies } }) => {
        dispatch(loadPoliciesWithConditionsFulfilled());
        const policyIds = securityVulnerabilityCategoryMaliciousCodePolicies.map((p) => p.id);
        const selectedIds = policies.filter((p) => policyIds.includes(p.id)).map((p) => p.id);
        dispatch(setQuarantineGridPolicyFilter(selectedIds));
        dispatch(loadQuarantineList());
      })
      .catch((error) => {
        dispatch(loadPoliciesWithConditionsFailed(Messages.getHttpErrorMessage(error)));
      });
  };
}

export const loadComponentPolicyViolations = (pathname, repository) => (dispatch) => {
  dispatch(loadComponentPolicyViolationsRequested());
  return axios
    .get(getComponentPolicyViolationsUrl(pathname, repository))
    .then(({ data }) => {
      dispatch(loadComponentPolicyViolationsFulfilled(data));
    })
    .catch((error) => {
      dispatch(loadComponentPolicyViolationsFailed(Messages.getHttpErrorMessage(error)));
    });
};

export const loadExistingWaiversData = (ownerType, publicId, hash) => (dispatch) => {
  dispatch(loadExistingWaiversDataRequested());
  return axios
    .get(getComponentWaivers(ownerType, publicId, hash))
    .then(({ data }) => {
      dispatch(loadExistingWaiversDataFulfilled(data));
    })
    .catch((error) => {
      dispatch(loadExistingWaiversDataFailed(Messages.getHttpErrorMessage(error)));
    });
};

export function loadConfiguration() {
  return function (dispatch) {
    dispatch(loadConfigurationRequested());
    return axios
      .get(getFirewallConfigurationUrl())
      .then(({ data }) => {
        dispatch(loadConfigurationFulfilled(data));
      })
      .catch((error) => {
        dispatch(loadConfigurationFailed(Messages.getHttpErrorMessage(error)));
      });
  };
}

export function saveConfiguration() {
  return (dispatch, getState) => {
    dispatch(saveConfigurationRequested());

    const serverData = getState().firewallConfigurationModal.formState.conditionTypes;

    const endpointUrl = getFirewallConfigurationUrl();
    return axios
      .put(endpointUrl, serverData)
      .then(() => {
        dispatch(saveConfigurationFulfilled(serverData));
        startSubmitMaskTimer(dispatch);
      })
      .catch((error) => {
        dispatch(saveConfigurationFailed(Messages.getHttpErrorMessage(error)));
      });
  };
}

export function loadQuarantineSummary() {
  return function (dispatch) {
    dispatch(quarantineSummaryRequested());
    return axios
      .get(getFirewallQuarantineSummaryUrl())
      .then(({ data }) => {
        dispatch(quarantineSummaryFulfilled(data));
      })
      .catch((error) => {
        dispatch(quarantineSummaryFailed(Messages.getHttpErrorMessage(error)));
      });
  };
}

export function loadTileMetrics() {
  return function (dispatch) {
    dispatch(loadTileMetricsRequested());
    return axios
      .get(getFirewallTileMetricsUrl())
      .then(({ data }) => {
        dispatch(loadTileMetricsFulfilled(data));
      })
      .catch((error) => {
        dispatch(loadTileMetricsFailed(Messages.getHttpErrorMessage(error)));
      });
  };
}

function startSubmitMaskTimer(dispatch) {
  setTimeout(() => {
    dispatch(configurationSaveMaskTimerDone());
    dispatch(setShowConfigurationModal(false));
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

export function openConfigurationModal() {
  return (dispatch) => {
    dispatch(loadConfiguration());
    dispatch(setShowConfigurationModal(true));
  };
}

export function closeConfigurationModal() {
  return setShowConfigurationModal(false);
}

export function setAutoUnquarantineGridPage(page) {
  return {
    type: FIREWALL_AUTO_UNQUARANTINE_GRID_SET_PAGE,
    payload: { currentPage: page },
  };
}

export function setAutoUnquarantineGridSorting(sortDir, sortField) {
  return {
    type: FIREWALL_AUTO_UNQUARANTINE_GRID_SET_SORTING,
    payload: { sortDir: sortDir, sortField: sortField },
  };
}

export function loadQuarantineList() {
  return function (dispatch, getState) {
    let gridState = getState().firewall.quarantineGridState,
      apiPage = gridState.currentPage ? gridState.currentPage + 1 : 1,
      filterPolicies = gridState.filterPolicies.length < 1 ? null : gridState.filterPolicies,
      filterComponentName = gridState.filterComponentName === '' ? null : gridState.filterComponentName,
      filterRepositoryPublicId = gridState.filterRepositoryPublicId === '' ? null : gridState.filterRepositoryPublicId,
      filterQuarantineTime = gridState.filterQuarantineTime === '' ? null : gridState.filterQuarantineTime,
      sortAsc = gridState.sortDir === 'asc';

    dispatch(loadQuarantineListRequested());
    return axios
      .get(
        getFirewallQuarantineListUrl(
          apiPage,
          gridState.pageSize,
          gridState.sortField,
          sortAsc,
          filterPolicies,
          filterComponentName,
          filterRepositoryPublicId,
          filterQuarantineTime
        )
      )
      .then(({ data }) => {
        dispatch(loadQuarantineListFulfilled(data));
        dispatch(setQuarantineGridLastUpdated(new Date()));
      })
      .catch((error) => {
        dispatch(loadQuarantineListFailed(Messages.getHttpErrorMessage(error)));
      });
  };
}

export function reevaluateComponent() {
  return function (dispatch, getState) {
    const { repositoryId, componentHash, pathname } = selectFirewallComponentDetailsPageRouteParams(getState());

    dispatch(reevaluateComponentRequested());
    return axios
      .post(getReevaluateComponentUrl(repositoryId, componentHash))
      .then(() => {
        dispatch(reevaluateComponentFulfilled());
        dispatch(loadComponentPolicyViolations(pathname, repositoryId));
        dispatch(loadExistingWaiversData('repository', repositoryId, componentHash));
        return dispatch(loadComponentDetails(selectFirewallComponentDetailsPageRouteParams(getState())));
      })
      .catch((error) => {
        dispatch(reevaluateComponentFailed(Messages.getHttpErrorMessage(error)));
      });
  };
}

export function loadComponentDetails(componentDetailsParams) {
  return function (dispatch) {
    dispatch(loadComponentDetailsRequested());
    const {
      repositoryId,
      componentIdentifier,
      componentHash,
      matchState,
      proprietary,
      identificationSource,
      scanId,
    } = componentDetailsParams;

    const requestParams = {
      clientType: 'ci',
      ownerType: 'repository',
      ownerId: repositoryId,
      componentIdentifier,
      hash: componentHash,
      matchState,
      proprietary,
      identificationSource,
      scanId,
    };

    return axios
      .get(getComponentDetailsUrl(requestParams))
      .then(({ data }) => dispatch(loadComponentDetailsFulfilled(data)))
      .then(() => dispatch(componentDetailsActions.loadFirewallComponentDetailsLabels()))
      .catch((error) => {
        dispatch(loadComponentDetailsFailed(Messages.getHttpErrorMessage(error)));
      });
  };
}

export function loadVersionExplorerData(componentDetailsParams) {
  return function (dispatch) {
    dispatch(loadVersionExplorerDataRequested());
    const {
      repositoryId,
      componentIdentifier,
      componentHash,
      matchState,
      proprietary,
      identificationSource,
      scanId,
    } = componentDetailsParams;

    const requestParams = {
      clientType: 'ci',
      ownerType: 'repository',
      ownerId: repositoryId,
      componentIdentifier,
      hash: componentHash,
      matchState,
      proprietary,
      identificationSource,
      scanId,
    };

    return axios
      .get(getVersionGraphUrl(requestParams))
      .then(({ data }) => {
        dispatch(loadVersionExplorerDataFulfilled(data));
      })
      .catch((error) => {
        dispatch(loadVersionExplorerDataFailed(Messages.getHttpErrorMessage(error)));
      });
  };
}

export function setQuarantineGridPage(page) {
  return (dispatch) => {
    dispatch(quarantineGridSetPage({ currentPage: page }));
    dispatch(loadQuarantineList());
  };
}

export function setQuarantineGridSorting(sortDir, sortField) {
  return (dispatch) => {
    dispatch(quarantineGridSetSorting({ sortDir: sortDir, sortField: sortField }));
    dispatch(loadQuarantineList());
  };
}

export function setQuarantineGridPolicyFilter(policies) {
  return (dispatch) => {
    dispatch(quarantineGridSetPolicyFilter({ policies }));
    dispatch(loadQuarantineList());
  };
}

export function setQuarantineGridComponentNameFilter(componentName) {
  return (dispatch) => {
    dispatch(quarantineGridSetComponentNameFilter({ componentName: componentName }));
    if (componentName.length !== 1) {
      dispatch(quarantineGridSetPage({ currentPage: null }));
      dispatch(loadQuarantineList());
    }
  };
}

export function setQuarantineGridRepositoryPublicIdFilter(repositoryPublicId) {
  return (dispatch) => {
    dispatch(quarantineGridSetRepositoryPublicIdFilter({ repositoryPublicId: repositoryPublicId }));
    if (repositoryPublicId.length !== 1) {
      dispatch(quarantineGridSetPage({ currentPage: null }));
      dispatch(loadQuarantineList());
    }
  };
}

export function setQuarantineGridQuarantineTimeFilter(quarantineTime) {
  return (dispatch) => {
    dispatch(quarantineGridSetQuarantineTimeFilter({ quarantineTime: quarantineTime }));
    if (quarantineTime) {
      dispatch(quarantineGridSetPage({ currentPage: null }));
      dispatch(loadQuarantineList());
    }
  };
}

export function setQuarantineGridLastUpdated(lastUpdated) {
  return {
    type: FIREWALL_QUARANTINE_GRID_SET_LAST_UPDATED,
    payload: { lastUpdated: lastUpdated },
  };
}

export const FIREWALL_SELECT_COMPONENT = 'FIREWALL_SELECT_COMPONENT';
const setSelectedComponent = payloadParamActionCreator(FIREWALL_SELECT_COMPONENT);

export function selectQuarantineComponent(componentIndex) {
  return (dispatch, getState) => {
    let components = getState().firewall.quarantineGridState.quarantineList;
    let component = components[componentIndex];
    dispatch(setSelectedComponent({ component, componentIndex, components }));
  };
}

export function selectReleaseQuarantineComponent(componentIndex) {
  return (dispatch, getState) => {
    let components = getState().firewall.autoUnquarantineState.autoUnquarantineGridState.releaseQuarantineList;
    let component = components[componentIndex];
    dispatch(setSelectedComponent({ component, componentIndex, components }));
  };
}

export function selectComponent(componentIndex) {
  return (dispatch, getState) => {
    let components = getState().firewall.cip.displayedEntries;
    let component = components[componentIndex];
    dispatch(setSelectedComponent({ component, componentIndex, components }));
  };
}

export function onComponentDetailsPageTabChange(tabId) {
  return (dispatch, getState) => {
    const abstractRouteName = selectIsStandaloneFirewall(getState()) ? 'firewall' : 'repository';
    return dispatch(stateGo(`${abstractRouteName}.componentDetailsPage.${tabId}`));
  };
}

export function onGoToRepositoryComponentWaiversPage(violationId) {
  return (dispatch, getState) => {
    const {
      repositoryId,
      componentIdentifier,
      componentHash,
      matchState,
      pathname,
      tabId,
      componentDisplayName,
    } = selectFirewallComponentDetailsPageRouteParams(getState());
    dispatch(
      stateGo(`${selectIsStandaloneFirewall(getState()) ? 'firewall' : 'repository'}.violationWaivers`, {
        repositoryId,
        componentIdentifier,
        componentHash,
        matchState,
        violationId,
        pathname,
        tabId,
        componentDisplayName,
      })
    );
    dispatch(isShowManageWaiverPage(true));
  };
}
function checkPermissionToAddWaivers(repositoryId) {
  return checkPermissions(['WAIVE_POLICY_VIOLATIONS'], 'repository', repositoryId);
}
export const loadFirewallViolationDetails = (policyViolationId) => (dispatch, getState) => {
  dispatch(loadViolationDetailRequested());
  const repositoryId = selectRepositoryId(getState());
  const parallelRequests = [
    axios.get(getRepositoryPolicyViolationUrl(repositoryId, policyViolationId)),
    dispatch(loadApplicableWaivers(policyViolationId)),
  ];

  return Promise.all(parallelRequests)
    .then((responses) => {
      const { data } = responses[0];
      const convertData = convertToWaiverViolationFormat(data);
      return checkPermissionToAddWaivers(repositoryId)
        .then((_) => {
          dispatch(loadViolationDetailFulfilled({ ...convertData, hasWaivePermission: true, _ }));
        })
        .catch(() => {
          dispatch(loadViolationDetailFulfilled(convertData));
        });
    })
    .catch((error) => {
      dispatch(loadViolationDetailFailed(Messages.getHttpErrorMessage(error)));
    });
};
export function loadComponentLicenses(repositoryId, componentIdentifier) {
  return (dispatch) => {
    dispatch(loadComponentLicensesRequested());
    return axios
      .all([
        axios.get(getLicensesWithSyntheticFilterUrl()),
        axios.get(
          getComponentMultiLicensesUrl({
            clientType: 'ci',
            ownerType: 'repository',
            ownerId: repositoryId,
            componentIdentifier,
          })
        ),
        axios.get(getLicenseOverrideUrl('repository', repositoryId, componentIdentifier)),
      ])
      .then(
        ([
          { data: allLicensesData },
          { data: multiLicensesData },
          {
            data: { licenseOverridesByOwner: overridenLicensesData },
          },
        ]) => {
          const licensesData = {
            ...multiLicensesData,
            licenseOverride: overridenLicensesData,
            allLicenses: allLicensesData.map(({ id, shortDisplayName }) => ({ id, displayName: shortDisplayName })),
          };

          dispatch(loadComponentLicensesFulfilled(licensesData));
          dispatch(componentDetailsLicenseDetectionsTileActions.load.fulfilled(licensesData));
        }
      )
      .catch((error) => {
        return dispatch(loadComponentLicensesFailed(Messages.getHttpErrorMessage(error)));
      });
  };
}

export function goToRepositoryComponentDetailsPage(
  repositoryId,
  componentIdentifier,
  componentHash,
  matchState,
  pathname,
  componentDisplayName
) {
  return (dispatch, getState) => {
    dispatch(
      stateGo(`${selectIsStandaloneFirewall(getState()) ? 'firewall' : 'repository'}.componentDetailsPage`, {
        repositoryId,
        componentIdentifier: JSON.stringify(componentIdentifier),
        componentHash,
        matchState,
        pathname,
        componentDisplayName,
      })
    );
  };
}
