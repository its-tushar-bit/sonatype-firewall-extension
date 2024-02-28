/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { map, pick, compose } from 'ramda';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { Messages } from '../../utilAngular/CommonServices';

import { getProxyConfigUrl, getLicenseSummaryUrl } from '../../util/CLMLocation';
import { checkPermissions } from '../../util/authorizationUtil';

import { noPayloadActionCreator, payloadParamActionCreator } from '../../util/reduxUtil';

export const FAKE_PASSWORD = '\x00\x00\x00\x00\x00';

export const PROXY_CONFIG_LOAD_REQUESTED = 'PROXY_CONFIG_LOAD_REQUESTED';
export const PROXY_CONFIG_LOAD_FULFILLED = 'PROXY_CONFIG_LOAD_FULFILLED';
export const PROXY_CONFIG_LOAD_FAILED = 'PROXY_CONFIG_LOAD_FAILED';

export const PROXY_CONFIG_LOAD_LICENSED_REQUESTED = 'PROXY_CONFIG_LOAD_LICENSED_REQUESTED';
export const PROXY_CONFIG_LOAD_LICENSED_FULFILLED = 'PROXY_CONFIG_LOAD_LICENSED_FULFILLED';
export const PROXY_CONFIG_LOAD_LICENSED_FAILED = 'PROXY_CONFIG_LOAD_LICENSED_FAILED';

export const PROXY_CONFIG_SAVE_REQUESTED = 'PROXY_CONFIG_SAVE_REQUESTED';
export const PROXY_CONFIG_SAVE_FULFILLED = 'PROXY_CONFIG_SAVE_FULFILLED';
export const PROXY_CONFIG_SAVE_FAILED = 'PROXY_CONFIG_SAVE_FAILED';

export const PROXY_CONFIG_DELETE_REQUESTED = 'PROXY_CONFIG_DELETE_REQUESTED';
export const PROXY_CONFIG_DELETE_FULFILLED = 'PROXY_CONFIG_DELETE_FULFILLED';
export const PROXY_CONFIG_DELETE_FAILED = 'PROXY_CONFIG_DELETE_FAILED';

export const PROXY_CONFIG_RESET_FORM = 'PROXY_CONFIG_RESET_FORM';

export const PROXY_CONFIG_SET_HOSTNAME = 'PROXY_CONFIG_SET_HOSTNAME';
export const PROXY_CONFIG_SET_PORT = 'PROXY_CONFIG_SET_PORT';
export const PROXY_CONFIG_SET_USERNAME = 'PROXY_CONFIG_SET_USERNAME';
export const PROXY_CONFIG_SET_PASSWORD = 'PROXY_CONFIG_SET_PASSWORD';
export const PROXY_CONFIG_SET_EXCLUDE_HOSTS = 'PROXY_CONFIG_SET_EXCLUDE_HOSTS';

export const PROXY_CONFIG_SUBMIT_MASK_TIMER_DONE = 'PROXY_CONFIG_SUBMIT_MASK_TIMER_DONE';
const saveMaskTimerDone = noPayloadActionCreator(PROXY_CONFIG_SUBMIT_MASK_TIMER_DONE);

export const PROXY_CONFIG_DELETE_MASK_TIMER_DONE = 'PROXY_CONFIG_DELETE_MASK_TIMER_DONE';
const deleteMaskTimerDone = noPayloadActionCreator(PROXY_CONFIG_DELETE_MASK_TIMER_DONE);

function toServerData(formState) {
  // pull the trimmedValue out of the input state object and convert empty strings to null
  const textPropMapper = ({ trimmedValue }) => trimmedValue || null;

  return {
    ...map(textPropMapper, pick(['hostname', 'username'], formState)),
    port: parseInt(formState.port.trimmedValue, 10),
    password: formState.password.value || null,
    passwordIsIncluded: formState.password.value !== FAKE_PASSWORD,
    excludeHosts:
      formState.excludeHosts.trimmedValue !== '' ? formState.excludeHosts.value.replace(/\s/g, '').split(',') : null,
  };
}

function startSubmitMaskSuccessTimer(dispatch, timerDoneFunc) {
  setTimeout(() => {
    dispatch(timerDoneFunc());
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

export function loadLicenced() {
  return function (dispatch) {
    dispatch(loadLicensedRequested());

    return axios
      .get(getLicenseSummaryUrl())
      .then(() => {
        dispatch(loadLicensedFulfilled());
      })
      .catch(compose(dispatch, loadLicensedFailed));
  };
}

export function load() {
  return function (dispatch) {
    dispatch(loadRequested());

    return checkPermissions(['CONFIGURE_SYSTEM'])
      .then(() => {
        return axios.get(getProxyConfigUrl()).then(({ data }) => {
          dispatch(loadFulfilled(data));
        });
      })
      .catch(compose(dispatch, loadFailed));
  };
}

export function save() {
  return function (dispatch, getState) {
    dispatch(saveRequested());

    const formState = getState().proxyConfig.formState;
    const serverData = toServerData(formState);

    return axios
      .put(getProxyConfigUrl(), serverData)
      .then(() => {
        dispatch(saveFulfilled(serverData));
        startSubmitMaskSuccessTimer(dispatch, saveMaskTimerDone);
      })
      .catch(compose(dispatch, saveFailed, Messages.getHttpErrorMessage));
  };
}

export function del(cb) {
  return function (dispatch) {
    dispatch(deleteRequested());

    return axios
      .delete(getProxyConfigUrl())
      .then(() => {
        dispatch(deleteFulfilled());
        startSubmitMaskSuccessTimer(dispatch, deleteMaskTimerDone);
        cb();
      })
      .catch(compose(dispatch, deleteFailed, Messages.getHttpErrorMessage));
  };
}

const loadRequested = noPayloadActionCreator(PROXY_CONFIG_LOAD_REQUESTED);
const loadFulfilled = payloadParamActionCreator(PROXY_CONFIG_LOAD_FULFILLED);
const loadFailed = payloadParamActionCreator(PROXY_CONFIG_LOAD_FAILED);

const loadLicensedRequested = noPayloadActionCreator(PROXY_CONFIG_LOAD_LICENSED_REQUESTED);
const loadLicensedFulfilled = payloadParamActionCreator(PROXY_CONFIG_LOAD_LICENSED_FULFILLED);
const loadLicensedFailed = payloadParamActionCreator(PROXY_CONFIG_LOAD_LICENSED_FAILED);

const saveRequested = noPayloadActionCreator(PROXY_CONFIG_SAVE_REQUESTED);
const saveFulfilled = payloadParamActionCreator(PROXY_CONFIG_SAVE_FULFILLED);
const saveFailed = payloadParamActionCreator(PROXY_CONFIG_SAVE_FAILED);

const deleteRequested = noPayloadActionCreator(PROXY_CONFIG_DELETE_REQUESTED);
const deleteFulfilled = payloadParamActionCreator(PROXY_CONFIG_DELETE_FULFILLED);
const deleteFailed = payloadParamActionCreator(PROXY_CONFIG_DELETE_FAILED);

export const resetForm = noPayloadActionCreator(PROXY_CONFIG_RESET_FORM);

export const setHostname = payloadParamActionCreator(PROXY_CONFIG_SET_HOSTNAME);
export const setPort = payloadParamActionCreator(PROXY_CONFIG_SET_PORT);
export const setUsername = payloadParamActionCreator(PROXY_CONFIG_SET_USERNAME);
export const setPassword = payloadParamActionCreator(PROXY_CONFIG_SET_PASSWORD);
export const setExcludeHosts = payloadParamActionCreator(PROXY_CONFIG_SET_EXCLUDE_HOSTS);
