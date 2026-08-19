/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk } from '@reduxjs/toolkit';
import createSlice from 'MainRoot/reduxConfig/createSlice';
import axios from 'axios';
import { getEndpointsUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/util/CommonServices';

const REDUCER_NAME = 'apiPage';

const initialState = {
  loading: false,
  loadError: null,
  publicOpenApi: {},
  experimentalOpenApi: {},
};

const loadOpenApi = createAsyncThunk(`${REDUCER_NAME}/loadOpenApi`, (endpoint, { rejectWithValue }) => {
  return axios
    .get(getEndpointsUrl(endpoint))
    .then(({ data }) => {
      return { endpoint, data };
    })
    .catch(rejectWithValue);
});

function loadOpenApiRequested(state) {
  return {
    ...state,
    loading: true,
    loadError: null,
  };
}

function loadOpenApiFulfilled(state, { payload }) {
  const nextState = {
    ...state,
    loading: false,
  };
  nextState[payload.endpoint + 'OpenApi'] = payload.data;
  return nextState;
}

function loadOpenApiFailed(state, { payload }) {
  return {
    ...state,
    loading: false,
    loadError: Messages.getHttpErrorMessage(payload),
  };
}

const apiPageSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {},
  extraReducers: {
    [loadOpenApi.pending]: loadOpenApiRequested,
    [loadOpenApi.fulfilled]: loadOpenApiFulfilled,
    [loadOpenApi.rejected]: loadOpenApiFailed,
  },
});

export default apiPageSlice.reducer;

export const actions = {
  ...apiPageSlice.actions,
  loadOpenApi,
};
