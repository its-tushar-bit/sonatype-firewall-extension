/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reducer from 'MainRoot/advancedSearch/advancedSearchReducer';
import {
  ADVANCED_SEARCH_QUERY_FULFILLED,
  ADVANCED_SEARCH_SET_CURRENT_QUERY,
} from 'MainRoot/advancedSearch/advancedSearchActions';

describe('advancedSearchReducer', () => {
  let mockState;

  beforeEach(() => {
    mockState = {
      formState: {
        currentQuery: '',
        searchedQuery: '',
        searchIncludedAllComponents: false,
        isToggleComponentResultsEnabled: false,
        isShowingAllComponentResults: false,
      },
    };
  });

  describe('ADVANCED_SEARCH_SET_CURRENT_QUERY action', () => {
    it('sets currentQuery', () => {
      let newState = reducer(mockState, {
        type: ADVANCED_SEARCH_SET_CURRENT_QUERY,
        payload: 'test search term',
      });

      expect(newState.formState.currentQuery).toEqual('test search term');
    });

    it('sets isToggleComponentResultsEnabled based on whether query includes a component-related search term', () => {
      let newState = reducer(mockState, {
        type: ADVANCED_SEARCH_SET_CURRENT_QUERY,
        payload: 'componentName',
      });

      expect(newState.formState.isToggleComponentResultsEnabled).toBe(true);

      // Expect it to reset to original state if component-related term is removed from the query
      let nextState = reducer(mockState, {
        type: ADVANCED_SEARCH_SET_CURRENT_QUERY,
        payload: 'componentLabelId', // label-related terms not included in criteria to show radio buttons
      });

      expect(nextState.formState.isToggleComponentResultsEnabled).toBe(false);
    });
  });

  describe('ADVANCED_SEARCH_QUERY_FULFILLED', () => {
    it('sets the payload as the search result with component flags set true and modifies the searched values', () => {
      const previousState = {
        formState: {
          ...mockState.formState,
          currentQuery: 'besto component',
          isShowingAllComponentResults: true,
          isToggleComponentResultsEnabled: true,
        },
        viewState: {
          waitingSearchResponse: true,
        },
      };

      const newState = reducer(previousState, {
        type: ADVANCED_SEARCH_QUERY_FULFILLED,
        payload: { data: ['result1', 'result2'] },
      });

      expect(newState.formState.searchResult).toEqual({ data: ['result1', 'result2'] });
      expect(newState.formState.searchedQuery).toBe('besto component');
      expect(newState.formState.searchIncludedAllComponents).toBe(true);
      expect(newState.viewState.waitingSearchResponse).toBe(false);
    });

    it('sets searchIncludedAllComponents to false when isToggleComponentResults is false', () => {
      const previousState = {
        formState: {
          ...mockState.formState,
          isShowingAllComponentResults: true,
          isToggleComponentResultsEnabled: false,
        },
      };

      const newState = reducer(previousState, {
        type: ADVANCED_SEARCH_QUERY_FULFILLED,
        payload: { data: [] },
      });

      expect(newState.formState.searchIncludedAllComponents).toBe(false);
    });
  });
});
