/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { getAdoptionGraphCicdData, getAdoptionGraphScmData } from 'MainRoot/util/CLMLocation';

const REDUCER_NAME = 'adoptionGraph';

const loadAdoptionGraphDataRequested = (state) => {
  return {
    ...state,
    loading: true,
    loadError: null,
  };
};

const loadAdoptionGraphDataFulfilled = (state, { payload }) => {
  return {
    ...state,
    graphData: payload,
    loading: false,
    loadError: null,
  };
};

const loadAdoptionGraphDataFailed = (state, { payload }) => {
  return {
    ...state,
    loading: false,
    loadError: Messages.getHttpErrorMessage(payload),
  };
};

const loadAdoptionGraphData = createAsyncThunk(`${REDUCER_NAME}/loadAdoptionGraphData`, (_, { rejectWithValue }) => {
  const cicdPromise = axios.get(getAdoptionGraphCicdData());
  const scmPromise = axios.get(getAdoptionGraphScmData());

  return Promise.all([cicdPromise, scmPromise])
    .then((data) => {
      return {
        cicd: data[0].data,
        scm: data[1].data,
      };
    })
    .catch(rejectWithValue);
});

const adoptionGraphDataSlice = createSlice({
  name: REDUCER_NAME,
  initialState: initialState(),
  extraReducers: {
    [loadAdoptionGraphData.pending]: loadAdoptionGraphDataRequested,
    [loadAdoptionGraphData.fulfilled]: loadAdoptionGraphDataFulfilled,
    [loadAdoptionGraphData.rejected]: loadAdoptionGraphDataFailed,
  },
});

function initialState() {
  return {
    graphData: null,
    loading: false,
    loadError: null,
  };
}

export default adoptionGraphDataSlice.reducer;

export const actions = {
  ...adoptionGraphDataSlice.actions,
  loadAdoptionGraphData,
};
