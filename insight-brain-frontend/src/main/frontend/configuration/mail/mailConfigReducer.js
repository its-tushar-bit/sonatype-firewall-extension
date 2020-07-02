/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as textInputStateHelpers from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';
import { __, any, complement, compose, curryN, eqProps, map, pick, prop, propEq, values } from 'ramda';

import { createReducerFromActionMap, propSetConst } from '../../util/reduxUtil';
import { pathSet, propSet } from '../../util/jsUtil';
import { combineValidators, hasValidationErrors, validateNonEmpty, validatePatternMatch }
  from '../../util/validationUtil';
import { Messages } from '../../util/CommonServices';

import {
  MAIL_CONFIG_LOAD_REQUESTED,
  MAIL_CONFIG_LOAD_FULFILLED,
  MAIL_CONFIG_LOAD_FAILED,
  MAIL_CONFIG_SAVE_REQUESTED,
  MAIL_CONFIG_SAVE_FULFILLED,
  MAIL_CONFIG_SAVE_FAILED,
  MAIL_CONFIG_DELETE_REQUESTED,
  MAIL_CONFIG_DELETE_FULFILLED,
  MAIL_CONFIG_DELETE_FAILED,
  MAIL_CONFIG_RESET_FORM,
  MAIL_CONFIG_SET_HOSTNAME,
  MAIL_CONFIG_SET_PORT,
  MAIL_CONFIG_SET_USERNAME,
  MAIL_CONFIG_SET_PASSWORD,
  MAIL_CONFIG_SET_SSL_ENABLED,
  MAIL_CONFIG_SET_STARTTLS_ENABLED,
  MAIL_CONFIG_SET_SYSTEM_EMAIL,
  MAIL_CONFIG_SET_SHOW_DELETE_MODAL,
  MAIL_CONFIG_SET_TEST_EMAIL,
  MAIL_CONFIG_SEND_TEST_MAIL_REQUESTED,
  MAIL_CONFIG_SEND_TEST_MAIL_FULFILLED,
  MAIL_CONFIG_SEND_TEST_MAIL_FAILED,
  MAIL_CONFIG_SUBMIT_MASK_TIMER_DONE
} from './mailConfigActions';

const SUBMIT_MASK_SAVING_MESSAGE = 'Saving';
const SUBMIT_MASK_SENDING_TEST_MAIL_MESSAGE = 'Sending Test Email';
const SUBMIT_MASK_DELETING_MESSAGE = 'Deleting';
export const FAKE_PASSWORD = '\x00\x00\x00\x00\x00';

const initialState = {
  // the data object as it is on the server, based on the last GET or synthesized after the last save
  serverData: null,
  formState: {
    hostname: textInputStateHelpers.initialState(''),
    port: textInputStateHelpers.initialState(''),
    username: textInputStateHelpers.initialState(''),
    password: textInputStateHelpers.initialState(''),
    sslEnabled: false,
    startTlsEnabled: false,
    systemEmail: textInputStateHelpers.initialState(''),
    testEmail: textInputStateHelpers.initialState('')
  },
  isDirty: false,
  isValid: true,
  hasAllRequiredData: false,
  loading: false,
  submitMaskState: null, // one of null, false, or true as patterned in the NxStatefulSubmitMask examples
  submitMaskMessage: null,
  error: null,
  showDeleteModal: false,
  mustReenterPassword: false,
  testEmailSent: false
};

const textProps = ['hostname', 'port', 'username', 'password', 'systemEmail'],
    booleanProps = ['startTlsEnabled', 'sslEnabled'];

const portValidator = combineValidators([validateNonEmpty, validatePatternMatch(/^\d+$/, 'Must be a number')]);

function setFormStateFromServerData(state) {
  const { serverData } = state,
      formState = {
        hostname: textInputStateHelpers.initialState(serverData.hostname),
        port: textInputStateHelpers.initialState(serverData.port.toString()),
        username: textInputStateHelpers.initialState(serverData.username || ''),
        password: textInputStateHelpers.initialState(FAKE_PASSWORD),
        sslEnabled: serverData.sslEnabled,
        startTlsEnabled: serverData.startTlsEnabled,
        systemEmail: textInputStateHelpers.initialState(serverData.systemEmail),
        testEmail: state.formState.testEmail
      };

  return computeHasAllRequiredData({ ...state, formState });
}

function computeHasAllRequiredData(state) {
  const { formState: { hostname, port, systemEmail } } = state,
      hasAllRequiredData = !!(hostname.value && port.value && systemEmail.value);

  return { ...state, hasAllRequiredData };
}

function computeIsDirty(state) {
  const { formState, serverData } = state;

  if (serverData) {
    const isTextPropDirty = prop => formState[prop].trimmedValue !== (serverData[prop] || ''),
        textPropsDirty = any(isTextPropDirty, ['hostname', 'username', 'systemEmail']),
        booleanPropsDirty = any(complement(eqProps(__, formState, serverData)), booleanProps),
        portDirty = serverData.port.toString() !== formState.port.value,
        passwordDirty = formState.password.value !== FAKE_PASSWORD;

    return { ...state, isDirty: textPropsDirty || booleanPropsDirty || portDirty || passwordDirty };
  }
  else {
    const textPropsDirty = any(prop => formState[prop].trimmedValue !== '', textProps),
        booleanPropsDirty = any(propEq(__, true, formState), booleanProps);

    return { ...state, isDirty: textPropsDirty || booleanPropsDirty };
  }
}

function computeIsValid(state) {
  const { formState } = state,
      validationErrorsByProp = map(prop('validationErrors'), pick(textProps, formState)),
      isValid = !any(hasValidationErrors, values(validationErrorsByProp));

  return { ...state, isValid };
}

function computeMustReenterPassword(state) {
  const { serverData } = state;

  if (!serverData) {
    return { ...state, mustReenterPassword: false };
  }

  const { formState } = state,
      hostname = formState.hostname.value,
      port = formState.port.value,
      password = formState.password.value,
      serverHostname = serverData.hostname,
      serverPort = serverData.port.toString();

  return {
    ...state,
    mustReenterPassword: (hostname !== serverHostname || port !== serverPort) && password === FAKE_PASSWORD
  };
}

const updatedComputedProps = compose(computeHasAllRequiredData, computeIsDirty, computeIsValid,
    computeMustReenterPassword);

function loadRequested() {
  return {
    ...initialState,
    loading: true
  };
}

function loadFulfilled(payload, state) {
  return setFormStateFromServerData({
    ...state,
    loading: false,
    isDirty: false,
    error: null,
    submitMaskState: initialState.submitMaskState,
    submitMaskMessage: initialState.submitMaskMessage,
    serverData: payload,
    mustReenterPassword: false,
    testEmailSent: false
  });
}

const resetForm = (_, state) => state.serverData ? loadFulfilled(state.serverData, state) : initialState;

function loadFailed(payload) {
  // 404 is fine, it just means there is no configuration
  const error = payload.response && payload.response.status === 404 ? null : Messages.getHttpErrorMessage(payload);

  return {
    ...initialState,
    loading: false,
    error
  };
}

function saveRequested(payload, state) {
  return { ...state, submitMaskState: false, submitMaskMessage: SUBMIT_MASK_SAVING_MESSAGE, testEmailSent: false };
}

function saveFulfilled(payload, state) {
  return setFormStateFromServerData({
    ...state,
    loading: false,
    submitMaskState: true,
    isDirty: false,
    error: null,
    serverData: payload
  });
}

function saveFailed(payload, state) {
  return {
    ...state,
    loading: false,
    submitMaskState: null,
    error: Messages.getHttpErrorMessage(payload)
  };
}

function sendTestMailRequested(payload, state) {
  return {
    ...state,
    submitMaskState: false,
    submitMaskMessage: SUBMIT_MASK_SENDING_TEST_MAIL_MESSAGE
  };
}

function sendTestMailFulfilled(payload, state) {
  return {
    ...state,
    submitMaskState: true,
    testEmailSent: true,
    error: null
  };
}

function sendTestMailFailed(payload, state) {
  return {
    ...state,
    submitMaskState: null,
    error: Messages.getHttpErrorMessage(payload),
    testEmailSent: false
  };
}

function deleteRequested(payload, state) {
  return {
    ...state,
    submitMaskState: false,
    submitMaskMessage: SUBMIT_MASK_DELETING_MESSAGE,
    showDeleteModal: false
  };
}

function deleteFulfilled() {
  return { ...initialState, submitMaskState: true, error: null };
}

function deleteFailed(payload, state) {
  return {
    ...state,
    loading: false,
    submitMaskState: null,
    error: Messages.getHttpErrorMessage(payload)
  };
}

const setTextInput = curryN(4, function setTextInput(fieldName, validator, payload, state) {
  const stateWithUpdatedValue =
      pathSet(['formState', fieldName], textInputStateHelpers.userInput(validator, payload), state);

  return updatedComputedProps(stateWithUpdatedValue);
});

const setCheckbox = curryN(3, function setCheckbox(fieldName, payload, state) {
  const stateWithUpdatedValue = pathSet(['formState', fieldName], payload, state);

  return updatedComputedProps(stateWithUpdatedValue);
});

const reducerActionMap = {
  [MAIL_CONFIG_LOAD_REQUESTED]: loadRequested,
  [MAIL_CONFIG_LOAD_FULFILLED]: loadFulfilled,
  [MAIL_CONFIG_LOAD_FAILED]: loadFailed,
  [MAIL_CONFIG_SAVE_REQUESTED]: saveRequested,
  [MAIL_CONFIG_SAVE_FULFILLED]: saveFulfilled,
  [MAIL_CONFIG_SAVE_FAILED]: saveFailed,
  [MAIL_CONFIG_DELETE_REQUESTED]: deleteRequested,
  [MAIL_CONFIG_DELETE_FULFILLED]: deleteFulfilled,
  [MAIL_CONFIG_DELETE_FAILED]: deleteFailed,
  [MAIL_CONFIG_RESET_FORM]: resetForm,
  [MAIL_CONFIG_SET_HOSTNAME]: setTextInput('hostname', validateNonEmpty),
  [MAIL_CONFIG_SET_PORT]: setTextInput('port', portValidator),
  [MAIL_CONFIG_SET_USERNAME]: setTextInput('username', null),
  [MAIL_CONFIG_SET_PASSWORD]: setTextInput('password', null),
  [MAIL_CONFIG_SET_SSL_ENABLED]: setCheckbox('sslEnabled'),
  [MAIL_CONFIG_SET_STARTTLS_ENABLED]: setCheckbox('startTlsEnabled'),
  [MAIL_CONFIG_SET_SYSTEM_EMAIL]: setTextInput('systemEmail', validateNonEmpty),
  [MAIL_CONFIG_SEND_TEST_MAIL_REQUESTED]: sendTestMailRequested,
  [MAIL_CONFIG_SEND_TEST_MAIL_FULFILLED]: sendTestMailFulfilled,
  [MAIL_CONFIG_SEND_TEST_MAIL_FAILED]: sendTestMailFailed,
  [MAIL_CONFIG_SET_TEST_EMAIL]: setTextInput('testEmail', null),
  [MAIL_CONFIG_SET_SHOW_DELETE_MODAL]: propSet('showDeleteModal'),
  [MAIL_CONFIG_SUBMIT_MASK_TIMER_DONE]: propSetConst('submitMaskState', null)
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
