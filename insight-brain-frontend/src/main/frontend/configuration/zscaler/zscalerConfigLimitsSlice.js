/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk } from '@reduxjs/toolkit';
import createSlice from 'MainRoot/reduxConfig/createSlice';
import { prop } from 'ramda';
import { getZscalerConfigLimitsUrl } from '../../util/CLMLocation';
import { Messages } from 'MainRoot/util/CommonServices';

const REDUCER_NAME = 'zscalerConfigLimits';

export const initialState = {
  loading: false,
  error: null,
  limits: null,
};

const load = createAsyncThunk(`${REDUCER_NAME}/load`, (_, { rejectWithValue }) => {
  return axios.get(getZscalerConfigLimitsUrl()).then(prop('data')).catch(rejectWithValue);
});

function loadRequested() {
  return {
    ...initialState,
    loading: true,
  };
}

function loadFulfilled(state, { payload }) {
  return {
    ...state,
    loading: false,
    limits: payload,
  };
}

function loadFailed(state, { payload }) {
  return {
    ...state,
    loading: false,
    error: Messages.getHttpErrorMessage(payload),
  };
}

const zscalerConfigLimitsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {},
  extraReducers: {
    [load.pending]: loadRequested,
    [load.fulfilled]: loadFulfilled,
    [load.rejected]: loadFailed,
  },
});

export default zscalerConfigLimitsSlice.reducer;

export const actions = {
  ...zscalerConfigLimitsSlice.actions,
  load,
};
