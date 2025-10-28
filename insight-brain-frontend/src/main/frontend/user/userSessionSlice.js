/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import axios from 'axios';
import { getSessionUrl } from 'MainRoot/util/CLMLocation';

const REDUCER_NAME = 'userSession';

const initialState = {
  data: null,
  loading: false,
  error: null,
};

export const fetchUserSession = createAsyncThunk(
  `${REDUCER_NAME}/fetchUserSession`,
  async (waitForLogin = true, { rejectWithValue }) => {
    try {
      // waitForLogin is passed in as a request configuration here so that axios interceptors can look for it
      // when deciding whether to show the login modal
      const response = await axios.get(getSessionUrl(), { waitForLogin });
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
};

export default userSessionSlice.reducer;
