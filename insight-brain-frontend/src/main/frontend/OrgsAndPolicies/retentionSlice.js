/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { prop } from 'ramda';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { deriveEditRoute } from 'MainRoot/OrgsAndPolicies/utility/util';
import { selectRouterSlice } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectEntityId } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { getRetentionPoliciesUrl } from 'MainRoot/util/CLMLocation';

export const NOT_ENABLED = "Don't Purge";
export const NOT_APPLICABLE = 'N/A';

const REDUCER_NAME = 'retention';

export const initialState = {
  applicationReports: null,
  successMetrics: {},
  loading: false,
  loadError: null,
};

const goToEditRetention = createAsyncThunk(`${REDUCER_NAME}/goToEditRetention`, (_, { getState, dispatch }) => {
  const router = selectRouterSlice(getState());
  const { to, params } = deriveEditRoute(router, 'edit-data-retention');

  dispatch(stateGo(to, params));
});

const loadRetention = createAsyncThunk(`${REDUCER_NAME}/loadRetention`, (_, { getState, rejectWithValue }) => {
  const entityId = selectEntityId(getState());
  return axios.get(getRetentionPoliciesUrl(entityId)).then(prop('data')).catch(rejectWithValue);
});

const loadRetentionRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadRetentionFulFilled = (state, { payload }) => {
  state.loading = false;
  state.applicationReports = payload.applicationReports;
  state.successMetrics = payload.successMetrics;
};

const loadRetentionFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const retentionSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {},
  extraReducers: {
    [loadRetention.pending]: loadRetentionRequested,
    [loadRetention.fulfilled]: loadRetentionFulFilled,
    [loadRetention.rejected]: loadRetentionFailed,
  },
});

export const actions = {
  ...retentionSlice.actions,
  loadRetention,
  goToEditRetention,
};

export default retentionSlice.reducer;
