/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk } from '@reduxjs/toolkit';
import createSlice from 'MainRoot/reduxConfig/createSlice';
import axios from 'axios';
import { Messages } from 'MainRoot/util/CommonServices';
import { getAutoWaiverExclusionsByAutoWaiverIdUrl } from 'MainRoot/util/CLMLocation';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { prop } from 'ramda';

const REDUCER_NAME = 'autoWaiverDetailsExclusions';

export const initialState = Object.freeze({
  loading: false,
  loadError: null,
  data: null,
  serverData: null,
});

const loadAutoWaiverExclusion = createAsyncThunk(
  `${REDUCER_NAME}/loadAutoWaiverExclusion`,
  async (_, { getState, rejectWithValue }) => {
    const state = getState();
    const { ownerType, autoWaiverOwnerId, autoWaiverId } = selectRouterCurrentParams(state);

    if (!autoWaiverId) {
      return rejectWithValue('No auto waiver ID found');
    }

    return axios
      .get(getAutoWaiverExclusionsByAutoWaiverIdUrl(ownerType, autoWaiverOwnerId, autoWaiverId))
      .then(prop('data'))
      .catch(rejectWithValue);
  }
);

const loadAutoWaiverExclusionRequested = (state) => {
  state.loading = true;
  state.error = null;
};

const loadAutoWaiverExclusionFulfilled = (state, { payload }) => {
  state.loading = false;
  state.data = payload;
  state.error = null;
};

const loadAutoWaiverExclusionFailed = (state, { payload }) => {
  state.data = null;
  state.loading = false;
  state.error = Messages.getHttpErrorMessage(payload);
};

const autoWaiverExclusionsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {},
  extraReducers: {
    [loadAutoWaiverExclusion.pending]: loadAutoWaiverExclusionRequested,
    [loadAutoWaiverExclusion.fulfilled]: loadAutoWaiverExclusionFulfilled,
    [loadAutoWaiverExclusion.rejected]: loadAutoWaiverExclusionFailed,
  },
});

export const actions = {
  ...autoWaiverExclusionsSlice.actions,
  loadAutoWaiverExclusion,
};

export default autoWaiverExclusionsSlice.reducer;
