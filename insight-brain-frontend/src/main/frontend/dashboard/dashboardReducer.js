/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  LOAD_RESULTS_REQUESTED,
  LOAD_RESULTS_FULFILLED,
  LOAD_RESULTS_FAILED,
  SORT_RESULTS_REQUESTED,
  SORT_RESULTS_FULFILLED,
  RESET_ALL_TABS,
  DASHBOARD_SET_PAGE,
} from './results/dashboardResultsActions';
import { LOAD_FILTER_REQUESTED } from './filter/dashboardFilterActions';
import { UI_ROUTER_ON_FINISH } from '../reduxUiRouter/routerActions';
import { addWaiversScopeProp } from 'MainRoot/util/waiverUtils';

const initState = {
  currentTab: 'violations',
  violations: {
    results: null,
    hasNextPage: false,
    error: null,
    sortFields: ['-firstOccurrenceTime', '-threatLevel'],
    hasMultiplePages: false,
    page: null,
  },
  components: {
    results: null,
    hasNextPage: false,
    classyBrew: null,
    error: null,
    sortFields: ['-score'],
    hasMultiplePages: false,
    page: null,
  },
  applications: {
    results: null,
    hasNextPage: false,
    classyBrew: null,
    error: null,
    sortFields: ['-totalApplicationRisk.totalRisk'],
    hasMultiplePages: false,
    page: null,
  },
  waivers: {
    results: null,
    hasNextPage: false,
    error: null,
    sortFields: ['expiryTime'],
    hasMultiplePages: false,
    page: null,
  },
};

export default function (state = initState, { type, payload }) {
  switch (type) {
    case UI_ROUTER_ON_FINISH:
      return setCurrentTab(state, payload);

    case LOAD_FILTER_REQUESTED:
      return resetAllTabs(state);

    case LOAD_RESULTS_REQUESTED:
      return resetResults(state, payload);

    case LOAD_RESULTS_FULFILLED: {
      const { resultsType, results, hasNextPage, classyBrew } = payload;
      // map results if type is waivers
      const mapResults = resultsType === 'waivers' && results ? addWaiversScopeProp(results) : results;
      let page = state[resultsType].page ?? 0;
      let hasMultiplePages = page > 0 || hasNextPage;
      return updateResults(state, resultsType, {
        results: mapResults,
        hasNextPage,
        classyBrew,
        hasMultiplePages,
        page,
      });
    }

    case LOAD_RESULTS_FAILED: {
      const { resultsType, error } = payload;
      return updateResults(state, resultsType, { error });
    }

    case SORT_RESULTS_REQUESTED: {
      const { resultsType, sortFields } = payload;
      return updateResults(state, resultsType, { sortFields, page: 0 });
    }

    case SORT_RESULTS_FULFILLED: {
      const { resultsType, results } = payload;
      return updateResults(state, resultsType, { results });
    }

    case DASHBOARD_SET_PAGE: {
      const { resultsType, page } = payload;
      return updateResults(state, resultsType, { page });
    }

    case RESET_ALL_TABS:
      return resetAllTabs(state);

    default:
      return state;
  }
}

function resetResults(state, resultsType) {
  const results = resetTabState(state[resultsType]);
  return { ...state, [resultsType]: results };
}

function resetTabState(tabState, resetCounters) {
  const hasMultiplePages = resetCounters ? null : tabState.hasMultiplePages;
  return { ...tabState, results: null, hasMultiplePages: hasMultiplePages, error: null };
}

function resetAllTabs(state) {
  const violations = resetTabState(state.violations, true);
  const components = resetTabState(state.components, true);
  const applications = resetTabState(state.applications, true);
  const waivers = resetTabState(state.waivers, true);
  return { ...state, violations, components, applications, waivers };
}

function updateResults(state, resultsType, props) {
  const tabState = state[resultsType];
  const newTabState = { ...tabState, ...props };
  return { ...state, [resultsType]: newTabState };
}

function setCurrentTab(state, { toState }) {
  switch (toState.name) {
    case 'dashboard.overview.violations':
    case 'dashboard.violation': // violation details
      return { ...state, currentTab: 'violations' };

    case 'dashboard.overview.components':
      return { ...state, currentTab: 'components' };

    case 'dashboard.overview.applications':
      return { ...state, currentTab: 'applications' };

    case 'waiver.details':
    case 'dashboard.overview.waivers':
      return { ...state, currentTab: 'waivers' };

    default:
      return state;
  }
}
