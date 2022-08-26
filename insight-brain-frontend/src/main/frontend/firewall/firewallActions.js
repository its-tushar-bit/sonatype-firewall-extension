/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import { noPayloadActionCreator, payloadParamActionCreator } from '../util/reduxUtil';
import {
  getFirewallConfigurationUrl,
  getFirewallQuarantineListUrl,
  getFirewallQuarantineSummaryUrl,
  getFirewallReleaseQuarantineListUrl,
  getFirewallReleaseQuarantineSummaryUrl,
  getPoliciesUrl,
  getComponentDetailsUrl,
  getVersionGraphUrl,
  getComponentPolicyViolationsUrl,
} from '../util/CLMLocation';
import { Messages } from '../utilAngular/CommonServices';
import { stateGo } from '../reduxUiRouter/routerActions';

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

export const FIREWALL_AUTO_UNQUARANTINE_GRID_SET_PAGE = 'FIREWALL_AUTO_UNQUARANTINE_GRID_SET_PAGE';
export const FIREWALL_AUTO_UNQUARANTINE_GRID_SET_SORTING = 'FIREWALL_AUTO_UNQUARANTINE_GRID_SET_SORTING';

export const FIREWALL_QUARANTINE_GRID_SET_PAGE = 'FIREWALL_QUARANTINE_GRID_SET_PAGE';
export const FIREWALL_QUARANTINE_GRID_SET_SORTING = 'FIREWALL_QUARANTINE_GRID_SET_SORTING';
export const FIREWALL_QUARANTINE_GRID_SET_FILTER = 'FIREWALL_QUARANTINE_GRID_SET_FILTER';
export const FIREWALL_QUARANTINE_GRID_SET_LAST_UPDATED = 'FIREWALL_QUARANTINE_GRID_SET_LAST_UPDATED';

const quarantineGridSetPage = payloadParamActionCreator(FIREWALL_QUARANTINE_GRID_SET_PAGE);
const quarantineGridSetSorting = payloadParamActionCreator(FIREWALL_QUARANTINE_GRID_SET_SORTING);
const quarantineGridSetFilter = payloadParamActionCreator(FIREWALL_QUARANTINE_GRID_SET_FILTER);

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

export function loadFirewallData() {
  return (dispatch) => {
    dispatch(loadFirewallDataRequested());
    dispatch(loadConfiguration());
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
      filterValue = gridState.filterPolicy === '' ? null : gridState.filterPolicy,
      sortAsc = gridState.sortDir === null ? gridState.sortDir : gridState.sortDir === 'asc';

    dispatch(loadQuarantineListRequested());
    return axios
      .get(getFirewallQuarantineListUrl(apiPage, gridState.pageSize, gridState.sortField, sortAsc, filterValue))
      .then(({ data }) => {
        dispatch(loadQuarantineListFulfilled(data));
        dispatch(setQuarantineGridLastUpdated(new Date()));
      })
      .catch((error) => {
        dispatch(loadQuarantineListFailed(Messages.getHttpErrorMessage(error)));
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

export function setQuarantineGridPolicyFilter(policy) {
  return (dispatch) => {
    dispatch(quarantineGridSetFilter({ policy: policy }));
    dispatch(loadQuarantineList());
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

export const FIREWALL_CIP_MODAL_CLOSED = 'FIREWALL_CIP_MODAL_CLOSED';
export const FIREWALL_CIP_MODAL_SHOW = 'FIREWALL_CIP_MODAL_SHOW';
export const cipModalClosed = noPayloadActionCreator(FIREWALL_CIP_MODAL_CLOSED);
export const cipModalShow = noPayloadActionCreator(FIREWALL_CIP_MODAL_SHOW);

export function selectQuarantineComponent(componentIndex) {
  return (dispatch, getState) => {
    let components = getState().firewall.quarantineGridState.quarantineList;
    let component = components[componentIndex];
    dispatch(setSelectedComponent({ component, componentIndex, components }));
    dispatch(cipModalShow());
  };
}

export function selectReleaseQuarantineComponent(componentIndex) {
  return (dispatch, getState) => {
    let components = getState().firewall.autoUnquarantineState.autoUnquarantineGridState.releaseQuarantineList;
    let component = components[componentIndex];
    dispatch(setSelectedComponent({ component, componentIndex, components }));
    dispatch(cipModalShow());
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
  return (dispatch) => {
    return dispatch(stateGo(`firewall.componentDetailsPage.${tabId}`));
  };
}
