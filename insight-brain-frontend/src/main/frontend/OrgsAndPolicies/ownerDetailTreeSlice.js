/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { prop } from 'ramda';

import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { propSet } from 'MainRoot/util/reduxToolkitUtil';
import { getOwnerDetailsUrl } from 'MainRoot/util/CLMLocation';

import { selectOwnerProperties } from './orgsAndPoliciesSelectors';
import { selectIsRepositories } from 'MainRoot/reduxUiRouter/routerSelectors';

const REDUCER_NAME = 'ownerDetailTree';

export const initialState = {
  loading: false,
  loadError: null,
  ownerDetails: {},
};

const loadOwnerDetails = createAsyncThunk(`${REDUCER_NAME}/loadOwnerDetails`, (_, { getState, rejectWithValue }) => {
  const state = getState();
  const { ownerType, ownerId } = selectOwnerProperties(state);
  const isRepositories = selectIsRepositories(state);
  return axios.get(getOwnerDetailsUrl(ownerType, ownerId, isRepositories)).then(prop('data')).catch(rejectWithValue);
});

const loadOwnerDetailsRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadOwnerDetailsFulfilled = (state, { payload }) => {
  state.loading = false;
  state.ownerDetails = payload;
};

const loadOwnerDetailsFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const ownerDetailTreeSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setLoading: propSet('loading'),
    setLoadError: propSet('loadError'),
  },
  extraReducers: {
    [loadOwnerDetails.pending]: loadOwnerDetailsRequested,
    [loadOwnerDetails.fulfilled]: loadOwnerDetailsFulfilled,
    [loadOwnerDetails.rejected]: loadOwnerDetailsFailed,
  },
});

export const actions = {
  ...ownerDetailTreeSlice.actions,
  loadOwnerDetails,
};

export default ownerDetailTreeSlice.reducer;
