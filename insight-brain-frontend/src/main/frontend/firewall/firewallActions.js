/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS} from '@sonatype/react-shared-components';

import {noPayloadActionCreator, payloadParamActionCreator} from '../util/reduxUtil';
import {getFirewallConfigurationUrl, getFirewallStatusUrl, getFirewallReleaseQuarantineSummaryUrl,
  getFirewallQuarantineSummaryUrl} from '../util/CLMLocation';
import {Messages} from '../util/CommonServices';

export const FIREWALL_LOAD_DATA_REQUESTED = 'FIREWALL_LOAD_DATA_REQUESTED';

const loadDataRequested = noPayloadActionCreator(FIREWALL_LOAD_DATA_REQUESTED);

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
export const toggleAutoUnquarantineAll = payloadParamActionCreator(FIREWALL_TOGGLE_AUTO_UNQUARANTINE_ALL);
const configurationSaveMaskTimerDone = noPayloadActionCreator(FIREWALL_CONFIGURATION_SAVE_MASK_TIMER_DONE);

export const FIREWALL_RELEASE_QUARANTINE_SUMMARY_REQUESTED = 'FIREWALL_RELEASE_QUARANTINE_SUMMARY_REQUESTED';
export const FIREWALL_RELEASE_QUARANTINE_SUMMARY_FULFILLED = 'FIREWALL_RELEASE_QUARANTINE_SUMMARY_FULFILLED';
export const FIREWALL_RELEASE_QUARANTINE_SUMMARY_FAILED = 'FIREWALL_RELEASE_QUARANTINE_SUMMARY_FAILED';

const loadReleaseQuarantineSummaryRequested = noPayloadActionCreator(FIREWALL_RELEASE_QUARANTINE_SUMMARY_REQUESTED);
const loadReleaseQuarantineSummaryFulfilled = payloadParamActionCreator(FIREWALL_RELEASE_QUARANTINE_SUMMARY_FULFILLED);
const loadReleaseQuarantineSummaryFailed = payloadParamActionCreator(FIREWALL_RELEASE_QUARANTINE_SUMMARY_FAILED);

export const FIREWALL_QUARANTINE_SUMMARY_REQUESTED = 'FIREWALL_QUARANTINE_SUMMARY_REQUESTED';
export const FIREWALL_QUARANTINE_SUMMARY_FULFILLED = 'FIREWALL_QUARANTINE_SUMMARY_FULFILLED';
export const FIREWALL_QUARANTINE_SUMMARY_FAILED = 'FIREWALL_QUARANTINE_SUMMARY_FAILED';

const quarantineSummaryRequested = noPayloadActionCreator(FIREWALL_QUARANTINE_SUMMARY_REQUESTED);
const quarantineSummaryFulfilled = payloadParamActionCreator(FIREWALL_QUARANTINE_SUMMARY_FULFILLED);
const quarantineSummaryFailed = payloadParamActionCreator(FIREWALL_QUARANTINE_SUMMARY_FAILED);

export function loadData() {
  return function(dispatch) {
    dispatch(loadDataRequested());
    dispatch(loadStatus());
    dispatch(loadConfiguration());
    dispatch(loadReleaseQuarantineSummary());
    dispatch(loadQuarantineSummary());
  };
}

export function loadStatus() {
  return function(dispatch) {
    dispatch(loadStatusRequested());
    return axios.get(getFirewallStatusUrl())
        .then(({data}) => {
          dispatch(loadStatusFulfilled(data));
        })
        .catch(error => {
          dispatch(loadStatusFailed(error));
        });
  };
}

export function loadReleaseQuarantineSummary() {
  return function(dispatch) {
    dispatch(loadReleaseQuarantineSummaryRequested());
    return axios.get(getFirewallReleaseQuarantineSummaryUrl())
        .then(({data}) => {
          dispatch(loadReleaseQuarantineSummaryFulfilled(data));
        })
        .catch(error => {
          dispatch(loadReleaseQuarantineSummaryFailed(Messages.getHttpErrorMessage(error)));
        });
  };
}

export function loadConfiguration() {
  return function(dispatch) {
    dispatch(loadConfigurationRequested());
    return axios.get(getFirewallConfigurationUrl())
        .then(({data}) => {
          dispatch(loadConfigurationFulfilled(data));
        })
        .catch(error => {
          dispatch(loadConfigurationFailed(Messages.getHttpErrorMessage(error)));
        });
  };
}

export function saveConfiguration() {
  return (dispatch, getState) => {
    dispatch(saveConfigurationRequested());

    const serverData = getState().firewallConfigurationModal.formState.conditionTypes;

    const endpointUrl = getFirewallConfigurationUrl();
    return axios.put(endpointUrl, serverData)
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
  return function(dispatch) {
    dispatch(quarantineSummaryRequested());
    return axios.get(getFirewallQuarantineSummaryUrl())
        .then(({data}) => {
          dispatch(quarantineSummaryFulfilled(data));
        })
        .catch(error => {
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
