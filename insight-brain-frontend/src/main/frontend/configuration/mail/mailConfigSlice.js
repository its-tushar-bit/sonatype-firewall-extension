/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { nxTextInputStateHelpers, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { __, any, complement, compose, curryN, eqProps, map, pick, prop, propEq, values } from 'ramda';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';

import {
  combineValidators,
  hasValidationErrors,
  validateEmailPatternMatch,
  validateHostname,
  validateNonEmpty,
  validatePatternMatch,
} from '../../util/validationUtil';
import { pathSet } from '../../util/jsUtil';
import { propSet, propSetConst } from '../../util/reduxToolkitUtil';
import { getMailConfigUrl, getTestMailUrl } from '../../util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';

const SUBMIT_MASK_SAVING_MESSAGE = 'Saving';
const SUBMIT_MASK_SENDING_TEST_MAIL_MESSAGE = 'Sending Test Email';
const SUBMIT_MASK_DELETING_MESSAGE = 'Deleting';
export const FAKE_PASSWORD = '\x00\x00\x00\x00\x00';

const REDUCER_NAME = 'mailConfig';

const hostnameValidator = combineValidators([validateNonEmpty, validateHostname]);
const systemEmailValidator = combineValidators([validateNonEmpty, validateEmailPatternMatch('Invalid system email')]);
const portValidator = combineValidators([validateNonEmpty, validatePatternMatch(/^\d+$/, 'Must be a number')]);

const initialState = {
  // the data object as it is on the server, based on the last GET or synthesized after the last save
  serverData: null,
  formState: {
    hostname: nxTextInputStateHelpers.initialState('', hostnameValidator),
    port: nxTextInputStateHelpers.initialState('', portValidator),
    username: nxTextInputStateHelpers.initialState(''),
    password: nxTextInputStateHelpers.initialState(''),
    sslEnabled: false,
    startTlsEnabled: false,
    systemEmail: nxTextInputStateHelpers.initialState('', systemEmailValidator),
    testEmail: nxTextInputStateHelpers.initialState(''),
  },
  isDirty: false,
  isValid: false,
  hasAllRequiredData: false,
  loading: false,
  submitMaskState: null, // one of null, false, or true as patterned in the NxStatefulSubmitMask examples
  submitMaskMessage: null,
  loadError: null,
  saveError: null,
  deleteError: null,
  testEmailError: null,
  showDeleteModal: false,
  mustReenterPassword: false,
  testEmailSent: false,
};

const textProps = ['hostname', 'port', 'username', 'password', 'systemEmail'],
  booleanProps = ['startTlsEnabled', 'sslEnabled'];

const clearedErrors = pick(['loadError', 'saveError', 'deleteError', 'testEmailError'], initialState);

function setFormStateFromServerData(state) {
  const { serverData } = state,
    formState = {
      hostname: nxTextInputStateHelpers.initialState(serverData.hostname),
      port: nxTextInputStateHelpers.initialState(serverData.port.toString()),
      username: nxTextInputStateHelpers.initialState(serverData.username || ''),
      password: nxTextInputStateHelpers.initialState(FAKE_PASSWORD),
      sslEnabled: serverData.sslEnabled,
      startTlsEnabled: serverData.startTlsEnabled,
      systemEmail: nxTextInputStateHelpers.initialState(serverData.systemEmail),
      testEmail: state.formState.testEmail,
    };

  return computeHasAllRequiredData({ ...state, formState });
}

function computeHasAllRequiredData(state) {
  const {
      formState: { hostname, port, systemEmail },
    } = state,
    hasAllRequiredData = !!(hostname.value && port.value && systemEmail.value);

  return { ...state, hasAllRequiredData };
}

function computeIsDirty(state) {
  const { formState, serverData } = state;

  if (serverData) {
    const isTextPropDirty = (prop) => formState[prop].trimmedValue !== (serverData[prop] || ''),
      textPropsDirty = any(isTextPropDirty, ['hostname', 'username', 'systemEmail']),
      booleanPropsDirty = any(complement(eqProps(__, formState, serverData)), booleanProps),
      portDirty = serverData.port.toString() !== formState.port.value,
      passwordDirty = formState.password.value !== FAKE_PASSWORD;

    return {
      ...state,
      isDirty: textPropsDirty || booleanPropsDirty || portDirty || passwordDirty,
    };
  } else {
    const textPropsDirty = any((prop) => formState[prop].trimmedValue !== '', textProps),
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
    mustReenterPassword: (hostname !== serverHostname || port !== serverPort) && password === FAKE_PASSWORD,
  };
}

const updatedComputedProps = compose(
  computeHasAllRequiredData,
  computeIsDirty,
  computeIsValid,
  computeMustReenterPassword
);

function loadRequested() {
  return {
    ...initialState,
    loading: true,
  };
}

function loadFulfilled(state, { payload }) {
  return setFormStateFromServerData({
    ...state,
    loading: false,
    isDirty: false,
    ...clearedErrors,
    submitMaskState: initialState.submitMaskState,
    submitMaskMessage: initialState.submitMaskMessage,
    serverData: payload,
    mustReenterPassword: false,
    testEmailSent: false,
  });
}

const resetForm = (state) => (state.serverData ? loadFulfilled(state, { payload: state.serverData }) : initialState);

function loadFailed(state, { payload }) {
  // 404 is fine, it just means there is no configuration
  const error = payload.response && payload.response.status === 404 ? null : payload;

  return {
    ...initialState,
    loading: false,
    ...clearedErrors,
    loadError: Messages.getHttpErrorMessage(error),
  };
}

function saveRequested(state) {
  return {
    ...state,
    submitMaskState: false,
    submitMaskMessage: SUBMIT_MASK_SAVING_MESSAGE,
    testEmailSent: false,
    ...clearedErrors,
  };
}

function saveFulfilled(state, { payload }) {
  return setFormStateFromServerData({
    ...state,
    loading: false,
    submitMaskState: true,
    isDirty: false,
    ...clearedErrors,
    serverData: payload,
  });
}

function saveFailed(state, { payload }) {
  return {
    ...state,
    loading: false,
    submitMaskState: null,
    ...clearedErrors,
    saveError: Messages.getHttpErrorMessage(payload),
  };
}

function sendTestEmailRequested(state) {
  return {
    ...state,
    submitMaskState: false,
    submitMaskMessage: SUBMIT_MASK_SENDING_TEST_MAIL_MESSAGE,
  };
}

function sendTestEmailFulfilled(state) {
  return {
    ...state,
    submitMaskState: true,
    testEmailSent: true,
    testEmailError: null,
  };
}

function sendTestEmailFailed(state, { payload }) {
  return {
    ...state,
    submitMaskState: null,
    testEmailError: Messages.getHttpErrorMessage(payload),
    testEmailSent: false,
  };
}

function deleteRequested(state) {
  return {
    ...state,
    submitMaskState: false,
    submitMaskMessage: SUBMIT_MASK_DELETING_MESSAGE,
    ...clearedErrors,
  };
}

function deleteFulfilled() {
  return { ...initialState, submitMaskState: true, showDeleteModal: false, ...clearedErrors };
}

function deleteFailed(state, { payload }) {
  return {
    ...state,
    loading: false,
    submitMaskState: null,
    ...clearedErrors,
    deleteError: Messages.getHttpErrorMessage(payload),
  };
}

const setTextInput = curryN(4, function setTextInput(fieldName, validator, state, { payload }) {
  const stateWithUpdatedValue = pathSet(
    ['formState', fieldName],
    nxTextInputStateHelpers.userInput(validator, payload),
    state
  );

  return updatedComputedProps(stateWithUpdatedValue);
});

const setCheckbox = curryN(3, function setCheckbox(fieldName, state, { payload }) {
  const stateWithUpdatedValue = pathSet(['formState', fieldName], payload, state);

  return updatedComputedProps(stateWithUpdatedValue);
});

const load = createAsyncThunk(`${REDUCER_NAME}/load`, (_, { rejectWithValue }) => {
  return axios.get(getMailConfigUrl()).then(prop('data')).catch(rejectWithValue);
});

const sendTestEmail = createAsyncThunk(
  `${REDUCER_NAME}/sendTestEmail`,
  (_, { getState, dispatch, rejectWithValue }) => {
    const { formState } = getState().mailConfig;

    return axios
      .post(getTestMailUrl(formState.testEmail.trimmedValue), toServerData(formState))
      .then(prop('data'))
      .then((data) => {
        startSubmitMaskSuccessTimer(dispatch);
        return data;
      })
      .catch(rejectWithValue);
  }
);

const save = createAsyncThunk(`${REDUCER_NAME}/save`, (_, { getState, dispatch, rejectWithValue }) => {
  const formState = getState().mailConfig.formState,
    serverData = toServerData(formState);

  return axios
    .put(getMailConfigUrl(), serverData)
    .then(() => {
      startSubmitMaskSuccessTimer(dispatch);
      return serverData;
    })
    .catch(rejectWithValue);
});

const del = createAsyncThunk(`${REDUCER_NAME}/delete`, (_, { dispatch, rejectWithValue }) => {
  return axios
    .delete(getMailConfigUrl())
    .then(() => {
      startSubmitMaskSuccessTimer(dispatch);
    })
    .catch(rejectWithValue);
});

function startSubmitMaskSuccessTimer(dispatch) {
  setTimeout(() => {
    dispatch(actions.submitMaskTimerDone());
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

function toServerData(formState) {
  // pull the trimmedValue out of the input state object and convert empty strings to null
  const textPropMapper = ({ trimmedValue }) => trimmedValue || null;

  return {
    ...pick(['startTlsEnabled', 'sslEnabled'], formState),
    ...map(textPropMapper, pick(['hostname', 'username', 'systemEmail'], formState)),
    port: parseInt(formState.port.trimmedValue, 10),
    password: formState.password.value || null,
    passwordIsIncluded: formState.password.value !== FAKE_PASSWORD,
  };
}

const mailConfigSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    resetForm: resetForm,
    setHostname: setTextInput('hostname', hostnameValidator),
    setPort: setTextInput('port', portValidator),
    setUsername: setTextInput('username', null),
    setPassword: setTextInput('password', null),
    setSslEnabled: setCheckbox('sslEnabled'),
    setStartTlsEnabled: setCheckbox('startTlsEnabled'),
    setSystemEmail: setTextInput('systemEmail', systemEmailValidator),
    setTestEmail: setTextInput('testEmail', null),
    setShowDeleteModal: propSet('showDeleteModal'),
    submitMaskTimerDone: propSetConst('submitMaskState', null),
  },
  extraReducers: {
    [load.pending]: loadRequested,
    [load.fulfilled]: loadFulfilled,
    [load.rejected]: loadFailed,
    [save.pending]: saveRequested,
    [save.fulfilled]: saveFulfilled,
    [save.rejected]: saveFailed,
    [del.pending]: deleteRequested,
    [del.fulfilled]: deleteFulfilled,
    [del.rejected]: deleteFailed,
    [sendTestEmail.pending]: sendTestEmailRequested,
    [sendTestEmail.fulfilled]: sendTestEmailFulfilled,
    [sendTestEmail.rejected]: sendTestEmailFailed,
  },
});

export default mailConfigSlice.reducer;
export const actions = {
  ...mailConfigSlice.actions,
  load,
  save,
  del,
  sendTestEmail,
};
