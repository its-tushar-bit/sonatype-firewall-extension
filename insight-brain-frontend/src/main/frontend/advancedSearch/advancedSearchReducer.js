/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createReducerFromActionMap } from '../util/reduxUtil';
import {
  ADVANCED_SEARCH_LOAD_FAILED,
  ADVANCED_SEARCH_LOAD_FULFILLED,
  ADVANCED_SEARCH_LOAD_REQUESTED,
  ADVANCED_SEARCH_SET_CURRENT_QUERY,
  ADVANCED_SEARCH_SET_SHOW_ALL_COMPONENT_RESULTS,
  ADVANCED_SEARCH_QUERY_REQUESTED,
  ADVANCED_SEARCH_QUERY_FULFILLED,
  ADVANCED_SEARCH_QUERY_FAILED,
  ADVANCED_SEARCH_RESET_QUERY,
  ADVANCED_SEARCH_TOGGLE_HELP,
} from './advancedSearchActions';
import { pathSet } from '../util/jsUtil';

const initialState = {
  viewState: {
    loading: true,
    loadError: null,
    waitingSearchResponse: false,
    showHelp: false,
  },
  configurationState: {
    isEnabled: true,
  },
  formState: {
    currentQuery: '',
    searchedQuery: '',
    searchIncludedAllComponents: false,
    searchResult: {
      page: 0,
      groupingByDTOS: [],
      totalNumberOfHits: 0,
      isExactTotalNumberOfHits: false,
    },
    queryError: null,
    isShowingAllComponentResults: false,
    isToggleComponentResultsEnabled: false,
  },
};

const componentToggleCriteria = [
  'componentHash',
  'componentFormat',
  'componentName',
  'componentCoordinateGroupId',
  'componentCoordinateArtifactId',
  'componentCoordinateVersion',
  'componentCoordinateClassifier',
  'componentCoordinateExtension',
  'componentCoordinateName',
  'componentCoordinateQualifier',
  'componentCoordinatePackageId',
  'componentCoordinateArchitecture',
  'componentCoordinatePlatform',
];

/*
  Set current query in state.
  
  Determine whether radio buttons for filtering component-related search
  criteria should be displayed based on whether those criteria exist in the 
  query.
*/
function setCurrentQuery(payload, state) {
  return {
    ...state,
    formState: {
      ...state.formState,
      currentQuery: payload,
      isToggleComponentResultsEnabled: componentToggleCriteria.some((criterion) => payload.includes(criterion)),
    },
  };
}

function loadRequested() {
  return {
    ...initialState,
    viewState: {
      ...initialState.viewState,
      loading: true,
      loadError: null,
    },
  };
}

function loadFulfilled(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      loading: false,
      loadError: null,
    },
    configurationState: payload,
  };
}

function loadFailed(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      loading: false,
      loadError: payload,
    },
  };
}

function queryRequested(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      waitingSearchResponse: true,
    },
  };
}

function resetQuery(payload, state) {
  return {
    ...state,
    formState: {
      ...state.formState,
      currentQuery: state.formState.searchedQuery,
    },
  };
}

function queryFulfilled(payload, state) {
  return {
    ...state,
    formState: {
      ...state.formState,
      searchResult: payload,
      queryError: null,
      searchedQuery: state.formState.currentQuery,
      searchIncludedAllComponents:
        state.formState.isToggleComponentResultsEnabled && state.formState.isShowingAllComponentResults,
    },
    viewState: {
      ...state.viewState,
      waitingSearchResponse: false,
    },
  };
}

function queryFailed(payload, state) {
  return {
    ...state,
    formState: {
      ...state.formState,
      queryError: payload,
    },
    viewState: {
      ...state.viewState,
      waitingSearchResponse: false,
    },
  };
}

function toggleHelp(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      showHelp: !state.viewState.showHelp,
    },
  };
}

const reducerActionMap = {
  [ADVANCED_SEARCH_LOAD_REQUESTED]: loadRequested,
  [ADVANCED_SEARCH_LOAD_FULFILLED]: loadFulfilled,
  [ADVANCED_SEARCH_LOAD_FAILED]: loadFailed,
  [ADVANCED_SEARCH_SET_CURRENT_QUERY]: setCurrentQuery,
  [ADVANCED_SEARCH_SET_SHOW_ALL_COMPONENT_RESULTS]: pathSet(['formState', 'isShowingAllComponentResults']),
  [ADVANCED_SEARCH_QUERY_REQUESTED]: queryRequested,
  [ADVANCED_SEARCH_QUERY_FULFILLED]: queryFulfilled,
  [ADVANCED_SEARCH_QUERY_FAILED]: queryFailed,
  [ADVANCED_SEARCH_RESET_QUERY]: resetQuery,
  [ADVANCED_SEARCH_TOGGLE_HELP]: toggleHelp,
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
