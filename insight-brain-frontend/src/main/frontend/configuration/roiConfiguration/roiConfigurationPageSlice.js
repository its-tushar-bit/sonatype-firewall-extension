/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
// import axios from 'axios';

import { checkPermissions } from 'MainRoot/util/authorizationUtil';

const REDUCER_NAME = 'roiConfigurationPage';

export const ROI_SECURITY_VIOLATION_TYPES = Object.freeze(['critical', 'high', 'medium', 'low']);

export const defaultConfiguration = Object.freeze({
  developerHourlyRate: 0,
  fixRate: 0,
  securityViolation: {
    criticalEnabled: true,
    critical: null,
    highEnabled: true,
    high: null,
    mediumEnabled: true,
    medium: null,
    lowEnabled: true,
    low: null,
  },
  supplyChainAttacksBlocked: 0,
  namespaceAttacksBlocked: 0,
  safeComponentsAutoSelected: 0,
  waivedViolations: true,
});

export const initialState = Object.freeze({
  loading: true,
  error: null,
  configuration: { ...defaultConfiguration },
});

const loadConfigurationRequested = (state) => {
  state.loading = true;
  state.error = null;
};

const loadConfigurationRejected = (state, { payload }) => {
  state.loading = false;
  state.error = payload;
};

const loadConfigurationFulfilled = (state) => {
  state.loading = false;
  state.error = null;
  // TODO: map payload to configuration state.
};

const loadConfiguration = createAsyncThunk(`${REDUCER_NAME}/loadConfiguration`, async (_, { rejectWithValue }) => {
  try {
    await checkPermissions(['CONFIGURE_SYSTEM']);
    return Promise.resolve({});
  } catch (error) {
    return rejectWithValue(error);
  }
});

const roiConfiguraionPageSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {},
  extraReducers: {
    [loadConfiguration.pending]: loadConfigurationRequested,
    [loadConfiguration.rejected]: loadConfigurationRejected,
    [loadConfiguration.fulfilled]: loadConfigurationFulfilled,
  },
});

export default roiConfiguraionPageSlice.reducer;

export const actions = {
  ...roiConfiguraionPageSlice.actions,
  loadConfiguration,
};
