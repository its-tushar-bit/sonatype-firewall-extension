/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  loadFilter,
  applyFilter,
  applySavedFilter,
  applyDefaultFilter, applyFilterCancelled
} from '../../../../main/frontend/dashboard/filter/dashboardFilterActions';
import {
  getApplicationsUrl,
  getOrganizationsUrl,
  getApplicationTagsUrl,
  getApplicationRisksUrl,
  getDashboardFilters,
  getDashboardSavedFilters,
  getNewestRisksUrl
} from '../../../../main/frontend/util/CLMLocation';

import defaultFilter from '../../../../main/frontend/dashboard/filter/defaultFilter';
import { filterToJson } from '../../../../main/frontend/dashboard/filter/dashboardFilterService';

describe('dashboardFilterActions: non-angular', function() {
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
    [getDashboardFilters()]: Promise.resolve({data: filterJson}),
    [getDashboardSavedFilters()]: Promise.resolve({data: 'saved filters data'})
  };

  const initialState = {
    stages: {
      dashboard: {
        stageTypes: [{stageTypeId: 1, stageName: 'stage'}]
      }
    },
    dashboardFilter: {
      appliedFilter: 'current filters'
    },
    dashboard: {
      currentTab: 'violations',
      violations: {sortFields: ['-time', '-threatLevel']},
      components: {sortFields: ['-score']},
      applications: {sortFields: ['-totalApplicationRisk.totalRisk']}
    }
  };

  const expectedRisksPayload = {
    orderBy: '-undefined,-THREAT_LEVEL',
    maxResults: 101,
    organizationIds: undefined,
    applicationIds: undefined,
    stageIds: undefined,
    tagIds: undefined,
    policyViolationStates: undefined,
    maxDaysOld: undefined,
    policyThreatLevelRange: undefined
  };

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
              expect(axios.get).toHaveBeenCalledWith(getDashboardSavedFilters());
              expect(axios.get).not.toHaveBeenCalledWith(getNewestRisksUrl());

              expect(store.getActions().length).toBe(3);

              expect(store.getActions()[1]).toEqual({
                type: 'FETCH_SAVED_FILTERS_FULFILLED',
                payload: 'saved filters data'
              });

              expect(store.getActions()[2]).toEqual({
                type: 'LOAD_FILTER_FAILED',
                payload: 'failed to get applications data'
              });
              done();
            });

        expect(store.getActions()[0]).toEqual({
          type: 'LOAD_FILTER_REQUESTED'
        });
      });
    });

    describe('when needsAcknowledgement is true', function() {
      it('fires the action, fetchAvailableFilterOptionsFulfilled and fetchCurrentFilterFulfilled', function(done) {
        filterJson.needsAcknowledgement = true;
        mockAxiosCalls({
          get: {
            ...mockGetData,
            [getDashboardFilters()]: Promise.resolve({data: filterJson})
          }
        });

        store = SpecUtil.mockReduxStore(initialState);

        store.dispatch(loadFilter()).then(() => {
          expect(axios.get).toHaveBeenCalledWith(getApplicationsUrl());
          expect(axios.get).toHaveBeenCalledWith(getOrganizationsUrl());
          expect(axios.get).toHaveBeenCalledWith(getApplicationTagsUrl());
          expect(axios.get).toHaveBeenCalledWith(getDashboardFilters());
          expect(axios.get).toHaveBeenCalledWith(getDashboardSavedFilters());
          expect(axios.get).not.toHaveBeenCalledWith(getNewestRisksUrl());

          expect(store.getActions().length).toBe(4);

          expect(store.getActions()[1]).toEqual({
            type: 'FETCH_SAVED_FILTERS_FULFILLED',
            payload: 'saved filters data'
          });

          expect(store.getActions()[2]).toEqual({
            type: 'FETCH_AVAILABLE_FILTER_OPTIONS_FULFILLED',
            payload: {
              organizations: 'organizations data',
              applications: 'applications data',
              stages: initialState.stages.dashboard.stageTypes,
              categories: 'tag data'
            }
          });

          expect(store.getActions()[3]).toEqual({
            type: 'FETCH_CURRENT_FILTER_FULFILLED',
            payload: {
              name: '',
              basedOnFilterName: 'Test1',
              filter: 'filter data',
              needsAcknowledgement: true
            }
          });
          done();
        });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0]).toEqual({
          type: 'LOAD_FILTER_REQUESTED'
        });
      });
    });

    describe('when needsAcknowledgement is false', function() {
      it('fires filter actions and calls loads results ' +
      'with the value of dashboard.currentTab if called with no param', function(done) {
        filterJson.needsAcknowledgement = false;
        mockAxiosCalls({
          get: {
            ...mockGetData,
            [getDashboardFilters()]: Promise.resolve({data: filterJson})
          },
          post: {
            [getNewestRisksUrl()]: Promise.resolve({
              data: {
                dashboardResults: 'results',
                numResults: 3
              }
            })
          }
        });

        store = SpecUtil.mockReduxStore(initialState);

        store.dispatch(loadFilter()).then(() => {
          expect(axios.get).toHaveBeenCalledWith(getApplicationsUrl());
          expect(axios.get).toHaveBeenCalledWith(getOrganizationsUrl());
          expect(axios.get).toHaveBeenCalledWith(getApplicationTagsUrl());
          expect(axios.get).toHaveBeenCalledWith(getDashboardFilters());
          expect(axios.get).toHaveBeenCalledWith(getDashboardSavedFilters());
          expect(axios.post).toHaveBeenCalledWith(getNewestRisksUrl(), expectedRisksPayload);

          expect(store.getActions().length).toBe(6);

          expect(store.getActions()[1]).toEqual({
            type: 'FETCH_SAVED_FILTERS_FULFILLED',
            payload: 'saved filters data'
          });

          expect(store.getActions()[2]).toEqual({
            type: 'FETCH_AVAILABLE_FILTER_OPTIONS_FULFILLED',
            payload: {
              organizations: 'organizations data',
              applications: 'applications data',
              stages: initialState.stages.dashboard.stageTypes,
              categories: 'tag data'
            }
          });

          expect(store.getActions()[3]).toEqual({
            type: 'FETCH_CURRENT_FILTER_FULFILLED',
            payload: {
              name: '',
              basedOnFilterName: 'Test1',
              filter: 'filter data',
              needsAcknowledgement: false
            }
          });

          expect(store.getActions()[4]).toEqual({
            type: 'LOAD_RESULTS_REQUESTED',
            payload: 'violations'
          });

          expect(store.getActions()[5]).toEqual({
            type: 'LOAD_RESULTS_FULFILLED',
            payload: {
              resultsType: 'violations',
              results: 'results',
              numResults: 3,
              classyBrew: undefined
            }
          });
          done();
        });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0]).toEqual({
          type: 'LOAD_FILTER_REQUESTED'
        });
      });

      it('fires filter actions and calls loads results ' +
      'with the param supplied when called', function(done) {
        filterJson.needsAcknowledgement = false;
        mockAxiosCalls({
          get: {
            ...mockGetData,
            [getDashboardFilters()]: Promise.resolve({data: filterJson})
          },
          post: {
            [getApplicationRisksUrl()]: Promise.resolve({
              data: {
                dashboardResults: [],
                numResults: 0
              }
            })
          }
        });

        store = SpecUtil.mockReduxStore(initialState);

        const expectedApplicationsRisksPayload = {
          ...expectedRisksPayload,
          orderBy: '-TOTAL_RISK'
        };

        store.dispatch(loadFilter('applications')).then(() => {
          expect(axios.get).toHaveBeenCalledWith(getApplicationsUrl());
          expect(axios.get).toHaveBeenCalledWith(getOrganizationsUrl());
          expect(axios.get).toHaveBeenCalledWith(getApplicationTagsUrl());
          expect(axios.get).toHaveBeenCalledWith(getDashboardFilters());
          expect(axios.get).toHaveBeenCalledWith(getDashboardSavedFilters());
          expect(axios.post).toHaveBeenCalledWith(getApplicationRisksUrl(), expectedApplicationsRisksPayload);

          expect(store.getActions().length).toBe(6);

          expect(store.getActions()[1]).toEqual({
            type: 'FETCH_SAVED_FILTERS_FULFILLED',
            payload: 'saved filters data'
          });

          expect(store.getActions()[2]).toEqual({
            type: 'FETCH_AVAILABLE_FILTER_OPTIONS_FULFILLED',
            payload: {
              organizations: 'organizations data',
              applications: 'applications data',
              stages: initialState.stages.dashboard.stageTypes,
              categories: 'tag data'
            }
          });

          expect(store.getActions()[3]).toEqual({
            type: 'FETCH_CURRENT_FILTER_FULFILLED',
            payload: {
              name: '',
              basedOnFilterName: 'Test1',
              filter: 'filter data',
              needsAcknowledgement: false
            }
          });

          expect(store.getActions()[4]).toEqual({
            type: 'LOAD_RESULTS_REQUESTED',
            payload: 'applications'
          });

          const lastAction = store.getActions()[5];
          expect(lastAction.type).toEqual('LOAD_RESULTS_FULFILLED');
          expect(lastAction.payload).not.toBeNull();
          expect(lastAction.payload.resultsType).toEqual('applications');
          done();
        });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0]).toEqual({
          type: 'LOAD_FILTER_REQUESTED'
        });
      });
    });
  });

  describe('applyFilter', function() {
    const expectedFailAction = {
      type: 'APPLY_FILTER_FAILED',
      payload: { status: 403 }
    };

    const action = applyFilter('test filters', 'test filter name');

    testSuccessfullyUpdatesFiltersAndLoadsResults(action, 'test filters', 'test filter name');
    testSuccessfullyUpdatesFiltersButFailsToLoadsResults(action, 'test filters', 'test filter name');
    testFailedToUpdateFilter(action, 'test filters', 'test filter name', expectedFailAction);
  });

  describe('applySavedFilter', function() {
    const savedFilter = {
      filter: 'test filters',
      name: 'test filter name'
    };

    const expectedFailAction = {
      type: 'APPLY_SAVED_FILTER_FAILED',
      payload: 'test filter name'
    };

    const action = applySavedFilter(savedFilter);

    testSuccessfullyUpdatesFiltersAndLoadsResults(action, 'test filters', 'test filter name');
    testSuccessfullyUpdatesFiltersButFailsToLoadsResults(action, 'test filters', 'test filter name');
    testFailedToUpdateFilter(action, 'test filters', 'test filter name', expectedFailAction);
  });

  describe('applyDefaultFilter', function() {
    const filter = filterToJson(defaultFilter);

    const expectedFailAction = {
      type: 'APPLY_SAVED_FILTER_FAILED',
      payload: 'Default filter'
    };

    const action = applyDefaultFilter();

    testSuccessfullyUpdatesFiltersAndLoadsResults(action, filter, null);
    testSuccessfullyUpdatesFiltersButFailsToLoadsResults(action, filter, null);
    testFailedToUpdateFilter(action, filter, null, expectedFailAction);
  });

  describe('applyFilterCancelled', function() {
    it('dispatches an APPLY_FILTER_CANCELLED function', () => {
      store = SpecUtil.mockReduxStore(initialState);

      store.dispatch(applyFilterCancelled());
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({type: 'APPLY_FILTER_CANCELLED'});
    });
  });

  function testFailedToUpdateFilter(action, expectedFilter, expectedFilterName, expectedFailAction) {
    it(`dispatches ${expectedFailAction.type} if failed to update filters`, function(done) {
      mockAxiosCalls({
        put: {
          [getDashboardFilters()]: Promise.reject({ status: 403 })
        },
        post: {}
      });

      store = SpecUtil.mockReduxStore(initialState);

      store.dispatch(action)
          .catch(() => {
            expect(axios.put).toHaveBeenCalledWith(getDashboardFilters(), {
              filter: expectedFilter,
              basedOnFilterName: expectedFilterName
            });
            expect(axios.post).not.toHaveBeenCalledWith(getNewestRisksUrl());

            expect(store.getActions().length).toBe(2);

            expect(store.getActions()[1]).toEqual(expectedFailAction);

            done();
          });

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({type: 'APPLY_FILTER_REQUESTED'});
    });
  }

  function testSuccessfullyUpdatesFiltersAndLoadsResults(action, expectedFilter, expectedFilterName) {
    it('updates filters and loads results', function(done) {
      mockAxiosCalls({
        put: {
          [getDashboardFilters()]: Promise.resolve({data: 'update filters response'})
        },
        post: {
          [getNewestRisksUrl()]: Promise.resolve({
            data: { dashboardResults: 'results', numResults: 3, classyBrew: 'classyBrew' }
          })
        }
      });

      store = SpecUtil.mockReduxStore(initialState);

      store.dispatch(action)
          .then(() => {
            expect(axios.post).toHaveBeenCalledWith(getNewestRisksUrl(), expectedRisksPayload);
            expect(axios.put).toHaveBeenCalledWith(getDashboardFilters(), {
              filter: expectedFilter,
              basedOnFilterName: expectedFilterName
            });

            expect(store.getActions().length).toBe(4);

            expect(store.getActions()[1]).toEqual({
              type: 'APPLY_FILTER_FULFILLED',
              payload: {
                filter: 'update filters response',
                basedOnFilterName: expectedFilterName
              }
            });
            expect(store.getActions()[2]).toEqual({
              type: 'LOAD_RESULTS_REQUESTED',
              payload: 'violations'
            });

            expect(store.getActions()[3]).toEqual({
              type: 'LOAD_RESULTS_FULFILLED',
              payload: {
                resultsType: 'violations',
                results: 'results',
                numResults: 3,
                classyBrew: undefined
              }
            });

            done();
          });

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({type: 'APPLY_FILTER_REQUESTED'});
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
              [getNewestRisksUrl()]: Promise.reject('load results error')
            }
          });

          store = SpecUtil.mockReduxStore(initialState);

          store.dispatch(action)
              .catch(() => {
                expect(axios.post).toHaveBeenCalledWith(getNewestRisksUrl(), expectedRisksPayload);
                expect(axios.put).toHaveBeenCalledWith(getDashboardFilters(), {
                  filter: expectedFilter,
                  basedOnFilterName: expectedFilterName
                });

                expect(store.getActions().length).toBe(4);

                expect(store.getActions()[1]).toEqual({
                  type: 'APPLY_FILTER_FULFILLED',
                  payload: {
                    filter: 'update filters response',
                    basedOnFilterName: expectedFilterName
                  }
                });

                expect(store.getActions()[2]).toEqual({
                  type: 'LOAD_RESULTS_REQUESTED',
                  payload: 'violations'
                });

                expect(store.getActions()[3]).toEqual({
                  type: 'LOAD_RESULTS_FAILED',
                  payload: {
                    error: 'load results error',
                    resultsType: 'violations'
                  }
                });

                done();
              });

          expect(store.getActions().length).toBe(1);
          expect(store.getActions()[0]).toEqual({type: 'APPLY_FILTER_REQUESTED'});
        }
    );
  }
});
