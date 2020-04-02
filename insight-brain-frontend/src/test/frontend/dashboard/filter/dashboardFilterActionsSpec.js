/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  loadFilter,
  applyFilter,
  applySavedFilter
} from '../../../../main/frontend/dashboard/filter/dashboardFilterActions';
import {
  getApplicationsUrl,
  getOrganizationsUrl,
  getApplicationTagsUrl,
  getDashboardFilters,
  getDashboardSavedFilters,
  getNewestRisksUrl
} from '../../../../main/frontend/util/CLMLocation';

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
      it('fires filter actions and also loads results', function(done) {
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
    });
  });

  describe('applyFilter', function() {
    it('updates filters and loads results', function(done) {
      mockAxiosCalls({
        put: {
          [getDashboardFilters()]: Promise.resolve({ data: 'update filters response' })
        },
        post: {
          [getNewestRisksUrl()]: Promise.resolve({ data: {
            dashboardResults: 'results', numResults: 3, classyBrew: 'classyBrew'
          }})
        }
      });

      store = SpecUtil.mockReduxStore(initialState);

      store.dispatch(applyFilter('test filters', 'test filter name'))
          .then(() => {
            expect(axios.put).toHaveBeenCalledWith(getDashboardFilters(), {
              filter: 'test filters',
              basedOnFilterName: 'test filter name'
            });

            expect(axios.post).toHaveBeenCalledWith(getNewestRisksUrl(), expectedRisksPayload);

            expect(store.getActions().length).toBe(4);

            expect(store.getActions()[1]).toEqual({
              type: 'APPLY_FILTER_FULFILLED',
              payload: {
                filter: 'update filters response',
                basedOnFilterName: 'test filter name'
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

    it('dispatches APPLY_FILTER_FAILED if failed to update filters', function(done) {
      mockAxiosCalls({
        put: {
          [getDashboardFilters()]: Promise.reject({ status: 403 })
        },
        post: {}
      });

      store = SpecUtil.mockReduxStore(initialState);

      store.dispatch(applyFilter('test filters', 'test filter name'))
          .catch(() => {
            expect(axios.put).toHaveBeenCalledWith(getDashboardFilters(), {
              filter: 'test filters',
              basedOnFilterName: 'test filter name'
            });

            expect(store.getActions().length).toBe(2);
            expect(store.getActions()[1].type).toBe('APPLY_FILTER_FAILED');
            expect(store.getActions()[1].payload.status).toEqual(403);

            expect(axios.post).not.toHaveBeenCalledWith(getNewestRisksUrl());
            done();
          });

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({type: 'APPLY_FILTER_REQUESTED'});
    });

    it('returns rejected promise and does not dispatch APPLY_FILTER_FAILED if failed to load results',
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

          store.dispatch(applyFilter('test filters', 'test filter name'))
              .catch(() => {
                expect(axios.put).toHaveBeenCalledWith(getDashboardFilters(), {
                  filter: 'test filters',
                  basedOnFilterName: 'test filter name'
                });
                expect(axios.post).toHaveBeenCalledWith(getNewestRisksUrl(), expectedRisksPayload);

                expect(store.getActions().length).toBe(4);

                expect(store.getActions()[1]).toEqual({
                  type: 'APPLY_FILTER_FULFILLED',
                  payload: {
                    filter: 'update filters response',
                    basedOnFilterName: 'test filter name'
                  }
                });

                expect(store.getActions()[2]).toEqual({
                  type: 'LOAD_RESULTS_REQUESTED',
                  payload: 'violations'
                });

                expect(store.getActions()[3].type).toBe('LOAD_RESULTS_FAILED');

                done();
              });

          expect(store.getActions().length).toBe(1);
          expect(store.getActions()[0]).toEqual({type: 'APPLY_FILTER_REQUESTED'});
        });
  });

  describe('applySavedFilter', function() {
    const savedFilter = {
      filter: 'test filters',
      name: 'test filter name'
    };

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

      store.dispatch(applySavedFilter(savedFilter))
          .then(() => {
            expect(axios.post).toHaveBeenCalledWith(getNewestRisksUrl(), expectedRisksPayload);
            expect(axios.put).toHaveBeenCalledWith(getDashboardFilters(), {
              filter: 'test filters',
              basedOnFilterName: 'test filter name'
            });

            expect(store.getActions().length).toBe(4);

            expect(store.getActions()[1]).toEqual({
              type: 'APPLY_FILTER_FULFILLED',
              payload: {
                filter: 'update filters response',
                basedOnFilterName: 'test filter name'
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

    it('dispatches APPLY_SAVED_FILTER_FAILED if failed to update filters', function(done) {
      mockAxiosCalls({
        put: {
          [getDashboardFilters()]: Promise.reject({ status: 403 })
        },
        post: {}
      });

      store = SpecUtil.mockReduxStore(initialState);

      store.dispatch(applySavedFilter(savedFilter))
          .catch(() => {
            expect(axios.put).toHaveBeenCalledWith(getDashboardFilters(), {
              filter: 'test filters',
              basedOnFilterName: 'test filter name'
            });
            expect(axios.post).not.toHaveBeenCalledWith(getNewestRisksUrl());

            expect(store.getActions().length).toBe(2);

            expect(store.getActions()[1]).toEqual({
              type: 'APPLY_SAVED_FILTER_FAILED',
              payload: 'test filter name'
            });

            done();
          });

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({type: 'APPLY_FILTER_REQUESTED'});
    });

    it('returns rejected promise and does not dispatch APPLY_SAVED_FILTER_FAILED if failed to load results',
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

          store.dispatch(applySavedFilter(savedFilter))
              .catch(() => {
                expect(axios.post).toHaveBeenCalledWith(getNewestRisksUrl(), expectedRisksPayload);
                expect(axios.put).toHaveBeenCalledWith(getDashboardFilters(), {
                  filter: 'test filters',
                  basedOnFilterName: 'test filter name'
                });

                expect(store.getActions().length).toBe(4);

                expect(store.getActions()[1]).toEqual({
                  type: 'APPLY_FILTER_FULFILLED',
                  payload: {
                    filter: 'update filters response',
                    basedOnFilterName: 'test filter name'
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
        });
  });
});
