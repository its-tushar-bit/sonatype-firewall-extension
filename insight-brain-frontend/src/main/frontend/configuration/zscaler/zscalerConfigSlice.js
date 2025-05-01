/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { nxTextInputStateHelpers, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { __, any, compose, curryN, map, pick, prop, propEq, values } from 'ramda';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';

import { hasValidationErrors, validateNonEmpty } from '../../util/validationUtil';
import { pathSet } from '../../util/jsUtil';
import { propSet, propSetConst } from '../../util/reduxToolkitUtil';
import { getZScalerConfigUrl } from '../../util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';

const SUBMIT_MASK_SAVING_MESSAGE = 'Saving';
const SUBMIT_MASK_DELETING_MESSAGE = 'Deleting';
export const FAKE_PASSWORD = '\x00\x00\x00\x00\x00';

const REDUCER_NAME = 'zscalerConfig';

export const initialState = {
  // the data object as it is on the server, based on the last GET or synthesized after the last save
  serverData: null,
  formState: {
    username: nxTextInputStateHelpers.initialState('', validateNonEmpty),
    password: nxTextInputStateHelpers.initialState('', validateNonEmpty),
    hostname: nxTextInputStateHelpers.initialState('', validateNonEmpty),
    apiKey: nxTextInputStateHelpers.initialState('', validateNonEmpty),
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
  showDeleteModal: false,
  mustReenterPassword: false,
};

const textProps = ['username', 'password', 'hostname', 'apiKey'];

const clearedErrors = pick(['loadError', 'saveError', 'deleteError'], initialState);

function setFormStateFromServerData(state) {
  const { serverData } = state,
    formState = {
      username: nxTextInputStateHelpers.initialState(serverData.username),
      password: nxTextInputStateHelpers.initialState(FAKE_PASSWORD),
      hostname: nxTextInputStateHelpers.initialState(serverData.hostname),
      apiKey: nxTextInputStateHelpers.initialState(serverData.apiKey),
    };

  return computeHasAllRequiredData({ ...state, formState });
}

function computeHasAllRequiredData(state) {
  const {
      formState: { username, password, hostname, apiKey },
    } = state,
    hasAllRequiredData = !!(username.value && password.value && hostname.value && apiKey.value);

  return { ...state, hasAllRequiredData };
}

function computeIsDirty(state) {
  const { formState, serverData } = state;

  if (serverData) {
    const isTextPropDirty = (prop) => formState[prop].trimmedValue !== (serverData[prop] || ''),
      textPropsDirty = any(isTextPropDirty, ['username', 'hostname', 'apiKey']),
      passwordDirty = formState.password.value !== FAKE_PASSWORD;

    return {
      ...state,
      isDirty: textPropsDirty || passwordDirty,
    };
  } else {
    const textPropsDirty = any((prop) => formState[prop].trimmedValue !== '', textProps),
      booleanPropsDirty = any(propEq(__, true, formState));

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
  const { formState, serverData } = state;

  if (!serverData) {
    return { ...state, mustReenterPassword: false };
  }

  const isTextPropDirty = (prop) => formState[prop].trimmedValue !== (serverData[prop] || ''),
    textPropsDirty = any(isTextPropDirty, ['username', 'hostname', 'apiKey']),
    password = formState.password.value;

  return {
    ...state,
    mustReenterPassword: textPropsDirty && password === FAKE_PASSWORD,
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

const load = createAsyncThunk(`${REDUCER_NAME}/load`, (_, { rejectWithValue }) => {
  return axios.get(getZScalerConfigUrl()).then(prop('data')).catch(rejectWithValue);
});

const save = createAsyncThunk(`${REDUCER_NAME}/save`, (_, { getState, dispatch, rejectWithValue }) => {
  const formState = getState().zscalerConfig.formState,
    serverData = toServerData(formState);

  return axios
    .put(getZScalerConfigUrl(), serverData)
    .then(() => {
      startSubmitMaskSuccessTimer(dispatch);
      return serverData;
    })
    .catch(rejectWithValue);
});

const del = createAsyncThunk(`${REDUCER_NAME}/delete`, (_, { dispatch, rejectWithValue }) => {
  return axios
    .delete(getZScalerConfigUrl())
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
    ...map(textPropMapper, pick(['hostname', 'username', 'apiKey'], formState)),
    password: formState.password.value || null,
  };
}

const zscalerConfigSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    resetForm: resetForm,
    setUsername: setTextInput('username', validateNonEmpty),
    setPassword: setTextInput('password', validateNonEmpty),
    setHostname: setTextInput('hostname', validateNonEmpty),
    setApiKey: setTextInput('apiKey', validateNonEmpty),
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
  },
});

export default zscalerConfigSlice.reducer;
export const actions = {
  ...zscalerConfigSlice.actions,
  load,
  save,
  del,
};
