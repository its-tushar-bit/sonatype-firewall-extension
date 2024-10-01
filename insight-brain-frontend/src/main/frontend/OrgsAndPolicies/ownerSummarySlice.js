/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { Messages } from 'MainRoot/utilAngular/CommonServices';

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
