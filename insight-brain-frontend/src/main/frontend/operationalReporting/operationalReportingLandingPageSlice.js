/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { Messages } from 'MainRoot/util/CommonServices';
import { actions as productFeaturesActions } from 'MainRoot/productFeatures/productFeaturesSlice';

const REDUCER_NAME = 'operationalReportingLandingPage';

const initialState = {
  loading: false,
  loadError: null,
};

const loadRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadFulfilled = (state) => {
  state.loading = false;
  state.loadError = null;
};

const loadFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const load = createAsyncThunk(`${REDUCER_NAME}/load`, (_, { dispatch, rejectWithValue }) => {
  return dispatch(productFeaturesActions.fetchProductFeaturesIfNeeded())
    .then(() => ({}))
    .catch(rejectWithValue);
});

const operationalReportingLandingPageSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  extraReducers: {
    [load.pending]: loadRequested,
    [load.fulfilled]: loadFulfilled,
    [load.rejected]: loadFailed,
  },
});

export const actions = {
  ...operationalReportingLandingPageSlice.actions,
  load,
};

export default operationalReportingLandingPageSlice.reducer;
