/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { getUsageOverTimeChartVisibility } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';

export const CHART_VISIBILITY_REDUCER_NAME = 'developerDashboardUsageOverTimeChartVisibility';

export const loadChartVisiblity = createAsyncThunk(
  `${CHART_VISIBILITY_REDUCER_NAME}/loadCartVisibility`,
  (_, { rejectWithValue }) => {
    return axios
      .get(getUsageOverTimeChartVisibility())
      .then(({ data }) => data)
      .catch(rejectWithValue);
  }
);

const chartVisibilitySlice = createSlice({
  name: CHART_VISIBILITY_REDUCER_NAME,
  initialState: initialState(),
  reducers: {},
  extraReducers: {
    [loadChartVisiblity.pending]: loadChartVisibilityPending,
    [loadChartVisiblity.fulfilled]: loadChartVisibilityFulfilled,
    [loadChartVisiblity.rejected]: loadChartVisibilityRejected,
  },
});

function initialState() {
  return {
    loading: false,
    uninitialized: true,
    loadError: null,
    usageOverTimeChartsShown: null,
  };
}

function loadChartVisibilityPending(state) {
  return {
    ...state,
    uninitialized: false,
    loading: true,
  };
}

function loadChartVisibilityFulfilled(state, { payload }) {
  return {
    ...state,
    loading: false,
    loadError: null,
    usageOverTimeChartsShown: payload.usageOverTimeChartsShown,
  };
}

function loadChartVisibilityRejected(state, { payload }) {
  return {
    ...state,
    loading: false,
    loadError: Messages.getHttpErrorMessage(payload),
    isUsageOverTimeChartsShown: null,
  };
}

export default chartVisibilitySlice.reducer;
export const actions = {
  ...chartVisibilitySlice.actions,
  loadChartVisiblity,
};
