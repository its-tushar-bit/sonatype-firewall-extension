/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice, unwrapResult } from '@reduxjs/toolkit';
import axios from 'axios';
import { prop, propEq, find } from 'ramda';

import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { propSet } from 'MainRoot/util/reduxToolkitUtil';
import { getOwnerDetailsUrl } from 'MainRoot/util/CLMLocation';

import { selectOwnerProperties, selectEntityId } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectIsApplication, selectIsRepositoriesRelated } from 'MainRoot/reduxUiRouter/routerSelectors';
import { actions as rootActions } from 'MainRoot/OrgsAndPolicies/rootSlice';
import { actions as ownerDetailTreeActions } from 'MainRoot/OrgsAndPolicies/ownerDetailTreeSlice';
import { actions as applicationCategoriesActions } from 'MainRoot/OrgsAndPolicies/assignApplicationCategoriesSlice';
import { actions as applicationActions } from 'MainRoot/OrgsAndPolicies/applicationsSlice';
import { actions as organizationsActions } from 'MainRoot/OrgsAndPolicies/organizationsSlice';
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
  const isRepositories = selectIsRepositoriesRelated(state);
  return axios.get(getOwnerDetailsUrl(ownerType, ownerId, isRepositories)).then(prop('data')).catch(rejectWithValue);
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
    dispatch(ownerSideNavActions.loadOwnerListIfNeeded()),
  ];

  const isApp = selectIsApplication(state);
  const isRepositories = selectIsRepositoriesRelated(state);

  if (isApp) {
    promises.push(dispatch(applicationActions.loadApplications()));
    promises.push(dispatch(applicationCategoriesActions.loadApplicableCategories()));
  } else if (!isRepositories) {
    promises.push(dispatch(organizationsActions.loadOrganizations()));
  }

  return Promise.all(promises)
    .then((results) => {
      if (!isRepositories) {
        dispatch(ownerSideNavActions.setDisplayedOrganizations(results[1]));

        const siblings = unwrapResult(results[2]);
        const entityId = selectEntityId(state);
        const owner = find(propEq(isApp ? 'publicId' : 'id', entityId))(siblings);
        if (!owner) {
          throw `Could not find an ${isApp ? 'application' : 'organization'} with ID ${entityId}.`;
        }

        dispatch(rootActions.setSelectedOwner(owner));
      } else {
        dispatch(rootActions.setSelectedOwner({ name: 'Repositories', id: 'REPOSITORY_CONTAINER_ID' }));
      }
    })
    .catch(rejectWithValue);
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
