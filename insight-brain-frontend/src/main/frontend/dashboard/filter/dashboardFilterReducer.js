/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {compose, curry, merge, pick, equals, find, propEq, prop, indexBy, sortBy} from 'ramda';
import {propSet, pathSet, lookup} from '../../util/jsUtil';
import {
  ages,
  defaultMaxDaysOld,
  policyTypes,
  policyViolationStates,
  uncategorizedCategory
} from './staticFilterEntries';
import {
  LOAD_FILTER_REQUESTED,
  LOAD_FILTER_FAILED,
  FETCH_AVAILABLE_FILTER_OPTIONS_FULFILLED,
  FETCH_CURRENT_FILTER_FULFILLED,
  APPLY_FILTER_REQUESTED,
  APPLY_FILTER_FULFILLED,
  APPLY_FILTER_FAILED,
  APPLY_SAVED_FILTER_FAILED,
  TOGGLE_FILTER,
  TOGGLE_APPS_AND_ORGS,
  SELECT_AGE,
  CLEAR_FILTER,
  REVERT_FILTER,
  SET_DISPLAY_SAVE_FILTER_MODAL
} from './dashboardFilterActions';

import {UI_ROUTER_ON_FINISH} from '../../reduxUiRouter/routerActions';

const initSelected = Object.freeze({
  organizations: new Set(),
  applications: new Set(),
  categories: new Set(),
  stages: new Set(),
  policyTypes: new Set(),
  policyViolationStates: new Set(['OPEN']),
  maxDaysOld: defaultMaxDaysOld,
  policyThreatLevels: [2, 10]
});

const initState = Object.freeze({
  loading: false,
  loadError: null,
  saveError: null,
  loadErrorFilterName: null,
  filtersAreDirty: false,
  needsAcknowledgement: false,
  isViolationsTab: false,
  showAgeFilter: false,
  showSaveFilterModal: false,

  // available filter items
  organizations: null,
  applications: null,
  categories: null,
  stages: null,
  ages,
  policyTypes,
  policyViolationStates,

  // selected filter items
  appliedFilter: initSelected,
  selected: initSelected
});

const resetProps = curry((propNames, state) => merge(state, pick(propNames, initState)));

export default function dashboardFilterReducer(state = initState, {type, payload}) {
  switch (type) {
    case UI_ROUTER_ON_FINISH: {
      const isViolationsTab = payload.toState.name === 'dashboard.overview.violations';
      const newState = {...state, isViolationsTab};
      return setShowAgeFilter(newState);
    }

    case LOAD_FILTER_REQUESTED:
      return compose(
          propSet('loading', true),
          resetProps(['loadError'])
      )(state);

    case LOAD_FILTER_FAILED:
      return {...state, loadError: payload, loading: false};

    case FETCH_AVAILABLE_FILTER_OPTIONS_FULFILLED:
      return setAvailable(state, payload);

    case FETCH_CURRENT_FILTER_FULFILLED:
      return compose(
          applyFilter(payload),
          propSet('needsAcknowledgement', payload.needsAcknowledgement),
          resetProps(['loading'])
      )(state);

    case APPLY_FILTER_REQUESTED:
      return resetProps(['saveError', 'loadErrorFilterName'], state);

    case APPLY_FILTER_FULFILLED: {
      return compose(
          applyFilter(payload),
          propSet('needsAcknowledgement', false)
      )(state);
    }

    case APPLY_FILTER_FAILED:
      return {...state, saveError: payload};

    case APPLY_SAVED_FILTER_FAILED:
      return {...state, loadErrorFilterName: payload};

    case TOGGLE_FILTER:
      return compose(
          setFiltersAreDirty,
          toggleFilter(payload)
      )(state);

    case TOGGLE_APPS_AND_ORGS:
      return compose(
          setFiltersAreDirty,
          toggleAppsAndOrgs(payload)
      )(state);

    case SELECT_AGE:
      return compose(
          setFiltersAreDirty,
          selectAge(payload)
      )(state);

    case CLEAR_FILTER:
      return compose(
          setFiltersAreDirty,
          resetProps(['selected', 'loadErrorFilterName'])
      )(state);

    case REVERT_FILTER:
      return compose(
          revertFilter,
          resetProps(['filtersAreDirty', 'loadErrorFilterName'])
      )(state);

    case SET_DISPLAY_SAVE_FILTER_MODAL:
      return {...state, showSaveFilterModal: payload};

    default:
      return state;
  }
}

function revertFilter(state) {
  return {...state, selected: state.appliedFilter};
}

const selectAge = maxDaysOld => state => {
  return pathSet(['selected', 'maxDaysOld'], getAge(state, maxDaysOld), state);
};

const toggleFilter = ({filterName, selectedIds}) => state => {
  return pathSet(['selected', filterName], selectedIds, state);
};

const toggleAppsAndOrgs = ({selectedOrganizations, selectedApplications}) => state => {
  return compose(
      pathSet(['selected', 'organizations'], selectedOrganizations),
      pathSet(['selected', 'applications'], selectedApplications)
  )(state);
};

function setFiltersAreDirty(state) {
  return {...state, filtersAreDirty: !equals(state.selected, state.appliedFilter)};
}

function setAvailable(state, payload) {
  const selectedOrgsLookup = indexBy(prop('id'), payload.organizations);
  const findOrgById = lookup(selectedOrgsLookup);
  const {applications} = payload;

  // add missing Orgs (no permission to org scenario)
  const appsWithNoOrg = applications.filter(app => findOrgById(app.organizationId) === undefined);
  const missingOrgs = appsWithNoOrg.map(app => ({id: app.organizationId, name: app.organizationName}));

  // filter out ROOT ORG
  const orgsWithoutRoot = payload.organizations.filter(organization => organization.id !== 'ROOT_ORGANIZATION_ID');
  const organizations = [...orgsWithoutRoot, ...missingOrgs];

  // populate categories owner
  const categoriesWithOwner = payload.categories.map(category => {
    const relatedOrg = findOrgById(category.organizationId);
    return relatedOrg ? {...category, owner: relatedOrg.name} : category;
  });

  // the "uncategorized applications" category should always be first, so we need to sort the rest of them here
  const sortedCategories = sortBy(prop('nameLowercaseNoWhitespace'), categoriesWithOwner);

  // add "uncategorized applications" Category
  const categories = [uncategorizedCategory, ...sortedCategories];

  // normalize stages
  const stages = payload.stages.map(({stageTypeId, stageName}) => ({id: stageTypeId, name: stageName}));

  return {...state, organizations, applications, categories, stages};
}

const applyFilter = ({filter}) => state => {
  if (filter == null) {
    return state;
  }

  // organizations: select only visible orgs
  const organizationFilters = filter.organizationFilters || [];
  const findAvailableOrgById = id => find(propEq('id', id), state.organizations);
  const visibleOrgIds = organizationFilters.filter(findAvailableOrgById);
  const organizations = new Set(visibleOrgIds);

  // applications: include potentially missing selected apps belonging to selected orgs
  const belongsToSelectedOrg = app => organizations.has(app.organizationId);
  const appsFromSelectedOrgs = state.applications.filter(belongsToSelectedOrg).map(prop('id'));
  const applications = new Set([...filter.applicationFilters, ...appsFromSelectedOrgs]);

  // categories: avoid adding no-longer-existing category ids to selected.categories
  const tagFilters = filter.tagFilters || [];
  const existingCategoryIds = new Set(state.categories.map(prop('id')));
  const selectedCategoryIds = tagFilters.filter(categoryId => existingCategoryIds.has(categoryId));
  const categories = new Set(selectedCategoryIds);

  const stages = new Set(filter.stageTypeFilters);
  const policyTypes = new Set(filter.policyThreatCategoryFilters);
  const policyViolationStates = new Set(filter.policyViolationStates);
  const maxDaysOld = getAge(state, filter.maxDaysOld);
  const policyThreatLevels = [filter.minPolicyThreatLevel, filter.maxPolicyThreatLevel];

  const selected = Object.freeze({
    organizations,
    applications,
    categories,
    stages,
    policyTypes,
    policyViolationStates,
    maxDaysOld,
    policyThreatLevels
  });
  return setShowAgeFilter({...state, selected, appliedFilter: selected, filtersAreDirty: false});
};

function getAge(state, maxDaysOld) {
  const current = find(propEq('id', maxDaysOld), state.ages);
  return current ? current.id : defaultMaxDaysOld;
}

function setShowAgeFilter(state) {
  const showAgeFilter = state.isViolationsTab;
  return {...state, showAgeFilter};
}
