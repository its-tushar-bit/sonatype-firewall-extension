/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { Messages } from 'MainRoot/utilAngular/CommonServices';

import { selectApplicationId, selectIsApplication, selectOrganizationId } from 'MainRoot/reduxUiRouter/routerSelectors';
import { actions as rootActions } from 'MainRoot/OrgsAndPolicies/rootSlice';
import { actions as stagesActions } from 'MainRoot/OrgsAndPolicies/stagesSlice';
import { getApplicationSummaryUrl } from 'MainRoot/util/CLMLocation';
import { propSet } from '../util/reduxToolkitUtil';
import { selectIsRepositoriesRelated, selectOwnerInfo } from 'MainRoot/reduxUiRouter/routerSelectors';
import { checkPermissions } from 'MainRoot/util/authorizationUtil';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';

const REDUCER_NAME = 'ownerSummary';

export const initialState = {
  loading: false,
  loadError: null,
  hasEditIqPermission: false,
};

const checkEditIqPermission = createAsyncThunk(
  `${REDUCER_NAME}/checkEditIqPermission`,
  (_, { rejectWithValue, getState }) => {
    const state = getState();
    const isRepositoriesRelated = selectIsRepositoriesRelated(state);
    const ownerInfo = selectOwnerInfo(state);
    const selectedOwner = selectSelectedOwner(state);
    const ownerType = isRepositoriesRelated ? 'repository_container' : ownerInfo.ownerType;
    const ownerId = selectedOwner.id;
    return checkPermissions(['WRITE'], ownerType, ownerId).catch(rejectWithValue);
  }
);

const checkEditIqPermissionFulfilled = (state) => {
  state.hasEditIqPermission = true;
};

const checkEditIqPermissionFailed = (state) => {
  state.hasEditIqPermission = false;
};

const loadOwnerSummaryRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadOwnerSummaryFulfilled = (state) => {
  state.loading = initialState.loading;
  state.loadError = initialState.loadError;
};

const loadOwnerSummaryFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const loadOwnerSummary = createAsyncThunk(
  `${REDUCER_NAME}/loadOwnerSummary`,
  (_, { getState, dispatch, rejectWithValue }) => {
    const state = getState();
    const isApp = selectIsApplication(state);
    const ownerId = isApp ? selectApplicationId(state) : selectOrganizationId(state);

    const promises = [dispatch(rootActions.loadSelectedOwner()), dispatch(rootActions.loadApplicablePoliciesByOwner())];
    if (isApp) {
      promises.push(dispatch(stagesActions.loadDashboardStages()));
      promises.push(axios.get(getApplicationSummaryUrl(ownerId)));
    }
    return Promise.all(promises)
      .then((results) => {
        if (isApp) {
          const applicationSummary = results[3].data;
          dispatch(rootActions.setSelectedOwnerContact(applicationSummary.contact));
        }
        return dispatch(actions.checkEditIqPermission());
      })
      .catch(rejectWithValue);
  }
);

const ownerSummarySlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setLoading: propSet('loading'),
    setLoadError: propSet('loadError'),
  },
  extraReducers: {
    [loadOwnerSummary.pending]: loadOwnerSummaryRequested,
    [loadOwnerSummary.fulfilled]: loadOwnerSummaryFulfilled,
    [loadOwnerSummary.rejected]: loadOwnerSummaryFailed,
    [checkEditIqPermission.fulfilled]: checkEditIqPermissionFulfilled,
    [checkEditIqPermission.rejected]: checkEditIqPermissionFailed,
  },
});

export const actions = {
  ...ownerSummarySlice.actions,
  loadOwnerSummary,
  checkEditIqPermission,
};

export default ownerSummarySlice.reducer;
