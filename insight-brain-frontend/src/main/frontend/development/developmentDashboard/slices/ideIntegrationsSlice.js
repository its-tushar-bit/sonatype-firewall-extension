/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { getIdeIntegratedUserCount } from 'MainRoot/util/CLMLocation';
import getThreeMonthsAgo from 'MainRoot/development/developmentDashboard/utils/getThreeMonthsAgo';

export const IDE_INTEGRATIONS_REDUCER_NAME = 'ideIntegrations';

export const initialState = {
  loading: false,
  loadError: null,
  ideIntegratedUserCount: null,
};

export const loadIdeIntegratedUserCount = createAsyncThunk(
  `${IDE_INTEGRATIONS_REDUCER_NAME}/loadIdeIntegratedUserCount`,
  (_, { rejectWithValue }) => {
    const sinceUtcTimestamp = getThreeMonthsAgo();

    return axios
      .get(getIdeIntegratedUserCount(), { params: { sinceUtcTimestamp } })
      .then(({ data }) => data.userCount)
      .catch(rejectWithValue);
  }
);

const loadIdeIntegratedUserCountRequested = (state) => {
  state.loading = true;
  state.loadError = null;
  state.ideIntegratedUserCount = null;
};

const loadIdeIntegratedUserCountFulfilled = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;
  state.ideIntegratedUserCount = payload;
};

const loadIdeIntegratedUserCountFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
  state.ideIntegratedUserCount = null;
};

const ideIntegrationsSlice = createSlice({
  name: IDE_INTEGRATIONS_REDUCER_NAME,
  initialState,
  reducers: {},
  extraReducers: {
    [loadIdeIntegratedUserCount.pending]: loadIdeIntegratedUserCountRequested,
    [loadIdeIntegratedUserCount.fulfilled]: loadIdeIntegratedUserCountFulfilled,
    [loadIdeIntegratedUserCount.rejected]: loadIdeIntegratedUserCountFailed,
  },
});

export default ideIntegrationsSlice.reducer;
export const actions = {
  ...ideIntegrationsSlice.actions,
  loadIdeIntegratedUserCount,
};
