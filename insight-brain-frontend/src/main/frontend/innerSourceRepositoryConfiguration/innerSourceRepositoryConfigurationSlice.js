/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { pathSet, propSet, propSetConst } from 'MainRoot/util/reduxToolkitUtil';
import { nxTextInputStateHelpers, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { compose, curryN } from 'ramda';
import axios from 'axios';
import { getRepositoryConnectionUrl, getTestRepositoryConnectionUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/util/CommonServices';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import {
  selectFormState,
  selectIsDirty,
  selectOwnerTypeAndOwnerId,
  selectRepositoryConnectionId,
} from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryConfigurationSelectors';
import {
  getOriginalValues,
  toFormState,
  toServerData,
} from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryConfigurationUtil';

export const FAKE_PASSWORD = '\x00\x00\x00\x00\x00';
export const SUBMIT_MASK_SAVING_CONFIGURATION_MESSAGE = 'Saving Configuration';
export const SUBMIT_MASK_TESTING_CONFIGURATION_MESSAGE = 'Testing Configuration';
export const SUBMIT_MASK_DELETING_CONFIGURATION_MESSAGE = 'Deleting Configuration';
export const MISSING_OR_INVALID_DATA_MESSAGE = 'Fields with missing or invalid data.';
export const NO_CHANGES_MESSAGE = 'No changes have been made.';
export const MUST_REENTER_PASSWORD_MESSAGE = 'Password must be provided.';

const REDUCER_NAME = 'innerSourceRepositoryConfiguration';

export const initialState = {
  serverData: null,
  formState: {
    format: 'generic',
    baseUrlState: nxTextInputStateHelpers.initialState(''),
    isAnonymous: true,
    usernameState: nxTextInputStateHelpers.initialState(''),
    passwordState: nxTextInputStateHelpers.initialState(''),
  },
  loading: false,
  loadConfigurationError: null,
  saveConfigurationError: null,
  testConfigurationSuccessful: false,
  testConfigurationError: null,
  showDeleteModal: false,
  deleteConfigurationError: null,
  deleteSubmitMaskState: null,
  submitMaskState: null,
  submitMaskMessage: null,
};

const setTextInput = curryN(4, function setTextInput(fieldName, validator, state, { payload }) {
  const newState = {
    ...state,
    formState: {
      ...state.formState,
    },
  };
  newState.formState[fieldName] = nxTextInputStateHelpers.userInput(validator, payload);
  return newState;
});

function resetFormState(state) {
  const originalValues = getOriginalValues(state.serverData);
  return {
    ...state,
    formState: {
      ...state.formState,
      format: originalValues.format,
      baseUrlState: nxTextInputStateHelpers.initialState(originalValues.baseUrl),
      isAnonymous: originalValues.isAnonymous,
      usernameState: nxTextInputStateHelpers.initialState(originalValues.username),
      passwordState: nxTextInputStateHelpers.initialState(originalValues.password),
    },
  };
}

function resetSaveAndTestConfigurations(state) {
  return {
    ...state,
    saveConfigurationError: null,
    testConfigurationSuccessful: false,
    testConfigurationError: null,
  };
}

function resetDeleteConfiguration(state) {
  return {
    ...state,
    deleteConfigurationError: null,
  };
}

const loadConfiguration = createAsyncThunk(`${REDUCER_NAME}/loadConfiguration`, (_, { getState, rejectWithValue }) => {
  const state = getState(),
    { ownerType, ownerId } = selectOwnerTypeAndOwnerId(state),
    repositoryConnectionId = selectRepositoryConnectionId(state);

  return axios
    .get(getRepositoryConnectionUrl(ownerType, ownerId, repositoryConnectionId))
    .then(({ data }) => data)
    .catch(rejectWithValue);
});

function loadConfigurationRequested() {
  return {
    ...initialState,
    loading: true,
    loadConfigurationError: null,
  };
}

function loadConfigurationFulfilled(state, { payload }) {
  return {
    ...state,
    loading: false,
    serverData: payload,
    formState: toFormState(payload),
  };
}

function loadConfigurationFailed(state, { payload }) {
  return {
    ...state,
    loading: false,
    loadConfigurationError: Messages.getHttpErrorMessage(payload),
  };
}

const saveConfiguration = createAsyncThunk(
  `${REDUCER_NAME}/saveConfiguration`,
  (_, { getState, dispatch, rejectWithValue }) => {
    const state = getState(),
      { ownerType, ownerId } = selectOwnerTypeAndOwnerId(state),
      repositoryConnectionId = selectRepositoryConnectionId(state),
      serverData = toServerData(selectFormState(state));

    if (repositoryConnectionId) {
      return axios
        .put(getRepositoryConnectionUrl(ownerType, ownerId, repositoryConnectionId), serverData)
        .then(({ data }) => {
          startSubmitMaskSuccessTimer(dispatch);
          return data;
        })
        .catch(rejectWithValue);
    } else {
      return axios
        .post(getRepositoryConnectionUrl(ownerType, ownerId), serverData)
        .then(({ data }) => {
          const nextState = stateGo(`repositoryConfiguration.${ownerType}.edit`, {
            ownerId,
            repositoryConnectionId: data.repositoryConnectionId,
          });
          startSubmitMaskSuccessTimer(dispatch, nextState);
          return data;
        })
        .catch(rejectWithValue);
    }
  }
);

function saveConfigurationRequested(state) {
  return {
    ...state,
    submitMaskState: false,
    submitMaskMessage: SUBMIT_MASK_SAVING_CONFIGURATION_MESSAGE,
    saveConfigurationError: null,
  };
}

function saveConfigurationFulfilled(state, { payload }) {
  const serverData = { ...payload };
  if (!state.formState.isAnonymous) {
    serverData.password = state.formState.passwordState.trimmedValue;
  }
  return {
    ...state,
    submitMaskState: true,
    serverData,
  };
}

function saveConfigurationFailed(state, { payload }) {
  return {
    ...state,
    submitMaskState: null,
    saveConfigurationError: Messages.getHttpErrorMessage(payload),
  };
}

const testConfiguration = createAsyncThunk(
  `${REDUCER_NAME}/testConfiguration`,
  (_, { getState, dispatch, rejectWithValue }) => {
    const state = getState(),
      { ownerType, ownerId } = selectOwnerTypeAndOwnerId(state),
      isDirty = selectIsDirty(state),
      repositoryConnectionId = selectRepositoryConnectionId(state),
      formState = selectFormState(state);

    return axios
      .post(
        getTestRepositoryConnectionUrl(ownerType, ownerId, isDirty ? null : repositoryConnectionId),
        isDirty ? toServerData(formState) : null
      )
      .then(({ data }) => {
        startSubmitMaskSuccessTimer(dispatch);
        return data;
      })
      .catch(rejectWithValue);
  }
);

function testConfigurationRequested(state) {
  return {
    ...state,
    submitMaskState: false,
    submitMaskMessage: SUBMIT_MASK_TESTING_CONFIGURATION_MESSAGE,
    testConfigurationError: null,
  };
}

function testConfigurationFulfilled(state, { payload }) {
  if (payload.code === 200) {
    return {
      ...state,
      submitMaskState: true,
      testConfigurationSuccessful: true,
    };
  }
  return {
    ...state,
    submitMaskState: null,
    testConfigurationSuccessful: false,
    testConfigurationError: payload.code + ' ' + payload.message,
  };
}

function testConfigurationFailed(state, { payload }) {
  return {
    ...state,
    submitMaskState: null,
    testConfigurationSuccessful: false,
    testConfigurationError: Messages.getHttpErrorMessage(payload),
  };
}

const deleteConfiguration = createAsyncThunk(
  `${REDUCER_NAME}/deleteConfiguration`,
  (_, { getState, dispatch, rejectWithValue }) => {
    const state = getState(),
      { ownerType, ownerId } = selectOwnerTypeAndOwnerId(state),
      repositoryConnectionId = selectRepositoryConnectionId(state);

    return axios
      .delete(getRepositoryConnectionUrl(ownerType, ownerId, repositoryConnectionId))
      .then(({ data }) => {
        const nextState = stateGo(`repositoryConfiguration.${ownerType}`, {
          ownerId,
        });
        startSubmitMaskSuccessTimer(dispatch, nextState);
        return data;
      })
      .catch(rejectWithValue);
  }
);

function deleteConfigurationRequested(state) {
  return {
    ...state,
    deleteSubmitMaskState: false,
    submitMaskMessage: SUBMIT_MASK_DELETING_CONFIGURATION_MESSAGE,
    deleteConfigurationError: null,
  };
}

function deleteConfigurationFulfilled() {
  return {
    ...initialState,
    deleteSubmitMaskState: true,
  };
}

function deleteConfigurationFailed(state, { payload }) {
  return {
    ...state,
    deleteSubmitMaskState: null,
    deleteConfigurationError: Messages.getHttpErrorMessage(payload),
  };
}

function startSubmitMaskSuccessTimer(dispatch, nextState) {
  setTimeout(() => {
    dispatch(actions.submitMaskTimerDone());
    if (nextState) {
      dispatch(nextState);
    }
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

const innerSourceRepositoryConfigurationSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setFormat: compose(resetSaveAndTestConfigurations, pathSet(['formState', 'format'])),
    setBaseUrl: compose(resetSaveAndTestConfigurations, setTextInput('baseUrlState', null)),
    setAnonymous: compose(resetSaveAndTestConfigurations, pathSet(['formState', 'isAnonymous'])),
    setUsername: compose(resetSaveAndTestConfigurations, setTextInput('usernameState', null)),
    setPassword: compose(resetSaveAndTestConfigurations, setTextInput('passwordState', null)),
    cancel: compose(resetSaveAndTestConfigurations, resetFormState),
    submitMaskTimerDone: compose(propSetConst('submitMaskState', null), propSetConst('deleteSubmitMaskState', null)),
    setShowDeleteModal: compose(resetDeleteConfiguration, propSet('showDeleteModal')),
  },
  extraReducers: {
    [loadConfiguration.pending]: loadConfigurationRequested,
    [loadConfiguration.fulfilled]: loadConfigurationFulfilled,
    [loadConfiguration.rejected]: loadConfigurationFailed,
    [saveConfiguration.pending]: saveConfigurationRequested,
    [saveConfiguration.fulfilled]: saveConfigurationFulfilled,
    [saveConfiguration.rejected]: saveConfigurationFailed,
    [testConfiguration.pending]: testConfigurationRequested,
    [testConfiguration.fulfilled]: testConfigurationFulfilled,
    [testConfiguration.rejected]: testConfigurationFailed,
    [deleteConfiguration.pending]: deleteConfigurationRequested,
    [deleteConfiguration.fulfilled]: deleteConfigurationFulfilled,
    [deleteConfiguration.rejected]: deleteConfigurationFailed,
  },
});

export default innerSourceRepositoryConfigurationSlice.reducer;
export const actions = {
  ...innerSourceRepositoryConfigurationSlice.actions,
  loadConfiguration,
  saveConfiguration,
  testConfiguration,
  deleteConfiguration,
};
