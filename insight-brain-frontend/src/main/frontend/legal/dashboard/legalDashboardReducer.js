/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  LEGAL_DASHBOARD_LOAD_RESULTS_FAILED,
  LEGAL_DASHBOARD_LOAD_RESULTS_FULFILLED,
  LEGAL_DASHBOARD_LOAD_RESULTS_REQUESTED,
  LEGAL_DASHBOARD_FETCH_BACKEND_PAGE,
  LEGAL_DASHBOARD_CHANGE_SORT_FIELD,
  LEGAL_DASHBOARD_SET_PAGE,
  LEGAL_DASHBOARD_COMPONENT_SEARCH,
  LEGAL_DASHBOARD_COMPONENT_SET_SEARCH_INPUT_VALUE,
} from './legalDashboardActions';
import {
  LEGAL_DASHBOARD_APPLY_FILTER_REQUESTED,
  LEGAL_DASHBOARD_LOAD_FILTER_REQUESTED,
} from './filter/legalDashboardFilterActions';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';

const validator = (value) => {
  const trimmedValue = value.trim();
  if (trimmedValue.length < 3 && trimmedValue.length > 0) {
    return 'You must input at least three characters to search';
  } else {
    return null;
  }
};

const { initialState, userInput } = nxTextInputStateHelpers;

const initState = {
  applications: {
    results: [],
    totalResultsCount: 0,
    page: 0,
    backendPage: 1,
    error: null,
    loading: false,
    sortField: null,
  },
  components: {
    results: [],
    page: 0,
    backendPage: 1,
    error: null,
    sortField: null,
    componentNameToSearch: '',
    componentSearchInput: initialState(''),
  },
  loading: false,
  loadError: null,
};

export default function (state = initState, { type, payload }) {
  switch (type) {
    case LEGAL_DASHBOARD_LOAD_FILTER_REQUESTED:
      return resetAllTabs(state);

    case LEGAL_DASHBOARD_APPLY_FILTER_REQUESTED:
      return resetAllTabs(state, true);

    case LEGAL_DASHBOARD_LOAD_RESULTS_REQUESTED:
      return resetResults(state, payload);

    case LEGAL_DASHBOARD_LOAD_RESULTS_FULFILLED: {
      const { resultsType, results } = payload;
      return updateResults(state, resultsType, { ...results, loading: false });
    }

    case LEGAL_DASHBOARD_LOAD_RESULTS_FAILED: {
      const { resultsType, error } = payload;
      return updateResults(state, resultsType, { error, loading: false });
    }

    case LEGAL_DASHBOARD_SET_PAGE: {
      const { resultsType, page } = payload;
      return updateResults(state, resultsType, { page: page });
    }

    case LEGAL_DASHBOARD_FETCH_BACKEND_PAGE: {
      const { resultsType, page } = payload;
      return updateResults(state, resultsType, { backendPage: page });
    }

    case LEGAL_DASHBOARD_CHANGE_SORT_FIELD: {
      const { resultsType, sortField } = payload;
      return updateResults(state, resultsType, { sortField: sortField });
    }

    case LEGAL_DASHBOARD_COMPONENT_SEARCH: {
      const preState = updateResults(state, 'components', {
        componentNameToSearch: state.components.componentSearchInput.trimmedValue,
      });
      return resetResults(preState, 'components', true);
    }

    case LEGAL_DASHBOARD_COMPONENT_SET_SEARCH_INPUT_VALUE: {
      return updateResults(state, 'components', {
        componentSearchInput: userInput(validator, payload),
      });
    }

    default:
      return state;
  }
}

function resetResults(state, resultsType, resetPagination = false) {
  const { sortField } = state[resultsType];
  const results = resetTabState(state[resultsType], resetPagination);
  results.loading = true;
  results.sortField = sortField;
  return { ...state, [resultsType]: results };
}

function resetTabState(tabState, resetPagination) {
  return {
    ...tabState,
    results: [],
    totalResultsCount: 0,
    error: null,
    loading: false,
    page: resetPagination ? 0 : tabState.page,
    backendPage: resetPagination ? 1 : tabState.backendPage,
  };
}

function updateResults(state, resultsType, props) {
  const tabState = state[resultsType];
  const newTabState = { ...tabState, ...props };
  return { ...state, [resultsType]: newTabState };
}

function resetAllTabs(state, resetPagination) {
  const components = resetTabState(state.components, resetPagination);
  const applications = resetTabState(state.applications, resetPagination);
  return { ...state, components, applications };
}
