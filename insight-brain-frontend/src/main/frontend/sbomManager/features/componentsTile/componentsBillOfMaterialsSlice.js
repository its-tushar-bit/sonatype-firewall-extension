/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';

import { getBillsOfMaterialsComponents } from 'MainRoot/util/CLMLocation';

// FOR FUTURE DEV, look at previous commits to implement pagination.

const REDUCER_NAME = 'componentsBillOfMaterialsTile';

export const initialState = {
  results: null,
  loading: false,
  error: null,
  sortDir: 'asc',
};

const toggleSortDir = (state) => {
  state.sortDir = state.sortDir === 'asc' ? 'desc' : 'asc';
  sortResults(state);
};

const sortResults = (state) => {
  const sortDir = state.sortDir;
  const results = state.results;
  let sortedResults;
  sortDir === 'asc'
    ? (sortedResults = results.sort((a, b) => a.displayName.localeCompare(b.displayName)))
    : (sortedResults = results.sort((a, b) => b.displayName.localeCompare(a.displayName)));
  state.results = sortedResults;
};

const loadSbomTableDataRequested = (state) => {
  state.results = null;
  state.loading = true;
};

const loadSbomTableDataFailed = (state, { payload }) => {
  try {
    state.error = payload.response.data;
  } catch (e) {
    state.error = payload.message;
  }
  state.loading = false;
};

const loadSbomTableDataFulfilled = (state, { payload }) => {
  state.results = payload.results;
  state.loading = false;
  sortResults(state);
};

const loadSbomTableData = createAsyncThunk(
  `${REDUCER_NAME}/loadSbomTableData`,
  async ({ internalAppId, sbomVersion }, { rejectWithValue }) => {
    return axios
      .get(getBillsOfMaterialsComponents(internalAppId, sbomVersion))
      .then((response) => {
        return response.data;
      })
      .catch((err) => rejectWithValue(err));
  }
);

const componentsBillOfMaterialsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    toggleSortDir,
    sortResults,
  },
  extraReducers: {
    [loadSbomTableData.pending]: loadSbomTableDataRequested,
    [loadSbomTableData.fulfilled]: loadSbomTableDataFulfilled,
    [loadSbomTableData.rejected]: loadSbomTableDataFailed,
  },
});

export const actions = {
  ...componentsBillOfMaterialsSlice.actions,
  loadSbomTableData,
};

export default componentsBillOfMaterialsSlice.reducer;
