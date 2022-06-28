/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { curryN, isNil, pick, prop } from 'ramda';

import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { getPolicyMonitoringUrl, getApplicablePolicyMonitoringUrl } from 'MainRoot/util/CLMLocation';
import { selectOwnerProperties } from '../orgsAndPoliciesSelectors';
import { selectPolicyMonitoringMonitoredStage } from './policyMonitoringSelectors';

import { propSet } from 'MainRoot/util/jsUtil';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';
import { actions as stagesActions } from 'MainRoot/OrgsAndPolicies/stagesSlice';

const REDUCER_NAME = 'policyMonitoring';

export const initialState = {
  loadError: null,
  submitError: null,
  loading: false,
  policyMonitoringByOwner: null,
  stages: null,
  actionStages: null,
  monitoredStage: null,
  originalStage: null,
  grandfatheringStatusMessage: null,
  isDirty: false,
  submitMaskState: null,
};

const loadApplicablePolicyMonitoring = createAsyncThunk(
  `${REDUCER_NAME}/loadApplicablePolicyMonitoring`,
  (_, { getState, rejectWithValue, dispatch }) => {
    const { ownerType, ownerId } = selectOwnerProperties(getState());
    dispatch(stagesActions.loadCliStages());
    return axios.get(getApplicablePolicyMonitoringUrl(ownerType, ownerId)).then(prop('data')).catch(rejectWithValue);
  }
);

const savePolicyMonitoring = createAsyncThunk(
  `${REDUCER_NAME}/savePolicyMonitoring`,
  (_, { getState, rejectWithValue, dispatch }) => {
    const monitoredStage = selectPolicyMonitoringMonitoredStage(getState());
    const { ownerType, ownerId } = selectOwnerProperties(getState());
    return axios
      .put(getPolicyMonitoringUrl(ownerType, ownerId), pick(['stageTypeId'], monitoredStage))
      .then(() => {
        dispatch(loadApplicablePolicyMonitoring());
        startSaveMaskSuccessTimer(dispatch, actions.saveMaskTimerDone);
        return monitoredStage;
      })
      .catch(rejectWithValue);
  }
);

const removePolicyMonitoring = createAsyncThunk(
  `${REDUCER_NAME}/removePolicyMonitoring`,
  (_, { getState, rejectWithValue, dispatch }) => {
    const { ownerType, ownerId } = selectOwnerProperties(getState());
    return axios
      .delete(getPolicyMonitoringUrl(ownerType, ownerId))
      .then(() => {
        dispatch(loadApplicablePolicyMonitoring());
        startSaveMaskSuccessTimer(dispatch, actions.saveMaskTimerDone);
      })
      .catch(rejectWithValue);
  }
);

const setMonitoredStage = curryN(2, function setMonitoredStage(state, { payload }) {
  return computeIsDirty(propSet('monitoredStage', payload, state));
});

const loadApplicablePolicyMonitoringRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadApplicablePolicyMonitoringFulfilled = (state, { payload }) => {
  const { policyMonitoringByOwner } = payload;
  const { isDirty } = state;

  state.loading = false;
  state.loadError = null;
  state.policyMonitoringByOwner = policyMonitoringByOwner;
  if (isDirty) {
    state.isDirty = !isDirty;
  }
  state.monitoredStage = null;
  state.originalStage = null;
};

const loadApplicablePolicyMonitoringFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const savePolicyMonitoringRequested = (state) => {
  state.submitError = null;
  state.submitMaskState = false;
};

const savePolicyMonitoringFulfilled = (state, { payload }) => {
  state.submitError = null;
  state.originalStage = payload;
  state.isDirty = false;
  state.submitMaskState = true;
  state.loading = false;
};

const savePolicyMonitoringFailed = (state, { payload }) => {
  state.submitError = Messages.getHttpErrorMessage(payload);
  state.submitMaskState = null;
};

const removePolicyMonitoringRequested = (state) => {
  state.submitError = null;
  state.submitMaskState = false;
};

const removePolicyMonitoringFulfilled = (state) => {
  const { actionStages = [] } = state;
  state.submitError = null;
  state.monitoredStage = { stageName: 'Do not monitor' };
  if (actionStages) {
    state.originalStage = actionStages.find((stage) => !stage.stageTypeId);
  }
  state.isDirty = false;
  state.submitMaskState = true;
  state.loading = false;
};

const removePolicyMonitoringFailed = (state, { payload }) => {
  state.submitError = Messages.getHttpErrorMessage(payload);
  state.submitMaskState = null;
};

const policyMonitoringSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setMonitoredStage,
    saveMaskTimerDone: propSet('submitMaskState', null),
  },
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

const computeIsDirty = (state) => {
  const { monitoredStage, originalStage, policyMonitoringByOwner } = state;
  const serverStageTypeId = originalStage?.stageTypeId || policyMonitoringByOwner[0].policyMonitoring?.stageTypeId;
  const isDirty = isNil(monitoredStage) ? true : monitoredStage?.stageTypeId !== serverStageTypeId;
  return propSet('isDirty', isDirty, state);
};

export default policyMonitoringSlice.reducer;
export const actions = {
  ...policyMonitoringSlice.actions,
  loadApplicablePolicyMonitoring,
  savePolicyMonitoring,
  removePolicyMonitoring,
};
