/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk } from '@reduxjs/toolkit';
import { always } from 'ramda';
import createSlice from 'MainRoot/reduxConfig/createSlice';
import axios from 'axios';
import { getAddRepositoryUrl, getVirtualRepositoryManagersUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/util/CommonServices';

const REDUCER_NAME = 'firewallIqProxy';

export const initialState = {
  saving: false,
  saveError: null,
  saveErrorId: 0,
  creatingManager: false,
  createManagerError: null,
  virtualRepositoryManagers: [],
  loadingVirtualRepositoryManagers: false,
  virtualRepositoryManagersLoadError: null,
};

const saveRepositoryRequested = (state) => {
  state.saving = true;
  state.saveError = null;
};

const saveRepositoryFulfilled = (state) => {
  state.saving = false;
};

const saveRepositoryFailed = (state, { payload }) => {
  state.saving = false;
  state.saveError = Messages.getHttpErrorMessage(payload) || 'An error occurred while saving.';
  state.saveErrorId += 1;
};

const createVirtualRepositoryManagerRequested = (state) => {
  state.creatingManager = true;
  state.createManagerError = null;
};

const createVirtualRepositoryManagerFulfilled = (state) => {
  state.creatingManager = false;
};

const createVirtualRepositoryManagerFailed = (state, { payload, meta }) => {
  state.creatingManager = false;
  const status = payload?.response?.status;
  const submittedName = meta?.arg?.name;
  const backendMessage = Messages.getHttpErrorMessage(payload);
  const isDuplicateName =
    status === 409 || (typeof backendMessage === 'string' && /already exists/i.test(backendMessage));
  if (isDuplicateName && submittedName) {
    state.createManagerError = `A Virtual Repository Manager named '${submittedName}' already exists.`;
    return;
  }
  state.createManagerError = backendMessage || 'An error occurred while creating the virtual repository manager.';
};

const fetchVirtualRepositoryManagersRequested = (state) => {
  state.loadingVirtualRepositoryManagers = true;
  state.virtualRepositoryManagersLoadError = null;
};

const fetchVirtualRepositoryManagersFulfilled = (state, { payload }) => {
  state.loadingVirtualRepositoryManagers = false;
  state.virtualRepositoryManagers = payload;
};

const fetchVirtualRepositoryManagersFailed = (state, { payload }) => {
  state.loadingVirtualRepositoryManagers = false;
  state.virtualRepositoryManagersLoadError =
    Messages.getHttpErrorMessage(payload) || 'An error occurred while loading virtual repository managers.';
};

export const saveRepository = createAsyncThunk(
  `${REDUCER_NAME}/saveRepository`,
  ({ repositoryManagerId, name, repoFormat, upstreamUrl }, { rejectWithValue }) =>
    axios
      .post(getAddRepositoryUrl(repositoryManagerId), { publicId: name, format: repoFormat, upstreamUrl })
      .then(({ data }) => data)
      .catch(rejectWithValue)
);

export const createVirtualRepositoryManager = createAsyncThunk(
  `${REDUCER_NAME}/createVirtualRepositoryManager`,
  ({ name }, { rejectWithValue }) =>
    axios
      .post(getVirtualRepositoryManagersUrl(), { name })
      .then(({ data }) => data)
      .catch(rejectWithValue)
);

export const fetchVirtualRepositoryManagers = createAsyncThunk(
  `${REDUCER_NAME}/fetchVirtualRepositoryManagers`,
  (_, { rejectWithValue }) =>
    axios
      .get(getVirtualRepositoryManagersUrl())
      .then(({ data }) => data?.virtualRepositoryManagers || [])
      .catch(rejectWithValue)
);

const firewallIqProxySlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    reset: always(initialState),
    clearCreateManagerError: (state) => {
      state.createManagerError = null;
    },
  },
  extraReducers: {
    [saveRepository.pending]: saveRepositoryRequested,
    [saveRepository.fulfilled]: saveRepositoryFulfilled,
    [saveRepository.rejected]: saveRepositoryFailed,
    [createVirtualRepositoryManager.pending]: createVirtualRepositoryManagerRequested,
    [createVirtualRepositoryManager.fulfilled]: createVirtualRepositoryManagerFulfilled,
    [createVirtualRepositoryManager.rejected]: createVirtualRepositoryManagerFailed,
    [fetchVirtualRepositoryManagers.pending]: fetchVirtualRepositoryManagersRequested,
    [fetchVirtualRepositoryManagers.fulfilled]: fetchVirtualRepositoryManagersFulfilled,
    [fetchVirtualRepositoryManagers.rejected]: fetchVirtualRepositoryManagersFailed,
  },
});

export const actions = {
  ...firewallIqProxySlice.actions,
  saveRepository,
  createVirtualRepositoryManager,
  fetchVirtualRepositoryManagers,
};

export default firewallIqProxySlice.reducer;
