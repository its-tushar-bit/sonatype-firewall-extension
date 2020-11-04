/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as textInputStateHelpers from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';
import { any, compose, curryN, map, pick, prop, values } from 'ramda';

import { createReducerFromActionMap, propSetConst } from '../../util/reduxUtil';
import { pathSet, propSet } from '../../util/jsUtil';
import { combineValidators, hasValidationErrors, validateNonEmpty, validatePatternMatch }
  from '../../util/validationUtil';

import {
  PROXY_CONFIG_LOAD_REQUESTED,
  PROXY_CONFIG_LOAD_FULFILLED,
  PROXY_CONFIG_LOAD_FAILED,
  PROXY_CONFIG_SAVE_REQUESTED,
  PROXY_CONFIG_SAVE_FULFILLED,
  PROXY_CONFIG_SAVE_FAILED,
  PROXY_CONFIG_DELETE_REQUESTED,
  PROXY_CONFIG_DELETE_FULFILLED,
  PROXY_CONFIG_DELETE_FAILED,
  PROXY_CONFIG_RESET_FORM,
  PROXY_CONFIG_SET_HOSTNAME,
  PROXY_CONFIG_SET_PORT,
  PROXY_CONFIG_SET_USERNAME,
  PROXY_CONFIG_SET_PASSWORD,
  PROXY_CONFIG_SET_EXCLUDE_HOSTS,
  PROXY_CONFIG_SET_SHOW_DELETE_MODAL,
  PROXY_CONFIG_SUBMIT_MASK_TIMER_DONE
} from './proxyConfigActions';

const SUBMIT_MASK_SAVING_MESSAGE = 'Saving';
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
    excludeHosts: textInputStateHelpers.initialState('')
  },
  isDirty: false,
  isValid: true,
  hasAllRequiredData: false,
  loading: false,
  submitMaskState: null, // one of null, false, or true as patterned in the NxStatefulSubmitMask examples
  submitMaskMessage: null,
  loadError: null,
  saveError: null,
  deleteError: null,
  showDeleteModal: false,
  mustReenterPassword: false
};

const textProps = ['hostname', 'port', 'username', 'password', 'excludeHosts'];

const portValidator = combineValidators([validateNonEmpty, validatePatternMatch(/^\d+$/, 'Must be a number')]);

const clearedErrors = pick(['loadError', 'saveError', 'deleteError'], initialState);

function setFormStateFromServerData(state) {
  const { serverData } = state,
      formState = {
        hostname: textInputStateHelpers.initialState(serverData.hostname),
        port: textInputStateHelpers.initialState(serverData.port.toString()),
        username: textInputStateHelpers.initialState(serverData.username || ''),
        password: textInputStateHelpers.initialState(FAKE_PASSWORD),
        excludeHosts: serverData.excludeHosts ? textInputStateHelpers.initialState(serverData.excludeHosts.join(',')) :
          textInputStateHelpers.initialState('')
      };

  return computeHasAllRequiredData({ ...state, formState });
}

function computeHasAllRequiredData(state) {
  const { formState: { hostname, port } } = state,
      hasAllRequiredData = !!(hostname.value && port.value);

  return { ...state, hasAllRequiredData };
}

function computeIsDirty(state) {
  const { formState, serverData } = state;

  if (serverData) {
    const isTextPropDirty = prop => formState[prop].trimmedValue !== (serverData[prop] || ''),
        textPropsDirty = any(isTextPropDirty, ['hostname', 'username', 'excludeHosts']),
        portDirty = serverData.port.toString() !== formState.port.value,
        passwordDirty = formState.password.value !== FAKE_PASSWORD;

    return { ...state, isDirty: textPropsDirty || portDirty || passwordDirty };
  }
  else {
    const textPropsDirty = any(prop => formState[prop].trimmedValue !== '', textProps);
    return { ...state, isDirty: textPropsDirty };
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

function loadFulfilled(payload, state) {
  return setFormStateFromServerData({
    ...state,
    loading: false,
    isDirty: false,
    ...clearedErrors,
    submitMaskState: initialState.submitMaskState,
    submitMaskMessage: initialState.submitMaskMessage,
    serverData: payload,
    mustReenterPassword: false
  });
}

const resetForm = (_, state) => state.serverData ? loadFulfilled(state.serverData, state) : initialState;

function loadFailed(payload) {
  return {
    ...initialState,
    loading: false,
    loadError: payload.response && payload.response.status === 404 ? null : payload
  };
}

function saveRequested(payload, state) {
  return { ...state, submitMaskState: false, submitMaskMessage: SUBMIT_MASK_SAVING_MESSAGE };
}

function saveFulfilled(payload, state) {
  return setFormStateFromServerData({
    ...state,
    loading: false,
    submitMaskState: true,
    isDirty: false,
    ...clearedErrors,
    serverData: payload
  });
}

function saveFailed(payload, state) {
  return {
    ...state,
    loading: false,
    submitMaskState: null,
    ...clearedErrors,
    saveError: payload
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
  return { ...initialState, submitMaskState: true, ...clearedErrors };
}

function deleteFailed(payload, state) {
  return {
    ...state,
    loading: false,
    submitMaskState: null,
    ...clearedErrors,
    deleteError: payload
  };
}

const setTextInput = curryN(4, function setTextInput(fieldName, validator, payload, state) {
  const stateWithUpdatedValue =
      pathSet(['formState', fieldName], textInputStateHelpers.userInput(validator, payload), state);

  return updatedComputedProps(stateWithUpdatedValue);
});

const reducerActionMap = {
  [PROXY_CONFIG_LOAD_REQUESTED]: propSetConst('loading', true),
  [PROXY_CONFIG_LOAD_FULFILLED]: loadFulfilled,
  [PROXY_CONFIG_LOAD_FAILED]: loadFailed,
  [PROXY_CONFIG_SAVE_REQUESTED]: saveRequested,
  [PROXY_CONFIG_SAVE_FULFILLED]: saveFulfilled,
  [PROXY_CONFIG_SAVE_FAILED]: saveFailed,
  [PROXY_CONFIG_DELETE_REQUESTED]: deleteRequested,
  [PROXY_CONFIG_DELETE_FULFILLED]: deleteFulfilled,
  [PROXY_CONFIG_DELETE_FAILED]: deleteFailed,
  [PROXY_CONFIG_RESET_FORM]: resetForm,
  [PROXY_CONFIG_SET_HOSTNAME]: setTextInput('hostname', validateNonEmpty),
  [PROXY_CONFIG_SET_PORT]: setTextInput('port', portValidator),
  [PROXY_CONFIG_SET_USERNAME]: setTextInput('username', null),
  [PROXY_CONFIG_SET_PASSWORD]: setTextInput('password', null),
  [PROXY_CONFIG_SET_EXCLUDE_HOSTS]: setTextInput('excludeHosts', null),
  [PROXY_CONFIG_SET_SHOW_DELETE_MODAL]: propSet('showDeleteModal'),
  [PROXY_CONFIG_SUBMIT_MASK_TIMER_DONE]: propSetConst('submitMaskState', null)
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
