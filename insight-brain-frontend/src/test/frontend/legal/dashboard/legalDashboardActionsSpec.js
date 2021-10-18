/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  loadResults,
  fetchBackendPage,
  changeSortField,
} from '../../../../main/frontend/legal/dashboard/legalDashboardActions';
import axios from 'axios';
import {
  getLegalDashboardApplicationsUrl,
  getLegalDashboardComponentsUrl,
} from '../../../../main/frontend/util/CLMLocation';

describe('legalDashboardActions', function () {
  describe('loadResults', function () {
    const legalDashboardApplicationsUrlSpy = jasmine.createSpy('getLegalDashboardApplicationsUrl');
    const legalDashboardComponentsUrlSpy = jasmine.createSpy('getLegalDashboardComponentsUrl');
    const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
    const tabs = [
      {
        resultsType: 'applications',
        serviceMethod: legalDashboardApplicationsUrlSpy,
      },
      {
        resultsType: 'components',
        serviceMethod: legalDashboardComponentsUrlSpy,
      },
    ];
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
        components: {},
      },
    };

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
              [getLegalDashboardComponentsUrl()]: Promise.reject(errorTest),
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
    testFetchBackendPageAction(tabs[0]);
    testChangeSortFieldAction(tabs[0]);
  });
});
