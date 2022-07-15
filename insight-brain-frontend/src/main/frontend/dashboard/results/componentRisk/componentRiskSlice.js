/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createAsyncThunk, createSlice, unwrapResult } from '@reduxjs/toolkit';
import axios from 'axios';
import { getComponentRiskDetailsUrl, getComponentNameUrl } from 'MainRoot/util/CLMLocation';
import { getComponentName } from 'MainRoot/util/componentNameUtils';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { setAppRiskAndSortViolationsByThreat } from './componentRiskUtils';
import { prop } from 'ramda';

const REDUCER_NAME = 'componentRiskDetails';

export const initialState = {
  applicationComponents: [],
  component: { displayName: [] },
  componentName: '',
  loading: false,
  loadError: null,
  totalRisk: 0,
};

// Load details
const loadDetailsFulfilled = (state, { payload }) => {
  state.component = { displayName: payload };
  state.componentName = getComponentName(state.component);
};

const loadDetails = createAsyncThunk(`${REDUCER_NAME}/loadDetails`, (hash, { rejectWithValue }) => {
  return axios.get(getComponentNameUrl(hash)).then(prop('data')).catch(rejectWithValue);
});

// Load app components
const loadAppComponentsFulfilled = (state, { payload }) => {
  state.applicationComponents = setAppRiskAndSortViolationsByThreat(payload);
  state.totalRisk = state.applicationComponents.reduce((totalRisk, { risk }) => totalRisk + risk, 0);
};

const loadAppComponents = createAsyncThunk(`${REDUCER_NAME}/loadAppComponents`, (hash, { rejectWithValue }) => {
  return axios.get(getComponentRiskDetailsUrl(hash)).then(prop('data')).catch(rejectWithValue);
});

// Load app components and details
const loadDetailsAndComponentsRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadDetailsAndComponentsFulfilled = (state) => {
  state.loading = false;
};

const loadDetailsAndComponentsFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const loadDetailsAndComponents = createAsyncThunk(
  `${REDUCER_NAME}/loadDetailsAndComponents`,
  (hash, { rejectWithValue, dispatch }) => {
    const promises = [
      dispatch(loadDetails(hash)).then(unwrapResult),
      dispatch(loadAppComponents(hash)).then(unwrapResult),
    ];
    return Promise.all(promises).catch(rejectWithValue);
  }
);

const reportsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  extraReducers: {
    [loadDetailsAndComponents.pending]: loadDetailsAndComponentsRequested,
    [loadDetailsAndComponents.fulfilled]: loadDetailsAndComponentsFulfilled,
    [loadDetailsAndComponents.rejected]: loadDetailsAndComponentsFailed,
    [loadDetails.fulfilled]: loadDetailsFulfilled,
    [loadAppComponents.fulfilled]: loadAppComponentsFulfilled,
  },
});

export default reportsSlice.reducer;

export const actions = {
  ...reportsSlice.actions,
  loadDetailsAndComponents,
};
