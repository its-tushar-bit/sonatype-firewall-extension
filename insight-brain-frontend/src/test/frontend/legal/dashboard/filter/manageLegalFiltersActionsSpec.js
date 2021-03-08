/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import {
  getLegalDashboardDeleteFilterUrl,
  getLegalDashboardSavedFilters
} from '../../../../../main/frontend/util/CLMLocation';
import { cancelSaveFilter } from '../../../../../main/frontend/legal/dashboard/filter/manageLegalFiltersActions';

describe('manageLegalFilterActions', function() {
  let saveFilter, fetchSavedFilters, deleteFilter;

  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);

  const filterJsonSpy = jasmine.createSpy('filterJson');

  beforeEach(function() {
    const module =
        require('inject-loader!../../../../../main/frontend/legal/dashboard/filter/manageLegalFiltersActions')({
          './legalDashboardFilterService': {
            filterToJson: filterJsonSpy
          }
        });
    deleteFilter = module.deleteFilter;
    fetchSavedFilters = module.fetchSavedFilters;
    saveFilter = module.saveFilter;
  });

  describe('fetchSavedFilters', function() {
    let store;

    beforeEach(function() {
      store = SpecUtil.mockReduxStore();
    });

    it('gets the saved filters from the backend and then dispatches LEGAL_DASHBOARD_FETCH_SAVE_FILTERS_FULFILLED',
        function(done) {
          mockAxiosCalls({
            get: {
              [getLegalDashboardSavedFilters()]: Promise.resolve({data: [{ name: 'foo' }]})
            }
          });

          store.dispatch(fetchSavedFilters())
              .then(() => {
                expect(axios.get).toHaveBeenCalledWith(getLegalDashboardSavedFilters());

                expect(store.getActions().length).toBe(1);

                expect(store.getActions()[0]).toEqual({
                  type: 'LEGAL_DASHBOARD_FETCH_SAVE_FILTERS_FULFILLED',
                  payload: [{ name: 'foo' }]
                });

                done();
              });
        });

    it('dispatches LEGAL_DASHBOARD_FETCH_SAVED_FILTERS_FAILED when the backend call fails', function(done) {
      mockAxiosCalls({
        get: {
          [getLegalDashboardSavedFilters()]: Promise.reject({ status: 403 })
        }
      });

      store.dispatch(fetchSavedFilters())
          .catch(() => {
            expect(axios.get).toHaveBeenCalledWith(getLegalDashboardSavedFilters());

            expect(store.getActions().length).toBe(1);

            expect(store.getActions()[0]).toEqual({
              type: 'LEGAL_DASHBOARD_FETCH_SAVED_FILTERS_FAILED',
              payload: {status: 403}
            });

            done();
          });
    });
  });

  describe('saveFilter', function() {
    let store;
    const initialState = {
          legalDashboardFilter: {
            appliedFilter: {
              policyThreatLevels: [1, 2]
            },
            filterJson: { applications: ['1234'] }
          },
          manageLegalFilters: {
            savedFilters: [{ name: 'bar' }]
          }
        },
        expectedPUTBody = {
          name: 'foo',
          filter: { applicationFilters: ['1234'] },
          type: 'ADVANCED_LEGAL_PACK_DASHBOARD'
        },
        dashboardSavedFiltersUrl = getLegalDashboardSavedFilters();

    beforeEach(function() {
      store = SpecUtil.mockReduxStore(initialState);
      filterJsonSpy.and.returnValue(expectedPUTBody.filter);
    });

    it('dispatches LEGAL_DASHBOARD_SAVE_FILTER_REQUESTED action if filter name is not duplicate and not overwriting',
        (done) => {
          mockAxiosCalls({
            get: {
              [dashboardSavedFiltersUrl]: Promise.resolve({ data: {} })
            },
            put: {
              [dashboardSavedFiltersUrl]: Promise.resolve({ data: {} })
            }
          });

          store.dispatch(saveFilter({ name: 'foo' })).then(done);

          const actions = store.getActions();

          expect(actions.length).toBe(1);
          expect(actions[0].type).toBe('LEGAL_DASHBOARD_SAVE_FILTER_REQUESTED');
          expect(actions[0].payload).toBeUndefined();
        });

    it('dispatches LEGAL_DASHBOARD_SAVE_FILTER_OVERWRITE_REQUESTED action if overwriting without warning', () => {
      store.dispatch(saveFilter({ name: 'foo', isOverwriting: true }));

      const actions = store.getActions();

      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('LEGAL_DASHBOARD_SAVE_FILTER_OVERWRITE_REQUESTED');
      expect(actions[0].payload).toBeUndefined();
    });

    it('dispatches LEGAL_DASHBOARD_SAVE_FILTER_OVERWRITE_REQUESTED action if name is duplicate', function() {
      store.dispatch(saveFilter({ name: 'bar', isOverwriting: true }));

      const actions = store.getActions();

      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('LEGAL_DASHBOARD_SAVE_FILTER_OVERWRITE_REQUESTED');
      expect(actions[0].payload).toBeUndefined();
    });

    it('dispatches LEGAL_DASHBOARD_SAVE_DUPLICATE_FILTER_REQUESTED if name is duplicate, not overwriting and warning',
        function() {
          store.dispatch(saveFilter({ name: 'bar', isOverwriting: false }));

          const actions = store.getActions();

          expect(actions.length).toBe(1);
          expect(actions[0].type).toBe('LEGAL_DASHBOARD_SAVE_DUPLICATE_FILTER_REQUESTED');
          expect(actions[0].payload).toEqual('bar');
        });

    it('dispatches LEGAL_DASHBOARD_SAVE_DUPLICATE_FILTER_REQUESTED if name is duplicate ignoring space',
        function() {
          store.dispatch(saveFilter({name: 'b ar', isOverwriting: false}));

          const actions = store.getActions();

          expect(actions.length).toBe(1);
          expect(actions[0].type).toBe('LEGAL_DASHBOARD_SAVE_DUPLICATE_FILTER_REQUESTED');
          expect(actions[0].payload).toEqual('bar');
        });

    it('dispatches LEGAL_DASHBOARD_SAVE_DUPLICATE_FILTER_REQUESTED if name is duplicate ignoring case',
        function() {
          store.dispatch(saveFilter({name: 'bAr', isOverwriting: false}));

          const actions = store.getActions();

          expect(actions.length).toBe(1);
          expect(actions[0].type).toBe('LEGAL_DASHBOARD_SAVE_DUPLICATE_FILTER_REQUESTED');
          expect(actions[0].payload).toEqual('bar');
        });

    it('PUTs the filter to the server, dispatches LEGAL_DASHBOARD_SAVE_FILTER_FULFILLED and fetches the saved filters',
        function(done) {
          const putSavedFilterResponse = { foo: 'bar' },
              getSavedFiltersResponse = { baz: 'buzz' };

          mockAxiosCalls({
            get: {
              [dashboardSavedFiltersUrl]: Promise.resolve({ data: getSavedFiltersResponse })
            },
            put: {
              [dashboardSavedFiltersUrl]: Promise.resolve({ data: putSavedFilterResponse })
            }
          });

          store.dispatch(saveFilter({ name: 'foo', isOverwriting: false }))
              .then(() => {
                actions = store.getActions();
                expect(axios.get).toHaveBeenCalledWith(dashboardSavedFiltersUrl);
                expect(axios.put).toHaveBeenCalledWith(dashboardSavedFiltersUrl, expectedPUTBody);
                expect(actions.length).toBe(3);
                expect(actions[1].type).toBe('LEGAL_DASHBOARD_SAVE_FILTER_FULFILLED');
                expect(actions[1].payload).toEqual(putSavedFilterResponse);
                expect(actions[2].type).toBe('LEGAL_DASHBOARD_FETCH_SAVE_FILTERS_FULFILLED');
                expect(actions[2].payload).toEqual(getSavedFiltersResponse);
                done();
              });

          let actions = store.getActions();
          expect(actions.length).toBe(1);
        }
    );

    it('dispatches LEGAL_DASHBOARD_SAVE_FILTER_FAILED and does not fetch the saved filters if PUT fails', (done) => {
      mockAxiosCalls({
        put: {
          [dashboardSavedFiltersUrl]: Promise.reject({ status: 403 })
        }
      });

      store.dispatch(saveFilter({ name: 'foo', isOverwriting: false }))
          .catch(() => {
            const actions = store.getActions();
            expect(axios.put).toHaveBeenCalledWith(dashboardSavedFiltersUrl, expectedPUTBody);
            expect(actions.length).toBe(2);
            expect(actions[1].type).toBe('LEGAL_DASHBOARD_SAVE_FILTER_FAILED');
            expect(actions[1].payload.status).toBe(403);
            done();
          });

      expect(store.getActions().length).toBe(1);
    });

    it('rejects the returned promise if the saved filter fetching fails', function(done) {
      const putSavedFilterResponse = { foo: 'bar' };

      mockAxiosCalls({
        put: {
          [dashboardSavedFiltersUrl]: Promise.resolve({ data: putSavedFilterResponse })
        },
        get: {
          [dashboardSavedFiltersUrl]: Promise.reject({ status: 403 })
        }
      });

      store.dispatch(saveFilter({ name: 'foo', isOverwriting: false }))
          .catch(() => {
            const actions = store.getActions();
            expect(axios.get).toHaveBeenCalledWith(dashboardSavedFiltersUrl);
            expect(axios.put).toHaveBeenCalledWith(dashboardSavedFiltersUrl, expectedPUTBody);
            expect(actions.length).toBe(3);
            expect(actions[2].type).toBe('LEGAL_DASHBOARD_FETCH_SAVED_FILTERS_FAILED');
            expect(actions[2].payload.status).toBe(403);
            done();
          });

      expect(store.getActions().length).toBe(1);
    });
  });

  describe('when warning is displayed', function() {
    let store;

    const initialStateWithWarning = {
          legalDashboardFilter: {
            appliedFilter: {
              policyThreatLevels: [1, 2]
            },
            filterJson: {applications: ['1234']}
          },
          manageLegalFilters: {
            savedFilters: [{name: 'bar'}],
            saveFilterWarning: 'xyz'
          }
        },
        dashboardSavedFiltersUrl = getLegalDashboardSavedFilters();

    beforeEach(function() {
      store = SpecUtil.mockReduxStore(initialStateWithWarning);
      mockAxiosCalls({
        get: {
          [dashboardSavedFiltersUrl]: Promise.resolve({ data: {} })
        },
        put: {
          [dashboardSavedFiltersUrl]: Promise.resolve({ data: {} })
        }
      });
    });

    it('dispatches LEGAL_DASHBOARD_SAVE_FILTER_REQUESTED if filter name is duplicate but warning is there', (done) => {
      store.dispatch(saveFilter({ name: 'bar' })).then(done);

      const actions = store.getActions();
      const state = store.getState();

      expect(state.manageLegalFilters.saveFilterWarning).toBe('xyz');
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('LEGAL_DASHBOARD_SAVE_FILTER_REQUESTED');
      expect(actions[0].payload).toBeUndefined();
    });

    it('dispatches LEGAL_DASHBOARD_SAVE_FILTER_REQUESTED if overwriting but warning is displayed', function(done) {
      store.dispatch(saveFilter({ name: 'foo', isOverwriting: true })).then(done);

      const actions = store.getActions();
      const state = store.getState();

      expect(state.manageLegalFilters.saveFilterWarning).toBe('xyz');
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('LEGAL_DASHBOARD_SAVE_FILTER_REQUESTED');
      expect(actions[0].payload).toBeUndefined();
    });
  });

  describe('cancelSaveFilter', function() {
    it('sends a LEGAL_DASHBOARD_SET_DISPLAY_SAVE_FILTER_MODAL action when saveFilterWarning is null', function() {
      const initialState = {
        manageLegalFilters: {
          saveFilterWarning: null
        }
      };

      const store = SpecUtil.mockReduxStore(initialState);

      store.dispatch(cancelSaveFilter());

      const actions = store.getActions();

      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('LEGAL_DASHBOARD_SET_DISPLAY_SAVE_FILTER_MODAL');
    });

    it('immediately sends a LEGAL_DASHBOARD_SAVE_CONFIRM_CANCELLED action when saveFilterWarning not null', function() {
      const initialState = {
        manageLegalFilters: {
          saveFilterWarning: 'xyz'
        }
      };

      const store = SpecUtil.mockReduxStore(initialState);

      store.dispatch(cancelSaveFilter());

      const actions = store.getActions();

      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('LEGAL_DASHBOARD_SAVE_CONFIRM_CANCELLED');
    });
  });

  describe('deleteFilter', function() {
    let store;

    const initialState = { manageLegalFilters: { savedFilters: null } },
        filterToDelete = 'foo',
        dashboardSavedFiltersUrl = getLegalDashboardSavedFilters(),
        deleteFiltersUrl = getLegalDashboardDeleteFilterUrl(filterToDelete);

    beforeEach(() => {
      store = SpecUtil.mockReduxStore(initialState);
    });

    it('immediately dispatches a LEGAL_DASHBOARD_DELETE_FILTER_REQUESTED action with no payload', function(done) {
      mockAxiosCalls({
        del: {
          [deleteFiltersUrl]: Promise.resolve({})
        },
        get: {
          [dashboardSavedFiltersUrl]: Promise.resolve({ data: {} })
        }
      });

      store.dispatch(deleteFilter(filterToDelete)).then(done);

      const actions = store.getActions();

      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('LEGAL_DASHBOARD_DELETE_FILTER_REQUESTED');
      expect(actions[0].payload).toBeUndefined();
    });

    it('DELETES to deleteSavedFilter with its parameter and then dispatches ' +
        'LEGAL_DASHBOARD_DELETE_FILTER_FULFILLED after that completes', function(done) {
      mockAxiosCalls({
        del: {
          [deleteFiltersUrl]: Promise.resolve({})
        },
        get: {
          [dashboardSavedFiltersUrl]: Promise.resolve({ data: {} })
        }
      });

      store.dispatch(deleteFilter(filterToDelete))
          .then(() => {
            const actions = store.getActions();
            expect(actions.length).toBe(3);
            expect(actions[1].type).toBe('LEGAL_DASHBOARD_DELETE_FILTER_FULFILLED');
            expect(actions[1].payload).toBe(filterToDelete);
            done();
          });

      expect(store.getActions().length).toBe(1);
    });

    it('fetches the saved filters after deleteSavedFilter completes', function(done) {
      const getSavedFiltersResponse = { foo: 'bar' };

      mockAxiosCalls({
        del: {
          [deleteFiltersUrl]: Promise.resolve({})
        },
        get: {
          [dashboardSavedFiltersUrl]: Promise.resolve({ data: getSavedFiltersResponse })
        }
      });

      store.dispatch(deleteFilter(filterToDelete))
          .then(() => {
            expect(axios.get).toHaveBeenCalledWith(dashboardSavedFiltersUrl);

            const actions = store.getActions();
            expect(actions.length).toBe(3);
            expect(actions[1].type).toBe('LEGAL_DASHBOARD_DELETE_FILTER_FULFILLED');
            expect(actions[1].payload).toBe(filterToDelete);
            expect(actions[2].type).toBe('LEGAL_DASHBOARD_FETCH_SAVE_FILTERS_FULFILLED');
            expect(actions[2].payload).toEqual(getSavedFiltersResponse);
            done();
          });

      expect(store.getActions().length).toBe(1);
    });

    it('closes filters dropdown and delete filter modal after deleteSavedFilters completes', function(done) {
      const getSavedFiltersResponse = { foo: 'bar' };

      mockAxiosCalls({
        del: {
          [deleteFiltersUrl]: Promise.resolve({})
        },
        get: {
          [dashboardSavedFiltersUrl]: Promise.resolve({ data: getSavedFiltersResponse })
        }
      });

      store.dispatch(deleteFilter(filterToDelete))
          .then(() => {

            const actions = store.getActions();
            expect(actions.length).toBe(3);

            setTimeout(function() {
              expect(actions.length).toBe(5);
              expect(actions[3].type).toBe('LEGAL_DASHBOARD_TOGGLE_FILTERS_DROPDOWN');
              expect(actions[3].payload).toBe(false);
              expect(actions[4].type).toBe('LEGAL_DASHBOARD_HIDE_DELETE_FILTER_MODAL');

              done();
            }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
          });

      expect(store.getActions().length).toBe(1);
    });

    it('dispatches LEGAL_DASHBOARD_DELETE_FILTER_FAILED and rejects promise if deleteSavedFilter fails', (done) => {
      mockAxiosCalls({
        del: {
          [deleteFiltersUrl]: Promise.reject('error!')
        }
      });

      store.dispatch(deleteFilter(filterToDelete))
          .catch(() => {
            const actions = store.getActions();
            expect(actions.length).toBe(2);
            expect(actions[1].type).toBe('LEGAL_DASHBOARD_DELETE_FILTER_FAILED');
            expect(actions[1].payload).toEqual('error!');
            done();
          });

      expect(store.getActions().length).toBe(1);
    });

    it('rejects if fetching the saved filters fails but does not dispatch LEGAL_DASHBOARD_DELETE_FILTER_FAILED',
        function(done) {
          mockAxiosCalls({
            del: {
              [deleteFiltersUrl]: Promise.resolve({})
            },
            get: {
              [dashboardSavedFiltersUrl]: Promise.reject({ status: 403 })
            }
          });

          store.dispatch(deleteFilter(filterToDelete))
              .catch(() => {
                const actions = store.getActions();
                expect(actions.length).toBe(3);
                expect(actions[2].type).toBe('LEGAL_DASHBOARD_FETCH_SAVED_FILTERS_FAILED');
                expect(actions[2].payload.status).toEqual(403);
                done();
              });
        }
    );
  });
});
