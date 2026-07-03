/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { compose, createAsyncThunk, original } from '@reduxjs/toolkit';
import { enableMapSet } from 'immer';
import createSlice from 'MainRoot/reduxConfig/createSlice';

// Enable Immer support for Set and Map in Redux state
enableMapSet();
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import axios from 'axios';
import {
  getRepositoriesUrl,
  getRepositoryInfoUrl,
  getRepositoryListUrl,
  getRepositoryManagerUrl,
} from 'MainRoot/util/CLMLocation';
import { pathSet, propSet, propSetConst } from 'MainRoot/util/reduxToolkitUtil';
import { Messages } from 'MainRoot/util/CommonServices';
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
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';

const REDUCER_NAME = 'repositories';

// View types for scoping state between Repository Container and Repository Manager views
export const VIEW_TYPES = {
  CONTAINER: 'container', // Repository Container view (all repos from all managers)
  MANAGER: 'manager', // Repository Manager view (repos for specific manager)
};

export const initialSortConfiguration = [
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
  // Current view context (container or manager)
  currentView: VIEW_TYPES.CONTAINER,

  // Guard against race conditions: tracks which view initiated the load request.
  // Fulfilled handlers check this to avoid writing stale data to the wrong view slot.
  pendingLoadView: null,

  // Keyed by view type to maintain independent state
  originalRepositories: {
    [VIEW_TYPES.CONTAINER]: [],
    [VIEW_TYPES.MANAGER]: [],
  },
  repositories: {
    [VIEW_TYPES.CONTAINER]: [],
    [VIEW_TYPES.MANAGER]: [],
  },
  sortConfiguration: {
    [VIEW_TYPES.CONTAINER]: [...initialSortConfiguration],
    [VIEW_TYPES.MANAGER]: [...initialSortConfiguration],
  },
  repositoryPublicIdFilter: {
    [VIEW_TYPES.CONTAINER]: '',
    [VIEW_TYPES.MANAGER]: '',
  },
  repositoryFormatsFilter: {
    [VIEW_TYPES.CONTAINER]: new Set(),
    [VIEW_TYPES.MANAGER]: new Set(),
  },

  // Shared state (not view-specific)
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
  const currentView = state.currentView;
  const sortConfiguration = [...original(state.sortConfiguration[currentView])];
  const index = sortConfiguration.findIndex((columnObj) => columnObj.key === column);
  if (index === 0)
    sortConfiguration[index] = { ...sortConfiguration[index], dir: getNextDir(sortConfiguration[index].dir) };
  else sortConfiguration.unshift(sortConfiguration.splice(index, 1)[0]);
  state.sortConfiguration[currentView] = sortConfiguration;
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
  const currentView = state.currentView;
  setSortConfiguration(state, column);
  state.repositories[currentView] = sortRepositoriesByConfig(
    state.repositories[currentView],
    state.sortConfiguration[currentView]
  );
};

const loadRepositoriesRequested = (state) => {
  state.loading = true;
  state.loadError = null;
  state.submitMaskState = null;
  // Store the view that initiated this request for race condition protection
  state.pendingLoadView = state.currentView;
};

const loadRepositoriesFulfilled = (state, { payload }) => {
  const currentView = state.currentView;
  // Race condition protection: skip if view changed during request
  // (pendingLoadView is set to the view that started the request)
  if (state.pendingLoadView !== null && state.pendingLoadView !== undefined && state.pendingLoadView !== currentView) {
    // View changed during in-flight request - discard stale response
    state.loading = false;
    state.pendingLoadView = null;
    return state;
  }
  const repos = sortRepositoriesByConfig(payload || [], [...original(state.sortConfiguration[currentView])]);
  state.loading = false;
  state.loadError = null;
  state.pendingLoadView = null;
  state.originalRepositories[currentView] = repos;
  filterRepositories(state);
};

const loadRepositoriesFailed = (state, { payload }) => {
  const currentView = state.currentView;
  return {
    ...state,
    loading: false,
    pendingLoadView: null,
    repositories: {
      ...state.repositories,
      [currentView]: null,
    },
    originalRepositories: {
      ...state.originalRepositories,
      [currentView]: null,
    },
    loadError: Messages.getHttpErrorMessage(payload),
  };
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

const loadRepositories = createAsyncThunk(
  `${REDUCER_NAME}/loadRepositories`,
  (forceReload = false, { getState, rejectWithValue }) => {
    const state = getState();
    const originalRepositories = selectOriginalRepositories(state);

    if (!isNilOrEmpty(originalRepositories) && !forceReload) {
      return originalRepositories;
    }

    return axios
      .get(getRepositoriesUrl())
      .then(path(['data', 'repositories']))
      .catch(rejectWithValue);
  }
);

const deleteRepository = createAsyncThunk(
  `${REDUCER_NAME}/deleteRepository`,
  (_, { getState, rejectWithValue, dispatch }) => {
    const { id: deletedRepositoryId } = selectDeleteModalInfo(getState());
    return axios
      .delete(getRepositoryInfoUrl(deletedRepositoryId))
      .then(() => {
        setTimeout(() => {
          const isRepositoryManager = selectIsRepositoryManager(getState());
          const { id: selectedOwnerId } = selectSelectedOwner(getState());
          const isManagementViewRoute = selectIncludesManagementView(getState());

          dispatch(actions.resetSubmitMaskState());
          dispatch(actions.setShowDeleteModal(false));

          if (isRepositoryManager) {
            dispatch(loadRepositoriesByManagerId(selectedOwnerId));
            if (isManagementViewRoute) {
              // The forced reload is the source of truth for the sidebar count here.
              dispatch(ownerSideNavSliceActions.loadOwnerList(true));
            } else {
              // No reload on this route, so the optimistic update is the source of truth.
              dispatch(ownerSideNavSliceActions.removeRepositoryFromOwnerHierarchy(deletedRepositoryId));
            }
          } else {
            dispatch(loadRepositories(true));
            dispatch(ownerSideNavSliceActions.loadOwnerList(true));
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
          dispatch(loadRepositories(true));
          dispatch(namespaceConfusionProtectionTileSliceActions.getComponentNamePatterns());
          dispatch(ownerSideNavSliceActions.forceReload());
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      })
      .catch(rejectWithValue);
  }
);

const setRepositoryPublicIdFilter = (state, { payload }) => {
  const currentView = state.currentView;
  state.repositoryPublicIdFilter[currentView] = payload;
  return filterRepositories(state);
};

const setRepositoryFormatsFilter = (state, { payload }) => {
  const currentView = state.currentView;
  state.repositoryFormatsFilter[currentView] = payload;
  return filterRepositories(state);
};

const filterRepositories = (state) => {
  const currentView = state.currentView;
  const publicIdFilter = state.repositoryPublicIdFilter?.[currentView] ?? '';
  const formatsFilter = state.repositoryFormatsFilter?.[currentView] ?? new Set();
  const originalRepos = state.originalRepositories[currentView];

  if (!originalRepos) {
    state.repositories[currentView] = null;
    return state;
  }

  state.repositories[currentView] = originalRepos.filter((repository) => {
    if (!repository?.repository?.publicId?.toLowerCase().includes(publicIdFilter.toLowerCase())) {
      return false;
    }
    if (formatsFilter.size > 0 && !formatsFilter.has(repository?.repository?.format)) {
      return false;
    }
    return true;
  });

  return state;
};

const loadRepositoriesByManagerIdRequested = (state) => {
  const currentView = state.currentView;
  return {
    ...state,
    loading: true,
    pendingLoadView: currentView,
    loadError: null,
    repositories: {
      ...state.repositories,
      [currentView]: [],
    },
    // originalRepositories is intentionally kept until fulfilled so that the
    // format filter options list stays stable during loading — clearing it here
    // causes NxFilterDropdown to crash when a filter is active on return nav.
    submitMaskState: null,
  };
};

const loadRepositoriesByManagerIdFulfilled = (state, { payload }) => {
  const currentView = state.currentView;
  // Race condition protection: skip if view changed during request
  // (pendingLoadView is set to the view that started the request)
  if (state.pendingLoadView !== null && state.pendingLoadView !== undefined && state.pendingLoadView !== currentView) {
    // View changed during in-flight request - discard stale response
    state.loading = false;
    state.pendingLoadView = null;
    return state;
  }
  const repos = sortRepositoriesByConfig(payload || [], [...original(state.sortConfiguration[currentView])]);
  state.loading = false;
  state.loadError = null;
  state.pendingLoadView = null;
  state.originalRepositories[currentView] = repos;

  // Strip any format filter values that don't exist in the newly loaded repos, and reset
  // the text filter. This handles the case where the component remounts with a fresh key
  // for a new manager (key="manager-view-{id}") — Redux retains the previous manager's
  // filter state but the new manager may not have those formats or matching repo names.
  if (state.repositoryPublicIdFilter) {
    state.repositoryPublicIdFilter[currentView] = '';
  }
  const availableFormats = new Set(repos.map((r) => r?.repository?.format).filter(Boolean));
  const formatsFilter = state.repositoryFormatsFilter?.[currentView];
  if (formatsFilter) {
    for (const format of [...formatsFilter]) {
      if (!availableFormats.has(format)) {
        formatsFilter.delete(format);
      }
    }
  }

  filterRepositories(state);
};

const loadRepositoriesByManagerIdFailed = (state, { payload }) => {
  const currentView = state.currentView;
  state.loading = false;
  state.pendingLoadView = null;
  // Use optional chaining for safety when state may be incomplete in tests
  if (state.repositories) {
    state.repositories[currentView] = null;
  }
  if (state.originalRepositories) {
    state.originalRepositories[currentView] = null;
  }
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

const goToRepositorySummaryView = (repositoryId) => {
  return (dispatch) => {
    dispatch(stateGo('management.view.repository', { repositoryId }));
  };
};

const setCurrentView = (state, { payload }) => {
  state.currentView = payload;
};

const resetViewFilters = (state, { payload: viewType }) => {
  state.repositoryPublicIdFilter[viewType] = '';
  state.repositoryFormatsFilter[viewType].clear();
  state.sortConfiguration[viewType] = [...initialSortConfiguration];
};

const repositoriesSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setShowDeleteModal: propSet('showDeleteModal'),
    setShowEditRepositoryManagerNameModal: propSet('showEditRepositoryManagerNameModal'),
    setRepositoryManagerName: pathSet(['editRepositoryManagerNameModalInfo', 'managerName']),
    resetSubmitMaskState: propSetConst('submitMaskState', null),
    setCurrentView,
    resetViewFilters,
    openDeleteModal,
    openEditRepositoryManagerNameModal,
    sortRepositories,
    setRepositoryPublicIdFilter,
    setRepositoryFormatsFilter,
    goToRepositorySummaryView,
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
  goToRepositorySummaryView,
};
