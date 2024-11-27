/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice, unwrapResult } from '@reduxjs/toolkit';
import { equals, isEmpty, omit, reject, clone, dissoc, propEq, curry } from 'ramda';
import debounce from 'debounce';

import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { getOwnerListUrl } from 'MainRoot/util/CLMLocation';
import { UI_ROUTER_ON_FINISH, stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { actions as repositoriesActions } from 'MainRoot/OrgsAndPolicies/repositories/repositoriesConfigurationSlice';
import { sortOwnerByName, fuzzyFilter, flatEntries } from './utils';
import {
  selectRouterCurrentParams,
  selectIsManagementViewRouterState,
  selectIsSbomManager,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import { toggleBooleanProp } from 'MainRoot/util/reduxUtil';
import { validateMinLength } from 'MainRoot/util/validationUtil';
import { selectOwnersMap } from './ownerSideNavSelectors';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { propSet } from 'MainRoot/util/reduxToolkitUtil';

const { initialState: rscInitialState, userInput } = nxTextInputStateHelpers;

export const FILTER_DEBOUNCE = 500;

const REDUCER_NAME = `ownerSideNav`;
export const initialState = {
  loading: false,
  loadError: null,
  ownersMap: {},
  topParentOrganizationId: 'ROOT_ORGANIZATION_ID',
  flattenEntries: {},
  displayedOrganization: null,
  toggleOrganizationsCheck: true,
  toggleApplicationsCheck: true,
  toggleRepositoryManagersCheck: false,
  toggleRepositoriesCheck: true,
  filteredEntries: {
    applications: [],
    organizations: [],
    repositoryManagers: [],
    repositories: [],
  },
  filterQuery: rscInitialState(''),
  filterLoading: false,
};

const loadOwnerList = createAsyncThunk(
  `${REDUCER_NAME}/loadOwnerList`,
  (_, { rejectWithValue, dispatch, getState }) => {
    const state = getState();
    const routerParams = selectRouterCurrentParams(state);
    return axios
      .get(getOwnerListUrl())
      .then(({ data }) => {
        const { ownersMap, topParentOrganizationId } = data || {};
        const displayedOrganization = getDisplayedOrganization(ownersMap, topParentOrganizationId, routerParams);
        dispatch(actions.setDisplayedOrganization(displayedOrganization));
        return { ownersMap, topParentOrganizationId };
      })
      .catch(rejectWithValue);
  }
);

const loadOwnerListFulfilled = (state, { payload = {} }) => {
  const { ownersMap, topParentOrganizationId } = payload;

  state.ownersMap = ownersMap;
  state.topParentOrganizationId = topParentOrganizationId;
};

const loadIfNeeded = (forceReload) => (dispatch, getState) => {
  const state = getState();
  const ownersMap = selectOwnersMap(state);
  const ownersMapExistInMemory = !isNilOrEmpty(ownersMap);
  if (forceReload || !ownersMapExistInMemory) {
    return dispatch(load());
  }
  return Promise.resolve({});
};

const load = createAsyncThunk(`${REDUCER_NAME}/load`, async (_, { getState, dispatch, rejectWithValue }) => {
  const state = getState();
  const promises = [dispatch(loadOwnerList()), dispatch(repositoriesActions.loadRepositories())];
  return Promise.all(promises)
    .then((results) => {
      const { ownersMap, topParentOrganizationId } = unwrapResult(results[0]) || {};
      const routerParams = selectRouterCurrentParams(state);
      const isSbomManager = selectIsSbomManager(state);

      const displayedOrganization = getDisplayedOrganization(ownersMap, topParentOrganizationId, routerParams);

      // for the case when user does not have access to root organization we want them to be
      // redirected to the closest available organization summary page down into n-level hierarchy
      // when the user has no permission to view any organization then redirect to ROOT_ORGANIZATION_ID
      const isManagementViewRoute = selectIsManagementViewRouterState(state);
      if (isManagementViewRoute) {
        dispatch(
          stateGo(
            `${isSbomManager ? 'sbomManager.' : ''}management.view.organization`,
            { organizationId: displayedOrganization.id ?? 'ROOT_ORGANIZATION_ID' },
            { location: 'replace' }
          )
        );
      }

      const flattenEntries = flatEntries(ownersMap);

      // if isManagementViewRoute and there is at least one organization with permission
      // keep loading while redirecting to the closest available organization
      return {
        loading: isManagementViewRoute,
        displayedOrganization,
        flattenEntries,
      };
    })
    .catch(rejectWithValue);
});

const sortOwnerIdListByOwnerName = (ownerIds = [], owners) => {
  const getFromOwnersMap = (ownerId) => owners[ownerId];
  return sortOwnerByName(getFromOwnersMap)(ownerIds);
};

const updateDisplayedOrganization = (state, { payload }) => {
  state.displayedOrganization = getDisplayedOrganization(state.ownersMap, state.topParentOrganizationId, {
    organizationId: payload.organizationId,
  });
};

const getDisplayedOrganization = (ownersMap, topParentOrganizationId = 'ROOT_ORGANIZATION_ID', routerParams) => {
  if (!ownersMap) {
    return null;
  }
  let displayedOrganization;

  const {
    organizationId = '',
    applicationPublicId = '',
    repositoryId = '',
    repositoryManagerId = '',
    repositoryContainerId = '',
  } = routerParams;
  if (organizationId) {
    displayedOrganization = ownersMap[organizationId];
  }

  if (repositoryId) {
    const repository = ownersMap[repositoryId];
    displayedOrganization = ownersMap[repository?.parentId];
  }

  if (repositoryManagerId) {
    displayedOrganization = ownersMap[repositoryManagerId];
  }

  if (repositoryContainerId) {
    displayedOrganization = ownersMap[repositoryContainerId];
  }

  if (applicationPublicId) {
    const application = ownersMap[applicationPublicId];
    displayedOrganization = ownersMap[application?.organizationId];
  }

  return displayedOrganization || ownersMap[topParentOrganizationId] || {};
};

const onRouterFinish = (state, { payload }) => {
  if (payload.toState.name.includes('management.view')) {
    state.displayedOrganization = getDisplayedOrganization(
      state.ownersMap,
      state.topParentOrganizationId,
      payload.toParams
    );
    state.toggleOrganizationsCheck = initialState.toggleOrganizationsCheck;
    state.toggleApplicationsCheck = initialState.toggleApplicationsCheck;
    state.toggleRepositoryManagersCheck = initialState.toggleRepositoryManagersCheck;
    state.toggleRepositoriesCheck = initialState.toggleRepositoriesCheck;
    resetFilter(state);
  }
};

const loadRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadFulfilled = (state, { payload }) => {
  state.loading = payload.loading;
  state.displayedOrganization = payload.displayedOrganization;
  state.toggleOrganizationsCheck = initialState.toggleOrganizationsCheck;
  state.toggleApplicationsCheck = initialState.toggleApplicationsCheck;
  state.toggleRepositoryManagersCheck = initialState.toggleRepositoryManagersCheck;
  state.toggleRepositoriesCheck = initialState.toggleRepositoriesCheck;
  state.flattenEntries = payload.flattenEntries;
};

const loadFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const removeOrganizationFromOwnerHierarchy = (state, { payload: organizationId }) => {
  const organization = state.ownersMap[organizationId];
  if (organization) {
    const ownerIdsToDelete = [];
    collectOwnerIdsToDelete(state.ownersMap, [organizationId], ownerIdsToDelete);
    state.ownersMap = omit(ownerIdsToDelete)(state.ownersMap);

    const parentOrganization = state.ownersMap[organization.parentOrganizationId];
    if (parentOrganization) {
      const organizationIds = reject(equals(organizationId))(parentOrganization.organizationIds);
      state.ownersMap[parentOrganization.id] = { ...parentOrganization, organizationIds };

      incrementOrgCountersInAllParents(state.ownersMap, organization.parentOrganizationId, -(organization.subOrgs + 1));
      incrementAppCountersInAllParents(state.ownersMap, organization.parentOrganizationId, -organization.totalApps);

      state.flattenEntries = flatEntries(state.ownersMap);
    }
  }
};

const removeRepositoryManagerFromOwnerHierarchy = (state, { payload: repositoryManagerId }) => {
  const repositoryManager = state.ownersMap[repositoryManagerId];
  if (repositoryManager) {
    const ownerIdsToDelete = [];
    collectOwnerIdsToDelete(state.ownersMap, [repositoryManagerId], ownerIdsToDelete);
    state.ownersMap = omit(ownerIdsToDelete)(state.ownersMap);

    const repositoryContainer = state.ownersMap[repositoryManager.repositoryContainerId];
    if (repositoryContainer) {
      const repositoryManagerIds = reject(equals(repositoryManagerId))(repositoryContainer.repositoryManagerIds);
      state.ownersMap[repositoryContainer.id] = { ...repositoryContainer, organizationIds: repositoryManagerIds };
    }
  }
};

function collectOwnerIdsToDelete(ownersMap, orgIds, acc) {
  acc.push(...orgIds);
  for (const orgId of orgIds) {
    const org = ownersMap[orgId];
    acc.push(...org.applicationIds);
    collectOwnerIdsToDelete(ownersMap, org.organizationIds, acc);
  }
}

const removeApplicationFromOwnerHierarchy = (state, { payload: applicationPublicId }) => {
  const application = state.ownersMap[applicationPublicId];
  if (application) {
    state.ownersMap = omit([applicationPublicId])(state.ownersMap);

    const parentOrganization = state.ownersMap[application.organizationId];
    if (parentOrganization) {
      state.ownersMap[parentOrganization.id].applicationIds = reject(equals(applicationPublicId))(
        parentOrganization.applicationIds
      );
    }

    incrementAppCountersInAllParents(state.ownersMap, application.organizationId, -1);

    state.flattenEntries.applications = reject(
      propEq('publicId', applicationPublicId),
      state.flattenEntries.applications
    );
  }
};

const filterEntries = (entries, term) => {
  if (isEmpty(entries.organizations)) {
    return null;
  }

  let filteredOrganizations = [];
  let filteredApplications = [];
  let filteredRepositoryManagers = [];
  let filteredRepositories = [];

  if (term && term.length >= 3) {
    filteredOrganizations = fuzzyFilter(entries.organizations, term, 'name');
    filteredApplications = fuzzyFilter(entries.applications, term, 'name');
    filteredRepositoryManagers = fuzzyFilter(entries.repositoryManagers, term, 'name');
    filteredRepositories = fuzzyFilter(entries.repositories, term, 'name');
  }

  return {
    organizations: sortOwnerByName(filteredOrganizations),
    applications: sortOwnerByName(filteredApplications),
    repositoryManagers: sortOwnerByName(filteredRepositoryManagers),
    repositories: sortOwnerByName(filteredRepositories),
  };
};

const resetFilter = (state) => {
  state.filterQuery = initialState.filterQuery;
  state.filteredEntries = initialState.filteredEntries;
  state.filterLoading = false;
};

const filterSidebarEntries = (value) => (dispatch) => {
  dispatch(actions.setFilterQuery(value));
  filterSidebarEntriesDebounce(dispatch, value);
};

const filterSidebarEntriesDebounce = debounce((dispatch, value) => {
  dispatch(actions.setFilteredEntries(value));
}, FILTER_DEBOUNCE);

const setFilterQuery = (state, { payload }) => {
  state.filterLoading = true;
  state.filterQuery = userInput(validateMinLength(3, 'Enter three characters to begin filtering.'), payload);
};

const setFilteredEntries = (state, { payload }) => {
  state.filteredEntries = filterEntries(state.flattenEntries, payload);
  state.filterLoading = false;
};

const updateOwnersMapWithNewAppId = (state, { payload }) => {
  const { currentOwner, newPublicId } = payload;
  const newOwnersMap = clone(dissoc(currentOwner.publicId, state.ownersMap));

  newOwnersMap[newPublicId] = {
    ...currentOwner,
    publicId: newPublicId,
  };

  const idx = state.displayedOrganization.applicationIds.indexOf(currentOwner.publicId);
  const newApplicationIds = [...state.displayedOrganization.applicationIds];

  newApplicationIds[idx] = newPublicId;

  newOwnersMap[state.displayedOrganization.id].applicationIds = newApplicationIds;

  state.ownersMap = newOwnersMap;
};

const updateOwnersMapWithNewEntry = (state, { payload }) => {
  const { isApp, entry } = payload;
  const { ownersMap, displayedOrganization } = state;
  const displayedOrgId = displayedOrganization.id;

  if (isApp) {
    ownersMap[entry.publicId] = { ...entry, type: 'application' };
    ownersMap[displayedOrgId].applicationIds.push(entry.publicId);
    ownersMap[displayedOrgId].applicationIds = sortOwnerIdListByOwnerName(
      ownersMap[displayedOrgId].applicationIds,
      ownersMap
    );

    state.flattenEntries = flatEntries(ownersMap);

    incrementAppCountersInAllParents(ownersMap, displayedOrgId, +1);
  } else {
    ownersMap[entry.id] = {
      ...entry,
      type: 'organization',
      applicationIds: [],
      organizationIds: [],
      subOrgs: 0,
      totalApps: 0,
    };

    ownersMap[displayedOrgId].organizationIds.push(entry.id);
    ownersMap[displayedOrgId].organizationIds = sortOwnerIdListByOwnerName(
      ownersMap[displayedOrgId].organizationIds,
      ownersMap
    );

    state.displayedOrganization = getDisplayedOrganization(ownersMap, state.topParentOrganizationId, {
      organizationId: entry.parentOrganizationId,
    });

    state.flattenEntries.organizations = [...state.flattenEntries.organizations, entry];

    incrementOrgCountersInAllParents(ownersMap, displayedOrgId, +1);
  }
};

const incrementCountersInAllParents = curry((isApp, ownerMap, immediateParentId, value) => {
  let currentOrgId = immediateParentId;
  while (currentOrgId) {
    const currentOrg = ownerMap[currentOrgId];
    if (!currentOrg) {
      currentOrgId = undefined;
    } else {
      if (isApp) {
        currentOrg.totalApps += value;
      } else {
        currentOrg.subOrgs += value;
      }
      currentOrgId = currentOrg.parentOrganizationId;
    }
  }
});

const incrementAppCountersInAllParents = incrementCountersInAllParents(true);
const incrementOrgCountersInAllParents = incrementCountersInAllParents(false);

const ownerSideNavSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    removeOrganizationFromOwnerHierarchy,
    removeApplicationFromOwnerHierarchy,
    removeRepositoryManagerFromOwnerHierarchy,
    toggleOrganizationsCollapse: toggleBooleanProp('toggleOrganizationsCheck'),
    toggleApplicationsCollapse: toggleBooleanProp('toggleApplicationsCheck'),
    toggleRepositoryManagersCollapse: toggleBooleanProp('toggleRepositoryManagersCheck'),
    toggleRepositoresCollapse: toggleBooleanProp('toggleRepositoriesCheck'),
    setDisplayedOrganization: propSet('displayedOrganization'),
    setFilterQuery,
    setFilteredEntries,
    resetFilter,
    updateOwnersMapWithNewAppId,
    updateOwnersMapWithNewEntry,
    updateDisplayedOrganization,
  },
  extraReducers: {
    [loadOwnerList.fulfilled]: loadOwnerListFulfilled,
    [load.pending]: loadRequested,
    [load.fulfilled]: loadFulfilled,
    [load.rejected]: loadFailed,
    [UI_ROUTER_ON_FINISH]: onRouterFinish,
  },
});

export const actions = {
  ...ownerSideNavSlice.actions,
  load,
  loadOwnerList,
  loadIfNeeded,
  filterSidebarEntries,
};

export default ownerSideNavSlice.reducer;
