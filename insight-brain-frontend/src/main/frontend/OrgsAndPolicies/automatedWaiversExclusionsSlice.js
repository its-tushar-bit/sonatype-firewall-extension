/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { prop } from 'ramda';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import {
  getAutoWaiverExclusionsUrl,
  getAutoWaiverExclusionsByAutoWaiverIdUrl,
  getAutoWaiverExclusionsByExclusionIdUrl,
} from 'MainRoot/util/CLMLocation';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';
import { selectApplicableAutoWaiver, selectViolationDetails } from 'MainRoot/violation/violationSelectors';
import { selectReportParameters } from 'MainRoot/applicationReport/applicationReportSelectors';
import { selectOwnerProperties, selectSelectedOwnerTypeAndId } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectWaivers } from 'MainRoot/OrgsAndPolicies/automatedWaiversSelectors';
import { propSet } from 'MainRoot/util/jsUtil';

const REDUCER_NAME = 'autoWaiversExclusionConfiguration';

export const initialState = {
  loading: false,
  loadError: null,
  data: null,
  serverData: null,
  isDirty: false,
  submitMaskState: null,
  submitError: null,
  deleteExclusionSubmitMaskState: null,
  deleteExclusionSubmitError: null,
};

const createAutoWaiverExclusionRequested = (state) => {
  state.submitMaskState = false;
};

const createAutoWaiverExclusionFulfilled = (state) => {
  state.submitMaskState = true;
  state.isDirty = false;
};

const createAutoWaiverExclusionFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const clearAutoWaiverExclusionMaskState = (state) => {
  state.submitMaskState = null;
  state.submitError = null;
};

const createAutoWaiverExclusion = createAsyncThunk(
  `${REDUCER_NAME}/createAutoWaiverExclusion`,
  async (_, { getState, dispatch, rejectWithValue }) => {
    const state = getState();
    const violationDetails = selectViolationDetails(state);
    const applicationPublicId = violationDetails?.applicationPublicId;
    const stageData = violationDetails?.stageData;
    const reportParameters = selectReportParameters(state);
    const scanId = getScanIdFromApplicationReport(reportParameters) || getScanIdFromStageData(stageData);
    const { autoWaiver } = selectApplicableAutoWaiver(state);

    const ownerType = autoWaiver.ownerType;
    const validatedOwnerType = ownerType === 'root_organization' ? 'organization' : ownerType;

    const ownerId = autoWaiver.ownerId;
    const policyViolationId = violationDetails?.policyViolationId;

    const putData = {
      ownerId,
      applicationPublicId,
      scanId,
      policyViolationId,
      autoPolicyWaiverId: autoWaiver.autoPolicyWaiverId,
      matchStrategy: 'POLICY_VIOLATION',
    };

    return axios
      .post(getAutoWaiverExclusionsUrl(validatedOwnerType, ownerId), putData)
      .then(() => {
        prop('data');
        startSaveMaskSuccessTimer(dispatch, actions.clearAutoWaiverExclusionMaskState);
      })
      .catch(rejectWithValue);
  }
);

const loadAutoWaiverExclusion = createAsyncThunk(
  `${REDUCER_NAME}/loadAutoWaiverExclusion`,
  async (_, { getState, rejectWithValue }) => {
    try {
      const state = getState();
      let { ownerType, ownerId } = selectSelectedOwnerTypeAndId(state);
      if (ownerId === undefined) {
        ({ ownerType, ownerId } = selectOwnerProperties(state));
      }

      const autoWaiver = selectWaivers(state);
      const autoPolicyWaiverId = autoWaiver?.autoPolicyWaiverId;

      if (!autoPolicyWaiverId) {
        return rejectWithValue('No auto waiver ID found');
      }

      const response = await axios.get(
        getAutoWaiverExclusionsByAutoWaiverIdUrl(ownerType, ownerId, autoPolicyWaiverId)
      );

      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

const loadAutoWaiverExclusionRequested = (state) => {
  state.loading = true;
  state.error = null;
};

const loadAutoWaiverExclusionFulfilled = (state, { payload }) => {
  state.loading = true;
  state.data = payload;
  state.error = null;
};

const loadAutoWaiverExclusionFailed = (state, { payload }) => {
  state.data = null;
  state.loading = false;
  state.error = Messages.getHttpErrorMessage(payload);
};

const deleteAutoWaiverExclusion = createAsyncThunk(
  `${REDUCER_NAME}/deleteExclusion`,
  async ({ autoPolicyWaiverId, autoPolicyWaiverExclusionId }, { getState, dispatch, rejectWithValue }) => {
    const state = getState();
    const { ownerType, ownerId } = selectSelectedOwnerTypeAndId(state);

    return axios
      .delete(
        getAutoWaiverExclusionsByExclusionIdUrl(ownerType, ownerId, autoPolicyWaiverId, autoPolicyWaiverExclusionId)
      )
      .then(() => {
        startSaveMaskSuccessTimer(dispatch, actions.saveDeleteExclusionMaskTimerDone);
        dispatch(actions.loadAutoWaiverExclusion());
      })
      .catch(rejectWithValue);
  }
);

const deleteAutoWaiverExclusionRequested = (state) => {
  state.deleteExclusionSubmitMaskState = false;
};

const deleteAutoWaiverExclusionFulfilled = (state) => {
  state.deleteExclusionSubmitMaskState = true;
};

const deleteAutoWaiverExclusionFailed = (state, { payload }) => {
  state.deleteExclusionSubmitMaskState = null;
  state.deleteExclusionSubmitError = Messages.getHttpErrorMessage(payload);
};

const automatedWaiversExclusionSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    clearAutoWaiverExclusionMaskState,
    saveDeleteExclusionMaskTimerDone: propSet('deleteExclusionSubmitMaskState', null),
  },
  extraReducers: {
    [createAutoWaiverExclusion.pending]: createAutoWaiverExclusionRequested,
    [createAutoWaiverExclusion.fulfilled]: createAutoWaiverExclusionFulfilled,
    [createAutoWaiverExclusion.rejected]: createAutoWaiverExclusionFailed,

    [loadAutoWaiverExclusion.pending]: loadAutoWaiverExclusionRequested,
    [loadAutoWaiverExclusion.fulfilled]: loadAutoWaiverExclusionFulfilled,
    [loadAutoWaiverExclusion.rejected]: loadAutoWaiverExclusionFailed,

    [deleteAutoWaiverExclusion.pending]: deleteAutoWaiverExclusionRequested,
    [deleteAutoWaiverExclusion.fulfilled]: deleteAutoWaiverExclusionFulfilled,
    [deleteAutoWaiverExclusion.rejected]: deleteAutoWaiverExclusionFailed,
  },
});

const getScanIdFromStageData = (stageData) => {
  if (stageData) {
    const validStagesBundle = [
      'build',
      'develop',
      'source',
      'stage-release',
      'release',
      'operate',
      'proxy',
      'compliance',
    ];

    for (const stage of validStagesBundle) {
      if (stageData[stage] && stageData[stage].mostRecentScanId != null) {
        return stageData[stage].mostRecentScanId;
      }
    }

    return null;
  }
};

const getScanIdFromApplicationReport = (reportParameters) => {
  return reportParameters?.scanId;
};

export const actions = {
  ...automatedWaiversExclusionSlice.actions,
  createAutoWaiverExclusion,
  loadAutoWaiverExclusion,
  deleteAutoWaiverExclusion,
};

export default automatedWaiversExclusionSlice.reducer;
