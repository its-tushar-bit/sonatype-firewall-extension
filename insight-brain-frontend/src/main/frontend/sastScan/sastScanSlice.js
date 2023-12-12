/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { getSastScanUrl } from 'MainRoot/util/CLMLocation';
import axios from 'axios';

export const SAST = 'sast';
const REDUCER_NAME = SAST;

export const loadSastScan = createAsyncThunk(
  `${REDUCER_NAME}/loadSastScan`,
  ({ applicationPublicId, sastScanId }, { rejectWithValue }) => {
    return axios
      .get(getSastScanUrl(applicationPublicId, sastScanId))
      .then(({ data }) => data)
      .catch(rejectWithValue);
  }
);

export const initialState = {
  loading: false,
  loadError: null,
  result: null,
};

const loadSastScanRequested = (state) => {
  return {
    ...state,
    loading: true,
    loadError: null,
    result: null,
  };
};

const loadSastScanFulfilled = (state, { payload }) => {
  return {
    ...state,
    loading: false,
    loadError: null,
    result: payload,
  };
};

const loadSastScanFailed = (state, { payload }) => {
  return {
    ...state,
    loading: false,
    loadError: Messages.getHttpErrorMessage(payload),
    result: null,
  };
};

const sastScanSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {},
  extraReducers: {
    [loadSastScan.pending]: loadSastScanRequested,
    [loadSastScan.fulfilled]: loadSastScanFulfilled,
    [loadSastScan.rejected]: loadSastScanFailed,
  },
});

export default sastScanSlice.reducer;
export const actions = {
  ...sastScanSlice.actions,
  loadSastScan: loadSastScan,
};
