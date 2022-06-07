/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { pathSet, propSetConst } from 'MainRoot/util/reduxToolkitUtil';
import { nxTextInputStateHelpers, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { always, compose, curryN } from 'ramda';
import axios from 'axios';
import { getArtifactoryConnectionUrl, getTestArtifactoryConnectionUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';

import {
  selectFormState,
  selectIsDirty,
  selectOwnerTypeAndOwnerId,
  selectArtifactoryConnectionId,
} from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryConfigurationModalSelectors';
import {
  toFormState,
  toServerData,
} from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryConfigurationModalUtil';
import { actions as baseConfigurationActions } from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryBaseConfigurationsSlice';

export const FAKE_PASSWORD = '\x00\x00\x00\x00\x00';
export const SUBMIT_MASK_SAVING_CONFIGURATION_MESSAGE = 'Saving Configuration';
export const SUBMIT_MASK_TESTING_CONFIGURATION_MESSAGE = 'Testing Configuration';

export const MISSING_OR_INVALID_DATA_MESSAGE = 'Fields with missing or invalid data.';
export const NO_CHANGES_MESSAGE = 'No changes have been made.';
export const MUST_REENTER_PASSWORD_MESSAGE = 'Password must be provided.';

const REDUCER_NAME = 'artifactoryRepositoryConfigurationModal';

export const initialState = {
  showModal: false,
  artifactoryConnectionId: null,

  serverData: null,

  formState: {
    baseUrlState: nxTextInputStateHelpers.initialState(''),
    isAnonymous: true,
    usernameState: nxTextInputStateHelpers.initialState(''),
    passwordState: nxTextInputStateHelpers.initialState(''),
  },

  loading: false,
  loadConfigurationError: null,

  testConfigurationSuccessful: false,
  testConfigurationError: null,

  saveConfigurationError: null,

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

function resetSaveConfigurationError(state) {
  return {
    ...state,
    saveConfigurationError: null,
  };
}

function resetTestConfigurationError(state) {
  return {
    ...state,
    testConfigurationSuccessful: false,
    testConfigurationError: null,
  };
}

const loadConfiguration = createAsyncThunk(
  `${REDUCER_NAME}/loadConfiguration`,
  (artifactoryConnectionId, { getState, rejectWithValue }) => {
    const state = getState();
    const { ownerType, ownerId } = selectOwnerTypeAndOwnerId(state);

    if (artifactoryConnectionId) {
      return axios
        .get(getArtifactoryConnectionUrl(ownerType, ownerId, artifactoryConnectionId))
        .then(({ data }) => data)
        .catch(rejectWithValue);
    }
  }
);

function loadConfigurationRequested(state, { meta }) {
  return {
    ...state,
    loading: true,
    loadConfigurationError: null,
    showModal: true,
    artifactoryConnectionId: meta.arg,
  };
}

function loadConfigurationFulfilled(state, { payload }) {
  return {
    ...state,
    loading: false,
    serverData: payload,
    formState: payload ? toFormState(payload) : state.formState,
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
    const state = getState();
    const { ownerType, ownerId } = selectOwnerTypeAndOwnerId(state);
    const serverData = toServerData(selectFormState(state));
    const artifactoryConnectionId = selectArtifactoryConnectionId(state);

    return axios[artifactoryConnectionId ? 'put' : 'post'](
      getArtifactoryConnectionUrl(ownerType, ownerId, artifactoryConnectionId),
      serverData
    )
      .then(() => {
        startSubmitMaskSuccessTimer(dispatch, () => {
          dispatch(actions.reset());
          dispatch(baseConfigurationActions.load());
        });
      })
      .catch(rejectWithValue);
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

function saveConfigurationFailed(state, { payload }) {
  return {
    ...state,
    submitMaskState: null,
    saveConfigurationError: Messages.getHttpErrorMessage(payload),
  };
}

function saveConfigurationFulfilled(state) {
  return {
    ...state,
    submitMaskState: true,
  };
}

const testConfiguration = createAsyncThunk(
  `${REDUCER_NAME}/testConfiguration`,
  (_, { getState, dispatch, rejectWithValue }) => {
    const state = getState(),
      { ownerType, ownerId } = selectOwnerTypeAndOwnerId(state),
      isDirty = selectIsDirty(state),
      artifactoryConnectionId = selectArtifactoryConnectionId(state),
      formState = selectFormState(state);

    return axios
      .post(
        getTestArtifactoryConnectionUrl(ownerType, ownerId, isDirty ? null : artifactoryConnectionId),
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

function startSubmitMaskSuccessTimer(dispatch, toDo) {
  setTimeout(() => {
    dispatch(actions.resetSubmitMask());
    if (toDo) {
      toDo();
    }
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

const resetSaveAndTestConfigurationError = compose(resetSaveConfigurationError, resetTestConfigurationError);

const artifactoryRepositoryConfigurationModalSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    reset: always(initialState),
    resetSubmitMask: compose(propSetConst('submitMaskMessage', null), propSetConst('submitMaskState', null)),
    resetTestConfigurationError,
    setBaseUrl: compose(resetSaveAndTestConfigurationError, setTextInput('baseUrlState', null)),
    setAnonymous: compose(resetSaveAndTestConfigurationError, pathSet(['formState', 'isAnonymous'])),
    setUsername: compose(resetSaveAndTestConfigurationError, setTextInput('usernameState', null)),
    setPassword: compose(resetSaveAndTestConfigurationError, setTextInput('passwordState', null)),
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
  },
});

export default artifactoryRepositoryConfigurationModalSlice.reducer;

export const actions = {
  ...artifactoryRepositoryConfigurationModalSlice.actions,
  loadConfiguration,
  saveConfiguration,
  testConfiguration,
};
