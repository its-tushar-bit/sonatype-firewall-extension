/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { getAppsWithoutScmIntegrations } from 'MainRoot/util/CLMLocation';

const REDUCER_NAME = 'appsWithoutScmIntegrations';

const PAGE_SIZE = 6;

export const initialState = Object.freeze({
  loading: false,
  loadError: null,
  dashboardResults: [],
  pageSize: PAGE_SIZE,
});

const loadAppsWithoutScmIntegrationsRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadAppsWithoutScmIntegrationsFulfilled = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;
  state.dashboardResults = payload.dashboardResults;
};

const loadAppsWithoutScmIntegrationsFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const loadAppsWithoutScmIntegrations = createAsyncThunk(
  `${REDUCER_NAME}/loadAppsWithoutScmIntegrations`,
  (_, { rejectWithValue }) => {
    return axios
      .get(getAppsWithoutScmIntegrations(), {
        params: {
          page: 0,
          pageSize: PAGE_SIZE,
        },
      })
      .then(({ data }) => data)
      .catch(rejectWithValue);
  }
);

const appsWithoutScmIntegrationsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {},
  extraReducers: {
    [loadAppsWithoutScmIntegrations.pending]: loadAppsWithoutScmIntegrationsRequested,
    [loadAppsWithoutScmIntegrations.fulfilled]: loadAppsWithoutScmIntegrationsFulfilled,
    [loadAppsWithoutScmIntegrations.rejected]: loadAppsWithoutScmIntegrationsFailed,
  },
});

export default appsWithoutScmIntegrationsSlice.reducer;

export const actions = {
  ...appsWithoutScmIntegrationsSlice.actions,
  loadAppsWithoutScmIntegrations,
};
