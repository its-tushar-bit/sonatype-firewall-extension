/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/*
 * TODO: TECHNICAL DEBT - State Duplication Between userSession and user
 *
 * Currently, user session data is stored in TWO places in Redux state:
 * 1. state.userSession.data (this slice) - handles async session fetching
 * 2. state.user.currentUser (userReducer.js) - handles UI concerns (password changes, warnings)
 *
 * This duplication exists because userSessionSlice was added in Oct 2025 to refactor session
 * management from module-scoped promises to Redux, but the migration was incomplete.
 *
 * PLAN: Consolidate into state.userSession and eliminate state.user
 * - Move password change logic and UI state (shouldDisplayNotice, canChangePassword, etc.) into this slice
 * - Migrate all code that uses state.user selectors to use state.userSession
 * - Remove userReducer.js and userActions.js once migration is complete
 *
 * TEMPORARY WORKAROUND:
 * - fetchUserSession dispatches LOAD_USER_FULFILLED to keep both slices in sync
 * - This dispatch should be REMOVED once state.user is eliminated
 * - See lines 30-36 below for the temporary sync logic
 */

import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import axios from 'axios';
import { getSessionUrl } from 'MainRoot/util/CLMLocation';
import { LOAD_USER_FULFILLED } from './userActions';

const REDUCER_NAME = 'userSession';

const initialState = {
  data: null,
  loading: false,
  error: null,
};

export const fetchUserSession = createAsyncThunk(
  `${REDUCER_NAME}/fetchUserSession`,
  async (waitForLogin = true, { rejectWithValue, dispatch }) => {
    try {
      // waitForLogin is passed in as a request configuration here so that axios interceptors can look for it
      // when deciding whether to show the login modal
      const response = await axios.get(getSessionUrl(), { waitForLogin });

      // TODO: REMOVE THIS DISPATCH when state.user is eliminated (see file header comment)
      // Temporary workaround to keep state.user.currentUser in sync with state.userSession.data
      // This dispatch ensures selectIsLoggedIn (which reads from state.user) works correctly
      dispatch({
        type: LOAD_USER_FULFILLED,
        payload: {
          currentUser: response.data,
          shouldDisplayWarning: false, // This will be handled by userActions if needed
        },
      });

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

// Action creator that ensures user is logged in
// Returns a promise that resolves immediately if user is already logged in,
// or once user logs in if they're not
export const ensureUserLoggedIn = createAsyncThunk(
  `${REDUCER_NAME}/ensureUserLoggedIn`,
  async (_, { getState, dispatch }) => {
    const state = getState();

    // If user is already logged in, return immediately
    if (state.userSession.data) {
      return state.userSession.data;
    }

    // Otherwise, dispatch fetchUserSession and wait for it to complete
    const result = await dispatch(fetchUserSession(true));

    // Return the user data from the fulfilled action
    if (fetchUserSession.fulfilled.match(result)) {
      return result.payload;
    }

    // If it was rejected, throw to propagate the error
    throw result.error;
  }
);

const userSessionSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    resetUserSession: () => initialState,
  },
  extraReducers: (builder) => {
    builder
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
      });
  },
});

export const actions = {
  ...userSessionSlice.actions,
  fetchUserSession,
  ensureUserLoggedIn,
};

export default userSessionSlice.reducer;
