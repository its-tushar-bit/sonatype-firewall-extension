/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  loadResults,
  loadDashboardUI,
  fetchBackendPage,
  changeSortField,
  legalDashboardSetPage,
  searchByComponentName,
} from '../../../../main/frontend/legal/dashboard/legalDashboardActions';
import axios from 'axios';
import {
  getApplicationsUrl,
  getApplicationTagsUrl,
  getLegalDashboardApplicationsUrl,
  getLegalDashboardComponentsUrl,
  getLegalDashboardFilters,
  getLegalDashboardSavedFilters,
  getOrganizationsUrl,
} from '../../../../main/frontend/util/CLMLocation';
import { DASHBOARD } from 'MainRoot/legal/advancedLegalConstants';

import 'TestRoot/SpecUtil';

describe('legalDashboardActions', function () {
  let legalDashboardApplicationsUrlSpy, legalDashboardComponentsUrlSpy, mockAxiosCalls;
  const tabs = [
    {
      resultsType: 'applications',
      serviceMethod: legalDashboardApplicationsUrlSpy,
    },
    {
      resultsType: 'components',
      componentNameToSearch: 'searchString',
      serviceMethod: legalDashboardComponentsUrlSpy,
    },
  ];

  beforeEach(() => {
    legalDashboardApplicationsUrlSpy = jest.fn().mockName('getLegalDashboardApplicationsUrl');
    legalDashboardComponentsUrlSpy = jest.fn().mockName('getLegalDashboardComponentsUrl');
    mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  });

  describe('loadResults', function () {
    const initialState = {
      legalDashboardFilter: {
        appliedFilter: {
          applications: [],
          organizations: [],
          stages: [],
          categories: [],
          progressOptions: [],
        },
      },
      legalDashboard: {
        applications: {},
        components: {
          componentNameToSearch: '',
          componentSearchInput: { isPristine: true, value: '', trimmedValue: '', validationErrors: null },
        },
      },
    };

    function testSearchByComponentNameAction(tab) {
      describe('searchByComponentName for components', function () {
        it('loads results', function (done) {
          const store = SpecUtil.mockReduxStore(initialState);
          mockAxiosCalls({
            post: {
              [getLegalDashboardApplicationsUrl()]: Promise.resolve({
                data: 'results',
              }),
              [getLegalDashboardComponentsUrl()]: Promise.resolve({
                data: 'results',
              }),
            },
          });

          store.dispatch(searchByComponentName()).then(() => {
            expect(store.getActions().length).toBe(2);
            expect(store.getActions()[1]).toEqual({
              type: 'LEGAL_DASHBOARD_LOAD_RESULTS_FULFILLED',
              payload: {
                resultsType: tab.resultsType,
                results: 'results',
              },
            });
            done();
          });

          expect(store.getActions().length).toBe(1);
          expect(store.getActions()[0]).toEqual({
            type: 'LEGAL_DASHBOARD_COMPONENT_SEARCH',
          });
        });

        it('handles failure to searchByComponentName results', function (done) {
          const store = SpecUtil.mockReduxStore(initialState);
          const errorTest = 'Error test';
          mockAxiosCalls({
            post: {
              [getLegalDashboardApplicationsUrl()]: () => Promise.reject(errorTest),
              [getLegalDashboardComponentsUrl()]: () => Promise.reject(errorTest),
            },
          });

          store.dispatch(searchByComponentName(tab.componentNameToSearch)).catch(() => {
            expect(store.getActions().length).toBe(2);
            expect(store.getActions()[1]).toEqual({
              type: 'LEGAL_DASHBOARD_LOAD_RESULTS_FAILED',
              payload: {
                resultsType: tab.resultsType,
                error: errorTest,
              },
            });
            done();
          });

          expect(store.getActions().length).toBe(1);
          expect(store.getActions()[0]).toEqual({
            type: 'LEGAL_DASHBOARD_COMPONENT_SEARCH',
          });
        });
      });
    }

    function testLoadResultsAction(tab) {
      describe('loadResults for ' + tab.resultsType, function () {
        it('loads results', function (done) {
          const store = SpecUtil.mockReduxStore(initialState);
          mockAxiosCalls({
            post: {
              [getLegalDashboardApplicationsUrl()]: Promise.resolve({
                data: 'results',
              }),
              [getLegalDashboardComponentsUrl()]: Promise.resolve({
                data: 'results',
              }),
            },
          });

          store.dispatch(loadResults(tab.resultsType)).then(() => {
            expect(store.getActions().length).toBe(2);
            expect(store.getActions()[1]).toEqual({
              type: 'LEGAL_DASHBOARD_LOAD_RESULTS_FULFILLED',
              payload: {
                resultsType: tab.resultsType,
                results: 'results',
              },
            });
            done();
          });

          expect(store.getActions().length).toBe(1);
          expect(store.getActions()[0]).toEqual({
            type: 'LEGAL_DASHBOARD_LOAD_RESULTS_REQUESTED',
            payload: tab.resultsType,
          });
        });

        it('handles failure to load results', function (done) {
          const store = SpecUtil.mockReduxStore(initialState);
          const errorTest = 'Error test';
          mockAxiosCalls({
            post: {
              [getLegalDashboardApplicationsUrl()]: () => Promise.reject(errorTest),
              [getLegalDashboardComponentsUrl()]: () => Promise.reject(errorTest),
            },
          });

          store.dispatch(loadResults(tab.resultsType)).catch(() => {
            expect(store.getActions().length).toBe(2);
            expect(store.getActions()[1]).toEqual({
              type: 'LEGAL_DASHBOARD_LOAD_RESULTS_FAILED',
              payload: {
                resultsType: tab.resultsType,
                error: errorTest,
              },
            });
            done();
          });

          expect(store.getActions().length).toBe(1);
          expect(store.getActions()[0]).toEqual({
            type: 'LEGAL_DASHBOARD_LOAD_RESULTS_REQUESTED',
            payload: tab.resultsType,
          });
        });
      });
    }

    function testLegalDashboardSetPage(tab) {
      describe('legalDashboardSetPage for ' + tab.resultsType, function () {
        it('sets the page', function () {
          const store = SpecUtil.mockReduxStore(initialState);
          store.dispatch(legalDashboardSetPage({ resultsType: tab.resultsType, page: 11 }));

          expect(store.getActions().length).toBe(1);
          expect(store.getActions()[0]).toEqual({
            type: 'LEGAL_DASHBOARD_SET_PAGE',
            payload: {
              resultsType: tab.resultsType,
              page: 11,
            },
          });
        });
      });
    }

    function testFetchBackendPageAction(tab) {
      describe('fetchBackendPage for ' + tab.resultsType, function () {
        it('sets the backend page', function (done) {
          const store = SpecUtil.mockReduxStore(initialState);

          mockAxiosCalls({
            post: {
              [getLegalDashboardApplicationsUrl()]: Promise.resolve({
                data: 'results',
              }),
            },
            [getLegalDashboardComponentsUrl()]: Promise.resolve({
              data: 'results',
            }),
          });

          store.dispatch(fetchBackendPage(tab.resultsType, 1)).then(() => {
            expect(store.getActions().length).toBe(3);
            expect(store.getActions()[0]).toEqual({
              type: 'LEGAL_DASHBOARD_FETCH_BACKEND_PAGE',
              payload: {
                resultsType: tab.resultsType,
                page: 1,
              },
            });
            expect(store.getActions()[1]).toEqual({
              type: 'LEGAL_DASHBOARD_LOAD_RESULTS_REQUESTED',
              payload: tab.resultsType,
            });
            expect(store.getActions()[2]).toEqual({
              type: 'LEGAL_DASHBOARD_LOAD_RESULTS_FULFILLED',
              payload: {
                resultsType: tab.resultsType,
                results: 'results',
              },
            });
            done();
          });
        });
      });
    }

    function testChangeSortFieldAction(tab) {
      describe('changeSortField for ' + tab.resultsType, function () {
        it('sets the sort field value', function (done) {
          const store = SpecUtil.mockReduxStore(initialState);

          mockAxiosCalls({
            post: {
              [getLegalDashboardApplicationsUrl()]: Promise.resolve({
                data: 'results',
              }),
            },
          });

          store.dispatch(changeSortField(tab.resultsType, 'TAG_NAMES_ASC')).then(() => {
            expect(store.getActions().length).toBe(4);
            expect(store.getActions()[0]).toEqual({
              type: 'LEGAL_DASHBOARD_CHANGE_SORT_FIELD',
              payload: {
                resultsType: tab.resultsType,
                sortField: 'TAG_NAMES_ASC',
              },
            });
            expect(store.getActions()[1]).toEqual({
              type: 'LEGAL_DASHBOARD_FETCH_BACKEND_PAGE',
              payload: {
                resultsType: tab.resultsType,
                page: 1,
              },
            });
            expect(store.getActions()[2]).toEqual({
              type: 'LEGAL_DASHBOARD_LOAD_RESULTS_REQUESTED',
              payload: 'applications',
            });
            expect(store.getActions()[3]).toEqual({
              type: 'LEGAL_DASHBOARD_LOAD_RESULTS_FULFILLED',
              payload: {
                resultsType: tab.resultsType,
                results: 'results',
              },
            });
            done();
          });
        });
      });
    }

    tabs.forEach(testLoadResultsAction);
    testSearchByComponentNameAction(tabs[1]);
    testLegalDashboardSetPage(tabs[0]);
    testFetchBackendPageAction(tabs[0]);
    testChangeSortFieldAction(tabs[0]);
  });

  describe('loadDashboardUI', function () {
    const initialState = {
      legalDashboardFilter: {
        appliedFilter: {
          applications: [],
          organizations: [],
          stages: [],
          categories: [],
          progressOptions: [],
        },
        applications: [],
        organizations: [],
        stages: [],
        categories: [],
        progressOptions: [],
      },
      stages: {
        dashboard: {
          stageTypes: [{ stageTypeId: 1, stageName: 'stage' }],
        },
      },
      legalDashboard: {
        applications: {},
        components: {
          componentNameToSearch: '',
          componentSearchInput: { isPristine: true, value: '', trimmedValue: '', validationErrors: null },
        },
      },
      router: {
        currentState: {
          data: {
            activeTab: 'applications',
          },
        },
      },
    };

    const filterJson = {
      name: 'nameTest',
      basedOnFilterName: 'Test1',
      filter: 'filter data',
    };

    const mockGetData = {
      [getApplicationsUrl()]: Promise.resolve({ data: 'applications data' }),
      [getOrganizationsUrl()]: Promise.resolve({ data: 'organizations data' }),
      [getApplicationTagsUrl()]: Promise.resolve({ data: 'tag data' }),
      [getLegalDashboardFilters()]: Promise.resolve({ data: filterJson }),
      [getLegalDashboardSavedFilters()]: Promise.resolve({ data: 'saved filters data' }),
    };

    function getEndpointUrl(activeTab) {
      return activeTab === 'components' ? getLegalDashboardComponentsUrl() : getLegalDashboardApplicationsUrl();
    }

    function getRequestPayload(activeTab) {
      const payload = {
        applicationIds: [],
        organizationIds: [],
        stageTypeIds: [],
        tagIds: [],
        reviewStatus: [],
        page: 1,
        pageSize: DASHBOARD[activeTab].itemsPerPage * DASHBOARD[activeTab].pagesToFill,
        order: undefined,
      };

      return activeTab === 'components' ? { ...payload, componentName: '', order: undefined } : payload;
    }

    function testLoadDashboardUI(tab) {
      const endpointUrl = getEndpointUrl(tab.resultsType);

      describe('loadDashboardUI for ' + tab.resultsType, function () {
        it('loads the filters and then the filtered data', function (done) {
          const store = SpecUtil.mockReduxStore(initialState);
          mockAxiosCalls({
            get: {
              ...mockGetData,
            },
            post: {
              [getLegalDashboardApplicationsUrl()]: Promise.resolve({
                data: 'results',
              }),
              [getLegalDashboardComponentsUrl()]: Promise.resolve({
                data: 'results',
              }),
            },
          });

          store.dispatch(loadDashboardUI(tab.resultsType)).then(() => {
            expect(axios.post).toHaveBeenCalledWith(endpointUrl, getRequestPayload(tab.resultsType));
            expect(store.getActions().length).toBe(8);

            expect(store.getActions()[0]).toEqual({
              type: 'LEGAL_DASHBOARD_LOAD_FILTER_REQUESTED',
            });

            expect(store.getActions()[6]).toEqual({
              type: 'LEGAL_DASHBOARD_LOAD_RESULTS_REQUESTED',
              payload: tab.resultsType,
            });

            expect(store.getActions()[7]).toEqual({
              type: 'LEGAL_DASHBOARD_LOAD_RESULTS_FULFILLED',
              payload: {
                resultsType: tab.resultsType,
                results: 'results',
              },
            });

            done();
          });
        });

        it('loads dashboard results without trying to retrieve saved filters when filters are already populated', function (done) {
          const nonEmptyFilters = {
            applications: ['aTestApp'],
            organizations: [],
            stages: [],
            categories: [],
            progressOptions: [],
          };

          const nonEmptyFiltersInitialState = {
            ...initialState,
            legalDashboardFilter: {
              appliedFilter: initialState.legalDashboardFilter.appliedFilter,
              ...nonEmptyFilters,
            },
          };

          const store = SpecUtil.mockReduxStore(nonEmptyFiltersInitialState);
          mockAxiosCalls({
            post: {
              [getLegalDashboardApplicationsUrl()]: Promise.resolve({
                data: 'results',
              }),
              [getLegalDashboardComponentsUrl()]: Promise.resolve({
                data: 'results',
              }),
            },
          });

          store.dispatch(loadDashboardUI(tab.resultsType)).then(() => {
            expect(store.getActions().length).toBe(2);
            expect(store.getActions()[0]).toEqual({
              type: 'LEGAL_DASHBOARD_LOAD_RESULTS_REQUESTED',
              payload: tab.resultsType,
            });
            expect(store.getActions()[1]).toEqual({
              type: 'LEGAL_DASHBOARD_LOAD_RESULTS_FULFILLED',
              payload: {
                resultsType: tab.resultsType,
                results: 'results',
              },
            });

            done();
          });
        });
      });
    }

    tabs.forEach(testLoadDashboardUI);
  });

  describe('fetchResults', function () {
    let store, mockAxiosCalls;

    beforeEach(() => {
      mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
    });

    it('should include only "compliance" in stageTypeIds when isSbomManager is true', function (done) {
      const initialState = {
        legalDashboardFilter: {
          appliedFilter: {
            applications: ['app1'],
            organizations: ['org1'],
            stages: ['stage1', 'stage2'],
            categories: ['cat1'],
            progressOptions: ['option1'],
          },
        },
        legalDashboard: {
          applications: {},
          components: {
            componentNameToSearch: '',
            componentSearchInput: { isPristine: true, value: '', trimmedValue: '', validationErrors: null },
          },
        },
        router: {
          currentState: {
            name: 'sbomManager.legal.dashboard',
            url: '/legal/dashboard',
            data: {
              activeTab: 'applications',
            },
          },
        },
      };

      store = SpecUtil.mockReduxStore(initialState);

      mockAxiosCalls({
        post: {
          [getLegalDashboardApplicationsUrl()]: Promise.resolve({
            data: 'results',
          }),
        },
      });

      store.dispatch(loadResults('applications')).then(() => {
        const appliedFilter = {
          applicationIds: ['app1'],
          organizationIds: ['org1'],
          stageTypeIds: ['compliance'],
          tagIds: ['cat1'],
          reviewStatus: ['option1'],
          page: 1,
          pageSize: DASHBOARD['applications'].itemsPerPage * DASHBOARD['applications'].pagesToFill,
          order: undefined,
        };

        expect(axios.post).toHaveBeenCalledWith(getLegalDashboardApplicationsUrl(), appliedFilter);
        done();
      });
    });

    it('should include stages in stageTypeIds when isSbomManager is false', function (done) {
      const initialState = {
        legalDashboardFilter: {
          appliedFilter: {
            applications: ['app1'],
            organizations: ['org1'],
            stages: ['stage1', 'stage2'],
            categories: ['cat1'],
            progressOptions: ['option1'],
          },
        },
        legalDashboard: {
          applications: {},
          components: {
            componentNameToSearch: '',
            componentSearchInput: { isPristine: true, value: '', trimmedValue: '', validationErrors: null },
          },
        },
        router: {
          currentState: {
            data: {
              activeTab: 'applications',
            },
          },
        },
      };

      store = SpecUtil.mockReduxStore(initialState);

      mockAxiosCalls({
        post: {
          [getLegalDashboardApplicationsUrl()]: Promise.resolve({
            data: 'results',
          }),
        },
      });

      store.dispatch(loadResults('applications')).then(() => {
        const appliedFilter = {
          applicationIds: ['app1'],
          organizationIds: ['org1'],
          stageTypeIds: ['stage1', 'stage2'],
          tagIds: ['cat1'],
          reviewStatus: ['option1'],
          page: 1,
          pageSize: DASHBOARD['applications'].itemsPerPage * DASHBOARD['applications'].pagesToFill,
          order: undefined,
        };

        expect(axios.post).toHaveBeenCalledWith(getLegalDashboardApplicationsUrl(), appliedFilter);
        done();
      });
    });
  });
});
