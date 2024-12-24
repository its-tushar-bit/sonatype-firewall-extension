/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { prop } from 'ramda';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { getAutoWaiverRevocationsUrl } from 'MainRoot/util/CLMLocation';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';
import { selectApplicableAutoWaiver, selectViolationDetails } from 'MainRoot/violation/violationSelectors';
import { selectReportParameters } from 'MainRoot/applicationReport/applicationReportSelectors';
import { actions as rootActions } from 'MainRoot/OrgsAndPolicies/rootSlice';
import { selectOwnerProperties, selectSelectedOwnerTypeAndId } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectWaivers } from 'MainRoot/OrgsAndPolicies/automatedWaiversSelectors';
import { propSet } from 'MainRoot/util/jsUtil';

const REDUCER_NAME = 'autoWaiversRevocationConfiguration';

export const initialState = {
  loading: false,
  loadError: null,
  data: null,
  serverData: null,
  isDirty: false,
  submitMaskState: null,
  submitError: null,
  deleteRevocationSubmitMaskState: null,
  deleteRevocationSubmitError: null,
};

const createAutoWaiverRevocationRequested = (state) => {
  state.submitMaskState = false;
};

const createAutoWaiverRevocationFulfilled = (state) => {
  state.submitMaskState = true;
  state.isDirty = false;
};

const createAutoWaiverRevocationFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const clearAutoWaiverRevocationMaskState = (state) => {
  state.submitMaskState = null;
  state.submitError = null;
};

const createAutoWaiverRevocation = createAsyncThunk(
  `${REDUCER_NAME}/createAutoWaiverRevocation`,
  async (_, { getState, dispatch, rejectWithValue }) => {
    const state = getState();
    const violationDetails = selectViolationDetails(state);
    const applicationPublicId = violationDetails?.applicationPublicId;
    const stageData = violationDetails?.stageData;
    const reportParameters = selectReportParameters(state);
    const scanId = getScanIdFromStageData(stageData) || getScanIdFromApplicationReport(reportParameters);
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
      .post(getAutoWaiverRevocationsUrl(validatedOwnerType, ownerId), putData)
      .then(() => {
        prop('data');
        startSaveMaskSuccessTimer(dispatch, actions.clearAutoWaiverRevocationMaskState);
      })
      .catch(rejectWithValue);
  }
);

const loadAutoWaiverRevocation = createAsyncThunk(
  `${REDUCER_NAME}/loadAutoWaiverRevocation`,
  async (_, { getState, dispatch, rejectWithValue }) => {
    try {
      await dispatch(rootActions.loadSelectedOwner());

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
        `/api/v2/autoPolicyWaiverRevocations/${ownerType}/${ownerId}/${autoPolicyWaiverId}`
      );

      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

const loadAutoWaiverRevocationRequested = (state) => {
  state.loading = true;
  state.error = null;
};

const loadAutoWaiverRevocationFulfilled = (state, { payload }) => {
  state.loading = true;
  state.data = payload;
  state.error = null;
};

const loadAutoWaiverRevocationFailed = (state, { payload }) => {
  state.data = null;
  state.loading = false;
  state.error = Messages.getHttpErrorMessage(payload);
};

const deleteAutoWaiverRevocation = createAsyncThunk(
  `${REDUCER_NAME}/deleteRevocation`,
  async ({ autoPolicyWaiverId, autoPolicyWaiverRevocationId }, { getState, dispatch, rejectWithValue }) => {
    const state = getState();
    const { ownerType, ownerId } = selectSelectedOwnerTypeAndId(state);

    return axios
      .delete(
        `/api/v2/autoPolicyWaiverRevocations/${ownerType}/${ownerId}/${autoPolicyWaiverId}/${autoPolicyWaiverRevocationId}`
      )
      .then(() => {
        startSaveMaskSuccessTimer(dispatch, actions.saveDeleteRevocationMaskTimerDone);
        dispatch(actions.loadAutoWaiverRevocation());
      })
      .catch(rejectWithValue);
  }
);

const deleteAutoWaiverRevocationRequested = (state) => {
  state.deleteRevocationSubmitMaskState = false;
};

const deleteAutoWaiverRevocationFulfilled = (state) => {
  state.deleteRevocationSubmitMaskState = true;
};

const deleteAutoWaiverRevocationFailed = (state, { payload }) => {
  state.deleteRevocationSubmitMaskState = null;
  state.deleteRevocationSubmitError = Messages.getHttpErrorMessage(payload);
};

const automatedWaiversRevocationSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    clearAutoWaiverRevocationMaskState,
    saveDeleteRevocationMaskTimerDone: propSet('deleteRevocationSubmitMaskState', null),
  },
  extraReducers: {
    [createAutoWaiverRevocation.pending]: createAutoWaiverRevocationRequested,
    [createAutoWaiverRevocation.fulfilled]: createAutoWaiverRevocationFulfilled,
    [createAutoWaiverRevocation.rejected]: createAutoWaiverRevocationFailed,

    [loadAutoWaiverRevocation.pending]: loadAutoWaiverRevocationRequested,
    [loadAutoWaiverRevocation.fulfilled]: loadAutoWaiverRevocationFulfilled,
    [loadAutoWaiverRevocation.rejected]: loadAutoWaiverRevocationFailed,

    [deleteAutoWaiverRevocation.pending]: deleteAutoWaiverRevocationRequested,
    [deleteAutoWaiverRevocation.fulfilled]: deleteAutoWaiverRevocationFulfilled,
    [deleteAutoWaiverRevocation.rejected]: deleteAutoWaiverRevocationFailed,
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
  ...automatedWaiversRevocationSlice.actions,
  createAutoWaiverRevocation,
  loadAutoWaiverRevocation,
  deleteAutoWaiverRevocation,
};

export default automatedWaiversRevocationSlice.reducer;
