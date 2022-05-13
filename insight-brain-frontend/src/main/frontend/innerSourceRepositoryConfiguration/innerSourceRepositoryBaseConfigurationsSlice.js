/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { selectOwnerTypeAndOwnerId } from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryConfigurationModalSelectors';
import axios from 'axios';
import { getRepositoryConnectionUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { selectFormState } from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryBaseConfigurationsSelectors';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import {
  getOriginalValues,
  toFormState,
  toServerData,
} from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryBaseConfigurationsUtil';
import { pathSet, propSetConst } from 'MainRoot/util/reduxToolkitUtil';

export const SUBMIT_MASK_SAVING_CONFIGURATION_MESSAGE = 'Saving Configuration';
export const SUBMIT_MASK_TESTING_CONFIGURATION_MESSAGE = 'Testing Configuration';

export const MUST_UPDATE_ENABLED_ADD_MESSAGE = 'Must update to Enable to add a repository connection.';
export const MUST_UPDATE_ENABLED_EDIT_MESSAGE = 'Must update to Enable to edit a repository connection.';
export const PARENT_ORGANIZATIONS_MUST_ALLOW_OVERRIDE = 'Parent organizations must Allow Override.';
const REDUCER_NAME = 'innerSourceRepositoryBaseConfigurations';

export const initialState = {
  serverData: null,
  formState: {
    enabled: null,
    allowOverride: true,
  },
  loading: false,
  loadError: null,
  saveError: null,

  submitMaskState: null,
  submitMaskMessage: null,
};

function resetFormState(state) {
  return {
    ...state,
    formState: getOriginalValues(state?.serverData?.repositoryConnectionStatus),
  };
}

const load = createAsyncThunk(
  `${REDUCER_NAME}/load`,
  //when the ownerId in the router slice is the publicApplicationID then the applicationId must be passed as a param
  (data, { getState, rejectWithValue }) => {
    const state = getState();
    const { ownerType, ownerId } = selectOwnerTypeAndOwnerId(state);
    return axios
      .get(getRepositoryConnectionUrl(ownerType, data?.ownerId || ownerId, null, data?.inherit))
      .then(({ data }) => data)
      .catch(rejectWithValue);
  }
);

function loadRequested(state) {
  return {
    ...state,
    loading: true,
    loadError: null,
  };
}

function loadFulfilled(state, { payload }) {
  return {
    ...state,
    loading: false,
    serverData: payload,
    formState: toFormState(payload.repositoryConnectionStatus),
  };
}

function loadFailed(state, { payload }) {
  return {
    ...state,
    loading: false,
    loadError: Messages.getHttpErrorMessage(payload),
  };
}

const save = createAsyncThunk(`${REDUCER_NAME}/save`, (_, { getState, dispatch, rejectWithValue }) => {
  const state = getState(),
    { ownerType, ownerId } = selectOwnerTypeAndOwnerId(state),
    serverData = toServerData(selectFormState(state));

  return axios
    .put(getRepositoryConnectionUrl(ownerType, ownerId, null), serverData)
    .then(({ data }) => {
      startSubmitMaskSuccessTimer(dispatch);
      return data;
    })
    .catch(rejectWithValue);
});

function saveRequested(state) {
  return {
    ...state,
    submitMaskState: false,
    submitMaskMessage: SUBMIT_MASK_SAVING_CONFIGURATION_MESSAGE,
    saveError: null,
  };
}

function saveFulfilled(state) {
  return {
    ...state,
    submitMaskState: true,
  };
}

function saveFailed(state, { payload }) {
  return {
    ...state,
    submitMaskState: null,
    saveError: Messages.getHttpErrorMessage(payload),
  };
}

function startSubmitMaskSuccessTimer(dispatch) {
  setTimeout(() => {
    dispatch(actions.submitMaskTimerDone());
    dispatch(load());
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

const innerSourceRepositoryBaseConfigurationsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setEnabled: pathSet(['formState', 'enabled']),
    setAllowOverride: pathSet(['formState', 'allowOverride']),
    cancel: resetFormState,
    submitMaskTimerDone: propSetConst('submitMaskState', null),
  },
  extraReducers: {
    [load.pending]: loadRequested,
    [load.fulfilled]: loadFulfilled,
    [load.rejected]: loadFailed,
    [save.pending]: saveRequested,
    [save.fulfilled]: saveFulfilled,
    [save.rejected]: saveFailed,
  },
});

export default innerSourceRepositoryBaseConfigurationsSlice.reducer;
export const actions = {
  ...innerSourceRepositoryBaseConfigurationsSlice.actions,
  load,
  save,
};
