/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { getEnterpriseReportingDashboardsUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { actions as productFeaturesActions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { path } from 'ramda';

const REDUCER_NAME = 'enterpriseReportingLandingPage';

export const initialState = {
  loading: false,
  loadError: null,
  dashboardsData: null,
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
    dashboardsData: payload,
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

const load = createAsyncThunk(`${REDUCER_NAME}/load`, (_, { rejectWithValue, dispatch }) => {
  return dispatch(productFeaturesActions.fetchProductFeaturesIfNeeded())
    .then(() => axios.get(getEnterpriseReportingDashboardsUrl()))
    .then(path(['data', 'dashboardMetadata']))
    .catch(rejectWithValue);
});

const enterpriseReportingLandingPageSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  extraReducers: {
    [load.pending]: loadRequested,
    [load.fulfilled]: loadFulfilled,
    [load.rejected]: loadFailed,
  },
});

export default enterpriseReportingLandingPageSlice.reducer;
export const actions = {
  ...enterpriseReportingLandingPageSlice.actions,
  load,
};
