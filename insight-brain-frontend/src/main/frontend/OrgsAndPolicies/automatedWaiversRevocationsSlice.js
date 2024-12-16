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

const REDUCER_NAME = 'autoWaiversRevocationConfiguration';

export const initialState = {
  loading: false,
  loadError: null,
  data: null,
  serverData: null,
  isDirty: false,
  submitMaskState: null,
  submitError: null,
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

const automatedWaiversRevocationSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: { clearAutoWaiverRevocationMaskState },
  extraReducers: {
    [createAutoWaiverRevocation.pending]: createAutoWaiverRevocationRequested,
    [createAutoWaiverRevocation.fulfilled]: createAutoWaiverRevocationFulfilled,
    [createAutoWaiverRevocation.rejected]: createAutoWaiverRevocationFailed,
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
};

export default automatedWaiversRevocationSlice.reducer;
