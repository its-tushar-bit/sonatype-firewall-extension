/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { compose, createAsyncThunk, createSlice, original } from '@reduxjs/toolkit';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import axios from 'axios';
import { getRepositoriesUrl, getRepositoryInfoUrl } from 'MainRoot/util/CLMLocation';
import { propSet, propSetConst } from 'MainRoot/util/reduxToolkitUtil';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { selectDeleteModalInfo } from './repositoriesConfigurationSelectors';
import { ascend, descend, path, prop, sortWith, toLower } from 'ramda';

const REDUCER_NAME = 'repositories';

const initialSortConfiguration = [
  {
    key: 'publicId',
    dir: 'asc',
  },
  {
    key: 'managerInstanceId',
    dir: 'asc',
  },
  {
    key: 'enabled',
    dir: 'asc',
  },
];

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
  sortConfiguration: initialSortConfiguration,
};

const openDeleteModal = (state, { payload: { publicId, id } }) => {
  state.showDeleteModal = true;
  state.deleteModalInfo = { publicId, id };
  state.deleteError = null;
};

const getNextDir = (currentDir) => (currentDir === 'asc' ? 'desc' : 'asc');

const setSortConfiguration = (state, column) => {
  const sortConfiguration = [...original(state.sortConfiguration)];
  const index = sortConfiguration.findIndex((columnObj) => columnObj.key === column);
  if (index === 0)
    sortConfiguration[index] = { ...sortConfiguration[index], dir: getNextDir(sortConfiguration[index].dir) };
  else sortConfiguration.unshift(sortConfiguration.splice(index, 1)[0]);
  state.sortConfiguration = sortConfiguration;
};

const getSortKey = (key) => {
  switch (key) {
    case 'managerInstanceId':
      return compose(toLower, prop(key));
    case 'publicId':
      return compose(toLower, path(['repository', key]));
    default:
      return path(['repository', key]);
  }
};

const sortRepositoriesByConfig = (repositories, sortConfiguration) => {
  const customSort = sortConfiguration.map((config) =>
    config.dir === 'desc' ? descend(getSortKey(config.key)) : ascend(getSortKey(config.key))
  );
  const sortedRepositories = sortWith(customSort, repositories);
  return sortedRepositories;
};

const sortRepositories = (state, { payload: column }) => {
  setSortConfiguration(state, column);
  state.repositories = sortRepositoriesByConfig(state.repositories, state.sortConfiguration);
};

const loadRepositoriesRequested = (state) => {
  state.loading = true;
  state.loadError = null;
  state.submitMaskState = null;
};

const loadRepositoriesFulfilled = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;
  state.repositories = sortRepositoriesByConfig(payload || [], [...original(state.sortConfiguration)]);
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
    openDeleteModal,
    sortRepositories,
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
