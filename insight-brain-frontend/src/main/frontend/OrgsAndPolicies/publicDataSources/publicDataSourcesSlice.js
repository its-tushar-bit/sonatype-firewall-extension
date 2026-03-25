/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk } from '@reduxjs/toolkit';
import createSlice from 'MainRoot/reduxConfig/createSlice';
import { selectSelectedOwnerTypeAndId } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { getCpeConfigurationUrl } from 'MainRoot/util/CLMLocation';
import axios from 'axios';
import { equals, prop } from 'ramda';
import { Messages } from 'MainRoot/util/CommonServices';
import { propSet } from 'MainRoot/util/jsUtil';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';

const REDUCER_NAME = 'publicDataSources';

export const initialState = {
  submitError: null,
  submitMaskState: null,
  loading: false,
  loadError: null,
  isDirty: false,
  serverData: null,
  data: null,
};

const loadCpeConfigurationRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadCpeConfigurationFulfilled = (state, { payload }) => {
  state.loading = false;
  state.data = payload;
  state.serverData = payload;
};

const loadCpeConfigurationFailed = (state, { payload }) => {
  state.data = initialState.data;
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const saveCpeConfigurationRequested = (state) => {
  state.submitMaskState = false;
  state.submitError = null;
};

const saveCpeConfigurationFulfilled = (state) => {
  state.submitMaskState = true;
  state.submitError = null;
  state.isDirty = false;
};

const saveCpeConfigurationFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.submitError = Messages.getHttpErrorMessage(payload);
  state.isDirty = true;
};

const loadCpeConfiguration = createAsyncThunk(
  `${REDUCER_NAME}/loadCpeConfiguration`,
  async (_, { getState, rejectWithValue }) => {
    const state = getState();
    const { ownerType, ownerId } = selectSelectedOwnerTypeAndId(state);

    if (!ownerType || !ownerId) {
      return;
    }

    const url = getCpeConfigurationUrl(ownerType, ownerId);

    return axios
      .get(url)
      .then(prop('data'))
      .catch((err) => rejectWithValue(err));
  }
);

const saveCpeConfiguration = createAsyncThunk(
  `${REDUCER_NAME}/saveCpeConfiguration`,
  async (_, { getState, rejectWithValue, dispatch }) => {
    const state = getState();
    const { ownerType, ownerId } = selectSelectedOwnerTypeAndId(state);
    const url = getCpeConfigurationUrl(ownerType, ownerId);

    const { allowOverride } = state.orgsAndPolicies.publicDataSources.data;
    const enabled = state.orgsAndPolicies.publicDataSources.data?.inheritedFromOrganizationName
      ? null
      : state.orgsAndPolicies.publicDataSources.data.enabled;
    const cpeConfiguration = { allowOverride, enabled };

    return axios
      .put(url, cpeConfiguration)
      .then(prop('data'))
      .then(
        startSaveMaskSuccessTimer(dispatch, actions.saveMaskTimerDone).then(() =>
          dispatch(actions.loadCpeConfiguration())
        )
      )
      .catch(rejectWithValue);
  }
);

const setCpeStatus = (state, { payload }) => {
  if (payload.inherited) {
    if (state.serverData?.inheritedFromOrganizationName) {
      return computeIsDirty({ ...state, data: { ...state.serverData } });
    } else {
      let newData = {
        ...state.serverData,
        enabled: null,
        inheritedFromOrganizationName: 'inherit',
      };
      return computeIsDirty({ ...state, data: newData });
    }
  } else {
    let newData = {
      ...state.data,
      enabled: !!payload.enabled,
      inheritedFromOrganizationName: null,
    };
    return computeIsDirty({ ...state, data: newData });
  }
};

const toggleCpeOverride = (state) => {
  const newData = { ...state.data, allowOverride: !state.data.allowOverride };
  return computeIsDirty({ ...state, data: newData });
};

const computeIsDirty = (state) => {
  const isDirty = !equals(state.data, state.serverData);
  return { ...state, isDirty };
};

const publicDataSourcesSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: { setCpeStatus, toggleCpeOverride, saveMaskTimerDone: propSet('submitMaskState', null) },
  extraReducers: {
    [loadCpeConfiguration.pending]: loadCpeConfigurationRequested,
    [loadCpeConfiguration.fulfilled]: loadCpeConfigurationFulfilled,
    [loadCpeConfiguration.rejected]: loadCpeConfigurationFailed,

    [saveCpeConfiguration.pending]: saveCpeConfigurationRequested,
    [saveCpeConfiguration.fulfilled]: saveCpeConfigurationFulfilled,
    [saveCpeConfiguration.rejected]: saveCpeConfigurationFailed,
  },
});

export const actions = {
  ...publicDataSourcesSlice.actions,
  loadCpeConfiguration,
  saveCpeConfiguration,
};

export default publicDataSourcesSlice.reducer;
