/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { always } from 'ramda';
import { getApplicationSummaryUrl, getAllApplicationSbomVersions } from 'MainRoot/util/CLMLocation';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';

const REDUCER_NAME = 'billOfMaterialsPage';

export const initialState = {
  loading: false,
  errorInternalAppId: null,
  internalAppId: null,
  publicAppId: null,
  errorSbomVersions: null,
  sbomVersions: null,
};

const loadInternalApplicationIdRequested = (state) => {
  state.loading = true;
  state.errorInternalAppId = null;
};

const loadInternalApplicationIdFailed = (state, { payload }) => {
  state.errorInternalAppId = payload.response.data;
  state.loading = false;
  state.internalAppId = null;
  state.publicAppId = null;
};

const loadInternalApplicationIdFulfilled = (state, { payload }) => {
  state.loading = false;
  state.errorInternalAppId = null;
  state.internalAppId = payload.id;
};

const loadApplicationSbomVersionsRequested = (state) => {
  state.loading = true;
  state.errorSbomVersions = null;
};

const loadApplicationSbomVersionsFailed = (state, { payload }) => {
  state.errorSbomVersions = payload;
  state.sbomVersions = null;
  state.loading = false;
};

const loadApplicationSbomVersionsFulfilled = (state, { payload }) => {
  state.loading = false;
  state.errorSbomVersions = null;
  state.sbomVersions = payload;
};

const loadInternalApplicationId = createAsyncThunk(
  `${REDUCER_NAME}/loadInternalApplicationId`,
  async (publicApplicationId, { rejectWithValue }) => {
    return axios
      .get(getApplicationSummaryUrl(publicApplicationId))
      .then((response) => response.data)
      .catch((err) => rejectWithValue(err));
  }
);

const loadApplicationSbomVersions = createAsyncThunk(
  `${REDUCER_NAME}/loadApplicationSbomVersions`,
  async (internalApplicationId, { rejectWithValue }) => {
    return axios
      .get(getAllApplicationSbomVersions(internalApplicationId))
      .then((response) => response.data)
      .catch((err) => rejectWithValue(err));
  }
);

const billsOfMaterialsPageSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setPublicAppId: (state, { payload }) => {
      state.publicAppId = payload;
    },
  },
  extraReducers: {
    [loadInternalApplicationId.pending]: loadInternalApplicationIdRequested,
    [loadInternalApplicationId.fulfilled]: loadInternalApplicationIdFulfilled,
    [loadInternalApplicationId.rejected]: loadInternalApplicationIdFailed,
    [loadApplicationSbomVersions.pending]: loadApplicationSbomVersionsRequested,
    [loadApplicationSbomVersions.fulfilled]: loadApplicationSbomVersionsFulfilled,
    [loadApplicationSbomVersions.rejected]: loadApplicationSbomVersionsFailed,
    [UI_ROUTER_ON_FINISH]: always(initialState),
  },
});

export const actions = {
  ...billsOfMaterialsPageSlice.actions,
  loadInternalApplicationId,
  loadApplicationSbomVersions,
};

export default billsOfMaterialsPageSlice.reducer;
