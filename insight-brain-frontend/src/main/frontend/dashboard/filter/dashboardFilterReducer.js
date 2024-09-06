/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { compose, curry, equals, find, indexBy, map, merge, pick, prop, propEq, sortBy, uniqBy } from 'ramda';
import { propSet, pathSet, lookup } from '../../util/jsUtil';
import defaultFilter from './defaultFilter';
import {
  ages,
  defaultMaxDaysOld,
  policyTypes,
  policyViolationStates,
  expirationDates,
  uncategorizedCategory,
  defaultMinExpiration,
  dashboardFilterOptionsTab,
} from './staticFilterEntries';
import {
  LOAD_FILTER_REQUESTED,
  LOAD_FILTER_FAILED,
  FETCH_AVAILABLE_FILTER_OPTIONS_FULFILLED,
  FETCH_CURRENT_FILTER_FULFILLED,
  APPLY_FILTER_REQUESTED,
  APPLY_FILTER_FULFILLED,
  APPLY_FILTER_FAILED,
  APPLY_FILTER_CANCELLED,
  APPLY_SAVED_FILTER_FAILED,
  TOGGLE_FILTER,
  TOGGLE_APPS_AND_ORGS,
  TOGGLE_REPOSITORIES,
  SELECT_AGE,
  SELECT_EXPIRATION_DATE,
  REVERT_FILTER,
  SET_DISPLAY_SAVE_FILTER_MODAL,
  TOGGLE_FILTER_SIDEBAR,
} from './dashboardFilterActions';

import { UI_ROUTER_ON_FINISH } from '../../reduxUiRouter/routerActions';

const initState = Object.freeze({
  filterSidebarOpen: false,
  loading: true,
  loadError: null,
  applyFilterError: null,
  loadErrorFilterName: null,
  filtersAreDirty: false,
  needsAcknowledgement: false,
  showAgeFilter: false,
  showStagesFilter: false,
  showRepositoriesFilter: false,
  showViolationStateFilter: false,
  showExpirationDateFilter: false,
  showSaveFilterModal: false,
  isWaiversTab: false,
  showPolicyWaiverReasonFilter: false,

  // available filter items
  organizations: null,
  applications: null,
  categories: null,
  stages: null,
  repositories: null,
  waiverReasons: null,

  ages,
  policyTypes,
  policyViolationStates,
  expirationDates,

  // selected filter items
  appliedFilter: defaultFilter,
  selected: defaultFilter,
});

const resetProps = curry((propNames, state) => merge(state, pick(propNames, initState)));

export default function dashboardFilterReducer(state = initState, { type, payload }) {
  switch (type) {
    case UI_ROUTER_ON_FINISH: {
      const filterOptions = dashboardFilterOptionsTab[payload.toState.name]
        ? { ...dashboardFilterOptionsTab[payload.toState.name] }
        : { ...dashboardFilterOptionsTab.default };
      return { ...state, ...filterOptions };
    }

    case LOAD_FILTER_REQUESTED:
      return compose(propSet('loading', true), resetProps(['loadError']))(state);

    case LOAD_FILTER_FAILED:
      return { ...state, loadError: payload, loading: false };

    case FETCH_AVAILABLE_FILTER_OPTIONS_FULFILLED:
      return setAvailable(state, payload);

    case FETCH_CURRENT_FILTER_FULFILLED:
      return compose(
        applyFilter(payload),
        propSet('needsAcknowledgement', payload.needsAcknowledgement),
        propSet('filterSidebarOpen', payload.needsAcknowledgement),
        propSet('loading', false)
      )(state);

    case APPLY_FILTER_REQUESTED:
      return resetProps(['applyFilterError', 'loadErrorFilterName'], state);

    case APPLY_FILTER_FULFILLED: {
      return compose(applyFilter(payload), propSet('needsAcknowledgement', false))(state);
    }

    case APPLY_FILTER_FAILED:
      return { ...state, applyFilterError: payload };

    case APPLY_SAVED_FILTER_FAILED:
      return { ...state, loadErrorFilterName: payload };

    case APPLY_FILTER_CANCELLED:
      return { ...state, applyFilterError: null };

    case TOGGLE_FILTER:
      return compose(setFiltersAreDirty, toggleFilter(payload))(state);

    case TOGGLE_APPS_AND_ORGS:
      return compose(setFiltersAreDirty, toggleAppsAndOrgs(payload))(state);

    case TOGGLE_REPOSITORIES:
      return compose(setFiltersAreDirty, toggleRepositories(payload))(state);

    case SELECT_AGE:
      return compose(setFiltersAreDirty, selectAge(payload))(state);

    case SELECT_EXPIRATION_DATE:
      return compose(setFiltersAreDirty, selectExpirationDate(payload))(state);

    case REVERT_FILTER:
      return compose(revertFilter, resetProps(['filtersAreDirty', 'loadErrorFilterName']))(state);

    case SET_DISPLAY_SAVE_FILTER_MODAL:
      return { ...state, showSaveFilterModal: payload };

    case TOGGLE_FILTER_SIDEBAR:
      return state.filterSidebarOpen && (state.filtersAreDirty || state.needsAcknowledgement)
        ? state
        : {
            ...state,
            filterSidebarOpen: payload,
          };

    default:
      return state;
  }
}

function revertFilter(state) {
  return { ...state, selected: state.appliedFilter };
}

const selectAge = (maxDaysOld) => (state) => {
  return pathSet(['selected', 'maxDaysOld'], getAge(state, maxDaysOld), state);
};

const selectExpirationDate = (expirationDate) => (state) => {
  return pathSet(['selected', 'expirationDate'], expirationDate || defaultMinExpiration, state);
};

const toggleFilter = ({ filterName, selectedIds }) => (state) => {
  return pathSet(['selected', filterName], selectedIds, state);
};

const toggleAppsAndOrgs = ({ selectedOrganizations, selectedApplications }) => (state) => {
  return compose(
    pathSet(['selected', 'organizations'], selectedOrganizations),
    pathSet(['selected', 'applications'], selectedApplications)
  )(state);
};

const toggleRepositories = (selectedRepositories) => (state) => {
  return compose(pathSet(['selected', 'repositories'], selectedRepositories))(state);
};

function setFiltersAreDirty(state) {
  return {
    ...state,
    filtersAreDirty: !equals(state.selected, state.appliedFilter),
  };
}

function setAvailable(state, payload) {
  const selectedOrgsLookup = indexBy(prop('id'), payload.organizations);
  const findOrgById = lookup(selectedOrgsLookup);
  const { applications } = payload;

  // add missing Orgs (no permission to org scenario)
  const appsWithNoOrg = applications.filter((app) => findOrgById(app.organizationId) === undefined);
  const missingOrgs = uniqBy(
    prop('id'),
    map((app) => ({ id: app.organizationId, name: app.organizationName }), appsWithNoOrg)
  );

  // filter out ROOT ORG
  const orgsWithoutRoot = payload.organizations.filter((organization) => organization.id !== 'ROOT_ORGANIZATION_ID');
  const organizations = [...orgsWithoutRoot, ...missingOrgs];

  // populate categories owner
  const categoriesWithOwner = payload.categories.map((category) => {
    const relatedOrg = findOrgById(category.organizationId);
    // we will want to sort app categories by nameLowercase
    category.nameLowercase = category.name.toLocaleLowerCase('en-US');
    return relatedOrg ? { ...category, owner: relatedOrg.name } : category;
  });

  // the "uncategorized applications" category should always be first, so we need to sort the rest of them here
  const sortedCategories = sortBy(prop('nameLowercase'), categoriesWithOwner);

  // add "uncategorized applications" Category
  const categories = [uncategorizedCategory, ...sortedCategories];

  // normalize stages
  const stages = payload.stages.map(({ stageTypeId, stageName }) => ({
    id: stageTypeId,
    name: stageName,
  }));

  const repositories = Array.isArray(payload.repositories)
    ? payload.repositories.map(({ managerInstanceId, repository }) => ({
        fullName: `${repository.publicId} - ${managerInstanceId}`,
        name: `${repository.publicId} - ${managerInstanceId.substring(0, managerInstanceId.indexOf('-'))}`,
        id: repository.id,
      }))
    : [];

  const waiverReasons = payload.waiverReasons;

  return { ...state, organizations, applications, categories, stages, repositories, waiverReasons };
}

const applyFilter = ({ filter }) => (state) => {
  if (filter == null) {
    return state;
  }

  // organizations: select only visible orgs
  const organizationFilters = filter.organizationFilters || [];
  const findAvailableOrgById = (id) => find(propEq('id', id), state.organizations);
  const visibleOrgIds = organizationFilters.filter(findAvailableOrgById);
  const organizations = new Set(visibleOrgIds);

  // applications: include potentially missing selected apps belonging to selected orgs
  const belongsToSelectedOrg = (app) => organizations.has(app.organizationId);
  const appsFromSelectedOrgs = state.applications.filter(belongsToSelectedOrg).map(prop('id'));
  const applications = new Set([...filter.applicationFilters, ...appsFromSelectedOrgs]);

  // repositories: select only visible repositories
  const repositoryFilters = filter.repositoryFilters || [];
  const findAvailableReposById = (id) => find(propEq('id', id), state.repositories);
  const visibleRepoIds = repositoryFilters.filter(findAvailableReposById);
  const repositories = new Set(visibleRepoIds);

  // categories: avoid adding no-longer-existing category ids to selected.categories
  const tagFilters = filter.tagFilters || [];
  const existingCategoryIds = new Set(state.categories.map(prop('id')));
  const selectedCategoryIds = tagFilters.filter((categoryId) => existingCategoryIds.has(categoryId));
  const categories = new Set(selectedCategoryIds);

  const stages = new Set(filter.stageTypeFilters);
  const policyTypes = new Set(filter.policyThreatCategoryFilters);
  const policyViolationStates = new Set(
    // For backward compatibility.
    filter.policyViolationStates.map((state) => (state === 'GRANDFATHERED' ? 'LEGACY_VIOLATION' : state))
  );
  const expirationDate = getExpiration(state, filter.expirationDate);
  const maxDaysOld = getAge(state, filter.maxDaysOld);
  const policyThreatLevels = [filter.minPolicyThreatLevel, filter.maxPolicyThreatLevel];

  const policyWaiverReasonIds = new Set(filter.policyWaiverReasonIds);

  const selected = Object.freeze({
    organizations,
    applications,
    repositories,
    categories,
    stages,
    policyTypes,
    policyViolationStates,
    maxDaysOld,
    policyThreatLevels,
    expirationDate,
    policyWaiverReasonIds,
  });
  return {
    ...state,
    selected,
    appliedFilter: selected,
    filtersAreDirty: false,
  };
};

function getAge(state, maxDaysOld) {
  const current = find(propEq('id', maxDaysOld), state.ages);
  return current ? current.id : defaultMaxDaysOld;
}

function getExpiration(state, selectedExpiration) {
  const current = find(propEq('id', selectedExpiration), state.expirationDates);
  return current ? current.id : defaultMinExpiration;
}
