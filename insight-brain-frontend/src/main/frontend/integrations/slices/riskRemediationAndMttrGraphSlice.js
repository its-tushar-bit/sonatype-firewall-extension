/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { getRiskRemediationAndMttrGraphData } from 'MainRoot/util/CLMLocation';

const REDUCER_NAME = 'riskRemediationAndMttrGraph';

const loadRiskRemediationAndMttrGraphDataRequested = (state) => {
  return {
    ...state,
    loading: true,
    loadError: null,
  };
};

const loadRiskRemediationAndMttrGraphDataFulfilled = (state, { payload }) => {
  return {
    ...state,
    graphData: payload,
    loading: false,
    loadError: null,
  };
};

const loadRiskRemediationAndMttrGraphDataFailed = (state, { payload }) => {
  return {
    ...state,
    loading: false,
    loadError: Messages.getHttpErrorMessage(payload),
  };
};

const loadRiskRemediationAndMttrGraphData = createAsyncThunk(
  `${REDUCER_NAME}/loadRiskRemediationAndMttrGraphData`,
  (_, { rejectWithValue }) => {
    return axios
      .get(getRiskRemediationAndMttrGraphData())
      .then(({ data }) => data)
      .catch(rejectWithValue);
  }
);

const riskRemediationAndMttrGraphDataSlice = createSlice({
  name: REDUCER_NAME,
  initialState: initialState(),
  extraReducers: {
    [loadRiskRemediationAndMttrGraphData.pending]: loadRiskRemediationAndMttrGraphDataRequested,
    [loadRiskRemediationAndMttrGraphData.fulfilled]: loadRiskRemediationAndMttrGraphDataFulfilled,
    [loadRiskRemediationAndMttrGraphData.rejected]: loadRiskRemediationAndMttrGraphDataFailed,
  },
});

function initialState() {
  return {
    graphData: null,
    loading: false,
    loadError: null,
  };
}

export default riskRemediationAndMttrGraphDataSlice.reducer;

export const actions = {
  ...riskRemediationAndMttrGraphDataSlice.actions,
  loadRiskRemediationAndMttrGraphData,
};
