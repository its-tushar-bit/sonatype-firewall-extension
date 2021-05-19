/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { any, compose, filter, flatten, map, prop } from 'ramda';
import { pathSet } from '../../util/jsUtil';
import { getLicenseThreatGroupsFromLicense } from '../legalUtility';
import {
  LEGAL_APPLICATION_DETAILS_SET_COMPONENT_NAME_FILTER,
  LEGAL_APPLICATION_DETAILS_SET_LICENSE_NAME_FILTER,
  LEGAL_APPLICATION_DETAILS_SET_SORT,
  LEGAL_APPLICATION_DETAILS_TOGGLE_FILTER,
  LEGAL_APPLICATION_DETAILS_TOGGLE_FILTER_SIDEBAR,
} from './filter/legalApplicationDetailsFilterActions';
import {
  LEGAL_APPLICATION_DETAILS_LOAD_APP_FAILED,
  LEGAL_APPLICATION_DETAILS_LOAD_APP_FULFILLED,
  LEGAL_APPLICATION_DETAILS_LOAD_APP_REQUESTED,
  LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_FAILED,
  LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_FULFILLED,
  LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_REQUESTED,
  LEGAL_APPLICATION_DETAILS_LOAD_STAGE_FAILED,
  LEGAL_APPLICATION_DETAILS_LOAD_STAGE_FULFILLED,
  LEGAL_APPLICATION_DETAILS_LOAD_STAGE_REQUESTED,
} from './legalApplicationDetailsActions';
import { statusRanking } from '../dashboard/legalDashboardConstants';

const initState = {
  application: {
    name: null,
    error: null,
    loading: false,
  },
  stageType: {
    name: null,
    error: null,
    loading: false,
  },
  components: {
    results: Object.freeze([]),
    filteredResults: Object.freeze([]),
    error: null,
    loading: false,
  },
  componentFilter: '',
  licenseFilter: '',
  filterSidebarOpen: false,
  reviewStatusFilter: Object.freeze([]),
  licenseThreatGroupFilter: Object.freeze([]),
  sort: {},
  page: 1,
  selected: Object.freeze({
    progressOptions: new Set(),
    licenseThreatGroups: new Set(),
  }),
};

export default function (state = initState, { type, payload }) {
  switch (type) {
    case LEGAL_APPLICATION_DETAILS_LOAD_APP_REQUESTED: {
      const application = { ...initState.application, loading: true };
      return { ...initState, application: application };
    }
    case LEGAL_APPLICATION_DETAILS_LOAD_APP_FULFILLED: {
      const application = { ...initState.application, name: payload.name };
      return { ...initState, application: application };
    }
    case LEGAL_APPLICATION_DETAILS_LOAD_APP_FAILED: {
      const application = { ...initState.application, error: payload };
      return { ...initState, application: application };
    }
    case LEGAL_APPLICATION_DETAILS_LOAD_STAGE_REQUESTED: {
      const stageType = { ...state.stageType, loading: true };
      return { ...state, stageType: stageType };
    }
    case LEGAL_APPLICATION_DETAILS_LOAD_STAGE_FULFILLED: {
      const stageType = { ...state.stageType, loading: false, name: payload };
      return { ...state, stageType: stageType };
    }
    case LEGAL_APPLICATION_DETAILS_LOAD_STAGE_FAILED: {
      const stageType = { ...state.stageType, loading: false, error: payload };
      return { ...state, stageType: stageType };
    }
    case LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_REQUESTED: {
      const components = { ...state.components, loading: true };
      return { ...state, components: components };
    }
    case LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_FULFILLED: {
      const components = { ...state.components, loading: false, results: payload, filteredResults: payload };
      return { ...state, components: components };
    }
    case LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_FAILED: {
      const components = {
        ...state.components,
        loading: false,
        error: payload,
      };
      return { ...state, components: components };
    }
    case LEGAL_APPLICATION_DETAILS_TOGGLE_FILTER:
      return applyFilters(toggleFilter(payload)(state));
    case LEGAL_APPLICATION_DETAILS_SET_COMPONENT_NAME_FILTER:
      return applyFilters(setComponentFilter(state, payload));
    case LEGAL_APPLICATION_DETAILS_SET_LICENSE_NAME_FILTER:
      return applyFilters(setLicenseFilter(state, payload));
    case LEGAL_APPLICATION_DETAILS_SET_SORT:
      return applyFilters(setSortOrder(state, payload));
    case LEGAL_APPLICATION_DETAILS_TOGGLE_FILTER_SIDEBAR:
      return { ...state, filterSidebarOpen: payload };
    default:
      return state;
  }
}

const toggleFilter = ({ filterName, selectedIds }) => (state) => {
  return pathSet(['selected', filterName], selectedIds, state);
};

const licensesAsString = (licenses) =>
  licenses
    .map((l) => l.licenseName)
    .join('\n')
    .toLowerCase();

const applyFilters = (state) => {
  const { componentFilter, licenseFilter, selected, sort } = state;
  const { progressOptions, licenseThreatGroups } = selected;
  let filteredResults = state.components.results.filter(
    (component) => progressOptions.size === 0 || progressOptions.has(component.reviewStatus)
  );

  const filterFunction = (licenseThreatGroupNames) =>
    any((name) => licenseThreatGroups.size === 0 || licenseThreatGroups.has(name), licenseThreatGroupNames);
  filteredResults = filter(
    compose(
      filterFunction,
      map(prop('licenseThreatGroupName')),
      flatten,
      map(getLicenseThreatGroupsFromLicense),
      prop('licenses')
    )
  )(filteredResults);

  const filterByComponentName = (component) =>
    !componentFilter || component.displayName.toLowerCase().includes(componentFilter.toLowerCase().trim());

  const filterByLicenseName = (component) =>
    !licenseFilter || licensesAsString(component.licenses).includes(licenseFilter.toLowerCase().trim());

  const reviewPercentage = (component) =>
    component.reviewTotalCount > 0
      ? Math.min(100, (component.reviewCompletedCount * 100) / component.reviewTotalCount)
      : 0;

  const sortFn = (a, b) => {
    let comparison = 0;

    if (!sort.sortOrder) return 0;

    switch (sort.column) {
      case 'component':
        comparison = a.displayName.toLowerCase().localeCompare(b.displayName.toLowerCase());
        break;
      case 'licenses':
        comparison = licensesAsString(a.licenses).localeCompare(licensesAsString(b.licenses));
        break;
      case 'progress':
        comparison = reviewPercentage(a) - reviewPercentage(b);
        break;
      case 'status':
        comparison = statusRanking[a.reviewStatus] - statusRanking[b.reviewStatus];
        break;
    }

    return sort.sortOrder === 'asc' ? comparison : comparison * -1;
  };

  filteredResults = filteredResults.filter(filterByComponentName).filter(filterByLicenseName).sort(sortFn);

  return pathSet(['components', 'filteredResults'], filteredResults, state);
};

const setComponentFilter = (state, payload) => ({ ...state, componentFilter: payload.filter });

const setLicenseFilter = (state, payload) => ({ ...state, licenseFilter: payload.filter });

const setSortOrder = (state, payload) => ({ ...state, sort: payload });
