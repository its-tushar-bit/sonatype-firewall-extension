/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import axios from 'axios';
import { getRepositoriesUrl, getRepositoryInfoUrl } from 'MainRoot/util/CLMLocation';
import { propSet, propSetConst } from 'MainRoot/util/reduxToolkitUtil';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { selectDeleteModalInfo } from './repositoriesConfigurationSelectors';

const REDUCER_NAME = 'repositories';

export const initialState = {
  repositories: [],
  loading: false,
  loadError: null,
  deleteError: null,
  showDeleteModal: false,
  submitMaskState: null,
  deleteModalInfo: {
    id: null,
    publicId: null,
  },
  sortConfiguration: null,
  unsortedRepositories: [],
};

const comparators = {
  publicId: (repositoryA, repositoryB) => (repositoryA.repository.publicId > repositoryB.repository.publicId ? 1 : -1),
  managerInstanceId: (repositoryA, repositoryB) =>
    repositoryA.managerInstanceId > repositoryB.managerInstanceId ? 1 : -1,
  enabled: (repositoryA, repositoryB) => (repositoryA.repository.enabled > repositoryB.repository.enabled ? 1 : -1),
};

const setSort = (state, { payload }) => {
  if (state.sortConfiguration?.column !== payload) {
    state.sortConfiguration = { dir: 'asc', column: payload };
    state.repositories = state.repositories.slice().sort(comparators[payload]);
  } else if (state.sortConfiguration?.dir === 'asc') {
    state.sortConfiguration = { dir: 'desc', column: payload };
    state.repositories = state.repositories.slice().sort((a, b) => -comparators[payload](a, b));
  } else {
    state.sortConfiguration = null;
    state.repositories = state.unsortedRepositories;
  }
};

const openDeleteModal = (state, { payload: { publicId, id } }) => {
  state.showDeleteModal = true;
  state.deleteModalInfo = { publicId, id };
  state.deleteError = null;
};

const loadRepositoriesRequested = (state) => {
  state.loading = true;
  state.loadError = null;
  state.submitMaskState = null;
};

const loadRepositoriesFulfilled = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;
  state.repositories = payload || [];
  state.unsortedRepositories = payload || [];
};

const loadRepositoriesFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const deleteRepositoryRequested = (state) => {
  state.loading = true;
  state.deleteError = null;
  state.submitMaskState = false;
};

const deleteRepositoryFulfilled = (state) => {
  state.loading = false;
  state.deleteError = null;
  state.submitMaskState = true;
};

const deleteRepositoryFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.loading = false;
  state.deleteError = Messages.getHttpErrorMessage(payload);
};

const loadRepositories = createAsyncThunk(`${REDUCER_NAME}/loadRepositories`, (_, { rejectWithValue }) => {
  return axios(getRepositoriesUrl())
    .then(({ data }) => data.repositories)
    .catch(rejectWithValue);
});

const deleteRepository = createAsyncThunk(
  `${REDUCER_NAME}/deleteRepository`,
  (_, { getState, rejectWithValue, dispatch }) => {
    const { id } = selectDeleteModalInfo(getState());
    return axios
      .delete(getRepositoryInfoUrl(id))
      .then(() => {
        setTimeout(() => {
          dispatch(actions.resetSubmitMaskState());
          dispatch(actions.setShowDeleteModal(false));
          dispatch(loadRepositories());
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      })
      .catch(rejectWithValue);
  }
);

const repositoriesSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setShowDeleteModal: propSet('showDeleteModal'),
    resetSubmitMaskState: propSetConst('submitMaskState', null),
    setSort,
    openDeleteModal,
  },
  extraReducers: {
    [loadRepositories.pending]: loadRepositoriesRequested,
    [loadRepositories.fulfilled]: loadRepositoriesFulfilled,
    [loadRepositories.rejected]: loadRepositoriesFailed,
    [deleteRepository.pending]: deleteRepositoryRequested,
    [deleteRepository.fulfilled]: deleteRepositoryFulfilled,
    [deleteRepository.rejected]: deleteRepositoryFailed,
  },
});

export default repositoriesSlice.reducer;

export const actions = {
  ...repositoriesSlice.actions,
  loadRepositories,
  deleteRepository,
};
