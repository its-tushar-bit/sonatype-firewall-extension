/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { searchFormSubmit } from 'MainRoot/advancedSearch/advancedSearchActions';

const ADVANCED_SEARCH_QUERY_REQUESTED = 'ADVANCED_SEARCH_QUERY_REQUESTED';
const ADVANCED_SEARCH_QUERY_FULFILLED = 'ADVANCED_SEARCH_QUERY_FULFILLED';
const ADVANCED_SEARCH_QUERY_FAILED = 'ADVANCED_SEARCH_QUERY_FAILED';

// TODO: We do not test at the action level anymore, we should make sure there is enough test coverage utilizing
// React Testing Library in AdvancedSearch.jestspec.jsx to handle the logic under test here. If there is then
// we should delete this test
describe('advancedSearchActions', () => {
  const defaultSearchUrl = '/api/v2/search/advanced?query=testQuery&page=0&allComponents=false';
  const defaultSearchUrlWithAllComponents = '/api/v2/search/advanced?query=testQuery&page=0&allComponents=true';
  let mockAxiosCalls, store, mockState;

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

    it('sets mode to sbomManager when selectIsSbomManager is true', (done) => {
      const mockResponse = { data: { searchQuery: 'testQuery' } };

      mockState = {
        router: {
          currentState: {
            name: 'sbomManager.advancedSearch',
          },
        },
        advancedSearch: {
          formState: {
            currentQuery: 'testQuery',
            isShowingAllComponentResults: false,
            isToggleComponentResultsEnabled: false,
            searchIncludedAllComponents: false,
          },
        },
      };

      store = SpecUtil.mockReduxStore(mockState);

      const defaultSearchUrlWithSbomManagerMode =
        '/api/v2/search/advanced?query=testQuery&page=0&allComponents=false&mode=sbomManager';

      mockAxiosCalls({
        get: {
          [defaultSearchUrlWithSbomManagerMode]: Promise.resolve(mockResponse),
        },
      });

      store.dispatch(searchFormSubmit()).then(() => {
        expect(axios.get).toHaveBeenCalledWith(defaultSearchUrlWithSbomManagerMode);
        done();
      });
    });
  });
});
