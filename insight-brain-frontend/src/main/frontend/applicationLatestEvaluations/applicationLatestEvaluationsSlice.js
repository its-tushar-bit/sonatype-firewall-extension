/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk } from '@reduxjs/toolkit';
import createSlice from 'MainRoot/reduxConfig/createSlice';
import axios from 'axios';
import { getApplicationReportHistoryUrl, getApplicationUrl, getHrcReportHistoryUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/util/CommonServices';

const REDUCER_NAME = 'applicationLatestEvaluationsPage';

const initialState = {
  loading: false,
  loadError: null,
  application: null,
  applicationReportHistory: null,
};

// Loads latest evaluations for either an Application or an HRC.
// For HRC (hrcId param present), skips the application lookup and hits the HRC-scoped
// history endpoint added by CLM-44276.
const load = createAsyncThunk(
  `${REDUCER_NAME}/load`,
  async ({ applicationPublicId, hrcId, stageId }, { rejectWithValue }) => {
    try {
      if (hrcId) {
        const historyResponse = await axios.get(getHrcReportHistoryUrl(hrcId, stageId));
        // The HRC endpoint's response shape isn't pinned yet (CLM-44276 is still evolving); if
        // it ever returns a bare array or an empty body, the page's `.reports.map(...)` throws
        // a JS error with no user-visible surface. Normalize to the app-shape here so the
        // downstream table renders "No evaluations" instead of crashing.
        return {
          application: null,
          applicationReportHistory: {
            reports: historyResponse.data?.reports || [],
          },
        };
      }
      const applicationResponse = await loadApplication(applicationPublicId);
      const applicationReportHistoryResponse = await loadApplicationReportHistory(applicationResponse.data.id, stageId);
      return {
        application: applicationResponse.data,
        applicationReportHistory: applicationReportHistoryResponse.data,
      };
    } catch (err) {
      return rejectWithValue(err);
    }
  }
);

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
