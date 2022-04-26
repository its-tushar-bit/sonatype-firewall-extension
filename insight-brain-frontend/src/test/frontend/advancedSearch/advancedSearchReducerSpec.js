/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reducer from 'MainRoot/advancedSearch/advancedSearchReducer';

const ADVANCED_SEARCH_SET_CURRENT_QUERY = 'ADVANCED_SEARCH_SET_CURRENT_QUERY';

describe('advancedSearchReducer', () => {
  let mockState;

  beforeEach(() => {
    mockState = {
      formState: {
        currentQuery: '',
        isToggleComponentResultsEnabled: false,
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

      expect(newState.formState.isToggleComponentResultsEnabled).toEqual(true);

      // Expect it to reset to original state if component-related term is removed from the query
      let nextState = reducer(mockState, {
        type: ADVANCED_SEARCH_SET_CURRENT_QUERY,
        payload: 'componentLabelId', // label-related terms not included in criteria to show radio buttons
      });

      expect(nextState.formState.isToggleComponentResultsEnabled).toEqual(false);
    });
  });
});
