/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { getEnterpriseReportingBaseUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { always, prop } from 'ramda';
import { actions as productFeaturesActions } from 'MainRoot/productFeatures/productFeaturesSlice';

const REDUCER_NAME = 'enterpriseReportingDashboard';

export const initialState = {
  loading: true,
  loadError: null,
  baseUrl: null,
  selectedDashboard: null,
};

function loadRequested(state) {
  state.loading = true;
  state.loadError = null;
}

const loadFulfilled = (state, { payload }) => {
  if (payload) {
    state.baseUrl = new URL(payload).host;
  }
  state.loading = false;
};

function loadFailed(state, { payload }) {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
}

const load = createAsyncThunk(`${REDUCER_NAME}/load`, (_, { rejectWithValue, dispatch }) => {
  return dispatch(productFeaturesActions.fetchProductFeaturesIfNeeded())
    .then(() => axios.get(getEnterpriseReportingBaseUrl()))
    .then(prop('data'))
    .catch(rejectWithValue);
});

const setSelectedDashboard = (state, { payload }) => {
  state.selectedDashboard = {
    dashboardId: payload.dashboardId,
    dashboardPath: payload.dashboardPath?.replace('dashboards/', ''),
  };
};

const enterpriseReportingDashboardSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setSelectedDashboard,
    reset: always(initialState),
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
