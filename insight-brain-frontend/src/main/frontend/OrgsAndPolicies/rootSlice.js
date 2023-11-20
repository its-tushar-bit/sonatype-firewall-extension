/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { path, prop } from 'ramda';

import { propSet } from 'MainRoot/util/reduxToolkitUtil';
import {
  getApplicablePolicies,
  getApplicationSummaryUrl,
  getOrganizationUrl,
  getRepositoryManagerById,
} from '../util/CLMLocation';
import { selectEntityId, selectOwnerProperties, selectSelectedOwner } from './orgsAndPoliciesSelectors';
import {
  selectCurrentRouteName,
  selectIsApplication,
  selectIsRepositoriesRelated,
  selectIsRepositoryManager,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import { Messages } from 'MainRoot/utilAngular/CommonServices';

const REDUCER_NAME = 'orgsAndPolicies';

export const initialState = {
  loading: false,
  loadError: null,
  selectedOwner: {},
  policiesByOwner: null,
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
  (forceReload, { getState, rejectWithValue }) => {
    const state = getState();
    const isApp = selectIsApplication(state);
    const isRepositories = selectIsRepositoriesRelated(state);
    const isRepositoryManager = selectIsRepositoryManager(state);
    const entityId = selectEntityId(state);
    const selectedOwner = selectSelectedOwner(state);
    const shouldReloadOwner = forceReload || entityId !== (isApp ? selectedOwner.publicId : selectedOwner.id);

    if (!shouldReloadOwner) {
      return Promise.resolve(selectedOwner);
    }

    if (isRepositories) {
      if (isRepositoryManager) {
        return axios.get(getRepositoryManagerById(entityId)).then(prop('data')).catch(rejectWithValue);
      }
      return Promise.resolve({ name: 'Repositories', id: 'REPOSITORY_CONTAINER_ID' });
    }

    const loadOwnerPromise = isApp
      ? axios.get(getApplicationSummaryUrl(entityId))
      : axios.get(getOrganizationUrl(entityId));

    return loadOwnerPromise.then(prop('data')).catch(rejectWithValue);
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
