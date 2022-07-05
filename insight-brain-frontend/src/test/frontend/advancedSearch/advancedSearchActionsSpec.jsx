/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { getAdvancedSearchUrl } from 'MainRoot/util/CLMLocation';
import { searchFormSubmit } from 'MainRoot/advancedSearch/advancedSearchActions';

const ADVANCED_SEARCH_QUERY_REQUESTED = 'ADVANCED_SEARCH_QUERY_REQUESTED';
const ADVANCED_SEARCH_QUERY_FULFILLED = 'ADVANCED_SEARCH_QUERY_FULFILLED';
const ADVANCED_SEARCH_QUERY_FAILED = 'ADVANCED_SEARCH_QUERY_FAILED';

describe('advancedSearchActions', () => {
  let mockAxiosCalls, store, mockState, defaultSearchUrl;

  beforeEach(() => {
    mockState = {
      advancedSearch: {
        formState: {
          currentQuery: 'testQuery',
          isShowingAllComponentResults: false,
          isToggleComponentResultsEnabled: false,
          searchIncludedAllComponents: false,
        },
      },
    };
    mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
    store = SpecUtil.mockReduxStore(mockState);
    defaultSearchUrl = getAdvancedSearchUrl(mockState.advancedSearch.formState.currentQuery, 0, false);
  });

  describe('submit search', () => {
    it('dispatches ADVANCED_SEARCH_QUERY_REQUESTED', () => {
      store.dispatch(searchFormSubmit());

      expect(store.getActions()[0].type).toBe(ADVANCED_SEARCH_QUERY_REQUESTED);
    });

    it('sends a GET request to the appropriate url and dispatches ADVANCED_SEARCH_QUERY_FULFILLED on successful response', (done) => {
      const mockResponse = { data: { searchQuery: 'testQuery' } };

      mockAxiosCalls({
        get: {
          [defaultSearchUrl]: Promise.resolve(mockResponse),
        },
      });

      store.dispatch(searchFormSubmit()).then(() => {
        expect(axios.get).toHaveBeenCalledWith(defaultSearchUrl);
        const actions = store.getActions();
        expect(actions).toHaveActionType(ADVANCED_SEARCH_QUERY_FULFILLED);
        done();
      });
    });

    it('dispatches ADVANCED_SEARCH_QUERY_FAILED after a failed response', (done) => {
      mockAxiosCalls({
        get: {
          [defaultSearchUrl]: () => Promise.reject('error'),
        },
      });

      store.dispatch(searchFormSubmit()).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(store.getActions()).toHaveAction({
          type: ADVANCED_SEARCH_QUERY_FAILED,
          payload: 'error',
        });
        done();
      });
    });

    it('sets showAllComponents correctly when false', (done) => {
      const mockResponse = { data: { searchQuery: 'testQuery' } };

      mockState = {
        advancedSearch: {
          formState: {
            currentQuery: 'testQuery',
            isShowingAllComponentResults: true,
            isToggleComponentResultsEnabled: false,
            searchIncludedAllComponents: false,
          },
        },
      };

      store = SpecUtil.mockReduxStore(mockState);

      const defaultSearchUrlWithAllComponents = getAdvancedSearchUrl(
        mockState.advancedSearch.formState.currentQuery,
        0,
        true
      );

      mockAxiosCalls({
        get: {
          [defaultSearchUrl]: Promise.resolve(mockResponse),
          [defaultSearchUrlWithAllComponents]: Promise.resolve(mockResponse),
        },
      });

      store.dispatch(searchFormSubmit()).then(() => {
        expect(axios.get).toHaveBeenCalledWith(defaultSearchUrl);
        done();
      });
    });

    it('sets showAllComponents correctly when true', (done) => {
      const mockResponse = { data: { searchQuery: 'testQuery' } };

      mockState = {
        advancedSearch: {
          formState: {
            currentQuery: 'testQuery',
            isShowingAllComponentResults: true,
            isToggleComponentResultsEnabled: true,
            searchIncludedAllComponents: false,
          },
        },
      };

      store = SpecUtil.mockReduxStore(mockState);

      const defaultSearchUrlWithAllComponents = getAdvancedSearchUrl(
        mockState.advancedSearch.formState.currentQuery,
        0,
        true
      );

      mockAxiosCalls({
        get: {
          [defaultSearchUrl]: Promise.resolve(mockResponse),
          [defaultSearchUrlWithAllComponents]: Promise.resolve(mockResponse),
        },
      });

      store.dispatch(searchFormSubmit()).then(() => {
        expect(axios.get).toHaveBeenCalledWith(defaultSearchUrlWithAllComponents);
        done();
      });
    });
  });
});
