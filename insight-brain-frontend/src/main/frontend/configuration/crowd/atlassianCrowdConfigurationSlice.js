/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { any, compose, curryN, map, pick, prop } from 'ramda';

import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { validateNonEmpty } from 'MainRoot/util/validationUtil';
import { pathSet } from 'MainRoot/util/jsUtil';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { getCrowdConfigurationTestUrl, getCrowdConfigurationUrl } from 'MainRoot/util/CLMLocation';
import { propSet, propSetConst } from 'MainRoot/util/reduxToolkitUtil';
import {
  selectFormState,
  selectServerData,
  selectIsDirty,
} from 'MainRoot/configuration/crowd/atlassianCrowdConfigurationSelectors';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS, nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import { FAKE_PASSWORD } from './util';

const REDUCER_NAME = 'atlassianCrowdConfiguration';

export const initialState = {
  loading: false,
  loadError: null,
  updateError: null,
  deleteError: null,
  testError: null,
  testSuccessMessage: null,
  isDirty: false,
  submitMaskState: null,
  submitMaskMessage: null,
  deleteMaskState: null,
  formState: {
    serverUrl: nxTextInputStateHelpers.initialState(''),
    applicationName: nxTextInputStateHelpers.initialState(''),
    applicationPassword: nxTextInputStateHelpers.initialState(''),
  },
  serverData: null,
  mustReenterPassword: false,
  showModal: false,
};

const clearedErrors = pick(['loadError', 'updateError', 'deleteError', 'testError'], initialState);
const textProps = ['serverUrl', 'applicationName', 'applicationPassword'];

function setFormStateFromServerData(state) {
  const { serverData } = state,
    formState = {
      serverUrl: nxTextInputStateHelpers.initialState(serverData.serverUrl),
      applicationName: nxTextInputStateHelpers.initialState(serverData.applicationName),
      applicationPassword: nxTextInputStateHelpers.initialState(FAKE_PASSWORD),
    };

  return { ...state, formState };
}

function computeIsDirty(state) {
  const { formState, serverData } = state;

  if (serverData) {
    const isDirty =
      serverData.serverUrl !== formState.serverUrl.trimmedValue ||
      serverData.applicationName !== formState.applicationName.trimmedValue ||
      FAKE_PASSWORD !== formState.applicationPassword.trimmedValue;
    return { ...state, isDirty };
  } else {
    const textPropsDirty = any((prop) => formState[prop].trimmedValue !== '', textProps);
    return { ...state, isDirty: textPropsDirty };
  }
}

const setTextInput = curryN(4, function setTextInput(fieldName, validator, state, payload) {
  const stateWithUpdatedValue = pathSet(
    ['formState', fieldName],
    nxTextInputStateHelpers.userInput(validator, payload.payload),
    state
  );

  return computeIsDirty(stateWithUpdatedValue);
});

const startSubmitMaskSuccessTimer = (dispatch) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(dispatch(actions.submitMaskTimerDone()));
    }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
  });
};

const startDeleteMaskSuccessTimer = (dispatch) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(dispatch(actions.deleteMaskTimerDone()));
    }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
  });
};

function toServerData(formState) {
  const textPropMapper = ({ trimmedValue }) => trimmedValue || null;

  return {
    ...map(textPropMapper, pick(['serverUrl', 'applicationName'], formState)),
    applicationPassword:
      !formState.applicationPassword.trimmedValue || formState.applicationPassword.trimmedValue === FAKE_PASSWORD
        ? null
        : formState.applicationPassword.trimmedValue,
  };
}

function loadFulfilled(state, { payload }) {
  return resetForm({
    ...state,
    serverData: payload,
  });
}

function loadFailed(state, { payload }) {
  return {
    ...state,
    loading: false,
    ...clearedErrors,
    testSuccessMessage: null,
    loadError: payload.response?.status === 404 ? null : Messages.getHttpErrorMessage(payload),
  };
}

function updateRequested(state) {
  return {
    ...state,
    ...clearedErrors,
    testSuccessMessage: null,
    submitMaskState: false,
    submitMaskMessage: 'Saving…',
  };
}

const updateFulfilled = (state) => {
  return {
    ...state,
    submitMaskState: true,
    isDirty: false,
    serverData: {
      serverUrl: state.formState.serverUrl.trimmedValue,
      applicationName: state.formState.applicationName.trimmedValue,
    },
  };
};

function updateFailed(state, { payload }) {
  return {
    ...state,
    updateError: Messages.getHttpErrorMessage(payload),
    submitMaskState: null,
  };
}

function resetForm(state) {
  if (state.serverData) {
    return setFormStateFromServerData({
      ...initialState,
      serverData: state.serverData,
    });
  }

  return initialState;
}

function deleteRequested(state) {
  return {
    ...state,
    ...clearedErrors,
    testSuccessMessage: null,
    deleteMaskState: false,
    submitMaskMessage: 'Deleting…',
  };
}

function deleteFulfilled() {
  return {
    ...initialState,
    showModal: true,
    deleteMaskState: true,
  };
}

function deleteFailed(state, { payload }) {
  return {
    ...state,
    deleteMaskState: null,
    deleteError: Messages.getHttpErrorMessage(payload),
  };
}

function testConnectionRequested(state) {
  return {
    ...state,
    ...clearedErrors,
    testSuccessMessage: null,
    submitMaskState: false,
    submitMaskMessage: 'Testing…',
  };
}

function testConnectionFulfilled(state, { payload }) {
  const nextState = { ...state, submitMaskState: null };
  if (payload.code === 200) {
    nextState.testSuccessMessage = 'Success!';
  } else {
    nextState.testError = Messages.getHttpErrorMessage(payload.message);
  }
  return nextState;
}

function testConnectionFailed(state, { payload }) {
  return {
    ...state,
    submitMaskState: null,
    testError: Messages.getHttpErrorMessage(payload),
  };
}

function resetTestAlertMessages(state) {
  return {
    ...state,
    testError: null,
    testSuccessMessage: null,
  };
}

const load = createAsyncThunk(`${REDUCER_NAME}/load`, (_, { rejectWithValue }) => {
  return axios.get(getCrowdConfigurationUrl()).then(prop('data')).catch(rejectWithValue);
});

const del = createAsyncThunk(`${REDUCER_NAME}/delete`, (_, { dispatch, rejectWithValue }) => {
  return axios
    .delete(getCrowdConfigurationUrl())
    .then(() => {
      startDeleteMaskSuccessTimer(dispatch).then(() => dispatch(actions.setShowModal(false)));
    })
    .catch(rejectWithValue);
});

const update = createAsyncThunk(`${REDUCER_NAME}/update`, (_, { getState, dispatch, rejectWithValue }) => {
  const state = getState();
  const formState = selectFormState(state);
  const serverData = toServerData(formState);
  return axios
    .put(getCrowdConfigurationUrl(), { ...serverData })
    .then(() => {
      startSubmitMaskSuccessTimer(dispatch).then(() => dispatch(load()));
    })
    .catch(rejectWithValue);
});

const test = createAsyncThunk(`${REDUCER_NAME}/test`, (_, { getState, dispatch, rejectWithValue }) => {
  const state = getState();
  const isDirty = selectIsDirty(state);
  const isConfigured = selectServerData(state);
  const serverData = isConfigured && !isDirty ? undefined : toServerData(selectFormState(state));
  return axios
    .post(getCrowdConfigurationTestUrl(), serverData)
    .then(({ data }) => {
      startSubmitMaskSuccessTimer(dispatch);
      return data;
    })
    .catch(rejectWithValue);
});

const atlassianCrowdConfigurationSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    resetTestAlertMessages,
    resetForm,
    setInputValueServerUrl: setTextInput('serverUrl', validateNonEmpty),
    setInputValueApplicationName: setTextInput('applicationName', validateNonEmpty),
    setInputValueApplicationPassword: setTextInput('applicationPassword', validateNonEmpty),
    submitMaskTimerDone: propSetConst('submitMaskState', null),
    deleteMaskTimerDone: propSetConst('deleteMaskState', null),
    setShowModal: compose(propSetConst('deleteError', null), propSet('showModal')),
  },
  extraReducers: {
    [load.pending]: propSetConst('loading', true),
    [load.fulfilled]: loadFulfilled,
    [load.rejected]: loadFailed,
    [del.pending]: deleteRequested,
    [del.fulfilled]: deleteFulfilled,
    [del.rejected]: deleteFailed,
    [update.pending]: updateRequested,
    [update.fulfilled]: updateFulfilled,
    [update.rejected]: updateFailed,
    [test.pending]: testConnectionRequested,
    [test.fulfilled]: testConnectionFulfilled,
    [test.rejected]: testConnectionFailed,
  },
});

export default atlassianCrowdConfigurationSlice.reducer;
export const actions = {
  ...atlassianCrowdConfigurationSlice.actions,
  load,
  del,
  update,
  test,
};
