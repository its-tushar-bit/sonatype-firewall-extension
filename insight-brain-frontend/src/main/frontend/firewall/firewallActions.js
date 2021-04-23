/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import { noPayloadActionCreator, payloadParamActionCreator } from '../util/reduxUtil';
import { Messages } from '../util/CommonServices';
import {
  getFirewallConfigurationUrl,
  getFirewallStatusUrl,
  getFirewallReleaseQuarantineSummaryUrl,
  getFirewallQuarantineSummaryUrl,
  getFirewallReleaseQuarantineListUrl,
  getFirewallQuarantineListUrl,
  getPoliciesUrl,
} from '../util/CLMLocation';

export const FIREWALL_LOAD_DATA_REQUESTED = 'FIREWALL_LOAD_DATA_REQUESTED';

const loadFirewallDataRequested = noPayloadActionCreator(FIREWALL_LOAD_DATA_REQUESTED);

export const FIREWALL_AUTO_UNQUARANTINE_DATA_REQUESTED = 'FIREWALL_AUTO_UNQUARANTINE_DATA_REQUESTED';

const loadAutoUnquarantineDataRequested = noPayloadActionCreator(FIREWALL_AUTO_UNQUARANTINE_DATA_REQUESTED);

export const FIREWALL_LOAD_STATUS_REQUESTED = 'FIREWALL_LOAD_STATUS_REQUESTED';
export const FIREWALL_LOAD_STATUS_FULFILLED = 'FIREWALL_LOAD_STATUS_FULFILLED';
export const FIREWALL_LOAD_STATUS_FAILED = 'FIREWALL_LOAD_STATUS_FAILED';

const loadStatusRequested = noPayloadActionCreator(FIREWALL_LOAD_STATUS_REQUESTED);
const loadStatusFulfilled = payloadParamActionCreator(FIREWALL_LOAD_STATUS_FULFILLED);
const loadStatusFailed = payloadParamActionCreator(FIREWALL_LOAD_STATUS_FAILED);

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

const quarantineGridSetPage = payloadParamActionCreator(FIREWALL_QUARANTINE_GRID_SET_PAGE);
const quarantineGridSetSorting = payloadParamActionCreator(FIREWALL_QUARANTINE_GRID_SET_SORTING);
const quarantineGridSetFilter = payloadParamActionCreator(FIREWALL_QUARANTINE_GRID_SET_FILTER);

export const FIREWALL_QUARANTINE_SUMMARY_REQUESTED = 'FIREWALL_QUARANTINE_SUMMARY_REQUESTED';
export const FIREWALL_QUARANTINE_SUMMARY_FULFILLED = 'FIREWALL_QUARANTINE_SUMMARY_FULFILLED';
export const FIREWALL_QUARANTINE_SUMMARY_FAILED = 'FIREWALL_QUARANTINE_SUMMARY_FAILED';

const quarantineSummaryRequested = noPayloadActionCreator(FIREWALL_QUARANTINE_SUMMARY_REQUESTED);
const quarantineSummaryFulfilled = payloadParamActionCreator(FIREWALL_QUARANTINE_SUMMARY_FULFILLED);
const quarantineSummaryFailed = payloadParamActionCreator(FIREWALL_QUARANTINE_SUMMARY_FAILED);

export function loadFirewallData() {
  return (dispatch) => {
    dispatch(loadFirewallDataRequested());
    dispatch(loadStatus());
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
    dispatch(loadStatus());
    dispatch(loadConfiguration());
    dispatch(loadReleaseQuarantineSummary());
    dispatch(loadReleaseQuarantineList());
  };
}

export function loadStatus() {
  return function (dispatch) {
    dispatch(loadStatusRequested());
    return axios
      .get(getFirewallStatusUrl())
      .then(({ data }) => {
        dispatch(loadStatusFulfilled(data));
      })
      .catch((error) => {
        dispatch(loadStatusFailed(error));
      });
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
      filterValue = gridState.filterPolicy ? gridState.filterPolicy.id : null,
      sortAsc = gridState.sortDir === null ? gridState.sortDir : gridState.sortDir === 'asc';

    dispatch(loadQuarantineListRequested());
    return axios
      .get(getFirewallQuarantineListUrl(apiPage, gridState.pageSize, gridState.sortField, sortAsc, filterValue))
      .then(({ data }) => {
        dispatch(loadQuarantineListFulfilled(data));
      })
      .catch((error) => {
        dispatch(loadQuarantineListFailed(Messages.getHttpErrorMessage(error)));
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
