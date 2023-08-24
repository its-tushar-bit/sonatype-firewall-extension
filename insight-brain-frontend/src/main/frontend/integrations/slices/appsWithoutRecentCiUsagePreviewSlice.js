/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { getAppsWithoutRecentCiUsageUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import getThreeMonthsAgo from 'MainRoot/integrations/utils/getThreeMonthsAgo';

const REDUCER_NAME = 'appsWithoutRecentCiUsagePreview';

export const PREVIEW_PAGE_SIZE = 6;
export const loadAppsWithoutRecentCiUsagePreview = createAsyncThunk(
  `${REDUCER_NAME}/loadCiUsage`,
  (_, { rejectWithValue }) =>
    axios
      .post(getAppsWithoutRecentCiUsageUrl(), {
        sinceUtcTimestamp: getThreeMonthsAgo(),
        pageSize: PREVIEW_PAGE_SIZE,
        page: 0,
      })
      .then(({ data }) => data)
      .catch(rejectWithValue)
);

const appsWithoutRecentCiUsagePreviewSlice = createSlice({
  name: REDUCER_NAME,
  initialState: initialState(),
  reducers: {},
  extraReducers: {
    [loadAppsWithoutRecentCiUsagePreview.pending]: loadAppsWithoutRecentCiUsageRequested,
    [loadAppsWithoutRecentCiUsagePreview.fulfilled]: loadAppsWithoutRecentCiUsageFulfilled,
    [loadAppsWithoutRecentCiUsagePreview.rejected]: loadAppsWithoutRecentCiUsageFailed,
  },
});

function loadAppsWithoutRecentCiUsageRequested(state) {
  return {
    ...state,
    ...initialState(),
    loading: true,
  };
}

function loadAppsWithoutRecentCiUsageFulfilled(state, { payload }) {
  return {
    ...state,
    ...initialState(),
    applicationsWithoutRecentCiUsage: payload.dashboardResults,
    totalNumberOfApplicationsWithoutRecentCiUsage: payload.numResults,
  };
}

function loadAppsWithoutRecentCiUsageFailed(state, { payload }) {
  return {
    ...state,
    ...initialState(),
    loadError: Messages.getHttpErrorMessage(payload),
  };
}

function initialState() {
  return {
    applicationsWithoutRecentCiUsage: [],
    totalNumberOfApplicationsWithoutRecentCiUsage: 0,
    currentPage: 0,
    loading: false,
    loadError: null,
  };
}

export default appsWithoutRecentCiUsagePreviewSlice.reducer;

export const actions = {
  ...appsWithoutRecentCiUsagePreviewSlice.actions,
  loadAppsWithoutRecentCiUsagePreview,
};
