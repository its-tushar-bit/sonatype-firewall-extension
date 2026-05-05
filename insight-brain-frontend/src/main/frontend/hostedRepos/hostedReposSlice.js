/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import axios from 'axios';
import { Messages } from 'MainRoot/util/CommonServices';

const REDUCER_NAME = 'hostedRepos';

const initialState = {
  repositoryManagers: [],
  loading: false,
  error: null,
};

// Async thunk to fetch repository managers
export const fetchRepositoryManagers = createAsyncThunk(
  `${REDUCER_NAME}/fetchRepositoryManagers`,
  async (_, { rejectWithValue }) => {
    try {
      const response = await axios.get('/api/v2/lifecycle/repositoryManagers');
      return response.data.repositoryManagers || [];
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

// Reducer functions
const fetchRepositoryManagersRequested = (state) => {
  state.loading = true;
  state.error = null;
};

const fetchRepositoryManagersFulfilled = (state, { payload }) => {
  state.loading = false;
  state.repositoryManagers = payload;
};

const fetchRepositoryManagersFailed = (state, { payload }) => {
  state.loading = false;
  state.error = Messages.getHttpErrorMessage(payload);
};

const clearError = (state) => {
  state.error = null;
};

const hostedReposSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    clearError,
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchRepositoryManagers.pending, fetchRepositoryManagersRequested)
      .addCase(fetchRepositoryManagers.fulfilled, fetchRepositoryManagersFulfilled)
      .addCase(fetchRepositoryManagers.rejected, fetchRepositoryManagersFailed);
  },
});

export const actions = {
  ...hostedReposSlice.actions,
  fetchRepositoryManagers,
};

export default hostedReposSlice.reducer;
