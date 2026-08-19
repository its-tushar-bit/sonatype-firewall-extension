/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk } from '@reduxjs/toolkit';
import createSlice from 'MainRoot/reduxConfig/createSlice';
import { path } from 'ramda';

import { propSet } from 'MainRoot/util/reduxToolkitUtil';
import {
  getApplicablePolicies,
  getApplicationSummaryUrl,
  getOrganizationUrl,
  getRepositoryContainer,
  getRepositoryInfoUrl,
  getRepositoryManagerById,
} from '../util/CLMLocation';
import { selectEntityId, selectOwnerProperties, selectSelectedOwner } from './orgsAndPoliciesSelectors';
import {
  selectIsApplication,
  selectIsRepositoriesRelated,
  selectIsRepository,
  selectIsRepositoryContainer,
  selectIsRepositoryManager,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import { Messages } from 'MainRoot/util/CommonServices';

const REDUCER_NAME = 'orgsAndPolicies';

export const initialState = {
  loading: false,
  loadError: null,
  selectedOwner: {},
  policiesByOwner: null,
  showLimitedFirewallAccessAlert: false,
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

const setShowLimitedFirewallAccessAlert = (state, { payload }) => {
  state.showLimitedFirewallAccessAlert = payload;
};

const handleOwnerLoadError = (error, dispatch, rejectWithValue, shouldHandleAlert = true) => {
  if (shouldHandleAlert) {
    if (error?.response?.status === 403) {
      dispatch(rootSlice.actions.setShowLimitedFirewallAccessAlert(true));
    } else {
      dispatch(rootSlice.actions.setShowLimitedFirewallAccessAlert(false));
    }
  }
  return rejectWithValue(error);
};

const loadApplicablePoliciesByOwner = createAsyncThunk(
  `${REDUCER_NAME}/loadApplicablePoliciesByOwner`,
  (_, { getState, rejectWithValue }) => {
    const ownerProperties = selectOwnerProperties(getState());
    const ownerType = ownerProperties.ownerType;
    const ownerId = ownerProperties.ownerId;
    if (!ownerId) {
      return;
    }
    return axios
      .get(getApplicablePolicies(ownerType, ownerId))
      .then(path(['data', 'policiesByOwner']))
      .catch(rejectWithValue);
  }
);

const loadSelectedOwner = createAsyncThunk(
  `${REDUCER_NAME}/loadSelectedOwner`,
  (forceReload, { getState, rejectWithValue, dispatch }) => {
    const state = getState();
    const isApp = selectIsApplication(state);
    const isRepositories = selectIsRepositoriesRelated(state);
    const isRepository = selectIsRepository(state);
    const isRepositoryManager = selectIsRepositoryManager(state);
    const isRepositoryContainer = selectIsRepositoryContainer(state);
    const entityId = selectEntityId(state);
    const selectedOwner = selectSelectedOwner(state);
    const shouldReloadOwner =
      forceReload || (entityId && entityId !== (isApp ? selectedOwner.publicId : selectedOwner.id));
    if (!shouldReloadOwner) {
      // Reset limited firewall access alert flag when returning cached repository-related data
      // to prevent stale state from previous navigations
      if (isRepositories && selectedOwner.id) {
        dispatch(rootSlice.actions.setShowLimitedFirewallAccessAlert(false));
      }
      return Promise.resolve(selectedOwner);
    }
    if (isRepositories) {
      if (isRepositoryManager) {
        return axios
          .get(getRepositoryManagerById(entityId))
          .then((response) => {
            dispatch(rootSlice.actions.setShowLimitedFirewallAccessAlert(false));
            return {
              ...response.data,
              type: 'repository_manager',
            };
          })
          .catch((error) => handleOwnerLoadError(error, dispatch, rejectWithValue));
      } else if (isRepositoryContainer) {
        return axios
          .get(getRepositoryContainer())
          .then((response) => {
            dispatch(rootSlice.actions.setShowLimitedFirewallAccessAlert(false));
            return {
              ...response.data,
              type: 'repository_container',
            };
          })
          .catch((error) => handleOwnerLoadError(error, dispatch, rejectWithValue));
      } else if (isRepository) {
        return axios
          .get(getRepositoryInfoUrl(entityId))
          .then((response) => {
            const repository = path(['data', 'repository'], response) || {};
            return { ...repository, name: repository?.publicId, type: 'repository' };
          })
          .catch(rejectWithValue);
      } else {
        return Promise.resolve({ id: entityId });
      }
    }
    const loadOwnerPromise = isApp
      ? axios.get(getApplicationSummaryUrl(entityId))
      : axios.get(getOrganizationUrl(entityId));
    return loadOwnerPromise
      .then((response) => {
        dispatch(rootSlice.actions.setShowLimitedFirewallAccessAlert(false));
        return {
          ...response.data,
          type: isApp ? 'application' : 'organization',
        };
      })
      .catch((error) => handleOwnerLoadError(error, dispatch, rejectWithValue));
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
    setShowLimitedFirewallAccessAlert,
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
