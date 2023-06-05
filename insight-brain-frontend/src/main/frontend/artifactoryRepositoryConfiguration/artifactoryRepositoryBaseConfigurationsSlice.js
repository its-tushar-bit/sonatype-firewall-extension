/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { selectOwnerTypeAndOwnerId } from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryConfigurationModalSelectors';
import axios from 'axios';
import { getArtifactoryConnectionUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { selectFormState } from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryBaseConfigurationsSelectors';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import {
  getOriginalValues,
  toFormState,
  toServerData,
} from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryBaseConfigurationsUtil';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { pathSet, propSetConst } from 'MainRoot/util/reduxToolkitUtil';
import { compose } from 'ramda';
import { selectIsOrganization } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectSelectedOwnerId } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';

export const SUBMIT_MASK_SAVING_CONFIGURATION_MESSAGE = 'Saving Configuration';

export const MUST_UPDATE_ENABLED_ADD_MESSAGE = 'Must update to Enable to add an artifactory connection.';
export const MUST_UPDATE_ENABLED_EDIT_MESSAGE = 'Must update to Enable to edit an artifactory connection.';
export const PARENT_ORGANIZATIONS_MUST_ALLOW_OVERRIDE = 'Parent organizations must Allow Override.';
const REDUCER_NAME = 'artifactoryRepositoryBaseConfigurations';

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

  isDirty: false,
};

function resetFormState(state) {
  return {
    ...state,
    formState: getOriginalValues(state?.serverData?.artifactoryConnectionStatus),
  };
}

const load = createAsyncThunk(
  `${REDUCER_NAME}/load`,
  //when the ownerId in the router slice is the publicApplicationID then the applicationId must be passed as a param
  (data, { getState, rejectWithValue }) => {
    const state = getState();
    const { ownerType, ownerId } = selectOwnerTypeAndOwnerId(state);
    return axios
      .get(getArtifactoryConnectionUrl(ownerType, data?.ownerId || ownerId, null, data?.inherit))
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
    formState: toFormState(payload.artifactoryConnectionStatus),
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
    .put(getArtifactoryConnectionUrl(ownerType, ownerId, null), serverData)
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

const goToEditPage = () => {
  return (dispatch, getState) => {
    const state = getState();
    const isOrganization = selectIsOrganization(state);
    const ownerType = isOrganization ? 'organization' : 'application';
    const ownerId = selectSelectedOwnerId(state);
    const params = { [`${ownerType}Id`]: ownerId };
    dispatch(stateGo(`artifactoryRepositoryBaseConfigurations.${ownerType}`, params));
  };
};

function startSubmitMaskSuccessTimer(dispatch) {
  setTimeout(() => {
    dispatch(actions.submitMaskTimerDone());
    dispatch(load());
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

function computeIsDirty(state) {
  const originalValues = getOriginalValues(state?.serverData?.artifactoryConnectionStatus);
  const values = state?.formState;
  if (values?.enabled !== originalValues?.enabled) {
    return true;
  }
  if (values?.allowOverride !== originalValues?.allowOverride) {
    return true;
  }
  return false;
}

function updateIsDirty(state) {
  return { ...state, isDirty: computeIsDirty(state) };
}

const artifactoryRepositoryBaseConfigurationsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setEnabled: compose(updateIsDirty, pathSet(['formState', 'enabled'])),
    setAllowOverride: compose(updateIsDirty, pathSet(['formState', 'allowOverride'])),
    cancel: compose(updateIsDirty, resetFormState),
    submitMaskTimerDone: propSetConst('submitMaskState', null),
  },
  extraReducers: {
    [load.pending]: loadRequested,
    [load.fulfilled]: compose(updateIsDirty, loadFulfilled),
    [load.rejected]: loadFailed,
    [save.pending]: saveRequested,
    [save.fulfilled]: saveFulfilled,
    [save.rejected]: saveFailed,
  },
});

export default artifactoryRepositoryBaseConfigurationsSlice.reducer;
export const actions = {
  ...artifactoryRepositoryBaseConfigurationsSlice.actions,
  load,
  save,
  goToEditPage,
};
