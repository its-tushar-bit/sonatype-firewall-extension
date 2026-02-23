/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { getValidPermissions } from 'MainRoot/util/permissionService';
import { stateRequiresAuthentication } from 'MainRoot/utility/services/routeStateUtilService';
import { selectIsLoggedIn } from 'MainRoot/user/userSessionSelectors';
import { selectRouterState } from 'MainRoot/reduxUiRouter/routerSelectors';

const REDUCER_NAME = 'mainHeader';

const MAIN_HEADER_PERMISSIONS = [
  'CONFIGURE_SYSTEM',
  'MANAGE_PROPRIETARY',
  'VIEW_ROLES',
  'MANAGE_AUTOMATIC_APPLICATION_CREATION',
  'MANAGE_AUTOMATIC_SCM_CONFIGURATION',
];

export const initialState = {
  permissions: {},
  shouldShowLoginButton: false,
  loading: false,
  loadError: null,
};

// Async thunk to load permissions
export const loadPermissions = createAsyncThunk(`${REDUCER_NAME}/loadPermissions`, async (_, { rejectWithValue }) => {
  try {
    // Let the axios interceptor handle authentication if needed
    const data = await getValidPermissions(MAIN_HEADER_PERMISSIONS);

    // Convert array to object for easier lookup
    const perms = {};
    data.forEach((permission) => {
      perms[permission] = true;
    });

    return perms;
  } catch (error) {
    return rejectWithValue(error || 'Failed to load permissions');
  }
});

// Async thunk to check if login button should be shown
export const checkShowLoginButton = createAsyncThunk(
  `${REDUCER_NAME}/checkShowLoginButton`,
  async (_, { getState, rejectWithValue }) => {
    try {
      const state = getState();
      const isLoggedIn = selectIsLoggedIn(state);
      const currentRouterState = selectRouterState(state);
      const stateRequiresAuth = await stateRequiresAuthentication(currentRouterState);

      return !stateRequiresAuth && !isLoggedIn;
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

// Reducer functions
const loadPermissionsPending = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadPermissionsFulfilled = (state, { payload }) => {
  state.loading = false;
  state.permissions = payload;
};

const loadPermissionsRejected = (state, { payload }) => {
  state.loading = false;
  state.loadError = payload;
};

const checkShowLoginButtonFulfilled = (state, { payload }) => {
  state.shouldShowLoginButton = payload;
};

// Create slice
const mainHeaderSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {},
  extraReducers: {
    [loadPermissions.pending]: loadPermissionsPending,
    [loadPermissions.fulfilled]: loadPermissionsFulfilled,
    [loadPermissions.rejected]: loadPermissionsRejected,
    [checkShowLoginButton.fulfilled]: checkShowLoginButtonFulfilled,
  },
});

export default mainHeaderSlice.reducer;

export const actions = {
  ...mainHeaderSlice.actions,
  loadPermissions,
  checkShowLoginButton,
};
