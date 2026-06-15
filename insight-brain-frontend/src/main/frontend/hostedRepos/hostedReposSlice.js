/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import axios from 'axios';
import { Messages } from 'MainRoot/util/CommonServices';
import { getRepositoryManagerUrl } from 'MainRoot/util/CLMLocation';

const REDUCER_NAME = 'hostedRepos';

const initialState = {
  repositoryManagers: [],
  loading: false,
  error: null,
  renameError: null,
  renaming: false,
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

// Async thunk to rename a repository manager
export const renameRepositoryManager = createAsyncThunk(
  `${REDUCER_NAME}/renameRepositoryManager`,
  async ({ id, instanceId, newName }, { rejectWithValue }) => {
    try {
      await axios.put(getRepositoryManagerUrl(id, newName));
      return { instanceId, newName };
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

const clearRenameError = (state) => {
  state.renameError = null;
};

const renameRepositoryManagerRequested = (state) => {
  state.renaming = true;
  state.renameError = null;
};

const renameRepositoryManagerFulfilled = (state, { payload: { instanceId, newName } }) => {
  state.renaming = false;
  const manager = state.repositoryManagers.find((rm) => rm.instanceId === instanceId);
  if (manager) {
    manager.name = newName;
  }
};

const renameRepositoryManagerFailed = (state, { payload }) => {
  state.renaming = false;
  state.renameError = Messages.getHttpErrorMessage(payload);
};

const hostedReposSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    clearError,
    clearRenameError,
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchRepositoryManagers.pending, fetchRepositoryManagersRequested)
      .addCase(fetchRepositoryManagers.fulfilled, fetchRepositoryManagersFulfilled)
      .addCase(fetchRepositoryManagers.rejected, fetchRepositoryManagersFailed)
      .addCase(renameRepositoryManager.pending, renameRepositoryManagerRequested)
      .addCase(renameRepositoryManager.fulfilled, renameRepositoryManagerFulfilled)
      .addCase(renameRepositoryManager.rejected, renameRepositoryManagerFailed);
  },
});

export const actions = {
  ...hostedReposSlice.actions,
  fetchRepositoryManagers,
  renameRepositoryManager,
};

export default hostedReposSlice.reducer;
