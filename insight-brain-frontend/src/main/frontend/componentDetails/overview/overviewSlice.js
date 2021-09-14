/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { getVersionGraphUrl } from '../../util/CLMLocation';

import { Messages } from '../../util/CommonServices';
import { selectVersionExplorerRequestData } from './overviewSelectors';

const REDUCER_NAME = 'componentDetailsOverview';

const initialState = {
  remediation: null,
  graphExplorerData: {
    loading: false,
    loadError: null,
    data: null,
  },
};

const loadRequested = (state) => {
  return {
    ...state,
    graphExplorerData: {
      ...state.graphExplorerData,
      loading: true,
      loadError: null,
    },
  };
};

const loadFulfilled = (state, { payload }) => ({
  ...state,
  remediation: payload.data.remediation,
  graphExplorerData: {
    loading: false,
    loadError: null,
    data: {
      versions: payload.data.allVersions,
    },
  },
});

function loadFailed(state, { payload }) {
  return {
    ...state,
    graphExplorerData: {
      ...state.graphExplorerData,
      loading: false,
      loadError: Messages.getHttpErrorMessage(payload),
    },
  };
}

const loadVersionGraphData = createAsyncThunk(
  `${REDUCER_NAME}/loadVersionGraphData`,
  (_, { getState, rejectWithValue }) => {
    return axios
      .get(getVersionGraphUrl(selectVersionExplorerRequestData(getState())))
      .then((result) => result)
      .catch(rejectWithValue);
  }
);

const componentDetailsOverviewSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {},
  extraReducers: {
    [loadVersionGraphData.pending]: loadRequested,
    [loadVersionGraphData.fulfilled]: loadFulfilled,
    [loadVersionGraphData.rejected]: loadFailed,
  },
});

export default componentDetailsOverviewSlice.reducer;
export const actions = {
  loadVersionGraphData,
};
