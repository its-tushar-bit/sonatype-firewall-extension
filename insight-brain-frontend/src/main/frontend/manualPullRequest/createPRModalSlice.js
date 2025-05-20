/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { always } from 'ramda';
import { selectCreatePRModal } from 'MainRoot/manualPullRequest/createPRModalSelectors';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';
import { selectApplicationReportMetaData } from 'MainRoot/applicationReport/applicationReportSelectors';
import { getCreatePullRequestUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';

export const CREATE_PR_MODAL_REDUCER_NAME = 'createPRModal';

export const initialState = Object.freeze({
  isModalOpen: false,
  name: null,
  fullName: null,
  currentVersion: null,
  targetVersion: null,
  breakingChangesCount: null,
  defaultBranch: null,
  scanId: null,
  identificationSource: null,
  componentHash: null,
  componentIdentifier: {},
  submitMaskState: null,
  error: null,
});

const openModal = (state, { payload }) => {
  state.isModalOpen = true;
  state.name = payload.name;
  state.fullName = payload.fullName;
  state.currentVersion = payload.currentVersion;
  state.targetVersion = payload.targetVersion;
  state.breakingChangesCount = payload.breakingChangesCount;
  state.defaultBranch = payload.defaultBranch;
  state.scanId = payload.scanId;
  state.identificationSource = payload.identificationSource;
  state.componentHash = payload.componentHash;
  state.componentIdentifier = payload.componentIdentifier;
};

const openSubmitMask = (state) => {
  state.submitMaskState = true;
};

function callCreatePREndpoint(appId, scanId, targetVersion, identificationSource, componentIdentifier) {
  return axios.post(getCreatePullRequestUrl(), {
    applicationId: appId,
    scanId: scanId,
    targetVersion: targetVersion,
    identificationSource: identificationSource,
    componentIdentifier: componentIdentifier,
  });
}

const createPR = createAsyncThunk(
  `${CREATE_PR_MODAL_REDUCER_NAME}/createPR`,
  async (_, { dispatch, getState, rejectWithValue }) => {
    const state = getState();
    const { application } = selectApplicationReportMetaData(state);
    const createPRModalState = selectCreatePRModal(state);
    const { scanId, targetVersion, identificationSource, componentIdentifier } = createPRModalState;

    try {
      const { data } = await callCreatePREndpoint(
        application.id,
        scanId,
        targetVersion,
        identificationSource,
        componentIdentifier
      );
      startSaveMaskSuccessTimer(dispatch, actions.reset);
      return { id: data.id, componentHash: createPRModalState.componentHash };
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

const createPRPending = (state) => {
  state.isModalOpen = false;
  state.error = null;
  // submitMask loading state
  state.submitMaskState = false;
};

const createPRFulfilled = (state) => {
  // submitMask success state
  state.submitMaskState = true;
};

const createPRFailed = (state, { payload }) => {
  state.isModalOpen = true;
  state.error = Messages.getHttpErrorMessage(payload);
  // submitMask hidden state
  state.submitMaskState = null;
};

const createPRModalSlice = createSlice({
  name: CREATE_PR_MODAL_REDUCER_NAME,
  initialState,
  reducers: {
    openModal,
    setSubmitMaskState: openSubmitMask,
    reset: always(initialState),
  },
  extraReducers: {
    [createPR.pending]: createPRPending,
    [createPR.fulfilled]: createPRFulfilled,
    [createPR.rejected]: createPRFailed,
  },
});

export default createPRModalSlice.reducer;

export const actions = {
  ...createPRModalSlice.actions,
  createPR,
};
