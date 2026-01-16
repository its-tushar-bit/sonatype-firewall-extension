/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import axios from 'axios';
import { getSessionUrl, getShouldDisplayDefaultPasswordWarning, getSessionLogoutUrl } from 'MainRoot/util/CLMLocation';
import { isAuthorized } from 'MainRoot/util/permissionService';
import { submitTelemetryData } from 'MainRoot/util/telemetryUtils';
import { logoutRedirection } from 'MainRoot/util/urlUtil';
import { actions as unsavedChangesModalActions } from 'MainRoot/modals/unsavedChangesModal/unsavedChangesModalSlice';
import pendoService from 'MainRoot/pendo/mainBundlePendoService';
import { selectShouldDisplayPasswordWarning, selectUsername } from './userSessionSelectors';
import { selectIsCurrentRouteDirty } from 'MainRoot/reduxUiRouter/routerSelectors';

const REDUCER_NAME = 'userSession';

const initialState = {
  // User session data from server
  data: null,
  loading: false,
  error: null,

  // Password warning UI state
  shouldDisplayPasswordWarning: false,
};

export const fetchUserSession = createAsyncThunk(
  `${REDUCER_NAME}/fetchUserSession`,
  async (waitForLogin = true, { rejectWithValue, dispatch }) => {
    try {
      // waitForLogin is passed in as request configuration so that axios interceptors can look for it
      // when deciding whether to show the login modal
      const response = await axios.get(getSessionUrl(), { waitForLogin });

      // Automatically fetch password warning for all users
      // The fetchPasswordWarning thunk will check if they have admin permissions
      dispatch(fetchPasswordWarning());

      return response.data;
    } catch (error) {
      // 401 means the user is not logged in (and waitForLogin was false), in which case do nothing.
      // Only report other errors
      if (error.response && error.response.status === 401) {
        // Don't reject for 401 errors - leave promise pending by returning a never-resolving promise
        return new Promise(() => {});
      }
      return rejectWithValue(error.response);
    }
  }
);

const fetchPasswordWarning = createAsyncThunk(`${REDUCER_NAME}/fetchPasswordWarning`, async () => {
  try {
    const isAdmin = await isAuthorized(['CONFIGURE_SYSTEM']);
    if (isAdmin) {
      const response = await axios.get(getShouldDisplayDefaultPasswordWarning());
      const shouldDisplay = !!response.data;
      if (shouldDisplay) {
        submitTelemetryData('ADMIN_PASSWORD_CHANGE', { action: 'WARNING_SHOWN' });
      }
      return shouldDisplay;
    }
    return false;
  } catch (error) {
    // If this call fails, don't show the warning
    return false;
  }
});

// Logout - checks for unsaved changes before logging out
export const logout = createAsyncThunk(`${REDUCER_NAME}/logout`, async (_, { getState, dispatch }) => {
  const state = getState();
  const isCurrentRouteDirty = selectIsCurrentRouteDirty(state);

  if (isCurrentRouteDirty) {
    // Show unsaved changes modal, then logout if confirmed
    await dispatch(unsavedChangesModalActions.open());
  }

  await pendoService.flush();
  const resultServerLogout = await axios.delete(getSessionLogoutUrl());

  // Clear any onbeforeunload handlers to avoid being prevented to leave the current page by using redirection.
  // Otherwise a browser blocking alert dialog will appear
  window.onbeforeunload = null;

  let toLocation;
  if (resultServerLogout?.error) {
    toLocation = {
      headers: {},
      error: resultServerLogout.error.message,
    };
  } else {
    toLocation = resultServerLogout.headers['Location'] || resultServerLogout.headers['location'];
  }
  logoutRedirection(toLocation);
});

/**
 * Handles password change completion - checks if we should clear the default admin password warning.
 * Call this after a user's password has been successfully changed.
 *
 * @param {string|undefined} username - Username of the user whose password was changed.
 *                                      If undefined, uses the current logged-in user's username.
 */
export const handlePasswordChangeForUser = createAsyncThunk(
  `${REDUCER_NAME}/handlePasswordChangeForUser`,
  async (username, { getState, dispatch }) => {
    const state = getState();
    const shouldDisplayWarning = selectShouldDisplayPasswordWarning(state);

    // Resolve the username - if not provided, use current user's username
    const isDefaultAdminUser = (username ?? selectUsername(state)) === 'admin';

    // Only clear warning and fire events if:
    // 1. The warning is currently displayed (meaning current user is an admin)
    // 2. The password that was changed belongs to the 'admin' user
    if (shouldDisplayWarning && isDefaultAdminUser) {
      submitTelemetryData('ADMIN_PASSWORD_CHANGE', { action: 'PASSWORD_CHANGED_FROM_DEFAULT' });
      dispatch(clearPasswordWarning());
    }
  }
);

const userSessionSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    resetUserSession: () => initialState,
    clearPasswordWarning: (state) => {
      state.shouldDisplayPasswordWarning = false;
    },
  },
  extraReducers: (builder) => {
    builder
      // User session fetching
      .addCase(fetchUserSession.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchUserSession.fulfilled, (state, { payload }) => {
        state.loading = false;
        state.data = payload;
      })
      .addCase(fetchUserSession.rejected, (state, { payload }) => {
        state.loading = false;
        state.error = payload;
      })
      // Password warning fetching
      .addCase(fetchPasswordWarning.fulfilled, (state, { payload }) => {
        state.shouldDisplayPasswordWarning = payload;
      })
      .addCase(fetchPasswordWarning.rejected, (state) => {
        state.shouldDisplayPasswordWarning = false;
      });
  },
});

export const { clearPasswordWarning } = userSessionSlice.actions;

export const actions = {
  ...userSessionSlice.actions,
  fetchUserSession,
  fetchPasswordWarning,
  logout,
  handlePasswordChangeForUser,
};

export default userSessionSlice.reducer;
