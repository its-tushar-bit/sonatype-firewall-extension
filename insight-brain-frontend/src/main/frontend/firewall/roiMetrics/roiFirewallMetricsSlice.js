/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { getPermissions } from 'MainRoot/util/authorizationUtil';
import { Messages } from 'MainRoot/utilAngular/CommonServices';

const REDUCER_NAME = 'roiFirewallMetrics';

export const initialState = Object.freeze({
  loading: false,
  error: null,
  hasConfigureSystemPermission: false,
  total: 0,
  supplyChainAttacksBlocked: 0,
  namespaceAttacksBlocked: 0,
  safeVersionsSelected: 0,
});

const metricsRequested = (state) => {
  state.loading = true;
  state.error = null;
};

const metricsFailed = (state, { payload }) => {
  state.loading = false;
  state.error = Messages.getHttpErrorMessage(payload);
};

const metricsFulfilled = (state, { payload }) => {
  state.loading = false;
  state.error = null;
  state.hasConfigureSystemPermission = payload.hasConfigureSystemPermission;
  state.total = payload.total;
  state.supplyChainAttacksBlocked = payload.supplyChainAttacksBlocked;
  state.namespaceAttacksBlocked = payload.namespaceAttacksBlocked;
  state.safeVersionsSelected = payload.safeVersionsSelected;
};

const loadMetrics = createAsyncThunk(`${REDUCER_NAME}/loadMetrics`, async (_, { rejectWithValue }) => {
  const mockData = {
    total: 600000,
    supplyChainAttacksBlocked: 100000,
    namespaceAttacksBlocked: 200000,
    safeVersionsSelected: 300000,
  };

  try {
    const permissions = await getPermissions(['CONFIGURE_SYSTEM']);
    const hasConfigureSystemPermission = permissions.includes('CONFIGURE_SYSTEM');
    return { ...mockData, hasConfigureSystemPermission: hasConfigureSystemPermission };
  } catch (error) {
    return rejectWithValue(error);
  }
});

const roiFirewallMetricsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {},
  extraReducers: {
    [loadMetrics.pending]: metricsRequested,
    [loadMetrics.rejected]: metricsFailed,
    [loadMetrics.fulfilled]: metricsFulfilled,
  },
});

export default roiFirewallMetricsSlice.reducer;

export const actions = {
  ...roiFirewallMetricsSlice.actions,
  loadMetrics,
};
