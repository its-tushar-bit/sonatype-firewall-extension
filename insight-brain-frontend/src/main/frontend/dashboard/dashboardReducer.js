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
} from './results/dashboardResultsActions';
import { LOAD_FILTER_REQUESTED } from './filter/dashboardFilterActions';
import { UI_ROUTER_ON_FINISH } from '../reduxUiRouter/routerActions';
import { addWaiversScopeProp } from 'MainRoot/util/waiverUtils';

const initState = {
  currentTab: 'violations',
  violations: {
    results: null,
    numResults: null,
    error: null,
    sortFields: ['-firstOccurrenceTime', '-threatLevel'],
  },
  components: {
    results: null,
    numResults: null,
    classyBrew: null,
    error: null,
    sortFields: ['-score'],
  },
  applications: {
    results: null,
    numResults: null,
    classyBrew: null,
    error: null,
    sortFields: ['-totalApplicationRisk.totalRisk'],
  },
  waivers: {
    results: null,
    numResults: null,
    error: null,
    sortFields: ['expiryTime'],
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
      const { resultsType, results, numResults, classyBrew } = payload;
      // map results if type is waivers
      const mapResults = resultsType === 'waivers' && results ? addWaiversScopeProp(results) : results;
      return updateResults(state, resultsType, {
        results: mapResults,
        numResults,
        classyBrew,
      });
    }

    case LOAD_RESULTS_FAILED: {
      const { resultsType, error } = payload;
      return updateResults(state, resultsType, { error });
    }

    case SORT_RESULTS_REQUESTED: {
      const { resultsType, sortFields } = payload;
      return updateResults(state, resultsType, { sortFields });
    }

    case SORT_RESULTS_FULFILLED: {
      const { resultsType, results } = payload;
      return updateResults(state, resultsType, { results });
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
  const numResults = resetCounters ? null : tabState.numResults;
  return { ...tabState, results: null, numResults: numResults, error: null };
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
