/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { map, pick } from 'ramda';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import { getMailConfigUrl, getTestMailUrl } from '../../util/CLMLocation';
import { noPayloadActionCreator, payloadParamActionCreator } from '../../util/reduxUtil';
import { FAKE_PASSWORD } from './mailConfigReducer';

export const MAIL_CONFIG_LOAD_REQUESTED = 'MAIL_CONFIG_LOAD_REQUESTED';
export const MAIL_CONFIG_LOAD_FULFILLED = 'MAIL_CONFIG_LOAD_FULFILLED';
export const MAIL_CONFIG_LOAD_FAILED = 'MAIL_CONFIG_LOAD_FAILED';

export const MAIL_CONFIG_SAVE_REQUESTED = 'MAIL_CONFIG_SAVE_REQUESTED';
export const MAIL_CONFIG_SAVE_FULFILLED = 'MAIL_CONFIG_SAVE_FULFILLED';
export const MAIL_CONFIG_SAVE_FAILED = 'MAIL_CONFIG_SAVE_FAILED';
export const MAIL_CONFIG_SUBMIT_MASK_TIMER_DONE = 'MAIL_CONFIG_SUBMIT_MASK_TIMER_DONE';

export const MAIL_CONFIG_SEND_TEST_MAIL_REQUESTED = 'MAIL_CONFIG_SEND_TEST_MAIL_REQUESTED';
export const MAIL_CONFIG_SEND_TEST_MAIL_FULFILLED = 'MAIL_CONFIG_SEND_TEST_MAIL_FULFILLED';
export const MAIL_CONFIG_SEND_TEST_MAIL_FAILED = 'MAIL_CONFIG_SEND_TEST_MAIL_FAILED';

export const MAIL_CONFIG_DELETE_REQUESTED = 'MAIL_CONFIG_DELETE_REQUESTED';
export const MAIL_CONFIG_DELETE_FULFILLED = 'MAIL_CONFIG_DELETE_FULFILLED';
export const MAIL_CONFIG_DELETE_FAILED = 'MAIL_CONFIG_DELETE_FAILED';

export const MAIL_CONFIG_RESET_FORM = 'MAIL_CONFIG_RESET_FORM';

export const MAIL_CONFIG_SET_HOSTNAME = 'MAIL_CONFIG_SET_HOSTNAME';
export const MAIL_CONFIG_SET_PORT = 'MAIL_CONFIG_SET_PORT';
export const MAIL_CONFIG_SET_USERNAME = 'MAIL_CONFIG_SET_USERNAME';
export const MAIL_CONFIG_SET_PASSWORD = 'MAIL_CONFIG_SET_PASSWORD';
export const MAIL_CONFIG_SET_SSL_ENABLED = 'MAIL_CONFIG_SET_SSL_ENABLED';
export const MAIL_CONFIG_SET_STARTTLS_ENABLED = 'MAIL_CONFIG_SET_STARTTLS_ENABLED';
export const MAIL_CONFIG_SET_SYSTEM_EMAIL = 'MAIL_CONFIG_SET_SYSTEM_EMAIL';
export const MAIL_CONFIG_SET_TEST_EMAIL = 'MAIL_CONFIG_SET_TEST_EMAIL';

export const MAIL_CONFIG_SET_SHOW_DELETE_MODAL = 'MAIL_CONFIG_SET_SHOW_DELETE_MODAL';

function toServerData(formState) {
  // pull the trimmedValue out of the input state object and convert empty strings to null
  const textPropMapper = ({ trimmedValue }) => trimmedValue || null;

  return {
    ...pick(['startTlsEnabled', 'sslEnabled'], formState),
    ...map(textPropMapper, pick(['hostname', 'username', 'systemEmail'], formState)),
    port: parseInt(formState.port.trimmedValue, 10),
    password: formState.password.value || null,
    passwordIsIncluded: formState.password.value !== FAKE_PASSWORD
  };
}

function startSubmitMaskSuccessTimer(dispatch) {
  setTimeout(() => {
    dispatch({ type: MAIL_CONFIG_SUBMIT_MASK_TIMER_DONE });
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

export function load() {
  return function(dispatch) {
    dispatch(loadRequested());

    axios.get(getMailConfigUrl())
        .then(({ data }) => { dispatch(loadFulfilled(data)); })
        .catch(error => { dispatch(loadFailed(error)); });
  };
}

export function sendTestEmail() {
  return function(dispatch, getState) {
    dispatch(sendTestMailRequested());

    const formState = getState().mailConfig.formState;
    return axios.post(getTestMailUrl(formState.testEmail.trimmedValue), toServerData(formState))
        .then(() => {
          dispatch(sendTestMailFulfilled());
          startSubmitMaskSuccessTimer(dispatch);
        })
        .catch(error => { dispatch(sendTestMailFailed(error)); });
  };
}

export function save() {
  return function(dispatch, getState) {
    dispatch(saveRequested());

    const formState = getState().mailConfig.formState,
        serverData = toServerData(formState);

    return axios.put(getMailConfigUrl(), serverData)
        .then(() => {
          dispatch(saveFulfilled(serverData));
          startSubmitMaskSuccessTimer(dispatch);
        })
        .catch(error => { dispatch(saveFailed(error)); });
  };
}

export function del() {
  return function(dispatch) {
    dispatch(deleteRequested());

    return axios.delete(getMailConfigUrl())
        .then(() => {
          dispatch(deleteFulfilled());
          startSubmitMaskSuccessTimer(dispatch);
        })
        .catch(error => { dispatch(deleteFailed(error)); });
  };
}

const loadRequested = noPayloadActionCreator(MAIL_CONFIG_LOAD_REQUESTED);
const loadFulfilled = payloadParamActionCreator(MAIL_CONFIG_LOAD_FULFILLED);
const loadFailed = payloadParamActionCreator(MAIL_CONFIG_LOAD_FAILED);

const saveRequested = noPayloadActionCreator(MAIL_CONFIG_SAVE_REQUESTED);
const saveFulfilled = payloadParamActionCreator(MAIL_CONFIG_SAVE_FULFILLED);
const saveFailed = payloadParamActionCreator(MAIL_CONFIG_SAVE_FAILED);

const sendTestMailRequested = noPayloadActionCreator(MAIL_CONFIG_SEND_TEST_MAIL_REQUESTED);
const sendTestMailFulfilled = noPayloadActionCreator(MAIL_CONFIG_SEND_TEST_MAIL_FULFILLED);
const sendTestMailFailed = payloadParamActionCreator(MAIL_CONFIG_SEND_TEST_MAIL_FAILED);

const deleteRequested = noPayloadActionCreator(MAIL_CONFIG_DELETE_REQUESTED);
const deleteFulfilled = payloadParamActionCreator(MAIL_CONFIG_DELETE_FULFILLED);
const deleteFailed = payloadParamActionCreator(MAIL_CONFIG_DELETE_FAILED);

export const resetForm = noPayloadActionCreator(MAIL_CONFIG_RESET_FORM);

export const setHostname = payloadParamActionCreator(MAIL_CONFIG_SET_HOSTNAME);
export const setPort = payloadParamActionCreator(MAIL_CONFIG_SET_PORT);
export const setUsername = payloadParamActionCreator(MAIL_CONFIG_SET_USERNAME);
export const setPassword = payloadParamActionCreator(MAIL_CONFIG_SET_PASSWORD);
export const setSslEnabled = payloadParamActionCreator(MAIL_CONFIG_SET_SSL_ENABLED);
export const setStartTlsEnabled = payloadParamActionCreator(MAIL_CONFIG_SET_STARTTLS_ENABLED);
export const setSystemEmail = payloadParamActionCreator(MAIL_CONFIG_SET_SYSTEM_EMAIL);
export const setTestEmail = payloadParamActionCreator(MAIL_CONFIG_SET_TEST_EMAIL);

export const setShowDeleteModal = payloadParamActionCreator(MAIL_CONFIG_SET_SHOW_DELETE_MODAL);
