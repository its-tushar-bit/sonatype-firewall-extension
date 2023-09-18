/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createSlice, createAsyncThunk, unwrapResult } from '@reduxjs/toolkit';
import { path, propEq, find } from 'ramda';

import { propSet } from 'MainRoot/util/reduxToolkitUtil';
import { getApplicablePolicies } from '../util/CLMLocation';
import { selectEntityId, selectOwnerProperties } from './orgsAndPoliciesSelectors';
import {
  selectCurrentRouteName,
  selectIsApplication,
  selectIsOrganization,
  selectIsRepositoriesRelated,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import { actions as applicationActions } from 'MainRoot/OrgsAndPolicies/applicationsSlice';
import { actions as organizationsActions } from 'MainRoot/OrgsAndPolicies/organizationsSlice';
import { Messages } from 'MainRoot/utilAngular/CommonServices';

const REDUCER_NAME = 'orgsAndPolicies';

export const initialState = {
  loading: false,
  loadError: null,
  selectedOwner: {},
  policiesByOwner: null,
};

const setSelectedOwner = (state, { payload }) => {
  state.selectedOwner = payload;
};

const setSelectedOwnerContact = (state, { payload }) => {
  state.selectedOwner.contact = payload;
};

const selectedOwnerParentOrganizationUpdated = (
  state,
  { payload: { organizationName, organizationId, parentOrganizationId } }
) => {
  state.selectedOwner.organizationName = organizationName;
  state.selectedOwner.organizationId = organizationId;
  state.selectedOwner.parentOrganizationId = parentOrganizationId;
};

const loadApplicablePoliciesByOwner = createAsyncThunk(
  `${REDUCER_NAME}/loadApplicablePoliciesByOwner`,
  (_, { getState, rejectWithValue }) => {
    let ownerType, ownerId;
    const currentRouteName = selectCurrentRouteName(getState());
    if (currentRouteName === 'management.view.repository_container') {
      ownerType = 'repository_container';
      ownerId = 'REPOSITORY_CONTAINER_ID';
    } else {
      const ownerProperties = selectOwnerProperties(getState());
      ownerType = ownerProperties.ownerType;
      ownerId = ownerProperties.ownerId;
    }
    return axios
      .get(getApplicablePolicies(ownerType, ownerId))
      .then(path(['data', 'policiesByOwner']))
      .catch(rejectWithValue);
  }
);

const loadSelectedOwner = createAsyncThunk(
  `${REDUCER_NAME}/loadSelectedOwner`,
  (_, { getState, rejectWithValue, dispatch }) => {
    const state = getState();
    const isApp = selectIsApplication(state);
    const isOrg = selectIsOrganization(state);
    const isRepositories = selectIsRepositoriesRelated(state);
    let loadOwnerPromise = Promise.resolve({});
    if (isApp) {
      loadOwnerPromise = dispatch(applicationActions.loadApplications());
    } else if (isOrg) {
      loadOwnerPromise = dispatch(organizationsActions.loadOrganizations());
    }
    return loadOwnerPromise
      .then((results) => {
        if (isRepositories) {
          return { name: 'Repositories', id: 'REPOSITORY_CONTAINER_ID' };
        } else {
          const siblings = unwrapResult(results);
          const entityId = selectEntityId(state);
          const owner = find(propEq(isApp ? 'publicId' : 'id', entityId))(siblings);
          if (!owner) {
            throw `Could not find an ${isApp ? 'application' : 'organization'} with ID ${entityId}.`;
          }
          return owner;
        }
      })
      .catch(rejectWithValue);
  }
);

const loadSelectedOwnerRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadSelectedOwnerFulFilled = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;
  state.selectedOwner = payload;
};

const loadSelectedOwnerFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const rootSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setSelectedOwner,
    setSelectedOwnerContact,
    selectedOwnerParentOrganizationUpdated,
  },
  extraReducers: {
    [loadSelectedOwner.pending]: loadSelectedOwnerRequested,
    [loadSelectedOwner.fulfilled]: loadSelectedOwnerFulFilled,
    [loadSelectedOwner.rejected]: loadSelectedOwnerFailed,
    [loadApplicablePoliciesByOwner.fulfilled]: propSet('policiesByOwner'),
  },
});

export const actions = {
  ...rootSlice.actions,
  loadSelectedOwner,
  loadApplicablePoliciesByOwner,
};

export default rootSlice.reducer;
