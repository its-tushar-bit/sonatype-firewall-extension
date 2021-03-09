/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  LEGAL_DASHBOARD_LOAD_RESULTS_FAILED,
  LEGAL_DASHBOARD_LOAD_RESULTS_FULFILLED,
  LEGAL_DASHBOARD_LOAD_RESULTS_REQUESTED,
  LEGAL_DASHBOARD_FETCH_BACKEND_PAGE
} from './legalDashboardActions';
import {
  LEGAL_DASHBOARD_APPLY_FILTER_REQUESTED,
  LEGAL_DASHBOARD_LOAD_FILTER_REQUESTED
} from './filter/legalDashboardFilterActions';

const initState = {
  applications: {
    results: [],
    totalResultsCount: 0,
    backendPage: 1,
    error: null,
    loading: false,
    sortFields: []
  },
  components: {
    results: [],
    error: null,
    sortFields: []
  },
  loading: false,
  loadError: null
};

export default function(state = initState, {type, payload}) {
  switch (type) {
    case LEGAL_DASHBOARD_LOAD_FILTER_REQUESTED:
    case LEGAL_DASHBOARD_APPLY_FILTER_REQUESTED:
      return resetAllTabs(state);

    case LEGAL_DASHBOARD_LOAD_RESULTS_REQUESTED:
      return resetResults(state, payload);

    case LEGAL_DASHBOARD_LOAD_RESULTS_FULFILLED: {
      const {resultsType, results} = payload;
      return updateResults(state, resultsType, { ...results, loading: false });
    }

    case LEGAL_DASHBOARD_LOAD_RESULTS_FAILED: {
      const {resultsType, error} = payload;
      return updateResults(state, resultsType, { error, loading: false });
    }

    case LEGAL_DASHBOARD_FETCH_BACKEND_PAGE: {
      const {resultsType, page} = payload;
      return updateResults(state, resultsType, { backendPage: page });
    }

    default:
      return state;
  }
}

function resetResults(state, resultsType) {
  const { backendPage } = state[resultsType];
  const results = resetTabState(state[resultsType]);
  results.loading = true;
  results.backendPage = backendPage;
  return {...state, [resultsType]: results};
}

function resetTabState(tabState) {
  return {
    ...tabState,
    results: [],
    totalResultsCount: 0,
    backendPage: 1,
    error: null,
    loading: false,
    sortFields: []
  };
}

function updateResults(state, resultsType, props) {
  const tabState = state[resultsType];
  const newTabState = {...tabState, ...props};
  return {...state, [resultsType]: newTabState};
}

function resetAllTabs(state) {
  const components = resetTabState(state.components);
  const applications = resetTabState(state.applications);
  return {...state, components, applications};
}
