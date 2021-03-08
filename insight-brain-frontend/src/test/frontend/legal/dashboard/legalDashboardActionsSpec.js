/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { loadResults } from '../../../../main/frontend/legal/dashboard/legalDashboardActions';
import axios from 'axios';
import { getLegalDashboardApplicationsUrl } from '../../../../main/frontend/util/CLMLocation';

describe('legalDashboardActions', function () {
  describe('loadResults', function () {
    const legalDashboardApplicationsUrlSpy = jasmine.createSpy('getLegalDashboardApplicationsUrl');
    const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
    const tabs = [{
      resultsType: 'applications',
      serviceMethod: legalDashboardApplicationsUrlSpy
    }];
    const initialState = {
      legalDashboardFilter: {
        appliedFilter: {
          applications: [],
          organizations: [],
          stages: [],
          categories: [],
          progressOptions: []
        }
      }
    };

    function testLoadResultsAction(tab) {
      describe('loadResults for ' + tab.resultsType, function() {
        it('loads results', function(done) {
          const store = SpecUtil.mockReduxStore(initialState);
          mockAxiosCalls({
            post: {
              [getLegalDashboardApplicationsUrl()]: Promise.resolve({ data: 'results' })
            }
          });

          store.dispatch(loadResults(tab.resultsType))
              .then(() => {
                expect(store.getActions().length).toBe(2);
                expect(store.getActions()[1]).toEqual({
                  type: 'LEGAL_DASHBOARD_LOAD_RESULTS_FULFILLED',
                  payload: {
                    resultsType: tab.resultsType,
                    results: 'results'
                  }
                });
                done();
              });

          expect(store.getActions().length).toBe(1);
          expect(store.getActions()[0]).toEqual({
            type: 'LEGAL_DASHBOARD_LOAD_RESULTS_REQUESTED',
            payload: tab.resultsType
          });
        });

        it('handles failure to load results', function(done) {
          const store = SpecUtil.mockReduxStore(initialState);
          const errorTest = 'Error test';
          mockAxiosCalls({
            post: {
              [getLegalDashboardApplicationsUrl()]: Promise.reject(errorTest)
            }
          });

          store.dispatch(loadResults(tab.resultsType))
              .catch(() => {
                expect(store.getActions().length).toBe(2);
                expect(store.getActions()[1]).toEqual({
                  type: 'LEGAL_DASHBOARD_LOAD_RESULTS_FAILED',
                  payload: {
                    resultsType: tab.resultsType,
                    error: errorTest
                  }
                });
                done();
              });

          expect(store.getActions().length).toBe(1);
          expect(store.getActions()[0]).toEqual({
            type: 'LEGAL_DASHBOARD_LOAD_RESULTS_REQUESTED',
            payload: tab.resultsType
          });
        });
      });
    }

    tabs.forEach(testLoadResultsAction);
  });
});
