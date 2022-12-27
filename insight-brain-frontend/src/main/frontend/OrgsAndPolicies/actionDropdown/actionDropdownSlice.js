/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { OWNER_ACTIONS } from 'MainRoot/OrgsAndPolicies/utility/constants';
import { selectApplicationId } from '../../reduxUiRouter/routerSelectors';
import { getApplicationSummaryUrl } from 'MainRoot/util/CLMLocation.js';
import { includes, prop } from 'ramda';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { selectOwnerProperties, selectSelectedOwnerId } from '../orgsAndPoliciesSelectors';
import { getPermissions } from 'MainRoot/util/authorizationUtil';

const REDUCER_NAME = `${OWNER_ACTIONS}/actionDropdown`;
export const permissions = ['WRITE', 'EVALUATE_APPLICATION'];

export const initialState = {
  loading: false,
  loadError: null,
  applicationSummary: null,
  hasPermissionToChangeAppId: null,
  hasPermissionToEvaluateApp: null,
};

const loadApplicationSummary = createAsyncThunk(
  `${REDUCER_NAME}/loadApplicationSummary`,
  (_, { getState, rejectWithValue }) => {
    const state = getState();
    const appId = selectApplicationId(state);

    return axios.get(getApplicationSummaryUrl(appId)).then(prop('data')).catch(rejectWithValue);
  }
);

const loadApplicationSummaryPending = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadApplicationSummaryRejected = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const loadApplicationSummaryFulfilled = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;
  state.applicationSummary = payload;
};

const loadPermissions = createAsyncThunk(`${REDUCER_NAME}/loadPermissions`, (_, { getState, rejectWithValue }) => {
  const state = getState();
  const { ownerType } = selectOwnerProperties(state);
  const ownerId = selectSelectedOwnerId(state);

  return getPermissions(permissions, ownerType, ownerId)
    .then((response) => {
      return {
        hasPermissionToChangeAppId: includes('WRITE', response),
        hasPermissionToEvaluateApp: includes('EVALUATE_APPLICATION', response),
      };
    })
    .catch(rejectWithValue);
});

const loadPermissionsPending = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadPermissionsRejected = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
  state.hasPermissionToChangeAppId = false;
  state.hasPermissionToEvaluateApp = false;
};

const loadPermissionsFulfilled = (state, { payload }) => {
  const { hasPermissionToChangeAppId, hasPermissionToEvaluateApp } = payload;
  state.loading = false;
  state.loadError = null;
  state.hasPermissionToChangeAppId = hasPermissionToChangeAppId;
  state.hasPermissionToEvaluateApp = hasPermissionToEvaluateApp;
};

const actionDropdown = createSlice({
  name: REDUCER_NAME,
  initialState,
  extraReducers: {
    [loadApplicationSummary.pending]: loadApplicationSummaryPending,
    [loadApplicationSummary.fulfilled]: loadApplicationSummaryFulfilled,
    [loadApplicationSummary.rejected]: loadApplicationSummaryRejected,
    [loadPermissions.pending]: loadPermissionsPending,
    [loadPermissions.fulfilled]: loadPermissionsFulfilled,
    [loadPermissions.rejected]: loadPermissionsRejected,
  },
});
export default actionDropdown.reducer;

export const actions = {
  ...actionDropdown.actions,
  loadApplicationSummary,
  loadPermissions,
};
