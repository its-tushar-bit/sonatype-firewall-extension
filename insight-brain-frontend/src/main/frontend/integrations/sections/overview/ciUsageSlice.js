/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { getCiUsageUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import axios from 'axios';

const REDUCER_NAME = 'ciUsage';

export const loadCiUsage = createAsyncThunk(
  `${REDUCER_NAME}/loadCiUsage`,
  ({ sinceUtcTimestamp }, { rejectWithValue }) => {
    return axios
      .get(getCiUsageUrl(), { params: { sinceUtcTimestamp } })
      .then(({ data }) => data)
      .catch(rejectWithValue);
  }
);

const ciUsageSlice = createSlice({
  name: REDUCER_NAME,
  initialState: initialState(),
  reducers: {},
  extraReducers: {
    [loadCiUsage.pending]: loadCiUsageRequested,
    [loadCiUsage.fulfilled]: loadCiUsageFulfilled,
    [loadCiUsage.rejected]: loadCiUsageFailed,
  },
});

function initialState() {
  return {
    loading: false,
    loadError: null,
    result: null,
  };
}

function loadCiUsageRequested(state) {
  return {
    ...state,
    ...initialState(),
    loading: true,
  };
}

function loadCiUsageFulfilled(state, { payload }) {
  return {
    ...state,
    ...initialState(),
    result: payload,
  };
}

function loadCiUsageFailed(state, { payload }) {
  return {
    ...state,
    ...initialState(),
    loadError: Messages.getHttpErrorMessage(payload),
  };
}

export default ciUsageSlice.reducer;
export const actions = {
  ...ciUsageSlice.actions,
  loadCiUsage,
};
