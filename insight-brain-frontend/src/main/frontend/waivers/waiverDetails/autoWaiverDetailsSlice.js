/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { getAutoWaiversConfigurationURLWaiver } from 'MainRoot/util/CLMLocation';
import { prop } from 'ramda';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';

const REDUCER_NAME = 'autoWaiverDetails';

export const initialState = Object.freeze({
  loading: false,
  loadError: null,
  autoWaiverDetails: null,
});

const mapWaiverOwnerType = {
  all_repositories: 'repository_container',
  root_organization: 'organization',
};

// Axios request to get waiver details
const loadAutoWaiverDetails = createAsyncThunk(`${REDUCER_NAME}/loadWaiver`, (_, { getState, rejectWithValue }) => {
  const { ownerType: ownerTypeRaw, ownerId, waiverId } = selectRouterCurrentParams(getState());
  const ownerType = mapWaiverOwnerType[ownerTypeRaw] || ownerTypeRaw;
  return axios
    .get(getAutoWaiversConfigurationURLWaiver(ownerType, ownerId, waiverId))
    .then(prop('data'))
    .catch(rejectWithValue);
});

const loadAutoWaiverRequestedDetails = (state) => ({
  ...state,
  loading: true,
  loadError: null,
});

const loadAutoWaiverFulfilledDetails = (state, { payload }) => ({
  ...state,
  loading: false,
  loadError: null,
  waiverDetails: payload,
});

const loadAutoWaiverFailedDetails = (state, { payload }) => ({
  ...state,
  loading: false,
  loadError: Messages.getHttpErrorMessage(payload),
});

const autoWaiverDetailsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {},
  extraReducers: {
    [loadAutoWaiverDetails.pending]: loadAutoWaiverRequestedDetails,
    [loadAutoWaiverDetails.fulfilled]: loadAutoWaiverFulfilledDetails,
    [loadAutoWaiverDetails.rejected]: loadAutoWaiverFailedDetails,
  },
});

export default autoWaiverDetailsSlice.reducer;
export const actions = {
  ...autoWaiverDetailsSlice.actions,
  loadAutoWaiverDetails,
};
