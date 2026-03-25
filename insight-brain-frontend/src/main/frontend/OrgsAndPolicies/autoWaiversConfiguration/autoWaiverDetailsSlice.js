/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk } from '@reduxjs/toolkit';
import createSlice from 'MainRoot/reduxConfig/createSlice';
import axios from 'axios';
import { Messages } from 'MainRoot/util/CommonServices';
import { getAutoWaiversConfigurationURLWaiver } from 'MainRoot/util/CLMLocation';
import { prop } from 'ramda';
import { selectCurrentRouteName, selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';

const REDUCER_NAME = 'autoWaiverDetails';

export const initialState = Object.freeze({
  loading: false,
  loadError: null,
  waiverDetails: null,
});

// Axios request to get waiver details
const loadAutoWaiverDetails = createAsyncThunk(
  `${REDUCER_NAME}/loadWaiver`,
  async (_, { getState, rejectWithValue }) => {
    const state = getState();
    const { ownerType, autoWaiverOwnerId, autoWaiverId, ownerId, waiverId } = selectRouterCurrentParams(state);
    const currentRoute = selectCurrentRouteName(state);

    if (currentRoute === 'waiver.details') {
      return axios
        .get(getAutoWaiversConfigurationURLWaiver(ownerType, ownerId, waiverId))
        .then(prop('data'))
        .catch(rejectWithValue);
    }

    return axios
      .get(getAutoWaiversConfigurationURLWaiver(ownerType, autoWaiverOwnerId, autoWaiverId))
      .then(prop('data'))
      .catch(rejectWithValue);
  }
);

const loadAutoWaiverRequestedDetails = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadAutoWaiverFulfilledDetails = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;
  state.waiverDetails = payload;
};

const loadAutoWaiverFailedDetails = (state, { payload }) => {
  state.waiverDetails = null;
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

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
