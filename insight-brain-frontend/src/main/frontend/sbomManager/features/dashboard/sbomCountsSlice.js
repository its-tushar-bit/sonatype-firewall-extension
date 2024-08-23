/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { always } from 'ramda';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { getSbomReleaseStatusUrl, getTotalSbomsAnalyzedUrl } from 'MainRoot/util/CLMLocation';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';

const REDUCER_NAME = 'sbomCounts';

export const initialState = {
  loading: true,
  loadError: null,
  releaseReadyCount: null,
  partiallyReadyCount: null,
  needsAttentionCount: null,
  totalSbomCount: null,
  sbomMaxThreshold: null,
};

const loadSbomCountsFailed = (state, { payload }) => {
  return {
    ...initialState,
    loading: false,
    loadError: payload,
  };
};

const loadSbomCountsFulfilled = (state, { payload }) => {
  state.loadError = null;
  state.loading = false;
  state.releaseReadyCount = payload.releaseReadyCount;
  state.partiallyReadyCount = payload.partiallyReadyCount;
  state.needsAttentionCount = payload.needsAttentionCount;
  state.totalSbomCount = payload.total;
  state.sbomMaxThreshold = payload.threshold;
};

const loadSbomReleaseStatus = async () => {
  return (await axios.get(getSbomReleaseStatusUrl())).data;
};

const loadTotalSbomsStored = async () => {
  return (await axios.get(getTotalSbomsAnalyzedUrl())).data;
};

const load = createAsyncThunk(`${REDUCER_NAME}/load`, async (_, { rejectWithValue }) => {
  let results;
  try {
    results = await Promise.all([loadSbomReleaseStatus(), loadTotalSbomsStored()]);
  } catch (error) {
    return rejectWithValue(error);
  }

  const { releaseReadyCount, partiallyReadyCount, needsAttentionCount } = results[0];
  const { total, threshold } = results[1];
  return {
    releaseReadyCount,
    partiallyReadyCount,
    needsAttentionCount,
    total,
    threshold,
  };
});

const sbomCountsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  extraReducers: {
    [load.pending]: always(initialState),
    [load.fulfilled]: loadSbomCountsFulfilled,
    [load.rejected]: loadSbomCountsFailed,
    [UI_ROUTER_ON_FINISH]: always(initialState),
  },
});

export const actions = {
  ...sbomCountsSlice.actions,
  load,
};

export default sbomCountsSlice.reducer;
