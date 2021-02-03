/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { compose, curry, equals, find, indexBy, map, merge, pick, prop, propEq, sortBy, uniqBy } from 'ramda';
import { propSet, pathSet, lookup } from '../../../util/jsUtil';
import defaultFilter from '../../../dashboard/filter/defaultFilter';
import { uncategorizedCategory } from '../../../dashboard/filter/staticFilterEntries';
import {
  APPLY_LEGAL_FILTER_FAILED,
  APPLY_LEGAL_FILTER_FULFILLED,
  APPLY_LEGAL_FILTER_REQUESTED,
  APPLY_SAVED_FILTER_FAILED,
  FETCH_LEGAL_AVAILABLE_FILTER_OPTIONS_FULFILLED,
  FETCH_LEGAL_CURRENT_FILTER_FULFILLED,
  LOAD_LEGAL_FILTER_FAILED,
  LOAD_LEGAL_FILTER_REQUESTED,
  REVERT_LEGAL_FILTER,
  TOGGLE_LEGAL_APPS_AND_ORGS,
  TOGGLE_LEGAL_FILTER
} from './legalDashboardFilterActions';

const initState = Object.freeze({
  loading: true,
  loadError: null,
  applyFilterError: null,
  loadErrorFilterName: null,
  filtersAreDirty: false,
  needsAcknowledgement: false,
  isViolationsTab: false,
  showAgeFilter: false,
  showSaveFilterModal: false,

  // available filter items
  organizations: [],
  applications: [],
  categories: [],
  stages: [],

  // selected filter items
  appliedFilter: defaultFilter,
  selected: defaultFilter
});

const resetProps = curry((propNames, state) => merge(state, pick(propNames, initState)));

export default function dashboardFilterReducer(state = initState, {type, payload}) {
  switch (type) {
    case LOAD_LEGAL_FILTER_REQUESTED:
      return compose(
          propSet('loading', true),
          resetProps(['loadError'])
      )(state);

    case LOAD_LEGAL_FILTER_FAILED:
      return {...state, loadError: payload, loading: false};

    case FETCH_LEGAL_AVAILABLE_FILTER_OPTIONS_FULFILLED:
      return setAvailable(state, payload);

    case FETCH_LEGAL_CURRENT_FILTER_FULFILLED:
      return compose(
          applyFilter(payload),
          propSet('needsAcknowledgement', payload.needsAcknowledgement),
          propSet('loading', false)
      )(state);

    case APPLY_LEGAL_FILTER_REQUESTED:
      return resetProps(['applyFilterError', 'loadErrorFilterName'], state);

    case APPLY_LEGAL_FILTER_FULFILLED: {
      return compose(
          applyFilter(payload),
          propSet('needsAcknowledgement', false)
      )(state);
    }

    case APPLY_LEGAL_FILTER_FAILED:
      return {...state, applyFilterError: payload};

    case APPLY_SAVED_FILTER_FAILED:
      return {...state, loadErrorFilterName: payload};

    case TOGGLE_LEGAL_FILTER:
      return compose(
          setFiltersAreDirty,
          toggleFilter(payload)
      )(state);

    case TOGGLE_LEGAL_APPS_AND_ORGS:
      return compose(
          setFiltersAreDirty,
          toggleAppsAndOrgs(payload)
      )(state);

    case REVERT_LEGAL_FILTER:
      return compose(
          revertFilter,
          resetProps(['filtersAreDirty', 'loadErrorFilterName'])
      )(state);

    default:
      return state;
  }
}

function revertFilter(state) {
  return {...state, selected: state.appliedFilter};
}

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
  const missingOrgs = uniqBy(prop('id'),
      map(app => ({id: app.organizationId, name: app.organizationName}), appsWithNoOrg));

  // filter out ROOT ORG
  const orgsWithoutRoot = payload.organizations.filter(organization => organization.id !== 'ROOT_ORGANIZATION_ID');
  const organizations = [...orgsWithoutRoot, ...missingOrgs];

  // populate categories owner
  const categoriesWithOwner = payload.categories.map(category => {
    const relatedOrg = findOrgById(category.organizationId);
    // we will want to sort app categories by nameLowercase
    category.nameLowercase = category.name.toLocaleLowerCase('en-US');
    return relatedOrg ? {...category, owner: relatedOrg.name} : category;
  });

  // the "uncategorized applications" category should always be first, so we need to sort the rest of them here
  const sortedCategories = sortBy(prop('nameLowercase'), categoriesWithOwner);

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

  const selected = Object.freeze({
    organizations,
    applications,
    categories,
    stages
  });

  return {...state, selected, appliedFilter: selected, filtersAreDirty: false};
};
