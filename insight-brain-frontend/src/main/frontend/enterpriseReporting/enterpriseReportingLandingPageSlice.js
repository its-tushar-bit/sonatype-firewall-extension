/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk } from '@reduxjs/toolkit';
import createSlice from 'MainRoot/reduxConfig/createSlice';
import axios from 'axios';
import { applySpec, path, prop, compose, nth } from 'ramda';

import { getEnterpriseReportingDashboardsUrl, getIqVersion } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/util/CommonServices';
import { actions as productFeaturesActions } from 'MainRoot/productFeatures/productFeaturesSlice';

const REDUCER_NAME = 'enterpriseReportingLandingPage';

export const initialState = {
  loading: false,
  loadError: null,
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

const load = createAsyncThunk(`${REDUCER_NAME}/load`, (_, { dispatch, rejectWithValue }) => {
  const promises = [
    dispatch(productFeaturesActions.fetchProductFeaturesIfNeeded()),
    axios.get(getEnterpriseReportingDashboardsUrl()),
    axios.get(getIqVersion()),
  ];

  return Promise.all(promises)
    .then(
      applySpec({
        dashboardsData: compose(prop('data'), nth(1)),
        iqVersion: compose(path(['data', 'version']), nth(2)),
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
