/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { always } from 'ramda';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';
import { getSbomsHighPriorityVulnerabilitiesUrl } from 'MainRoot/util/CLMLocation';

const REDUCER_NAME = 'highPriorityVulnerabilitiesTile';

export const initialState = Object.freeze({
  loading: true,
  loadError: null,
  vulnerabilities: null,
});

const loadHighPriorityVulnerabilitiesRequested = (state) => {
  state.loading = true;
  state.loadError = null;
  state.vulnerabilities = null;
};

const loadHighPriorityVulnerabilitiesFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = payload;
  state.vulnerabilities = null;
};

const loadHighPriorityVulnerabilitiesFulfilled = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;
  state.vulnerabilities = payload;
};

const loadHighPriorityVulnerabilities = createAsyncThunk(
  `${REDUCER_NAME}/loadHighPriorityVulnerabilities`,
  async (_, { rejectWithValue }) =>
    axios
      .get(getSbomsHighPriorityVulnerabilitiesUrl())
      .then((response) => response.data)
      .catch((err) => rejectWithValue(err))
);

const highPriorityVulnerabilitiesTileSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  extraReducers: {
    [loadHighPriorityVulnerabilities.pending]: loadHighPriorityVulnerabilitiesRequested,
    [loadHighPriorityVulnerabilities.fulfilled]: loadHighPriorityVulnerabilitiesFulfilled,
    [loadHighPriorityVulnerabilities.rejected]: loadHighPriorityVulnerabilitiesFailed,
    [UI_ROUTER_ON_FINISH]: always(initialState),
  },
});

export const actions = {
  ...highPriorityVulnerabilitiesTileSlice.actions,
  loadHighPriorityVulnerabilities,
};

export default highPriorityVulnerabilitiesTileSlice.reducer;
