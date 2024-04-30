/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { compose, createAsyncThunk, createSlice, original } from '@reduxjs/toolkit';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import axios from 'axios';
import {
  getRepositoriesUrl,
  getRepositoryInfoUrl,
  getRepositoryListUrl,
  getRepositoryManagerUrl,
} from 'MainRoot/util/CLMLocation';
import { pathSet, propSet, propSetConst } from 'MainRoot/util/reduxToolkitUtil';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import {
  selectDeleteModalInfo,
  selectEditRepositoryManagerNameModalInfo,
  selectOriginalRepositories,
} from './repositoriesConfigurationSelectors';
import { selectIsRepositoryManager, selectIncludesManagementView } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { actions as namespaceConfusionProtectionTileSliceActions } from 'MainRoot/OrgsAndPolicies/repositories/namespaceConfusionProtectionTile/namespaceConfusionProtectionTileSlice';
import { actions as ownerSideNavSliceActions } from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSlice';
import { ascend, descend, path, pathOr, prop, sortWith, toLower } from 'ramda';

const REDUCER_NAME = 'repositories';

const initialSortConfiguration = [
  {
    key: 'publicId',
    dir: 'asc',
  },
  {
    key: 'format',
    dir: 'asc',
  },
  {
    key: 'repositoryType',
    dir: 'asc',
  },
  {
    key: 'managerInstanceId',
    dir: 'asc',
  },
];

export const initialState = {
  originalRepositories: [],
  repositories: [],
  loading: false,
  loadError: null,
  deleteError: null,
  editRepositoryManagerNameError: null,
  showDeleteModal: false,
  showEditRepositoryManagerNameModal: false,
  submitMaskState: null,
  deleteModalInfo: {
    id: null,
    publicId: null,
  },
  editRepositoryManagerNameModalInfo: {
    managerInstanceId: null,
    managerName: null,
    repoManagerId: null,
  },
  sortConfiguration: initialSortConfiguration,
  repositoryPublicIdFilter: '',
  repositoryFormatsFilter: new Set(),
};

const openDeleteModal = (state, { payload: { publicId, id } }) => {
  state.showDeleteModal = true;
  state.deleteModalInfo = { publicId, id };
  state.deleteError = null;
};

const openEditRepositoryManagerNameModal = (state, { payload: { managerInstanceId, managerName, repoManagerId } }) => {
  state.showEditRepositoryManagerNameModal = true;
  state.editRepositoryManagerNameModalInfo = { managerInstanceId, managerName, repoManagerId };
  state.editRepositoryManagerNameError = null;
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
    case 'format':
      return compose(toLower, pathOr('', ['repository', key]));
    case 'repositoryType':
      return compose(toLower, path(['repository', key]));
    default:
      return pathOr('', ['repository', key]);
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
  const repos = sortRepositoriesByConfig(payload || [], [...original(state.sortConfiguration)]);
  return {
    ...state,
    loading: false,
    loadError: null,
    originalRepositories: repos,
    repositories: repos,
  };
};

const loadRepositoriesFailed = (state, { payload }) => {
  state.loading = false;
  state.repositories = null;
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

const editRepositoryManagerNameRequested = (state) => {
  state.loading = true;
  state.submitMaskState = false;
  state.editRepositoryManagerNameError = null;
};

const editRepositoryManagerNameFulfilled = (state) => {
  state.loading = false;
  state.submitMaskState = true;
};

const editRepositoryManagerNameFailed = (state, { payload }) => {
  state.loading = false;
  state.submitMaskState = null;
  state.editRepositoryManagerNameError = Messages.getHttpErrorMessage(payload);
};

const loadRepositories = createAsyncThunk(`${REDUCER_NAME}/loadRepositories`, (_, { rejectWithValue }) => {
  return axios
    .get(getRepositoriesUrl())
    .then(path(['data', 'repositories']))
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
          const isRepositoryManager = selectIsRepositoryManager(getState());
          const { id } = selectSelectedOwner(getState());
          const isManagementViewRoute = selectIncludesManagementView(getState());

          dispatch(actions.resetSubmitMaskState());
          dispatch(actions.setShowDeleteModal(false));

          if (isRepositoryManager) {
            dispatch(loadRepositoriesByManagerId(id));
            if (isManagementViewRoute) {
              dispatch(ownerSideNavSliceActions.loadOwnerList());
            }
          } else {
            dispatch(loadRepositories());
          }
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      })
      .catch(rejectWithValue);
  }
);

const editRepositoryManagerName = createAsyncThunk(
  `${REDUCER_NAME}/editRepositoryManagerName`,
  (_, { getState, rejectWithValue, dispatch }) => {
    const state = getState();
    const { managerInstanceId, managerName, repoManagerId } = selectEditRepositoryManagerNameModalInfo(getState());
    const originalRepositories = selectOriginalRepositories(state);
    const managerId =
      repoManagerId ||
      originalRepositories.find((repository) => repository.managerInstanceId === managerInstanceId).repository
        .repositoryManagerId;
    return axios
      .put(getRepositoryManagerUrl(managerId, managerName))
      .then(() => {
        setTimeout(() => {
          dispatch(actions.resetSubmitMaskState());
          dispatch(actions.setShowEditRepositoryManagerNameModal(false));
          dispatch(loadRepositories());
          dispatch(namespaceConfusionProtectionTileSliceActions.getComponentNamePatterns());
          dispatch(ownerSideNavSliceActions.loadIfNeeded(true));
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      })
      .catch(rejectWithValue);
  }
);

const setRepositoryPublicIdFilter = (state, { payload }) => {
  const newState = {
    ...state,
    repositoryPublicIdFilter: payload,
  };
  return filterRepositories(newState);
};

const setRepositoryFormatsFilter = (state, { payload }) => {
  const newState = {
    ...state,
    repositoryFormatsFilter: payload,
  };
  return filterRepositories(newState);
};

const filterRepositories = (state) => {
  return {
    ...state,
    repositories: state.originalRepositories.filter((repository) => {
      if (!repository.repository.publicId.toLowerCase().includes(state.repositoryPublicIdFilter.toLowerCase())) {
        return false;
      }
      if (state.repositoryFormatsFilter.size > 0 && !state.repositoryFormatsFilter.has(repository.repository.format)) {
        return false;
      }
      return true;
    }),
  };
};

const loadRepositoriesByManagerIdRequested = (state) => {
  state.loading = true;
  state.loadError = null;
  state.repositories = [];
  state.submitMaskState = null;
};

const loadRepositoriesByManagerIdFulfilled = (state, { payload }) => {
  const repos = sortRepositoriesByConfig(payload || [], [...original(state.sortConfiguration)]);
  return {
    ...state,
    loading: false,
    loadError: null,
    originalRepositories: repos,
    repositories: repos,
  };
};

const loadRepositoriesByManagerIdFailed = (state, { payload }) => {
  state.loading = false;
  state.repositories = null;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const loadRepositoriesByManagerId = createAsyncThunk(
  `${REDUCER_NAME}/loadRepositoriesByManagerId`,
  (managerId, { rejectWithValue }) => {
    return axios
      .get(getRepositoryListUrl(managerId))
      .then(path(['data', 'repositories']))
      .catch(rejectWithValue);
  }
);

const repositoriesSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setShowDeleteModal: propSet('showDeleteModal'),
    setShowEditRepositoryManagerNameModal: propSet('showEditRepositoryManagerNameModal'),
    setRepositoryManagerName: pathSet(['editRepositoryManagerNameModalInfo', 'managerName']),
    resetSubmitMaskState: propSetConst('submitMaskState', null),
    openDeleteModal,
    openEditRepositoryManagerNameModal,
    sortRepositories,
    setRepositoryPublicIdFilter,
    setRepositoryFormatsFilter,
  },
  extraReducers: {
    [loadRepositories.pending]: loadRepositoriesRequested,
    [loadRepositories.fulfilled]: loadRepositoriesFulfilled,
    [loadRepositories.rejected]: loadRepositoriesFailed,
    [deleteRepository.pending]: deleteRepositoryRequested,
    [deleteRepository.fulfilled]: deleteRepositoryFulfilled,
    [deleteRepository.rejected]: deleteRepositoryFailed,
    [editRepositoryManagerName.pending]: editRepositoryManagerNameRequested,
    [editRepositoryManagerName.fulfilled]: editRepositoryManagerNameFulfilled,
    [editRepositoryManagerName.rejected]: editRepositoryManagerNameFailed,
    [loadRepositoriesByManagerId.pending]: loadRepositoriesByManagerIdRequested,
    [loadRepositoriesByManagerId.fulfilled]: loadRepositoriesByManagerIdFulfilled,
    [loadRepositoriesByManagerId.rejected]: loadRepositoriesByManagerIdFailed,
  },
});

export default repositoriesSlice.reducer;

export const actions = {
  ...repositoriesSlice.actions,
  loadRepositories,
  deleteRepository,
  editRepositoryManagerName,
  loadRepositoriesByManagerId,
};
