/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { getAppsWithoutCiIntegrations } from 'MainRoot/util/CLMLocation';
import {
  selectCurrentPage,
  selectPageSize,
} from 'MainRoot/integrations/sections/AppsWithoutCiIntegrations/appsWithoutCiIntegrationsSelectors';

const REDUCER_NAME = 'appsWithoutCiIntegrations';

const PAGE_SIZE = 14;

const SINCE_NUMBER_OF_MONTHS = 3;

export const initialState = {
  loading: false,
  loadError: null,
  dashboardResults: [],
  pageCount: 0,
  pageSize: PAGE_SIZE,
  currentPage: 0,
};

const loadAppsWithoutCiIntegrationsRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadAppsWithoutCiIntegrationsFulfilled = (state, { payload }) => {
  const pageCount = Math.ceil(payload.numResults / PAGE_SIZE);

  state.loading = false;
  state.loadError = null;
  state.dashboardResults = payload.dashboardResults;
  state.pageCount = pageCount;
};

const loadAppsWithoutCiIntegrationsFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

function setCurrentPage(state, { payload }) {
  state.currentPage = payload.currentPage;
}

function getXMonthsAgo(x) {
  const date = new Date();
  date.setMonth(date.getMonth() - x);

  return date.getTime();
}

const loadAppsWithoutCiIntegrations = createAsyncThunk(
  `${REDUCER_NAME}/loadAppsWithoutCiIntegrations`,
  (_, { getState, rejectWithValue }) => {
    const state = getState();

    return axios
      .post(getAppsWithoutCiIntegrations(), {
        page: selectCurrentPage(state),
        pageSize: selectPageSize(state),
        sinceUtcTimestamp: getXMonthsAgo(SINCE_NUMBER_OF_MONTHS),
      })
      .then(({ data }) => data)
      .catch(rejectWithValue);
  }
);

const appsWithoutCiIntegrationsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: { setCurrentPage },
  extraReducers: {
    [loadAppsWithoutCiIntegrations.pending]: loadAppsWithoutCiIntegrationsRequested,
    [loadAppsWithoutCiIntegrations.fulfilled]: loadAppsWithoutCiIntegrationsFulfilled,
    [loadAppsWithoutCiIntegrations.rejected]: loadAppsWithoutCiIntegrationsFailed,
  },
});

export default appsWithoutCiIntegrationsSlice.reducer;

export const actions = {
  ...appsWithoutCiIntegrationsSlice.actions,
  loadAppsWithoutCiIntegrations,
};
