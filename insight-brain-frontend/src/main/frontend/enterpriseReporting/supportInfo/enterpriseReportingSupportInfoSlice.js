/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk } from '@reduxjs/toolkit';
import createSlice from 'MainRoot/reduxConfig/createSlice';
import axios from 'axios';
import { prop } from 'ramda';

import { getTelemetryStatusUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/util/CommonServices';

const REDUCER_NAME = 'enterpriseReportingSupportInfo';

export const initialState = {
  loading: true,
  telemetryStatus: {},
  loadError: null,
};

const loadRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadFulfilled = (state, { payload }) => {
  state.telemetryStatus = payload;
  state.loading = false;
  state.loadError = null;
};

const loadFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const load = createAsyncThunk(`${REDUCER_NAME}/load`, (_, { rejectWithValue }) => {
  return axios.get(getTelemetryStatusUrl()).then(prop('data')).catch(rejectWithValue);
});

const enterpriseReportingSupportInfoSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  extraReducers: {
    [load.pending]: loadRequested,
    [load.fulfilled]: loadFulfilled,
    [load.rejected]: loadFailed,
  },
});

export default enterpriseReportingSupportInfoSlice.reducer;
export const actions = {
  ...enterpriseReportingSupportInfoSlice.actions,
  load,
};
