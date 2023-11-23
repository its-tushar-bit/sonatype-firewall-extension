/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { getEnterpriseReportingEmbedUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { prop } from 'ramda';
import { pathSet } from 'MainRoot/util/reduxToolkitUtil';
import { selectSelectedDashboard } from 'MainRoot/enterpriseReporting/dashboard/enterpriseReportingDashboardSelectors';

const REDUCER_NAME = 'enterpriseReportingDashboard';

export const initialState = {
  loading: false,
  loadError: null,
  embedUrlData: null,
  selectedDashboard: { dashboardId: null },
};

function loadRequested(state) {
  state.loading = true;
  state.loadError = null;
}

const loadFulfilled = (state, { payload }) => {
  state.embedUrlData = payload;
  state.loading = false;
};

function loadFailed(state, { payload }) {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
}

const load = createAsyncThunk(`${REDUCER_NAME}/load`, (_, { getState, rejectWithValue }) => {
  const state = getState();
  const selectedDashboard = selectSelectedDashboard(state);
  return axios
    .post(getEnterpriseReportingEmbedUrl(), { dashboard: selectedDashboard.dashboardId })
    .then(prop('data'))
    .catch(rejectWithValue);
});

const enterpriseReportingDashboardSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setSelectedDashboard: pathSet(['selectedDashboard', 'dashboardId']),
  },
  extraReducers: {
    [load.pending]: loadRequested,
    [load.fulfilled]: loadFulfilled,
    [load.rejected]: loadFailed,
  },
});

export default enterpriseReportingDashboardSlice.reducer;
export const actions = {
  ...enterpriseReportingDashboardSlice.actions,
  load,
};
