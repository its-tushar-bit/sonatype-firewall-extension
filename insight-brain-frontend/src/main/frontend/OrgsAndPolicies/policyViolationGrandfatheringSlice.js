/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { equals, prop } from 'ramda';
import { propSet } from 'MainRoot/util/jsUtil';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { selectOwnerProperties } from './orgsAndPoliciesSelectors';
import { getGrandfatheringUrl } from 'MainRoot/util/CLMLocation';
import { selectPolicyViolationGrandfathering } from './policyViolationGrandfatheringSelectors';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';

const REDUCER_NAME = 'policyViolationGrandfathering';

export const initialState = {
  loading: false,
  loadError: null,
  data: null,
  serverData: null,
  isDirty: false,
  submitMaskState: null,
  submitError: null,
};

const loadPolicyViolationGrandfatheringRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadPolicyViolationGrandfatheringFulfilled = (state, { payload }) => {
  state.loading = false;
  state.data = payload;
  state.serverData = payload;
};

const loadPolicyViolationGrandfatheringFailed = (state, { payload }) => {
  state.data = null;
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const loadPolicyViolationGrandfathering = createAsyncThunk(
  `${REDUCER_NAME}/loadPolicyViolationGrandfathering`,
  (_, { getState, rejectWithValue }) => {
    const state = getState();
    const { ownerType, ownerId } = selectOwnerProperties(state);

    return axios.get(getGrandfatheringUrl(ownerType, ownerId)).then(prop('data')).catch(rejectWithValue);
  }
);

const savePolicyViolationGrandfatheringRequested = (state) => {
  state.submitMaskState = false;
};

const savePolicyViolationGrandfatheringFulfilled = (state) => {
  state.submitMaskState = true;
  state.isDirty = false;
};

const savePolicyViolationGrandfatheringFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const savePolicyViolationGrandfathering = createAsyncThunk(
  `${REDUCER_NAME}/savePolicyViolationGrandfathering`,
  (_, { getState, dispatch, rejectWithValue }) => {
    const state = getState();
    const { ownerType, ownerId } = selectOwnerProperties(state);
    const data = selectPolicyViolationGrandfathering(state);

    const putData = {
      allowOverride: data.allowOverride,
      enabled: data.inheritedFromOrganizationName ? null : data.enabled,
    };

    return axios
      .put(getGrandfatheringUrl(ownerType, ownerId), putData)
      .then(prop('data'))
      .then(
        startSaveMaskSuccessTimer(dispatch, actions.saveMaskTimerDone).then(() =>
          dispatch(actions.loadPolicyViolationGrandfathering())
        )
      )
      .catch(rejectWithValue);
  }
);

const setGrandfatheringStatus = (state, { payload }) => {
  let newData;
  switch (payload) {
    case 'inherit':
      newData = {
        ...state.data,
        enabled: state.serverData.enabled ? true : false,
        inheritedFromOrganizationName: state.serverData.inheritedFromOrganizationName
          ? state.serverData.inheritedFromOrganizationName
          : 'inherit',
      };
      break;
    case 'enabled':
      newData = { ...state.data, enabled: true, inheritedFromOrganizationName: null };
      break;
    case 'disabled':
      newData = { ...state.data, enabled: false, inheritedFromOrganizationName: null };
      break;
  }
  return computeIsDirty({ ...state, data: newData });
};

const toggleOverride = (state) => {
  const newData = { ...state.data, allowOverride: !state.data.allowOverride };
  return computeIsDirty({ ...state, data: newData });
};

const computeIsDirty = (state) => {
  const { data, serverData } = state;
  const isDirty = !equals(data, serverData);
  return { ...state, isDirty };
};

const policyViolationGrandfatheringSlice = createSlice({
  name: REDUCER_NAME,
  initialState,

  reducers: { setGrandfatheringStatus, toggleOverride, saveMaskTimerDone: propSet('submitMaskState', null) },
  extraReducers: {
    [loadPolicyViolationGrandfathering.pending]: loadPolicyViolationGrandfatheringRequested,
    [loadPolicyViolationGrandfathering.fulfilled]: loadPolicyViolationGrandfatheringFulfilled,
    [loadPolicyViolationGrandfathering.rejected]: loadPolicyViolationGrandfatheringFailed,

    [savePolicyViolationGrandfathering.pending]: savePolicyViolationGrandfatheringRequested,
    [savePolicyViolationGrandfathering.fulfilled]: savePolicyViolationGrandfatheringFulfilled,
    [savePolicyViolationGrandfathering.rejected]: savePolicyViolationGrandfatheringFailed,
  },
});

export const actions = {
  ...policyViolationGrandfatheringSlice.actions,
  loadPolicyViolationGrandfathering,
  savePolicyViolationGrandfathering,
};

export default policyViolationGrandfatheringSlice.reducer;
