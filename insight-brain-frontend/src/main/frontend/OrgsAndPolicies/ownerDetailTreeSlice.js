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

import { selectOwnerProperties } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectIsApplication, selectIsRepositoryContainer } from 'MainRoot/reduxUiRouter/routerSelectors';
import { actions as rootActions } from 'MainRoot/OrgsAndPolicies/rootSlice';
import { actions as ownerDetailTreeActions } from 'MainRoot/OrgsAndPolicies/ownerDetailTreeSlice';
import { actions as applicationCategoriesActions } from 'MainRoot/OrgsAndPolicies/assignApplicationCategoriesSlice';
import { actions as ownerSideNavActions } from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSlice';

const REDUCER_NAME = 'ownerDetailTree';

export const initialState = {
  loading: false,
  loadError: null,
  ownerDetails: {},
};

const loadOwnerDetails = createAsyncThunk(`${REDUCER_NAME}/loadOwnerDetails`, (_, { getState, rejectWithValue }) => {
  const state = getState();
  const { ownerType, ownerId } = selectOwnerProperties(state);
  const isRepositoryContainer = selectIsRepositoryContainer(state);
  return axios
    .get(getOwnerDetailsUrl(ownerType, ownerId, isRepositoryContainer))
    .then(prop('data'))
    .catch(rejectWithValue);
});

const loadOwnerDetailsRequested = (state) => {
  state.loading = true;
  state.loadError = null;
  state.ownerDetails = {};
};

const loadOwnerDetailsFulfilled = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;
  state.ownerDetails = payload;
};

const loadOwnerDetailsFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
  state.ownerDetails = {};
};

const loadSidebar = createAsyncThunk(`${REDUCER_NAME}/loadSidebar`, (_, { getState, rejectWithValue, dispatch }) => {
  const state = getState();
  const promises = [
    dispatch(ownerDetailTreeActions.loadOwnerDetails()),
    dispatch(ownerSideNavActions.loadOwnerList()),
    dispatch(rootActions.loadSelectedOwner()),
  ];

  const isApp = selectIsApplication(state);

  if (isApp) {
    promises.push(dispatch(applicationCategoriesActions.loadApplicableCategories()));
  }

  return Promise.all(promises).catch(rejectWithValue);
});

const loadSideBarRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadSideBarFulFilled = (state) => {
  state.loading = false;
  state.loadError = null;
};

const loadSidebarFailed = (state, { payload }) => {
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
    [loadSidebar.pending]: loadSideBarRequested,
    [loadSidebar.fulfilled]: loadSideBarFulFilled,
    [loadSidebar.rejected]: loadSidebarFailed,
    [loadOwnerDetails.pending]: loadOwnerDetailsRequested,
    [loadOwnerDetails.fulfilled]: loadOwnerDetailsFulfilled,
    [loadOwnerDetails.rejected]: loadOwnerDetailsFailed,
  },
});

export const actions = {
  ...ownerDetailTreeSlice.actions,
  loadOwnerDetails,
  loadSidebar,
};

export default ownerDetailTreeSlice.reducer;
