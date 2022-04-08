/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice, unwrapResult } from '@reduxjs/toolkit';
import { pick, prop } from 'ramda';

import { Messages } from 'MainRoot/util/CommonServices';
import * as MonitoredStageService from 'MainRoot/owner.manager/utility/monitored.stage.service';
import PolicyViolationGrandfatheringService from 'MainRoot/owner.manager/policyViolationGrandfathering/policyViolationGrandfatheringService';
import { getPolicyMonitoringUrl, getApplicablePolicyMonitoringUrl } from 'MainRoot/util/CLMLocation';
import { selectOwnerProperties } from './orgsAndPoliciesSelectors';
import { selectPolicyMonitoringMonitoredStage } from './orgsAndPoliciesPolicyMonitoringSelectors';
import { actions as policyActions } from 'MainRoot/OrgsAndPolicies/policySlice';
import { actions as productFeaturesActions } from 'MainRoot/productFeatures/productFeaturesSlice';

const REDUCER_NAME = 'orgsAndPoliciesPolicyMonitoring';

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
  localProprietaryCount: 0,
  inheritedProprietaryCount: 0,
  grandfatheringStatusMessage: undefined,
};

const loadApplicablePolicyMonitoring = createAsyncThunk(
  `${REDUCER_NAME}/loadApplicablePolicyMonitoring`,
  ({ promises = () => Promise.resolve({}) } = {}, { getState, dispatch, rejectWithValue }) => {
    const { ownerType, ownerId } = selectOwnerProperties(getState());
    return Promise.all([
      axios.get(getApplicablePolicyMonitoringUrl(ownerType, ownerId)).then(prop('data')),
      dispatch(policyActions.loadApplicablePoliciesByOwner()).then(unwrapResult),
      promises(),
      dispatch(productFeaturesActions.fetchProductFeaturesIfNeeded()),
    ]).catch(rejectWithValue);
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
  const [{ policyMonitoringByOwner }, { policiesByOwner }, { stages, actionStages, grandfathering }] = payload;

  state.loading = false;
  state.loadError = null;
  state.policyMonitoringByOwner = policyMonitoringByOwner;
  if (stages) setStages(state, policyMonitoringByOwner, stages);
  if (actionStages) setMonitoredStageFromActionStages(state, policyMonitoringByOwner, actionStages);
  if (policiesByOwner && actionStages) setPoliciesByOwner(state, policiesByOwner, actionStages);
  if (grandfathering) setGrandfatheringStatusMessage(state, grandfathering);
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

const setStages = (state, policyMonitoringByOwner, stages) => {
  const stagesWithInheritOrNoMonitorOption = [
    MonitoredStageService.createInheritOrNoMonitorOption(policyMonitoringByOwner, stages),
    ...stages,
  ];
  const monitoredStage = MonitoredStageService.getMonitoredStage(
    policyMonitoringByOwner[0].policyMonitoring,
    stagesWithInheritOrNoMonitorOption
  );

  state.originalStage = monitoredStage;
  state.monitoredStage = monitoredStage;
  state.stages = stagesWithInheritOrNoMonitorOption;
};

const setMonitoredStageFromActionStages = (state, policyMonitoringByOwner, stages) => {
  const monitoredStage = MonitoredStageService.getMonitoredStage(policyMonitoringByOwner[0].policyMonitoring, stages);
  const inheritOrNoMonitorOption = MonitoredStageService.createInheritOrNoMonitorOption(
    policyMonitoringByOwner,
    stages
  );

  state.actionStages = stages;
  state.monitoredStage = monitoredStage || inheritOrNoMonitorOption;
};

const setPoliciesByOwner = (state, policiesByOwner, actionStages) => {
  state.policiesByOwner = policiesByOwner.map((policyOwner, index) => {
    const policies = policyOwner.policies.map(function (policy) {
      const enforcementAction = {};
      actionStages.forEach((actionStage) => {
        if (policy.actions[actionStage.stageTypeId]) {
          enforcementAction[actionStage.stageTypeId] = policy.actions[actionStage.stageTypeId];
        }
      });
      return { ...policy, enforcementAction };
    });
    return { ...policyOwner, policies, inherited: index > 0 };
  });
};

const setGrandfatheringStatusMessage = (state, configuration) => {
  state.grandfatheringStatusMessage = PolicyViolationGrandfatheringService().getStatusMessage(configuration);
};

const orgsAndPoliciesPolicyMonitoringSlice = createSlice({
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

export default orgsAndPoliciesPolicyMonitoringSlice.reducer;
export const actions = {
  ...orgsAndPoliciesPolicyMonitoringSlice.actions,
  loadApplicablePolicyMonitoring,
  savePolicyMonitoring,
  removePolicyMonitoring,
};
