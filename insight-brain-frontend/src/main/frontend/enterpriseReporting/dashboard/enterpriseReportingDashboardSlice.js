/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { getEnterpriseReportingBaseUrl, getEnterpriseReportingDashboardsUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { always, path, prop, compose, nth, applySpec } from 'ramda';
import { actions as productFeaturesActions } from 'MainRoot/productFeatures/productFeaturesSlice';

const REDUCER_NAME = 'enterpriseReportingDashboard';

export const initialState = {
  loading: true,
  loadError: null,
  baseUrl: null,
  selectedDashboard: null,
  dashboardsData: null,
};

const loadRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadFulfilled = (state, { payload }) => {
  if (payload) {
    state.baseUrl = new URL(payload.baseUrl).host;
    state.dashboardsData = payload.dashboards;
  }
  state.loading = false;
};

const loadFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const load = createAsyncThunk(`${REDUCER_NAME}/load`, (_, { rejectWithValue, dispatch }) => {
  const promises = [
    dispatch(productFeaturesActions.fetchProductFeaturesIfNeeded()),
    axios.get(getEnterpriseReportingDashboardsUrl()),
    axios.get(getEnterpriseReportingBaseUrl()),
  ];

  return Promise.all(promises)
    .then(
      applySpec({
        dashboards: compose(path(['data', 'dashboardMetadata']), nth(1)),
        baseUrl: compose(prop('data'), nth(2)),
      })
    )
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
