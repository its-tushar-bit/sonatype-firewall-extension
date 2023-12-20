/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { getDeveloperDashboardGraphsData } from 'MainRoot/util/CLMLocation';

export const DEVELOPER_DASHBOARD_GRAPHS_REDUCER_NAME = 'developerDashboardGraphs';

const loadDeveloperDashboardGraphsDataRequested = (state) => {
  return {
    ...state,
    loading: true,
    loadError: null,
  };
};

const loadDeveloperDashboardGraphsDataFulfilled = (state, { payload }) => {
  return {
    ...state,
    graphData: payload,
    loading: false,
    loadError: null,
  };
};

const loadDeveloperDashboardGraphsDataFailed = (state, { payload }) => {
  return {
    ...state,
    loading: false,
    loadError: Messages.getHttpErrorMessage(payload),
  };
};

const loadDeveloperDashboardGraphsData = createAsyncThunk(
  `${DEVELOPER_DASHBOARD_GRAPHS_REDUCER_NAME}/loadDeveloperDashboardGraphsData`,
  (_, { rejectWithValue }) => {
    return axios
      .get(getDeveloperDashboardGraphsData())
      .then(({ data }) => data)
      .catch(rejectWithValue);
  }
);

const developerDashboardGraphsDataSlice = createSlice({
  name: DEVELOPER_DASHBOARD_GRAPHS_REDUCER_NAME,
  initialState: initialState(),
  extraReducers: {
    [loadDeveloperDashboardGraphsData.pending]: loadDeveloperDashboardGraphsDataRequested,
    [loadDeveloperDashboardGraphsData.fulfilled]: loadDeveloperDashboardGraphsDataFulfilled,
    [loadDeveloperDashboardGraphsData.rejected]: loadDeveloperDashboardGraphsDataFailed,
  },
});

function initialState() {
  return {
    graphData: null,
    loading: false,
    loadError: null,
  };
}

export default developerDashboardGraphsDataSlice.reducer;

export const actions = {
  ...developerDashboardGraphsDataSlice.actions,
  loadDeveloperDashboardGraphsData: loadDeveloperDashboardGraphsData,
};
