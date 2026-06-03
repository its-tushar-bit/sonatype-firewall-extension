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
  ADVANCED_SEARCH_RESET_SEARCH_AFTERS,
  ADVANCED_SEARCH_ADD_SEARCH_ITEM,
  ADVANCED_SEARCH_SET_EASY_QUERY_VALUE,
  ADVANCED_SEARCH_SET_EASY_QUERY_FIELD,
  ADVANCED_SEARCH_REMOVE_SEARCH_ITEM,
} from './advancedSearchActions';
import { pathSet } from '../util/jsUtil';
import { addIndex, filter, map } from 'ramda';
import { buildSearchQuery } from './utils';

const initialState = {
  viewState: {
    loading: true,
    loadError: null,
    waitingSearchResponse: false,
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
    searchAfters: [],
  },
  easyQueryBuilder: {
    searchItems: [],
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

function addSearchItem(payload, state) {
  const newSearchItem = {
    operator: 'OR',
    field: '',
    value: '',
    isExactMatch: false,
  };
  const searchItems = [...state.easyQueryBuilder.searchItems, newSearchItem];
  const currentQuery = buildSearchQuery(searchItems);
  return {
    ...state,
    easyQueryBuilder: {
      ...state.easyQueryBuilder,
      searchItems,
    },
    formState: {
      ...state.formState,
      currentQuery,
    },
  };
}

function setEasyQueryField(payload, state) {
  const { index, value } = payload;
  const mapIndexed = addIndex(map);
  const searchItems = mapIndexed(
    (item, i) => (i === index ? { ...item, field: { ...value } } : item),
    state.easyQueryBuilder.searchItems
  );
  const currentQuery = buildSearchQuery(searchItems);
  return {
    ...state,
    easyQueryBuilder: {
      ...state.easyQueryBuilder,
      searchItems,
    },
    formState: {
      ...state.formState,
      currentQuery,
    },
  };
}

function setEasyQueryValue(payload, state) {
  const { index, value, key } = payload;
  const mapIndexed = addIndex(map);
  const searchItems = mapIndexed(
    (item, i) => (i === index ? { ...item, [key]: value } : item),
    state.easyQueryBuilder.searchItems
  );
  const currentQuery = buildSearchQuery(searchItems);
  return {
    ...state,
    easyQueryBuilder: {
      ...state.easyQueryBuilder,
      searchItems,
    },
    formState: {
      ...state.formState,
      currentQuery,
    },
  };
}

function removeSearchItem(payload, state) {
  const filterIndexed = addIndex(filter);
  const searchItems = filterIndexed((_, i) => i !== payload, state.easyQueryBuilder.searchItems);
  const currentQuery = buildSearchQuery(searchItems);
  return {
    ...state,
    easyQueryBuilder: {
      ...state.easyQueryBuilder,
      searchItems,
    },
    formState: {
      ...state.formState,
      currentQuery,
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

function resetSearchAfters(payload, state) {
  return {
    ...state,
    formState: {
      ...state.formState,
      searchAfters: [],
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
      searchAfters: Object.assign([], state.formState.searchAfters, {
        [payload.page + 1]: payload.searchAfter,
      }),
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
  [ADVANCED_SEARCH_RESET_SEARCH_AFTERS]: resetSearchAfters,
  [ADVANCED_SEARCH_ADD_SEARCH_ITEM]: addSearchItem,
  [ADVANCED_SEARCH_REMOVE_SEARCH_ITEM]: removeSearchItem,
  [ADVANCED_SEARCH_SET_EASY_QUERY_FIELD]: setEasyQueryField,
  [ADVANCED_SEARCH_SET_EASY_QUERY_VALUE]: setEasyQueryValue,
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
