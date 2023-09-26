/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { getEnterpriseReportingUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { prop } from 'ramda';

const REDUCER_NAME = 'enterpriseReporting';

export const initialState = {
  loading: false,
  loadError: null,
  embedUrlData: null,
};

function loadRequested(state) {
  return {
    ...state,
    loading: true,
    loadError: null,
  };
}

const loadFulfilled = (state, { payload }) => {
  return {
    ...state,
    embedUrlData: payload,
    loading: false,
    loadError: null,
  };
};

function loadFailed(state, { payload }) {
  return {
    ...state,
    loading: false,
    loadError: Messages.getHttpErrorMessage(payload),
  };
}

const load = createAsyncThunk(`${REDUCER_NAME}/load`, (_, { rejectWithValue }) => {
  return axios
    .post(`${getEnterpriseReportingUrl()}`, { dashboard: 'rolling-recap' })
    .then(prop('data'))
    .catch(rejectWithValue);
});

const enterpriseReportingSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  extraReducers: {
    [load.pending]: loadRequested,
    [load.fulfilled]: loadFulfilled,
    [load.rejected]: loadFailed,
  },
});

export default enterpriseReportingSlice.reducer;
export const actions = {
  ...enterpriseReportingSlice.actions,
  load,
};
