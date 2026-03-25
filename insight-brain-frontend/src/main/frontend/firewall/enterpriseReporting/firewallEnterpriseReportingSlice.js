/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk } from '@reduxjs/toolkit';
import createSlice from 'MainRoot/reduxConfig/createSlice';
import axios from 'axios';
import { always, applySpec, compose, find, nth, path, prop, propEq } from 'ramda';
import { Messages } from 'MainRoot/util/CommonServices';
import {
  getEnterpriseReportingBaseUrl,
  getEnterpriseReportingDashboardsUrl,
  getIqVersion,
} from 'MainRoot/util/CLMLocation';

const REDUCER_NAME = 'firewallEnterpriseReporting';

// Initial state
export const initialState = {
  // Landing page state
  dashboards: [],
  loading: false,
  loadError: null,
  iqVersion: null,
  // Dashboard detail page state
  baseUrl: null,
  selectedDashboard: null,
  selectedDashboardName: null,
};

// Landing page - load dashboards list
const loadDashboardsRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadDashboardsFulfilled = (state, { payload }) => {
  state.loading = false;
  state.dashboards = payload.dashboards || [];
  state.iqVersion = payload.iqVersion;
  state.loadError = null;
};

const loadDashboardsFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

// Dashboard detail page - load dashboard config and baseUrl
const loadDashboardDetailRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadDashboardDetailFulfilled = (state, { payload }) => {
  state.loading = false;
  if (payload.baseUrl) {
    state.baseUrl = new URL(payload.baseUrl).host;
  }
  state.dashboards = payload.dashboards?.dashboardMetadata || state.dashboards;
  state.loadError = null;
};

const loadDashboardDetailFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

// Async thunk for loading dashboards list (landing page)
export const loadDashboards = createAsyncThunk(`${REDUCER_NAME}/loadDashboards`, (_, { rejectWithValue }) => {
  const promises = [axios.get(getEnterpriseReportingDashboardsUrl()), axios.get(getIqVersion())];
  return Promise.all(promises)
    .then(
      applySpec({
        dashboards: compose(prop('dashboardMetadata'), prop('data'), nth(0)),
        iqVersion: compose(path(['data', 'version']), nth(1)),
      })
    )
    .catch(rejectWithValue);
});

// Async thunk for loading dashboard detail page
export const loadDashboardDetail = createAsyncThunk(`${REDUCER_NAME}/loadDashboardDetail`, (_, { rejectWithValue }) => {
  const promises = [axios.get(getEnterpriseReportingDashboardsUrl()), axios.get(getEnterpriseReportingBaseUrl())];
  return Promise.all(promises)
    .then(
      applySpec({
        dashboards: compose(prop('data'), nth(0)),
        baseUrl: compose(prop('data'), nth(1)),
      })
    )
    .catch(rejectWithValue);
});

// Action to update selected dashboard
const updateSelectedDashboard = (dashboardId) => {
  return (dispatch, getState) => {
    const state = getState();
    const { dashboards } = state.firewallEnterpriseReporting;

    if (!dashboards || dashboards.length === 0) {
      return;
    }

    // Find the dashboard by ID
    const dashboard = find(propEq('dashboardId', dashboardId), dashboards);

    if (dashboard) {
      dispatch(actions.setSelectedDashboard(dashboard));
    } else {
      // Dashboard not found, reset selection
      dispatch(actions.resetSelectedDashboard());
    }
  };
};

// Create the slice
const firewallEnterpriseReportingSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setSelectedDashboard: (state, { payload }) => {
      state.selectedDashboard = {
        dashboardId: payload.dashboardId,
        dashboardPath: payload.dashboardPath?.replace('dashboards/', ''),
        category: payload.category,
      };
      state.selectedDashboardName = payload.title;
    },
    resetSelectedDashboard: (state) => {
      state.selectedDashboard = null;
      state.selectedDashboardName = null;
    },
    reset: always(initialState),
  },
  extraReducers: {
    [loadDashboards.pending]: loadDashboardsRequested,
    [loadDashboards.fulfilled]: loadDashboardsFulfilled,
    [loadDashboards.rejected]: loadDashboardsFailed,
    [loadDashboardDetail.pending]: loadDashboardDetailRequested,
    [loadDashboardDetail.fulfilled]: loadDashboardDetailFulfilled,
    [loadDashboardDetail.rejected]: loadDashboardDetailFailed,
  },
});

// Export actions
export const actions = {
  ...firewallEnterpriseReportingSlice.actions,
  loadDashboards,
  loadDashboardDetail,
  updateSelectedDashboard,
};

// Export reducer
export default firewallEnterpriseReportingSlice.reducer;
