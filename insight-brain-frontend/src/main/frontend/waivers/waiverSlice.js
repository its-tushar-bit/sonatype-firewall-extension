/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { getPolicyWaiverReasonsUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { prop } from 'ramda';

const REDUCER_NAME = 'waivers';

const initialState = {
  waiverReasons: {
    loading: false,
    loadError: null,
    data: [],
  },
};

const loadCachedWaiverReasons = createAsyncThunk(
  `${REDUCER_NAME}/loadCachedWaiverReasons`,
  (_, { getState, rejectWithValue }) => {
    const waiverReasons = getState().waivers.waiverReasons.data;
    const waiverReasonsPromise =
      waiverReasons.length > 0 ? Promise.resolve({ data: waiverReasons }) : axios.get(getPolicyWaiverReasonsUrl());
    return waiverReasonsPromise.then(prop('data')).catch(rejectWithValue);
  }
);

const loadCachedWaiverReasonsRequested = (state) => {
  state.waiverReasons.loading = true;
  state.waiverReasons.loadError = null;
};

const loadCachedWaiverReasonsFulfilled = (state, { payload }) => {
  state.waiverReasons.loading = false;
  state.waiverReasons.loadError = null;
  state.waiverReasons.data = payload;
};

const loadCachedWaiverReasonsFailed = (state, { payload }) => {
  state.waiverReasons.loading = false;
  state.waiverReasons.loadError = Messages.getHttpErrorMessage(payload);
};

const waiverSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  extraReducers: {
    [loadCachedWaiverReasons.pending]: loadCachedWaiverReasonsRequested,
    [loadCachedWaiverReasons.fulfilled]: loadCachedWaiverReasonsFulfilled,
    [loadCachedWaiverReasons.rejected]: loadCachedWaiverReasonsFailed,
  },
});

export default waiverSlice.reducer;
export const actions = {
  ...waiverSlice.actions,
  loadCachedWaiverReasons,
};
