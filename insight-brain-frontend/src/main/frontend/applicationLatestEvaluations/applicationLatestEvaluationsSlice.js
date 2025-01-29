/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { getApplicationReportHistoryUrl, getApplicationUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';

const REDUCER_NAME = 'applicationLatestEvaluationsPage';

const initialState = {
  loading: false,
  loadError: null,
  application: null,
  applicationReportHistory: null,
};

const load = createAsyncThunk(`${REDUCER_NAME}/load`, async ({ applicationPublicId, stageId }, { rejectWithValue }) => {
  try {
    const applicationResponse = await loadApplication(applicationPublicId);
    const applicationReportHistoryResponse = await loadApplicationReportHistory(applicationResponse.data.id, stageId);
    return {
      application: applicationResponse.data,
      applicationReportHistory: applicationReportHistoryResponse.data,
    };
  } catch (err) {
    return rejectWithValue(err);
  }
});

const loadRequested = (state) => {
  state.loading = true;
  state.loadError = null;
  state.application = null;
  state.applicationReportHistory = null;
};

const loadFulfilled = (state, { payload }) => {
  state.loading = false;
  state.application = payload.application;
  state.applicationReportHistory = payload.applicationReportHistory;
};

const loadFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const loadApplication = async (applicationPublicId) => axios.get(getApplicationUrl(applicationPublicId));

const loadApplicationReportHistory = async (applicationId, stageId) =>
  axios.get(getApplicationReportHistoryUrl(applicationId, stageId));

const applicationLatestEvaluationsPageSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {},
  extraReducers: {
    [load.pending]: loadRequested,
    [load.fulfilled]: loadFulfilled,
    [load.rejected]: loadFailed,
  },
});

export default applicationLatestEvaluationsPageSlice.reducer;

export const actions = {
  ...applicationLatestEvaluationsPageSlice.actions,
  load,
};
