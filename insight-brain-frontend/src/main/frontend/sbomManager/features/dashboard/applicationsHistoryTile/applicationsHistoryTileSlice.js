/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { always } from 'ramda';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';
import { getSbomsHistoryUrl } from 'MainRoot/util/CLMLocation';

const REDUCER_NAME = 'applicationsHistoryTile';

export const initialState = {
  loading: true,
  loadError: null,
  totalScannedApplications: null,
  applicationsUpdatedLastYear: null,
  applicationsUpdatedLastMonth: null,
  applicationsUpdatedLastWeek: null,
};

const loadApplicationsHistoryRequested = (state) => {
  state.loading = true;
  state.totalScannedApplications = null;
  state.applicationsUpdatedLastYear = null;
  state.applicationsUpdatedLastMonth = null;
  state.applicationsUpdatedLastWeek = null;
};

const loadApplicationsHistoryFailed = (state, { payload }) => {
  state.loadError = payload;
  state.loading = false;
  state.totalScannedApplications = null;
  state.applicationsUpdatedLastYear = null;
  state.applicationsUpdatedLastMonth = null;
  state.applicationsUpdatedLastWeek = null;
};

const loadApplicationsHistoryFulfilled = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;
  state.totalScannedApplications = payload.totalScannedApplications;
  state.applicationsUpdatedLastYear = payload.applicationsUpdatedLastYear;
  state.applicationsUpdatedLastMonth = payload.applicationsUpdatedLastMonth;
  state.applicationsUpdatedLastWeek = payload.applicationsUpdatedLastWeek;
};

const loadApplicationsHistory = createAsyncThunk(
  `${REDUCER_NAME}/loadApplicationsHistory`,
  async (_, { rejectWithValue }) => {
    return axios
      .get(getSbomsHistoryUrl())
      .then((response) => response.data)
      .catch((err) => rejectWithValue(err));
  }
);

const applicationsHistoryTileSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  extraReducers: {
    [loadApplicationsHistory.pending]: loadApplicationsHistoryRequested,
    [loadApplicationsHistory.fulfilled]: loadApplicationsHistoryFulfilled,
    [loadApplicationsHistory.rejected]: loadApplicationsHistoryFailed,
    [UI_ROUTER_ON_FINISH]: always(initialState),
  },
});

export const actions = {
  ...applicationsHistoryTileSlice.actions,
  loadApplicationsHistory,
};

export default applicationsHistoryTileSlice.reducer;
