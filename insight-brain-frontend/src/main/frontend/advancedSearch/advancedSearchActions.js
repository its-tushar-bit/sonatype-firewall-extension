/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import { noPayloadActionCreator, payloadParamActionCreator } from '../util/reduxUtil';
import { getAdvancedSearchConfigUrl, getAdvancedSearchUrl } from '../util/CLMLocation';
import { selectIsSbomManager } from 'MainRoot/reduxUiRouter/routerSelectors';

export const ADVANCED_SEARCH_LOAD_REQUESTED = 'ADVANCED_SEARCH_LOAD_REQUESTED';
export const ADVANCED_SEARCH_LOAD_FULFILLED = 'ADVANCED_SEARCH_LOAD_FULFILLED';
export const ADVANCED_SEARCH_LOAD_FAILED = 'ADVANCED_SEARCH_LOAD_FAILED';

const loadRequested = noPayloadActionCreator(ADVANCED_SEARCH_LOAD_REQUESTED);
const loadFulfilled = payloadParamActionCreator(ADVANCED_SEARCH_LOAD_FULFILLED);
const loadFailed = payloadParamActionCreator(ADVANCED_SEARCH_LOAD_FAILED);

export function load() {
  return function (dispatch, getState) {
    const advancedSearchState = getState().advancedSearch,
      { formState, viewState } = advancedSearchState;

    // When the user navigates to any other page and comes to Advanced Search, we want to retain the state
    // For example, you search for CVE-2016-* and on page 3 click on a link and you use browsers BACK button
    // we want to let users continue going through search results.
    // But there is no need to retain the page if there was a query error, better present a fresh search page.
    if (!formState.queryError && !viewState.loadError && !viewState.loading) {
      return;
    }

    dispatch(loadRequested());
    axios
      .get(getAdvancedSearchConfigUrl())
      .then(({ data }) => {
        dispatch(loadFulfilled(data));
      })
      .catch((error) => {
        dispatch(loadFailed(error));
      });
  };
}

export const ADVANCED_SEARCH_SET_CURRENT_QUERY = 'ADVANCED_SEARCH_SET_CURRENT_QUERY';
export const setCurrentQuery = payloadParamActionCreator(ADVANCED_SEARCH_SET_CURRENT_QUERY);

export const ADVANCED_SEARCH_SET_SHOW_ALL_COMPONENT_RESULTS = 'ADVANCED_SEARCH_SET_SHOW_ALL_COMPONENT_RESULTS';
export const setShowAllComponentResults = payloadParamActionCreator(ADVANCED_SEARCH_SET_SHOW_ALL_COMPONENT_RESULTS);

export const ADVANCED_SEARCH_QUERY_REQUESTED = 'ADVANCED_SEARCH_QUERY_REQUESTED';
export const ADVANCED_SEARCH_QUERY_FULFILLED = 'ADVANCED_SEARCH_QUERY_FULFILLED';
export const ADVANCED_SEARCH_QUERY_FAILED = 'ADVANCED_SEARCH_QUERY_FAILED';
export const ADVANCED_SEARCH_RESET_QUERY = 'ADVANCED_SEARCH_RESET_QUERY';

const queryRequested = noPayloadActionCreator(ADVANCED_SEARCH_QUERY_REQUESTED);
const queryFulfilled = payloadParamActionCreator(ADVANCED_SEARCH_QUERY_FULFILLED);
const queryFailed = payloadParamActionCreator(ADVANCED_SEARCH_QUERY_FAILED);
const resetSearchQuery = noPayloadActionCreator(ADVANCED_SEARCH_RESET_QUERY);

export function searchFormSubmit(pageIncrement) {
  return function (dispatch, getState) {
    if (pageIncrement) {
      dispatch(resetSearchQuery());
    }

    const state = getState();
    const formState = state.advancedSearch.formState;
    // If next or previous is not requested, request page 0. Requesting page 0 means, firing initial search.
    const page = pageIncrement ? formState.searchResult.page + pageIncrement : 0;
    const showAllComponents = formState.isToggleComponentResultsEnabled && formState.isShowingAllComponentResults;
    const isSbomManager = selectIsSbomManager(state);

    dispatch(queryRequested());

    return axios
      .get(getAdvancedSearchUrl(formState.currentQuery, page, showAllComponents, isSbomManager))
      .then(({ data }) => {
        dispatch(queryFulfilled(data));
      })
      .catch((error) => {
        dispatch(queryFailed(error));
      });
  };
}

export const ADVANCED_SEARCH_TOGGLE_HELP = 'ADVANCED_SEARCH_TOGGLE_HELP';

export function toggleHelp() {
  return function (dispatch) {
    dispatch(noPayloadActionCreator(ADVANCED_SEARCH_TOGGLE_HELP)());
  };
}
