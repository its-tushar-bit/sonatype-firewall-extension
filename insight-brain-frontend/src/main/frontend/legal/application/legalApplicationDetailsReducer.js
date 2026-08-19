/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { any, compose, filter, flatten, map, prop, pipe, chain, uniq, intersection } from 'ramda';
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
  LEGAL_APPLICATION_DETAILS_APPLY_FILTERS,
  LEGAL_APPLICATION_DETAILS_LOAD_DATA_FAILED,
  LEGAL_APPLICATION_DETAILS_LOAD_DATA_FULFILLED,
  LEGAL_APPLICATION_DETAILS_LOAD_DATA_REQUESTED,
} from './legalApplicationDetailsActions';
import { statusRanking } from '../dashboard/legalDashboardConstants';

const initState = {
  error: null,
  loading: false,
  applicationName: null,
  stageName: null,
  components: {
    results: Object.freeze([]),
    filteredResults: Object.freeze([]),
    licenseThreatGroups: Object.freeze([]),
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
    case LEGAL_APPLICATION_DETAILS_LOAD_DATA_REQUESTED: {
      return { ...state, loading: true };
    }
    case LEGAL_APPLICATION_DETAILS_LOAD_DATA_FULFILLED: {
      const licenseThreatGroups = getLicenseThreatGroupsFromComponentsResults(payload.components);
      const components = {
        results: payload.components,
        filteredResults: payload.components,
        licenseThreatGroups,
      };

      const selectedLicenseThreatGroups = new Set(
        intersection([...state.selected.licenseThreatGroups], licenseThreatGroups)
      );

      return {
        ...state,
        applicationName: payload.application.name,
        stageName: payload.stageName,
        components,
        selected: {
          ...state.selected,
          licenseThreatGroups: selectedLicenseThreatGroups,
        },
        loading: false,
      };
    }
    case LEGAL_APPLICATION_DETAILS_LOAD_DATA_FAILED: {
      return {
        ...state,
        loading: false,
        error: payload,
      };
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
    case LEGAL_APPLICATION_DETAILS_APPLY_FILTERS:
      return applyFilters(state);

    default:
      return state;
  }
}

const getLicenseThreatGroupsFromComponentsResults = pipe(
  chain(prop('licenses')),
  chain(getLicenseThreatGroupsFromLicense),
  map(prop('licenseThreatGroupName')),
  uniq
);

const toggleFilter = ({ filterName, selectedIds }) => (state) => {
  return pathSet(['selected', filterName], selectedIds, state);
};

const licensesAsString = (licenses) =>
  licenses
    .map((l) => l.licenseName)
    .join('\n')
    .toLowerCase();

function filterResults(state) {
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

  return filteredResults.filter(filterByComponentName).filter(filterByLicenseName).sort(sortFn);
}

const applyFilters = (state) => {
  let filteredResults = filterResults(state);

  return pathSet(['components', 'filteredResults'], filteredResults, state);
};

const setComponentFilter = (state, payload) => ({ ...state, componentFilter: payload.filter });

const setLicenseFilter = (state, payload) => ({ ...state, licenseFilter: payload.filter });

const setSortOrder = (state, payload) => ({ ...state, sort: payload });
