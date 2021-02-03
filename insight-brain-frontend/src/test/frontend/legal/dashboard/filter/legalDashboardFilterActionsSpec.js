/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  loadFilter,
  applyFilter
} from '../../../../../main/frontend/legal/dashboard/filter/legalDashboardFilterActions';
import {
  getApplicationsUrl,
  getOrganizationsUrl,
  getApplicationTagsUrl,
  getDashboardFilters, getLegalDashboardApplicationsUrl
} from '../../../../../main/frontend/util/CLMLocation';

describe('legalDashboardFilterActions', function() {
  let store;

  const filterJson = {
    name: '',
    basedOnFilterName: 'Test1',
    filter: 'filter data',
    needsAcknowledgement: false
  };

  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  const mockGetData = {
    [getApplicationsUrl()]: Promise.resolve({data: 'applications data'}),
    [getOrganizationsUrl()]: Promise.resolve({data: 'organizations data'}),
    [getApplicationTagsUrl()]: Promise.resolve({data: 'tag data'}),
    [getDashboardFilters()]: Promise.resolve({data: filterJson})
  };

  const initialState = {
    stages: {
      dashboard: {
        stageTypes: [{stageTypeId: 1, stageName: 'stage'}]
      }
    },
    legalDashboardFilter: {
      appliedFilter: {
        applications: [],
        organizations: [],
        stages: [],
        categories: []
      }
    },
    legalDashboard: {
      currentTab: 'applications',
      components: {sortFields: ['-score']},
      applications: {sortFields: ['-totalApplicationRisk.totalRisk']}
    }
  };

  const applicationsPayload = { applicationIds: [], organizationIds: [], stageTypeIds: [], tagIds: [] };

  describe('loadFilter', function() {
    describe('when failed fetching filter data', function() {
      it('fires loadFiltersFailed action', function(done) {
        mockAxiosCalls({
          get: {
            ...mockGetData,
            [getApplicationsUrl()]: Promise.reject('failed to get applications data')
          }
        });

        store = SpecUtil.mockReduxStore(initialState);

        store.dispatch(loadFilter())
            .catch(() => {
              expect(axios.get).toHaveBeenCalledWith(getApplicationsUrl());
              expect(axios.get).toHaveBeenCalledWith(getOrganizationsUrl());
              expect(axios.get).toHaveBeenCalledWith(getApplicationTagsUrl());
              expect(axios.get).toHaveBeenCalledWith(getDashboardFilters());

              expect(store.getActions().length).toBe(2);

              expect(store.getActions()[1]).toEqual({
                type: 'LOAD_LEGAL_FILTER_FAILED',
                payload: 'failed to get applications data'
              });
              done();
            });

        expect(store.getActions()[0]).toEqual({
          type: 'LOAD_LEGAL_FILTER_REQUESTED'
        });
      });
    });
  });

  describe('applyFilter', function() {
    const expectedFailAction = {
      type: 'APPLY_LEGAL_FILTER_FAILED',
      payload: 'Error 403'
    };

    const action = applyFilter('test filters', 'test filter name');

    testSuccessfullyUpdatesFiltersAndLoadsResults(action, 'test filters', 'test filter name');
    testSuccessfullyUpdatesFiltersButFailsToLoadsResults(action, 'test filters', 'test filter name');
    testFailedToUpdateFilter(action, 'test filters', 'test filter name', expectedFailAction);
  });

  // describe('applyDefaultFilter', function() {
  //   const filter = filterToJson(defaultFilter);
  //
  //   const expectedFailAction = {
  //     type: 'APPLY_SAVED_FILTER_FAILED',
  //     payload: 'Default filter'
  //   };
  //
  //   const action = applyDefaultFilter();
  //
  //   testSuccessfullyUpdatesFiltersAndLoadsResults(action, filter, null);
  //   testSuccessfullyUpdatesFiltersButFailsToLoadsResults(action, filter, null);
  //   testFailedToUpdateFilter(action, filter, null, expectedFailAction);
  // });

  // describe('applyFilterCancelled', function() {
  //   it('dispatches an APPLY_FILTER_CANCELLED function', () => {
  //     store = SpecUtil.mockReduxStore(initialState);
  //
  //     store.dispatch(applyFilterCancelled());
  //     expect(store.getActions().length).toBe(1);
  //     expect(store.getActions()[0]).toEqual({type: 'APPLY_FILTER_CANCELLED'});
  //   });
  // });

  function testFailedToUpdateFilter(action, expectedFilter, expectedFilterName, expectedFailAction) {
    it(`dispatches ${expectedFailAction.type} if failed to update filters`, function(done) {
      mockAxiosCalls({
        put: {
          [getDashboardFilters()]: Promise.reject({ status: 403 })
        },
        post: {
          [getLegalDashboardApplicationsUrl()]: Promise.resolve({
            data: { dashboardResults: 'results' }
          })
        }
      });

      store = SpecUtil.mockReduxStore(initialState);

      store.dispatch(action)
          .catch(() => {
            expect(axios.put).toHaveBeenCalledWith(getDashboardFilters(), {
              filter: expectedFilter,
              basedOnFilterName: expectedFilterName
            });
            expect(axios.post).not.toHaveBeenCalledWith(getLegalDashboardApplicationsUrl());

            expect(store.getActions().length).toBe(2);

            expect(store.getActions()[1]).toEqual(expectedFailAction);

            done();
          });

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({type: 'APPLY_LEGAL_FILTER_REQUESTED'});
    });
  }

  function testSuccessfullyUpdatesFiltersAndLoadsResults(action, expectedFilter, expectedFilterName) {
    it('updates filters and loads results', function(done) {
      mockAxiosCalls({
        put: {
          [getDashboardFilters()]: Promise.resolve({data: 'update filters response'})
        },
        post: {
          [getLegalDashboardApplicationsUrl()]: Promise.resolve({
            data: { applications: { results: [] }}
          })
        }
      });

      store = SpecUtil.mockReduxStore(initialState);

      store.dispatch(action)
          .then(() => {
            expect(axios.post).toHaveBeenCalledWith(getLegalDashboardApplicationsUrl(), applicationsPayload);
            expect(axios.put).toHaveBeenCalledWith(getDashboardFilters(), {
              filter: expectedFilter,
              basedOnFilterName: expectedFilterName
            });

            expect(store.getActions().length).toBe(4);

            expect(store.getActions()[1]).toEqual({
              type: 'APPLY_LEGAL_FILTER_FULFILLED',
              payload: {
                filter: 'update filters response',
                basedOnFilterName: expectedFilterName
              }
            });
            expect(store.getActions()[2]).toEqual({
              type: 'LOAD_LEGAL_RESULTS_REQUESTED',
              payload: 'applications'
            });

            expect(store.getActions()[3]).toEqual({
              type: 'LOAD_LEGAL_RESULTS_FULFILLED',
              payload: {
                resultsType: 'applications',
                results: { applications: { results: [] }}
              }
            });

            done();
          });

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({type: 'APPLY_LEGAL_FILTER_REQUESTED'});
    });
  }

  function testSuccessfullyUpdatesFiltersButFailsToLoadsResults(action, expectedFilter, expectedFilterName) {
    it('returns rejected promise and does not dispatch apply filter failed action if failed to load results',
        function(done) {
          mockAxiosCalls({
            put: {
              [getDashboardFilters()]: Promise.resolve({data: 'update filters response'})
            },
            post: {
              [getLegalDashboardApplicationsUrl()]: Promise.reject('load results error')
            }
          });

          store = SpecUtil.mockReduxStore(initialState);

          store.dispatch(action)
              .catch(() => {
                expect(axios.post).toHaveBeenCalledWith(getLegalDashboardApplicationsUrl(), applicationsPayload);
                expect(axios.put).toHaveBeenCalledWith(getDashboardFilters(), {
                  filter: expectedFilter,
                  basedOnFilterName: expectedFilterName
                });

                expect(store.getActions().length).toBe(4);

                expect(store.getActions()[1]).toEqual({
                  type: 'APPLY_LEGAL_FILTER_FULFILLED',
                  payload: {
                    filter: 'update filters response',
                    basedOnFilterName: expectedFilterName
                  }
                });

                expect(store.getActions()[2]).toEqual({
                  type: 'LOAD_LEGAL_RESULTS_REQUESTED',
                  payload: 'applications'
                });

                expect(store.getActions()[3]).toEqual({
                  type: 'LOAD_LEGAL_RESULTS_FAILED',
                  payload: {
                    error: 'load results error',
                    resultsType: 'applications'
                  }
                });

                done();
              });

          expect(store.getActions().length).toBe(1);
          expect(store.getActions()[0]).toEqual({type: 'APPLY_LEGAL_FILTER_REQUESTED'});
        }
    );
  }
});
