/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { selectOwnerProperties } from './orgsAndPoliciesSelectors';
import { getGrandfatheringUrl } from 'MainRoot/util/CLMLocation';
import { prop } from 'ramda';

const REDUCER_NAME = 'policyViolationGrandfathering';

export const initialState = {
  loading: false,
  loadError: null,
  data: null,
};

const loadPolicyViolationGrandfatheringRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadPolicyViolationGrandfatheringFulfilled = (state, { payload }) => {
  state.loading = false;
  state.data = payload;
};

const loadPolicyViolationGrandfatheringFailed = (state, { payload }) => {
  state.data = null;
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const loadPolicyViolationGrandfathering = createAsyncThunk(
  `${REDUCER_NAME}/loadPolicyViolationGrandfathering`,
  (_, { getState, rejectWithValue }) => {
    const state = getState();
    const { ownerType, ownerId } = selectOwnerProperties(state);

    return axios.get(getGrandfatheringUrl(ownerType, ownerId)).then(prop('data')).catch(rejectWithValue);
  }
);

const policyViolationGrandfatheringSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {},
  extraReducers: {
    [loadPolicyViolationGrandfathering.pending]: loadPolicyViolationGrandfatheringRequested,
    [loadPolicyViolationGrandfathering.fulfilled]: loadPolicyViolationGrandfatheringFulfilled,
    [loadPolicyViolationGrandfathering.rejected]: loadPolicyViolationGrandfatheringFailed,
  },
});

export const actions = {
  ...policyViolationGrandfatheringSlice.actions,
  loadPolicyViolationGrandfathering,
};

export default policyViolationGrandfatheringSlice.reducer;
