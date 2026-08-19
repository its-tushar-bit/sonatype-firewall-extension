/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk } from '@reduxjs/toolkit';
import createSlice from 'MainRoot/reduxConfig/createSlice';
import { Messages } from 'MainRoot/util/CommonServices';
import { getAutoWaiverExclusionsUrl } from 'MainRoot/util/CLMLocation';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';
import { selectApplicableAutoWaiver, selectViolationDetails } from 'MainRoot/violation/violationSelectors';
import { selectReportParameters } from 'MainRoot/applicationReport/applicationReportSelectors';
import { loadApplicableAutoWaiver, loadApplicableWaivers } from 'MainRoot/violation/violationActions';

const REDUCER_NAME = 'autoWaiverExclusionCreateModal';

export const initialState = {
  isOpen: false,
  submitMaskState: null,
  submitError: null,
};

const createAutoWaiverExclusionRequested = (state) => {
  state.submitMaskState = false;
};

const createAutoWaiverExclusionFulfilled = (state) => {
  state.submitMaskState = true;
};

const createAutoWaiverExclusionFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const openModal = (state) => {
  state.isOpen = true;
};

const closeModal = (state) => {
  state.isOpen = false;
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
        startSaveMaskSuccessTimer(dispatch, actions.closeModal).then(() => {
          dispatch(loadApplicableWaivers(policyViolationId));
          dispatch(loadApplicableAutoWaiver(policyViolationId));
        });
      })
      .catch(rejectWithValue);
  }
);

const autoWaiversExclusionSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    openModal,
    closeModal,
  },
  extraReducers: {
    [createAutoWaiverExclusion.pending]: createAutoWaiverExclusionRequested,
    [createAutoWaiverExclusion.fulfilled]: createAutoWaiverExclusionFulfilled,
    [createAutoWaiverExclusion.rejected]: createAutoWaiverExclusionFailed,
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
  ...autoWaiversExclusionSlice.actions,
  createAutoWaiverExclusion,
};

export default autoWaiversExclusionSlice.reducer;
