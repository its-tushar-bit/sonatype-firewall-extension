/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { getPrioritiesPageTableData, getReportMetadataUrl } from 'MainRoot/util/CLMLocation';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';

export const PRIORITIES_PAGE_REDUCER_NAME = 'prioritiesPage';

const loadTableDataRequested = (state) => {
  return {
    ...state,
    tableData: null,
    loadingTableData: true,
    loadErrorTableData: null,
  };
};

const loadTableDataFulfilled = (state, { payload }) => {
  return {
    ...state,
    tableData: payload,
    loadingTableData: false,
    loadErrorTableData: null,
  };
};

const loadTableDataFailed = (state, { payload }) => {
  return {
    ...state,
    tableData: null,
    loadingTableData: false,
    loadErrorTableData: Messages.getHttpErrorMessage(payload),
  };
};

const loadTableData = createAsyncThunk(
  `${PRIORITIES_PAGE_REDUCER_NAME}/loadTableData`,
  (_, { getState, rejectWithValue }) => {
    const state = getState();
    const { publicAppId, scanId } = selectRouterCurrentParams(state);
    const tableDataUrl = getPrioritiesPageTableData(publicAppId, scanId);

    return axios
      .get(tableDataUrl)
      .then(({ data }) => data)
      .catch(rejectWithValue);
  }
);

const loadMetadataRequested = (state) => {
  return {
    ...state,
    metadata: null,
    loadingMetadata: true,
    loadErrorMetadata: null,
  };
};

const loadMetadataFulfilled = (state, { payload }) => {
  return {
    ...state,
    metadata: payload,
    loadingMetadata: false,
    loadErrorMetadata: null,
  };
};

const loadMetadataFailed = (state, { payload }) => {
  return {
    ...state,
    metadata: null,
    loadingMetadata: false,
    loadErrorMetadata: Messages.getHttpErrorMessage(payload),
  };
};

const loadMetadata = createAsyncThunk(
  `${PRIORITIES_PAGE_REDUCER_NAME}/loadMetadata`,
  (_, { getState, rejectWithValue }) => {
    const state = getState();
    const { publicAppId, scanId } = selectRouterCurrentParams(state);
    const metadataUrl = getReportMetadataUrl(publicAppId, scanId);

    return axios
      .get(metadataUrl)
      .then(({ data }) => data)
      .catch(rejectWithValue);
  }
);

const prioritiesPageSlice = createSlice({
  name: PRIORITIES_PAGE_REDUCER_NAME,
  initialState: initialState(),
  reducers: { resetState: () => initialState() },
  extraReducers: {
    [loadTableData.pending]: loadTableDataRequested,
    [loadTableData.fulfilled]: loadTableDataFulfilled,
    [loadTableData.rejected]: loadTableDataFailed,
    [loadMetadata.pending]: loadMetadataRequested,
    [loadMetadata.fulfilled]: loadMetadataFulfilled,
    [loadMetadata.rejected]: loadMetadataFailed,
  },
});

function initialState() {
  return {
    tableData: null,
    loadingTableData: false,
    loadErrorTableData: null,
    metadata: null,
    loadingMetadata: false,
    loadErrorMetaData: null,
  };
}

export default prioritiesPageSlice.reducer;

export const actions = {
  ...prioritiesPageSlice.actions,
  loadTableData,
  loadMetadata,
};
