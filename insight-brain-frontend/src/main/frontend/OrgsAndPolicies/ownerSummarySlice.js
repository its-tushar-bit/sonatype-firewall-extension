/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk } from '@reduxjs/toolkit';
import createSlice from 'MainRoot/reduxConfig/createSlice';
import { Messages } from 'MainRoot/util/CommonServices';

import { selectIsApplication } from 'MainRoot/reduxUiRouter/routerSelectors';
import { actions as rootActions } from 'MainRoot/OrgsAndPolicies/rootSlice';
import { actions as stagesActions } from 'MainRoot/OrgsAndPolicies/stagesSlice';
import { propSet } from '../util/reduxToolkitUtil';
import { selectOwnerInfo } from 'MainRoot/reduxUiRouter/routerSelectors';
import { checkPermissions } from 'MainRoot/util/authorizationUtil';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';

const REDUCER_NAME = 'ownerSummary';

export const initialState = {
  loading: false,
  loadError: null,
  hasEditIqPermission: false,
  hasViewIqPermission: false,
};

const checkEditIqPermission = createAsyncThunk(
  `${REDUCER_NAME}/checkEditIqPermission`,
  (_, { rejectWithValue, getState }) => {
    const state = getState();
    const ownerInfo = selectOwnerInfo(state);
    const selectedOwner = selectSelectedOwner(state);
    const ownerType = ownerInfo.ownerType;
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

const checkViewIqPermission = createAsyncThunk(
  `${REDUCER_NAME}/checkViewIqPermission`,
  (_, { rejectWithValue, getState }) => {
    const state = getState();
    const ownerInfo = selectOwnerInfo(state);
    const selectedOwner = selectSelectedOwner(state);
    const ownerType = ownerInfo.ownerType;
    const ownerId = selectedOwner.id;
    return checkPermissions(['READ'], ownerType, ownerId).catch(rejectWithValue);
  }
);

const checkViewIqPermissionFulfilled = (state) => {
  state.hasViewIqPermission = true;
};

const checkViewIqPermissionFailed = (state) => {
  state.hasViewIqPermission = false;
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
    const promises = [dispatch(rootActions.loadSelectedOwner())];
    if (isApp) {
      promises.push(dispatch(stagesActions.loadDashboardStages()));
    }
    return Promise.all(promises)
      .then(() => {
        return Promise.all([dispatch(actions.checkEditIqPermission()), dispatch(actions.checkViewIqPermission())]);
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
    [checkViewIqPermission.fulfilled]: checkViewIqPermissionFulfilled,
    [checkViewIqPermission.rejected]: checkViewIqPermissionFailed,
  },
});

export const actions = {
  ...ownerSummarySlice.actions,
  loadOwnerSummary,
  checkEditIqPermission,
  checkViewIqPermission,
};

export default ownerSummarySlice.reducer;
