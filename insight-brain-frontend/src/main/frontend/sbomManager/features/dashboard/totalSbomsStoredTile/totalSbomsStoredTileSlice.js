/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { always } from 'ramda';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';
import { getTotalSbomsAnalyzedUrl } from 'MainRoot/util/CLMLocation';

const REDUCER_NAME = 'totalSbomsStoredTile';

export const initialState = {
  loading: true,
  loadError: null,
  total: null,
  threshold: null,
};

const loadTotalSbomsStoredRequested = (state) => {
  state.loading = true;
  state.total = null;
  state.threshold = null;
};

const loadTotalSbomsStoredFailed = (state, { payload }) => {
  state.loadError = payload;
  state.loading = false;
  state.total = null;
  state.threshold = null;
};

const loadTotalSbomsStoredFulfilled = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;
  state.total = payload.total;
  state.threshold = payload.threshold;
};

const loadTotalSbomsStored = createAsyncThunk(
  `${REDUCER_NAME}/loadTotalSbomsStored`,
  async (_, { rejectWithValue }) => {
    return axios
      .get(getTotalSbomsAnalyzedUrl())
      .then((response) => response.data)
      .catch((err) => rejectWithValue(err));
  }
);

const totalSbomsStoredTileSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  extraReducers: {
    [loadTotalSbomsStored.pending]: loadTotalSbomsStoredRequested,
    [loadTotalSbomsStored.fulfilled]: loadTotalSbomsStoredFulfilled,
    [loadTotalSbomsStored.rejected]: loadTotalSbomsStoredFailed,
    [UI_ROUTER_ON_FINISH]: always(initialState),
  },
});

export const actions = {
  ...totalSbomsStoredTileSlice.actions,
  loadTotalSbomsStored,
};

export default totalSbomsStoredTileSlice.reducer;
