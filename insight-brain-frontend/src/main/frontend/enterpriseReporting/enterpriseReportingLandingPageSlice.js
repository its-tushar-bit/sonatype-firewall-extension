/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import {
  getEnterpriseReportingDashboardsUrl,
  getAdvancedReportingInsightsUrl,
  getIqVersion,
} from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { actions as productFeaturesActions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { applySpec, path, compose, nth } from 'ramda';

const REDUCER_NAME = 'enterpriseReportingLandingPage';

export const initialState = {
  loading: false,
  loadError: null,
  advancedReporting: null,
  dashboardsData: null,
  iqVersion: null,
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
    advancedReporting: payload.advancedReporting,
    dashboardsData: payload.dashboardsData,
    iqVersion: payload.iqVersion,
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
  const promises = [
    dispatch(productFeaturesActions.fetchProductFeaturesIfNeeded()),
    axios.get(getEnterpriseReportingDashboardsUrl()),
    axios.get(getAdvancedReportingInsightsUrl()),
    axios.get(getIqVersion()),
  ];

  return Promise.all(promises)
    .then(
      applySpec({
        dashboardsData: compose(path(['data', 'dashboardMetadata']), nth(1)),
        advancedReporting: compose(path(['data', 'ADVANCED_REPORTING_INSIGHTS_ENABLED']), nth(2)),
        iqVersion: compose(path(['data', 'version']), nth(3)),
      })
    )
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
