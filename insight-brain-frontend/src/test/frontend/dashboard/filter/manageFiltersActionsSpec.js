/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import { getDashboardSavedFilters } from '../../../../main/frontend/util/CLMLocation';

describe('manageFilterActions', function() {
  let saveFilter, fetchSavedFilters, deleteSpecifiedFilters, resetDeleteFiltersStatus;

  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);

  const filterJsonSpy = jasmine.createSpy('filterJson'),
      deleteSavedFiltersSpy = jasmine.createSpy('deleteSavedFilters');

  beforeEach(function() {
    const module = require('inject-loader!../../../../main/frontend/dashboard/filter/manageFiltersActions')({
      './dashboardFilterService': {
        filterToJson: filterJsonSpy,
        deleteSavedFilters: deleteSavedFiltersSpy
      }
    });
    deleteSpecifiedFilters = module.deleteSpecifiedFilters;
    fetchSavedFilters = module.fetchSavedFilters;
    resetDeleteFiltersStatus = module.resetDeleteFiltersStatus;
    saveFilter = module.saveFilter;
  });

  describe('fetchSavedFilters', function() {
    let store;

    beforeEach(function() {
      store = SpecUtil.mockReduxStore();
    });

    it('gets the saved filters from the backend and then dispatches FETCH_SAVED_FILTERS_FULFILLED', function(done) {
      mockAxiosCalls({
        get: {
          [getDashboardSavedFilters()]: Promise.resolve({data: [{ name: 'foo' }]})
        }
      });

      store.dispatch(fetchSavedFilters())
          .then(() => {
            expect(axios.get).toHaveBeenCalledWith(getDashboardSavedFilters());

            expect(store.getActions().length).toBe(1);

            expect(store.getActions()[0]).toEqual({
              type: 'FETCH_SAVED_FILTERS_FULFILLED',
              payload: [{ name: 'foo' }]
            });

            done();
          });
    });

    it('dispatches FETCH_SAVED_FILTERS_FAILED when the backend call fails', function(done) {
      mockAxiosCalls({
        get: {
          [getDashboardSavedFilters()]: Promise.reject({ status: 403 })
        }
      });

      store.dispatch(fetchSavedFilters())
          .catch(() => {
            expect(axios.get).toHaveBeenCalledWith(getDashboardSavedFilters());

            expect(store.getActions().length).toBe(1);

            expect(store.getActions()[0]).toEqual({
              type: 'FETCH_SAVED_FILTERS_FAILED',
              payload: {status: 403}
            });

            done();
          });
    });
  });

  describe('saveFilter', function() {
    let store;
    const initialState = {
          dashboardFilter: {
            appliedFilter: {
              policyThreatLevels: [1, 2]
            },
            filterJson: { applications: ['1234'] }
          }
        },
        expectedPUTBody = {
          name: 'foo',
          filter: { applicationFilters: ['1234'] }
        },
        dashboardSavedFiltersUrl = getDashboardSavedFilters();

    beforeEach(function() {
      store = SpecUtil.mockReduxStore(initialState);
      filterJsonSpy.and.returnValue(expectedPUTBody.filter);
    });

    it('immediately sends a SAVE_FILTER_REQUESTED action', function() {
      mockAxiosCalls({
        get: {
          [dashboardSavedFiltersUrl]: Promise.resolve({ data: {} })
        },
        put: {
          [dashboardSavedFiltersUrl]: Promise.resolve({ data: {} })
        }
      });

      store.dispatch(saveFilter('foo'));

      var actions = store.getActions();

      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('SAVE_FILTER_REQUESTED');
      expect(actions[0].payload).toBeUndefined();
    });

    it('PUTs the filter to the server and then dispatches SAVE_FILTERS_FULFILLED and fetches the saved filters',
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

          store.dispatch(saveFilter('foo'))
              .then(() => {
                actions = store.getActions();
                expect(axios.get).toHaveBeenCalledWith(dashboardSavedFiltersUrl);
                expect(axios.put).toHaveBeenCalledWith(dashboardSavedFiltersUrl, expectedPUTBody);
                expect(actions.length).toBe(3);
                expect(actions[1].type).toBe('SAVE_FILTER_FULFILLED');
                expect(actions[1].payload).toEqual(putSavedFilterResponse);
                expect(actions[2].type).toBe('FETCH_SAVED_FILTERS_FULFILLED');
                expect(actions[2].payload).toEqual(getSavedFiltersResponse);
                done();
              });

          let actions = store.getActions();
          expect(actions.length).toBe(1);
        }
    );

    it('dispatches SAVE_FILTER_FAILED and does not fetch the saved filters if the PUT fails', function(done) {
      mockAxiosCalls({
        put: {
          [dashboardSavedFiltersUrl]: Promise.reject({ status: 403 })
        }
      });

      store.dispatch(saveFilter('foo'))
          .catch(() => {
            const actions = store.getActions();
            expect(axios.put).toHaveBeenCalledWith(dashboardSavedFiltersUrl, expectedPUTBody);
            expect(actions.length).toBe(2);
            expect(actions[1].type).toBe('SAVE_FILTER_FAILED');
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

      store.dispatch(saveFilter('foo'))
          .catch(() => {
            const actions = store.getActions();
            expect(axios.get).toHaveBeenCalledWith(dashboardSavedFiltersUrl);
            expect(axios.put).toHaveBeenCalledWith(dashboardSavedFiltersUrl, expectedPUTBody);
            expect(actions.length).toBe(3);
            expect(actions[2].type).toBe('FETCH_SAVED_FILTERS_FAILED');
            expect(actions[2].payload.status).toBe(403);
            done();
          });

      expect(store.getActions().length).toBe(1);
    });
  });

  describe('deleteSpecifiedFilters', function() {
    let store;

    const initialState = { manageFilters: { savedFilters: null } },
        dashboardSavedFiltersUrl = getDashboardSavedFilters(),
        filtersToDelete = ['foo', 'bar'];

    beforeEach(() => {
      store = SpecUtil.mockReduxStore(initialState);
    });

    it('immediately dispatches a DELETE_SPECIFIED_FILTERS_REQUESTED action with no payload', function() {
      mockAxiosCalls({
        get: {
          [dashboardSavedFiltersUrl]: Promise.resolve({ data: {} })
        }
      });
      deleteSavedFiltersSpy.and.returnValue(Promise.resolve({ data: {} }));

      store.dispatch(deleteSpecifiedFilters(filtersToDelete));

      const actions = store.getActions();

      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('DELETE_SPECIFIED_FILTERS_REQUESTED');
      expect(actions[0].payload).toBeUndefined();
    });

    it('POSTS to deleteSavedFilters with its parameter and then dispatches ' +
        'DELETE_SPECIFIED_FILTERS_FULFILLED after that completes', function(done) {
      mockAxiosCalls({
        get: {
          [dashboardSavedFiltersUrl]: Promise.resolve({ data: {} })
        }
      });

      deleteSavedFiltersSpy.and.returnValue(Promise.resolve({}));

      store.dispatch(deleteSpecifiedFilters(filtersToDelete))
          .then(() => {
            const actions = store.getActions();
            expect(actions.length).toBe(3);
            expect(actions[1].type).toBe('DELETE_SPECIFIED_FILTERS_FULFILLED');
            expect(actions[1].payload).toBe(filtersToDelete);
            done();
          });

      expect(store.getActions().length).toBe(1);
    });

    it('fetches the saved filters after deleteSavedFilters completes', function(done) {
      const getSavedFiltersResponse = { foo: 'bar' };

      mockAxiosCalls({
        get: {
          [dashboardSavedFiltersUrl]: Promise.resolve({ data: getSavedFiltersResponse })
        }
      });
      deleteSavedFiltersSpy.and.returnValue(Promise.resolve({}));

      store.dispatch(deleteSpecifiedFilters(filtersToDelete))
          .then(() => {
            expect(axios.get).toHaveBeenCalledWith(dashboardSavedFiltersUrl);

            const actions = store.getActions();
            expect(actions.length).toBe(3);
            expect(actions[1].type).toBe('DELETE_SPECIFIED_FILTERS_FULFILLED');
            expect(actions[1].payload).toBe(filtersToDelete);
            expect(actions[2].type).toBe('FETCH_SAVED_FILTERS_FULFILLED');
            expect(actions[2].payload).toEqual(getSavedFiltersResponse);
            done();
          });

      expect(store.getActions().length).toBe(1);
    });

    it('dispatches DELETE_SPECIFIED_FILTERS_FAILED and rejects the promise if deleteSavedFilters fails', function() {
      deleteSavedFiltersSpy.and.returnValue(Promise.reject('error!'));

      store.dispatch(deleteSpecifiedFilters())
          .catch(() => {
            const actions = store.getActions();
            expect(actions.length).toBe(2);
            expect(actions[1].type).toBe('DELETE_SPECIFIED_FILTERS_FAILED');
            expect(actions[1].payload).toEqual('error!');
          });

      expect(store.getActions().length).toBe(1);
    });

    it('rejects if fetching the saved filters fails but does not dispatch DELETE_SPECIFIED_FILTERS_FAILED',
        function(done) {
          deleteSavedFiltersSpy.and.returnValue(Promise.resolve({}));
          mockAxiosCalls({
            get: {
              [dashboardSavedFiltersUrl]: Promise.reject({ status: 403 })
            }
          });

          store.dispatch(deleteSpecifiedFilters(filtersToDelete))
              .catch(() => {
                const actions = store.getActions();
                expect(actions.length).toBe(3);
                expect(actions[2].type).toBe('FETCH_SAVED_FILTERS_FAILED');
                expect(actions[2].payload.status).toEqual(403);
                done();
              });
        }
    );
  });

  describe('resetDeleteFiltersStatus', function() {
    it('immediately sends a RESET_DELETE_FILTERS_STATUS action with no payload', function() {
      const payload = {},
          initialState = { manageFilters: { savedFilters: null } },
          mockReduxStore = SpecUtil.mockReduxStore(initialState);

      mockReduxStore.dispatch(resetDeleteFiltersStatus(payload));

      var actions = mockReduxStore.getActions();

      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('RESET_DELETE_FILTERS_STATUS');
      expect(actions[0].payload).toBeUndefined();
    });
  });
});
