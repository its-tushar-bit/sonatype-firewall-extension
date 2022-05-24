/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { pick, prop } from 'ramda';

import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { getPolicyMonitoringUrl, getApplicablePolicyMonitoringUrl } from 'MainRoot/util/CLMLocation';
import { selectOwnerProperties } from './orgsAndPoliciesSelectors';
import { selectPolicyMonitoringMonitoredStage } from './policyMonitoringSelectors';

const REDUCER_NAME = 'policyMonitoring';

export const initialState = {
  loadError: null,
  submitError: null,
  loading: false,
  policyMonitoringByOwner: undefined,
  policiesByOwner: undefined,
  stages: undefined,
  actionStages: undefined,
  monitoredStage: undefined,
  originalStage: undefined,
  grandfatheringStatusMessage: undefined,
};

const loadApplicablePolicyMonitoring = createAsyncThunk(
  `${REDUCER_NAME}/loadApplicablePolicyMonitoring`,
  (_, { getState, rejectWithValue }) => {
    const { ownerType, ownerId } = selectOwnerProperties(getState());
    return axios.get(getApplicablePolicyMonitoringUrl(ownerType, ownerId)).then(prop('data')).catch(rejectWithValue);
  }
);

const savePolicyMonitoring = createAsyncThunk(
  `${REDUCER_NAME}/savePolicyMonitoring`,
  (_, { getState, rejectWithValue }) => {
    const monitoredStage = selectPolicyMonitoringMonitoredStage(getState());
    const { ownerType, ownerId } = selectOwnerProperties(getState());
    return axios
      .put(getPolicyMonitoringUrl(ownerType, ownerId), pick(['stageTypeId'], monitoredStage))
      .then(() => monitoredStage)
      .catch(rejectWithValue);
  }
);

const removePolicyMonitoring = createAsyncThunk(
  `${REDUCER_NAME}/removePolicyMonitoring`,
  (_, { getState, rejectWithValue }) => {
    const { ownerType, ownerId } = selectOwnerProperties(getState());
    return axios.delete(getPolicyMonitoringUrl(ownerType, ownerId)).then(prop('data')).catch(rejectWithValue);
  }
);

const setMonitoredStage = (state, { payload }) => {
  state.monitoredStage = payload;
};

const loadApplicablePolicyMonitoringRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadApplicablePolicyMonitoringFulfilled = (state, { payload }) => {
  const { policyMonitoringByOwner } = payload;

  state.loading = false;
  state.loadError = null;
  state.policyMonitoringByOwner = policyMonitoringByOwner;
};

const loadApplicablePolicyMonitoringFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const savePolicyMonitoringRequested = (state) => {
  state.loading = true;
  state.submitError = null;
};

const savePolicyMonitoringFulfilled = (state, { payload }) => {
  state.loading = false;
  state.submitError = null;
  state.originalStage = payload;
};

const savePolicyMonitoringFailed = (state, { payload }) => {
  state.loading = false;
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const removePolicyMonitoringRequested = (state) => {
  state.loading = true;
  state.submitError = null;
};

const removePolicyMonitoringFulfilled = (state) => {
  const { actionStages = [] } = state;

  state.loading = false;
  state.submitError = null;
  state.originalStage = actionStages.find((stage) => !stage.stageTypeId);
};

const removePolicyMonitoringFailed = (state, { payload }) => {
  state.loading = false;
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const policyMonitoringSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: { setMonitoredStage },
  extraReducers: {
    [loadApplicablePolicyMonitoring.pending]: loadApplicablePolicyMonitoringRequested,
    [loadApplicablePolicyMonitoring.fulfilled]: loadApplicablePolicyMonitoringFulfilled,
    [loadApplicablePolicyMonitoring.rejected]: loadApplicablePolicyMonitoringFailed,

    [savePolicyMonitoring.pending]: savePolicyMonitoringRequested,
    [savePolicyMonitoring.fulfilled]: savePolicyMonitoringFulfilled,
    [savePolicyMonitoring.rejected]: savePolicyMonitoringFailed,

    [removePolicyMonitoring.pending]: removePolicyMonitoringRequested,
    [removePolicyMonitoring.fulfilled]: removePolicyMonitoringFulfilled,
    [removePolicyMonitoring.rejected]: removePolicyMonitoringFailed,
  },
});

export default policyMonitoringSlice.reducer;
export const actions = {
  ...policyMonitoringSlice.actions,
  loadApplicablePolicyMonitoring,
  savePolicyMonitoring,
  removePolicyMonitoring,
};
