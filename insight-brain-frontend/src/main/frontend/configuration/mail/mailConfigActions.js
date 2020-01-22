/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { map, pick } from 'ramda';

import { getMailConfigUrl, getTestMailUrl } from '../../util/CLMLocation';
import { noPayloadActionCreator, payloadParamActionCreator } from '../../util/reduxUtil';
import { FAKE_PASSWORD } from './mailConfigReducer';

export const LOAD_REQUESTED = 'LOAD_REQUESTED';
export const LOAD_FULFILLED = 'LOAD_FULFILLED';
export const LOAD_FAILED = 'LOAD_FAILED';

export const SAVE_REQUESTED = 'SAVE_REQUESTED';
export const SAVE_FULFILLED = 'SAVE_FULFILLED';
export const SAVE_FAILED = 'SAVE_FAILED';

export const SEND_TEST_MAIL_REQUESTED = 'SEND_TEST_MAIL_REQUESTED';
export const SEND_TEST_MAIL_FULFILLED = 'SEND_TEST_MAIL_FULFILLED';
export const SEND_TEST_MAIL_FAILED = 'SEND_TEST_MAIL_FAILED';

export const DELETE_REQUESTED = 'DELETE_REQUESTED';
export const DELETE_FULFILLED = 'DELETE_FULFILLED';
export const DELETE_FAILED = 'DELETE_FAILED';

export const RESET_FORM = 'RESET_FORM';

export const SET_HOSTNAME = 'SET_HOSTNAME';
export const SET_PORT = 'SET_PORT';
export const SET_USERNAME = 'SET_USERNAME';
export const SET_PASSWORD = 'SET_PASSWORD';
export const SET_SSL_ENABLED = 'SET_SSL_ENABLED';
export const SET_STARTTLS_ENABLED = 'SET_STARTTLS_ENABLED';
export const SET_SYSTEM_EMAIL = 'SET_SYSTEM_EMAIL';
export const SET_TEST_EMAIL = 'SET_TEST_EMAIL';

export const SET_SHOW_DELETE_MODAL = 'SET_SHOW_DELETE_MODAL';

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
    axios.post(getTestMailUrl(formState.testEmail.trimmedValue), toServerData(formState))
        .then(() => { dispatch(sendTestMailFulfilled()); })
        .catch(error => { dispatch(sendTestMailFailed(error)); });
  };
}

export function save() {
  return function(dispatch, getState) {
    dispatch(saveRequested());

    const formState = getState().mailConfig.formState,
        serverData = toServerData(formState);

    axios.put(getMailConfigUrl(), serverData)
        .then(() => { dispatch(saveFulfilled(serverData)); })
        .catch(error => { dispatch(saveFailed(error)); });
  };
}

export function del() {
  return function(dispatch) {
    dispatch(deleteRequested());

    axios.delete(getMailConfigUrl())
        .then(() => { dispatch(deleteFulfilled()); })
        .catch(error => { dispatch(deleteFailed(error)); });
  };
}

const loadRequested = noPayloadActionCreator(LOAD_REQUESTED);
const loadFulfilled = payloadParamActionCreator(LOAD_FULFILLED);
const loadFailed = payloadParamActionCreator(LOAD_FAILED);

const saveRequested = noPayloadActionCreator(SAVE_REQUESTED);
const saveFulfilled = payloadParamActionCreator(SAVE_FULFILLED);
const saveFailed = payloadParamActionCreator(SAVE_FAILED);

const sendTestMailRequested = noPayloadActionCreator(SEND_TEST_MAIL_REQUESTED);
const sendTestMailFulfilled = noPayloadActionCreator(SEND_TEST_MAIL_FULFILLED);
const sendTestMailFailed = payloadParamActionCreator(SEND_TEST_MAIL_FAILED);

const deleteRequested = noPayloadActionCreator(DELETE_REQUESTED);
const deleteFulfilled = payloadParamActionCreator(DELETE_FULFILLED);
const deleteFailed = payloadParamActionCreator(DELETE_FAILED);

export const resetForm = noPayloadActionCreator(RESET_FORM);

export const setHostname = payloadParamActionCreator(SET_HOSTNAME);
export const setPort = payloadParamActionCreator(SET_PORT);
export const setUsername = payloadParamActionCreator(SET_USERNAME);
export const setPassword = payloadParamActionCreator(SET_PASSWORD);
export const setSslEnabled = payloadParamActionCreator(SET_SSL_ENABLED);
export const setStartTlsEnabled = payloadParamActionCreator(SET_STARTTLS_ENABLED);
export const setSystemEmail = payloadParamActionCreator(SET_SYSTEM_EMAIL);
export const setTestEmail = payloadParamActionCreator(SET_TEST_EMAIL);

export const setShowDeleteModal = payloadParamActionCreator(SET_SHOW_DELETE_MODAL);
