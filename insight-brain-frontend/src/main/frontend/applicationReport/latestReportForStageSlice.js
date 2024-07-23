/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { getLatestReportInformation } from 'MainRoot/util/CLMLocation';
import { prop } from 'ramda';

export const LATEST_REPORT_FOR_STAGE_REDUCER_NAME = 'APPLICATION_REPORT/LATEST_REPORT_FOR_STAGE_REQUEST';

export function getInitialState() {
  return {
    uninitialized: true,
    loading: false,
    error: null,
    latestReportForStage: null,
  };
}

const loadLatestReportForStage = createAsyncThunk(
  LATEST_REPORT_FOR_STAGE_REDUCER_NAME,
  ({ applicationPublicId, stageTypeId }, { rejectWithValue }) => {
    return axios
      .get(getLatestReportInformation(applicationPublicId, stageTypeId))
      .then(prop('data'))
      .catch(rejectWithValue);
  }
);

const latestReportForStageSlice = createSlice({
  name: LATEST_REPORT_FOR_STAGE_REDUCER_NAME,
  initialState: getInitialState(),
  reducers: {},
  extraReducers: {
    [loadLatestReportForStage.pending]: loadLatestReportForStageRequested,
    [loadLatestReportForStage.fulfilled]: loadLatestReportForStageFulfilled,
    [loadLatestReportForStage.rejected]: loadLatestReportForStageFailed,
  },
});

function loadLatestReportForStageRequested(state) {
  return {
    ...state,
    uninitialized: false,
    loading: true,
  };
}

function loadLatestReportForStageFulfilled(state, { payload }) {
  return {
    ...state,
    loading: false,
    error: null,
    latestReportForStage: payload,
  };
}

function loadLatestReportForStageFailed(state, { payload }) {
  return {
    ...state,
    loading: false,
    latestReportForStage: null,
    error: Messages.getHttpErrorMessage(payload),
  };
}

export default latestReportForStageSlice.reducer;

export const actions = {
  ...latestReportForStageSlice.actions,
  loadLatestReportForStage,
};
