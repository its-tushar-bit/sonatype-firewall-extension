/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {createReducerFromActionMap} from '../util/reduxUtil';
import {
  ADVANCED_SEARCH_LOAD_FAILED,
  ADVANCED_SEARCH_LOAD_FULFILLED,
  ADVANCED_SEARCH_LOAD_REQUESTED,
  ADVANCED_SEARCH_SET_CURRENT_QUERY,
  ADVANCED_SEARCH_QUERY_REQUESTED,
  ADVANCED_SEARCH_QUERY_FULFILLED,
  ADVANCED_SEARCH_QUERY_FAILED,
  ADVANCED_SEARCH_RESET_QUERY,
  ADVANCED_SEARCH_TOGGLE_HELP
} from './advancedSearchActions';
import { pathSet } from '../util/jsUtil';

const initialState = {
  viewState: {
    loading: true,
    error: null,
    waitingSearchResponse: false,
    showHelp: false
  },
  configurationState: {
    isEnabled: true
  },
  formState: {
    currentQuery: '',
    searchedQuery: '',
    searchResult: {
      page: 0,
      groupingByDTOS: [],
      totalNumberOfHits: 0,
      isExactTotalNumberOfHits: false
    },
    queryError: null
  }
};

function loadRequested() {
  return {
    ...initialState,
    viewState: {
      ...initialState.viewState,
      loading: true,
      error: null
    }
  };
}

function loadFulfilled(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      loading: false,
      error: null
    },
    configurationState: payload
  };
}

function loadFailed(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      loading: false,
      error: payload
    }
  };
}

function queryRequested(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      waitingSearchResponse: true
    }
  };
}

function resetQuery(payload, state) {
  return {
    ...state,
    formState: {
      ...state.formState,
      currentQuery: state.formState.searchedQuery
    }
  };
}

function queryFulfilled(payload, state) {
  return {
    ...state,
    formState: {
      ...state.formState,
      searchResult: payload,
      queryError: null,
      searchedQuery: state.formState.currentQuery
    },
    viewState: {
      ...state.viewState,
      waitingSearchResponse: false
    }
  };
}

function queryFailed(payload, state) {
  return {
    ...state,
    formState: {
      ...initialState.formState,
      queryError: payload
    },
    viewState: {
      ...state.viewState,
      waitingSearchResponse: false
    }
  };
}

function toggleHelp(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      showHelp: !state.viewState.showHelp
    }
  };
}

const reducerActionMap = {
  [ADVANCED_SEARCH_LOAD_REQUESTED]: loadRequested,
  [ADVANCED_SEARCH_LOAD_FULFILLED]: loadFulfilled,
  [ADVANCED_SEARCH_LOAD_FAILED]: loadFailed,
  [ADVANCED_SEARCH_SET_CURRENT_QUERY]: pathSet(['formState', 'currentQuery']),
  [ADVANCED_SEARCH_QUERY_REQUESTED]: queryRequested,
  [ADVANCED_SEARCH_QUERY_FULFILLED]: queryFulfilled,
  [ADVANCED_SEARCH_QUERY_FAILED]: queryFailed,
  [ADVANCED_SEARCH_RESET_QUERY]: resetQuery,
  [ADVANCED_SEARCH_TOGGLE_HELP]: toggleHelp
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
