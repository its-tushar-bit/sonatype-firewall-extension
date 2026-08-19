/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { Messages } from 'MainRoot/util/CommonServices';

const REDUCER_NAME = 'hostedReposList';

const initialSortConfiguration = [
  {
    key: 'publicId',
    dir: 'asc',
  },
];

export const initialState = {
  repositories: [],
  totalCount: 0,
  loading: false,
  loadError: null,
  sortConfiguration: [...initialSortConfiguration],
  repositoryFormatsFilter: '',
  searchText: '',
  availableFormats: [],
  availableFormatsLoading: false,
  managerInstanceId: null,
  managerBaseUrl: null,
  managerName: null,
};

const getNextDir = (currentDir) => (currentDir === 'asc' ? 'desc' : 'asc');

const setSortConfiguration = (state, column) => {
  const sortConfiguration = [...state.sortConfiguration];
  const index = sortConfiguration.findIndex((columnObj) => columnObj.key === column);

  if (index === 0) {
    // Clicking on the currently sorted column - toggle direction
    sortConfiguration[index] = { ...sortConfiguration[index], dir: getNextDir(sortConfiguration[index].dir) };
  } else if (index > 0) {
    // Column exists but not primary - move to front
    sortConfiguration.unshift(sortConfiguration.splice(index, 1)[0]);
  } else {
    // Column not in array (index === -1) - add as new primary sort with asc direction
    sortConfiguration.unshift({ key: column, dir: 'asc' });
  }

  state.sortConfiguration = sortConfiguration;
};

const sortRepositories = (state, { payload: column }) => {
  setSortConfiguration(state, column);
};

const setRepositoryFormatsFilter = (state, { payload }) => {
  state.repositoryFormatsFilter = payload;
};

const setSearchText = (state, { payload }) => {
  state.searchText = payload;
};

const setManagerInfo = (state, { payload }) => {
  state.managerInstanceId = payload.instanceId;
  state.managerBaseUrl = payload.baseUrl ?? null;
  state.managerName = payload.name ?? null;
};

const loadRepositoriesRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadRepositoriesFulfilled = (state, { payload }) => {
  const { repositories, manager, totalCount } = payload;
  state.loading = false;
  state.loadError = null;
  state.repositories = repositories || [];
  state.totalCount = totalCount ?? 0;
  if (manager) {
    state.managerInstanceId = manager.instanceId;
    state.managerBaseUrl = manager.baseUrl;
    state.managerName = manager.name ?? null;
  }
};

const loadRepositoriesFailed = (state, { payload }) => {
  state.loading = false;
  state.repositories = [];
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const loadRepositories = createAsyncThunk(
  `${REDUCER_NAME}/loadRepositories`,
  ({ repositoryManagerId, searchText, format, sortBy, sortDir, page, pageSize }, { rejectWithValue }) => {
    const params = {};
    if (searchText) params.searchText = searchText;
    if (format) params.format = format;
    if (sortBy) params.sortBy = sortBy;
    if (sortDir) params.sortDir = sortDir;
    if (page !== undefined) params.page = page;
    if (pageSize !== undefined) params.pageSize = pageSize;

    return axios
      .get(`/rest/integration/repositories/${repositoryManagerId}/ui/configuredRepositories`, { params })
      .then((response) => response.data)
      .catch(rejectWithValue);
  }
);

const loadAvailableFormats = createAsyncThunk(
  `${REDUCER_NAME}/loadAvailableFormats`,
  (repositoryManagerId, { rejectWithValue }) => {
    return axios
      .get(`/rest/integration/repositories/repositoryManager/${repositoryManagerId}/availableFormats`)
      .then((response) => response.data)
      .catch(rejectWithValue);
  }
);

const loadAvailableFormatsRequested = (state) => {
  state.availableFormatsLoading = true;
};

const loadAvailableFormatsFulfilled = (state, { payload }) => {
  state.availableFormatsLoading = false;
  state.availableFormats = payload;
};

const loadAvailableFormatsFailed = (state) => {
  state.availableFormatsLoading = false;
  state.availableFormats = [];
};

const hostedReposListSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    sortRepositories,
    setRepositoryFormatsFilter,
    setSearchText,
    setManagerInfo,
  },
  extraReducers: (builder) => {
    builder
      .addCase(loadRepositories.pending, loadRepositoriesRequested)
      .addCase(loadRepositories.fulfilled, loadRepositoriesFulfilled)
      .addCase(loadRepositories.rejected, loadRepositoriesFailed)
      .addCase(loadAvailableFormats.pending, loadAvailableFormatsRequested)
      .addCase(loadAvailableFormats.fulfilled, loadAvailableFormatsFulfilled)
      .addCase(loadAvailableFormats.rejected, loadAvailableFormatsFailed);
  },
});

export default hostedReposListSlice.reducer;

export const actions = {
  ...hostedReposListSlice.actions,
  loadRepositories,
  loadAvailableFormats,
};
