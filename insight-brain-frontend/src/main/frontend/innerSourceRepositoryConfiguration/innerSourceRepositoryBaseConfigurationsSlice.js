/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { compose, prop } from 'ramda';
import axios from 'axios';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { getRepositoryConnectionUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { selectFormState } from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryBaseConfigurationsSelectors';
import {
  getOriginalValues,
  toFormState,
  toServerData,
} from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryBaseConfigurationsUtil';
import { pathSet, propSetConst } from 'MainRoot/util/reduxToolkitUtil';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { selectIsOrganization } from 'MainRoot/reduxUiRouter/routerSelectors';
import {
  selectSelectedOwnerTypeAndId,
  selectSelectedOwnerId,
  selectOwnerProperties,
} from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectOwnerTypeAndOwnerId } from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryConfigurationModalSelectors';

export const SUBMIT_MASK_SAVING_CONFIGURATION_MESSAGE = 'Saving Configuration';

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

  isDirty: false,
};

function resetFormState(state) {
  return {
    ...state,
    formState: getOriginalValues(state?.serverData?.repositoryConnectionStatus),
  };
}

const load = createAsyncThunk(`${REDUCER_NAME}/load`, (data, { getState, rejectWithValue }) => {
  const state = getState();
  const { ownerId } = selectSelectedOwnerTypeAndId(state);
  const { ownerId: routerOwnerId, ownerType } = selectOwnerProperties(state);

  return axios
    .get(getRepositoryConnectionUrl(ownerType, ownerId || routerOwnerId, null, data?.inherit))
    .then(prop('data'))
    .catch(rejectWithValue);
});

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

const goToEditPage = () => {
  return (dispatch, getState) => {
    const state = getState();
    const isOrganization = selectIsOrganization(state);
    const ownerType = isOrganization ? 'organization' : 'application';
    const ownerId = selectSelectedOwnerId(state);
    const params = { [`${ownerType}Id`]: ownerId };

    dispatch(stateGo(`repositoryBaseConfigurations.${ownerType}`, params));
  };
};

function computeIsDirty(state) {
  const originalValues = getOriginalValues(state?.serverData?.repositoryConnectionStatus);
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

const innerSourceRepositoryBaseConfigurationsSlice = createSlice({
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

export default innerSourceRepositoryBaseConfigurationsSlice.reducer;
export const actions = {
  ...innerSourceRepositoryBaseConfigurationsSlice.actions,
  load,
  save,
  goToEditPage,
};
