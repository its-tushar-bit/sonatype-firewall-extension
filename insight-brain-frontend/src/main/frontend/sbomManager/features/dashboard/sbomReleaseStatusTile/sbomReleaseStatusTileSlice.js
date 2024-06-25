/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { always } from 'ramda';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';
import { getSbomReleaseStatusUrl } from 'MainRoot/util/CLMLocation';

const REDUCER_NAME = 'sbomReleaseStatusTile';

export const initialState = {
  loading: true,
  loadError: null,
  releaseReadyCount: null,
  partiallyReadyCount: null,
  needsAttentionCount: null,
};

const loadSbomReleaseStatusRequested = (state) => {
  state.loading = true;
  state.releaseReadyCount = null;
  state.partiallyReadyCount = null;
  state.needsAttentionCount = null;
};

const loadSbomReleaseStatusFailed = (state, { payload }) => {
  state.loadError = payload;
  state.loading = false;
  state.releaseReadyCount = null;
  state.partiallyReadyCount = null;
  state.needsAttentionCount = null;
};

const loadSbomReleaseStatusFulfilled = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;
  state.releaseReadyCount = payload.releaseReadyCount;
  state.partiallyReadyCount = payload.partiallyReadyCount;
  state.needsAttentionCount = payload.needsAttentionCount;
};

const loadSbomReleaseStatus = createAsyncThunk(
  `${REDUCER_NAME}/sbomReleaseStatusTile`,
  async (_, { rejectWithValue }) => {
    return axios
      .get(getSbomReleaseStatusUrl())
      .then((response) => response.data)
      .catch((err) => rejectWithValue(err));
  }
);

const totalSbomsStoredTileSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  extraReducers: {
    [loadSbomReleaseStatus.pending]: loadSbomReleaseStatusRequested,
    [loadSbomReleaseStatus.fulfilled]: loadSbomReleaseStatusFulfilled,
    [loadSbomReleaseStatus.rejected]: loadSbomReleaseStatusFailed,
    [UI_ROUTER_ON_FINISH]: always(initialState),
  },
});

export const actions = {
  ...totalSbomsStoredTileSlice.actions,
  loadSbomReleaseStatus,
};

export default totalSbomsStoredTileSlice.reducer;
