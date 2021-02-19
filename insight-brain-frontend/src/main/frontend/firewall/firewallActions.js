/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS} from '@sonatype/react-shared-components';

import {noPayloadActionCreator, payloadParamActionCreator} from '../util/reduxUtil';
import {getFirewallConfigurationUrl, getFirewallStatusUrl} from '../util/CLMLocation';
import {Messages} from '../util/CommonServices';

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
export const FIREWALL_CONFIGURATION_SAVE_MASK_TIMER_DONE = 'FIREWALL_CONFIGURATION_SAVE_MASK_TIMER_DONE';

export const toggleAutoUnquarantineEnabled = noPayloadActionCreator(FIREWALL_TOGGLE_AUTO_UNQUARANTINE_ENABLED);
const configurationSaveMaskTimerDone = noPayloadActionCreator(FIREWALL_CONFIGURATION_SAVE_MASK_TIMER_DONE);

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

    const serverData = getState().firewallConfigurationModal.formState;

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
