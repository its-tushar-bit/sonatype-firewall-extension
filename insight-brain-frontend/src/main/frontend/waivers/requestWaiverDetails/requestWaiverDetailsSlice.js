/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk } from '@reduxjs/toolkit';
import createSlice from 'MainRoot/reduxConfig/createSlice';
import axios from 'axios';
import { Messages } from 'MainRoot/util/CommonServices';
import { getViewOrUpdatePolicyWaiverRequestUrl } from 'MainRoot/util/CLMLocation';
import { always, prop } from 'ramda';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';

const REDUCER_NAME = 'requestWaiverDetails';

export const initialState = Object.freeze({
  loading: false,
  loadError: null,
  waiverRequestDetails: null,
});

// Axios request to get request waiver details
const loadWaiverRequest = createAsyncThunk(`${REDUCER_NAME}/load`, (_, { getState, rejectWithValue }) => {
  const { ownerType, ownerId, policyWaiverRequestId } = selectRouterCurrentParams(getState());
  return axios
    .get(getViewOrUpdatePolicyWaiverRequestUrl(ownerType, ownerId, policyWaiverRequestId))
    .then(prop('data'))
    .catch(rejectWithValue);
});

const loadWaiverRequestRequested = (state) => ({
  ...state,
  loading: true,
  loadError: null,
});

const loadWaiverRequestFulfilled = (state, { payload }) => ({
  ...state,
  loading: false,
  loadError: null,
  waiverRequestDetails: payload,
});

const loadWaiverRequestFailed = (state, { payload }) => ({
  ...state,
  loading: false,
  loadError: Messages.getHttpErrorMessage(payload),
});

const requestWaiverDetailsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    clearWaiverRequestDetails: always(initialState),
  },
  extraReducers: {
    [loadWaiverRequest.pending]: loadWaiverRequestRequested,
    [loadWaiverRequest.fulfilled]: loadWaiverRequestFulfilled,
    [loadWaiverRequest.rejected]: loadWaiverRequestFailed,
  },
});

export default requestWaiverDetailsSlice.reducer;
export const actions = {
  ...requestWaiverDetailsSlice.actions,
  loadWaiverRequest,
};
