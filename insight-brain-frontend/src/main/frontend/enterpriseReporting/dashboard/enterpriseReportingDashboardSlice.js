/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk } from '@reduxjs/toolkit';
import createSlice from 'MainRoot/reduxConfig/createSlice';
import axios from 'axios';
import { always, find, findIndex, prop, propEq } from 'ramda';

import { getEnterpriseReportingBaseUrl, getEnterpriseReportingDashboardsUrl, getIqVersion } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/util/CommonServices';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { selectRouterState } from 'MainRoot/reduxUiRouter/routerSelectors';
import { actions as productFeaturesActions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { selectVisibleDashboards } from './enterpriseReportingDashboardSelectors';

const REDUCER_NAME = 'enterpriseReportingDashboard';

export const initialState = {
  loading: true,
  loadError: null,
  baseUrl: null,
  selectedDashboard: null,
  selectedDashboardName: null,
  dashboardsData: null,
  dashboardTabs: [],
  activeDashboardTab: 0,
  iqVersion: null,
};

const loadRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadFulfilled = (state, { payload }) => {
  if (payload.baseUrl) {
    state.baseUrl = new URL(payload.baseUrl).host;
  }
  state.dashboardsData = payload.dashboards;
  state.iqVersion = payload.iqVersion;
  state.loading = false;
};

const loadFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const load = createAsyncThunk(`${REDUCER_NAME}/load`, (_, { rejectWithValue, dispatch }) => {
  const corePromises = [
    dispatch(productFeaturesActions.fetchProductFeaturesIfNeeded()),
    axios.get(getEnterpriseReportingDashboardsUrl()),
    axios.get(getEnterpriseReportingBaseUrl()),
  ];

  return Promise.all(corePromises)
    .then(([, dashboardsRes, baseUrlRes]) =>
      axios
        .get(getIqVersion())
        .then((versionRes) => versionRes?.data?.version)
        .catch(() => null)
        .then((iqVersion) => ({
          dashboards: dashboardsRes.data,
          baseUrl: baseUrlRes.data,
          iqVersion,
        }))
    )
    .catch(rejectWithValue);
});

const setSelectedDashboard = (state, { payload }) => {
  state.selectedDashboard = {
    dashboardId: payload.dashboardId,
    dashboardPath: payload.dashboardPath?.replace('dashboards/', ''),
    category: payload.category,
  };
};

const setSelectedDashboardName = (state, { payload }) => {
  state.selectedDashboardName = payload;
};

const resetSelectedDashboard = (state) => {
  state.selectedDashboard = null;
};

const setActiveDashboardTab = (state, { payload }) => {
  state.activeDashboardTab = payload;
};

const setDashboardTabs = (state, { payload }) => {
  state.dashboardTabs = payload;
};

const updateDashboardPage = (id, groupId, isDashboardDisabled) => {
  return (dispatch, getState) => {
    const state = getState();
    const routerState = selectRouterState(state);
    const combinedDashboards = selectVisibleDashboards(state);

    const dashboardFromUrl = find(propEq('dashboardId', id), combinedDashboards);
    const dashboardGroupFromUrl = find(propEq('groupId', groupId), combinedDashboards);

    // If navigating to a standard dashboardPage, clear dashboardTabs to ensure no tabs render and set
    // selected dashboard
    if (dashboardFromUrl) {
      dispatch(actions.setDashboardTabs([]));
      dispatch(actions.setSelectedDashboard(dashboardFromUrl));
      dispatch(actions.setSelectedDashboardName(dashboardFromUrl.title));

      // If navigating to a "Group" page, update dashboardTabs and trigger activeTab & selectedDashboard
      // (and default to first enabled tab if selectedDashboard doesn't exist)
    } else if (dashboardGroupFromUrl) {
      const { groupedDashboards } = dashboardGroupFromUrl;
      dispatch(actions.setDashboardTabs(groupedDashboards));
      dispatch(actions.setSelectedDashboardName(dashboardGroupFromUrl.title));

      const selectedDashboard = find(propEq('dashboardId', id), groupedDashboards);
      if (selectedDashboard) {
        const selectedIndex = groupedDashboards.indexOf(selectedDashboard);
        const isDisabled = isDashboardDisabled(selectedDashboard);
        dispatch(actions.activateTabAndSelectDashboard(selectedIndex, selectedDashboard, isDisabled));
      } else {
        const firstEnabledIdx = findIndex((dash) => !isDashboardDisabled(dash), groupedDashboards);
        const isDisabled = firstEnabledIdx === -1;
        const idx = !isDisabled ? firstEnabledIdx : 0;
        dispatch(actions.activateTabAndSelectDashboard(idx, groupedDashboards[idx]), isDisabled);
      }
      // Used to navigate to Landing Page if there is no matching Looker dashboard / dashboardGroup. Router
      // required to prevent navigation to Landing Page if user attempts to navigate elsewhere in Lifecycle
    } else if (
      routerState.name === 'enterpriseReportingDashboard' ||
      routerState.name === 'enterpriseReportingDashboardGroup'
    ) {
      dispatch(stateGo('enterpriseReporting'));
    }
  };
};

const activateTabAndSelectDashboard = (tabIndex, dashboard, disabled) => {
  return (dispatch) => {
    dispatch(actions.setActiveDashboardTab(tabIndex));
    if (!disabled) {
      dispatch(actions.setSelectedDashboard(dashboard));
    } else {
      dispatch(actions.resetSelectedDashboard());
    }
  };
};

const enterpriseReportingDashboardSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setSelectedDashboard,
    resetSelectedDashboard,
    setSelectedDashboardName,
    reset: always(initialState),
    setActiveDashboardTab,
    setDashboardTabs,
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
  activateTabAndSelectDashboard,
  updateDashboardPage,
};
