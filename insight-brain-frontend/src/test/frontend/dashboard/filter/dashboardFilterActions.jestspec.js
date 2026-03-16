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
  applyDefaultFilter,
  applyFilterCancelled,
} from '../../../../main/frontend/dashboard/filter/dashboardFilterActions';
import {
  getApplicationsUrl,
  getOrganizationsUrl,
  getApplicationTagsUrl,
  getDashboardFilters,
  getDashboardSavedFilters,
  getNewestRisksUrl,
  getRepositoriesUrl,
  getPolicyWaiverReasonsUrl,
} from '../../../../main/frontend/util/CLMLocation';

import defaultFilter from '../../../../main/frontend/dashboard/filter/defaultFilter';
import { filterToJson } from '../../../../main/frontend/dashboard/filter/dashboardFilterService';

import 'TestRoot/SpecUtil';

describe('dashboardFilterActions', function () {
  let store;

  const filterJson = {
    name: '',
    basedOnFilterName: 'Test1',
    filter: 'filter data',
    needsAcknowledgement: false,
  };

  const repositoriesMockResponse = {
    data: {
      repositories: [
        { repository: { id: 'id-foo', publicId: 'foo' } },
        { repository: { id: 'id-bar', publicId: 'bar' } },
        { repository: { id: 'id-foobar', publicId: 'foobar' } },
      ],
    },
  };

  const policyWaiverReasonsResponse = [{ id: 'some-id', type: 'system', reasonText: 'some-text' }];

  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  const mockGetData = {
    [getApplicationsUrl()]: Promise.resolve({ data: 'applications data' }),
    [getOrganizationsUrl()]: Promise.resolve({ data: 'organizations data' }),
    [getApplicationTagsUrl()]: Promise.resolve({ data: 'tag data' }),
    [getRepositoriesUrl()]: Promise.resolve(repositoriesMockResponse),
    [getDashboardFilters()]: Promise.resolve({ data: filterJson }),
    [getDashboardSavedFilters()]: Promise.resolve({
      data: 'saved filters data',
    }),
    [getPolicyWaiverReasonsUrl()]: Promise.resolve({ data: policyWaiverReasonsResponse }),
  };

  const initialState = {
    stages: {
      dashboard: {
        stageTypes: [{ stageTypeId: 1, stageName: 'stage' }],
      },
    },
    dashboardFilter: {
      appliedFilter: 'current filters',
    },
    dashboard: {
      currentTab: 'violations',
      violations: { sortFields: ['-time', '-threatLevel'] },
      components: { sortFields: ['-score'] },
      applications: { sortFields: ['-totalApplicationRisk.totalRisk'] },
    },
    waivers: { waiverReasons: { data: [] } },
  };

  const expectedRisksPayload = {
    orderBy: '-undefined,-THREAT_LEVEL',
    organizationIds: undefined,
    applicationIds: undefined,
    repositoryIds: undefined,
    stageIds: undefined,
    tagIds: undefined,
    policyViolationStates: undefined,
    maxDaysOld: undefined,
    policyThreatLevelRange: undefined,
    expirationDate: undefined,
    policyWaiverReasonIds: undefined,
    pageSize: 100,
    page: 0,
  };

  describe('loadFilter', function () {
    describe('when failed fetching filter data', function () {
      it('fires loadFiltersFailed action', function (done) {
        mockAxiosCalls({
          get: {
            ...mockGetData,
            [getApplicationsUrl()]: () => Promise.reject('failed to get applications data'),
          },
        });

        store = SpecUtil.mockReduxStore(initialState);

        store.dispatch(loadFilter()).then(() => {
          expect(axios.get).toHaveBeenCalledWith(getApplicationsUrl());
          expect(axios.get).toHaveBeenCalledWith(getOrganizationsUrl());
          expect(axios.get).toHaveBeenCalledWith(getApplicationTagsUrl());
          expect(axios.get).toHaveBeenCalledWith(getDashboardFilters());
          expect(axios.get).toHaveBeenCalledWith(getDashboardSavedFilters());

          const actions = store.getActions();
          expect(actions.length).toBe(6);

          expect(actions[0]).toEqual({
            type: 'LOAD_FILTER_REQUESTED',
          });

          expect(actions[4]).toEqual({
            type: 'FETCH_SAVED_FILTERS_FULFILLED',
            payload: 'saved filters data',
          });

          expect(actions[5]).toEqual({
            type: 'LOAD_FILTER_FAILED',
            payload: 'failed to get applications data',
          });
          done();
        });
      });
    });

    describe('when successful fetching filter data', function () {
      it('fetchAvailableFilterOptionsFulfilled and fetchCurrentFilterFulfilled called', function (done) {
        mockAxiosCalls({
          get: {
            ...mockGetData,
            [getDashboardFilters()]: Promise.resolve({ data: filterJson }),
          },
        });

        store = SpecUtil.mockReduxStore(initialState);

        store.dispatch(loadFilter()).then(() => {
          expect(axios.get).toHaveBeenCalledWith(getApplicationsUrl());
          expect(axios.get).toHaveBeenCalledWith(getOrganizationsUrl());
          expect(axios.get).toHaveBeenCalledWith(getApplicationTagsUrl());
          expect(axios.get).toHaveBeenCalledWith(getRepositoriesUrl());
          expect(axios.get).toHaveBeenCalledWith(getDashboardFilters());
          expect(axios.get).toHaveBeenCalledWith(getDashboardSavedFilters());
          expect(axios.get).toHaveBeenCalledWith(getPolicyWaiverReasonsUrl());

          expect(store.getActions().length).toBe(8);

          expect(store.getActions()[4]).toEqual({
            type: 'FETCH_SAVED_FILTERS_FULFILLED',
            payload: 'saved filters data',
          });

          expect(store.getActions()[6]).toEqual({
            type: 'FETCH_AVAILABLE_FILTER_OPTIONS_FULFILLED',
            payload: {
              organizations: 'organizations data',
              applications: 'applications data',
              stages: initialState.stages.dashboard.stageTypes,
              categories: 'tag data',
              repositories: repositoriesMockResponse.data.repositories,
            },
          });

          expect(store.getActions()[7]).toEqual({
            type: 'FETCH_CURRENT_FILTER_FULFILLED',
            payload: {
              name: '',
              basedOnFilterName: 'Test1',
              filter: 'filter data',
              needsAcknowledgement: false,
            },
          });
          done();
        });

        expect(store.getActions().length).toBe(4);
        expect(store.getActions()[0]).toEqual({
          type: 'LOAD_FILTER_REQUESTED',
        });
      });
    });
  });

  describe('applyFilter', function () {
    const expectedFailAction = {
      type: 'APPLY_FILTER_FAILED',
      payload: 'Error 403',
    };

    const action = applyFilter('test filters', 'test filter name');

    testSuccessfullyUpdatesFiltersAndLoadsResults(action, 'test filters', 'test filter name');
    testSuccessfullyUpdatesFiltersButFailsToLoadsResults(action, 'test filters', 'test filter name');
    testFailedToUpdateFilter(action, 'test filters', 'test filter name', expectedFailAction);
  });

  describe('applySavedFilter', function () {
    const savedFilter = {
      filter: 'test filters',
      name: 'test filter name',
    };

    const expectedFailAction = {
      type: 'APPLY_SAVED_FILTER_FAILED',
      payload: 'test filter name Error: Error 403',
    };

    const action = applySavedFilter(savedFilter);

    testSuccessfullyUpdatesFiltersAndLoadsResults(action, 'test filters', 'test filter name');
    testSuccessfullyUpdatesFiltersButFailsToLoadsResults(action, 'test filters', 'test filter name');
    testFailedToUpdateFilter(action, 'test filters', 'test filter name', expectedFailAction);
  });

  describe('applyDefaultFilter', function () {
    const filter = filterToJson(defaultFilter);

    const expectedFailAction = {
      type: 'APPLY_SAVED_FILTER_FAILED',
      payload: 'Default Filter Error: Error 403',
    };

    const action = applyDefaultFilter();

    testSuccessfullyUpdatesFiltersAndLoadsResults(action, filter, null);
    testSuccessfullyUpdatesFiltersButFailsToLoadsResults(action, filter, null);
    testFailedToUpdateFilter(action, filter, null, expectedFailAction);
  });

  describe('applyFilterCancelled', function () {
    it('dispatches an APPLY_FILTER_CANCELLED function', () => {
      store = SpecUtil.mockReduxStore(initialState);

      store.dispatch(applyFilterCancelled());
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({ type: 'APPLY_FILTER_CANCELLED' });
    });
  });

  function testFailedToUpdateFilter(action, expectedFilter, expectedFilterName, expectedFailAction) {
    it(`dispatches ${expectedFailAction.type} if failed to update filters`, function (done) {
      mockAxiosCalls({
        put: {
          [getDashboardFilters()]: () => Promise.reject({ status: 403 }),
        },
        post: {},
      });

      store = SpecUtil.mockReduxStore(initialState);

      store.dispatch(action).then(() => {
        expect(axios.put).toHaveBeenCalledWith(getDashboardFilters(), {
          filter: expectedFilter,
          basedOnFilterName: expectedFilterName,
        });
        expect(axios.post).not.toHaveBeenCalledWith(getNewestRisksUrl());

        expect(store.getActions().length).toBe(2);

        expect(store.getActions()[1]).toEqual(expectedFailAction);

        done();
      });

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({ type: 'APPLY_FILTER_REQUESTED' });
    });
  }

  function testSuccessfullyUpdatesFiltersAndLoadsResults(action, expectedFilter, expectedFilterName) {
    it('updates filters and loads results', function (done) {
      mockAxiosCalls({
        put: {
          [getDashboardFilters()]: Promise.resolve({
            data: 'update filters response',
          }),
        },
        post: {
          [getNewestRisksUrl()]: Promise.resolve({
            data: {
              dashboardResults: 'results',
              hasNextPage: true,
              classyBrew: 'classyBrew',
            },
          }),
        },
      });

      store = SpecUtil.mockReduxStore(initialState);

      store.dispatch(action).then(() => {
        expect(axios.post).toHaveBeenCalledWith(getNewestRisksUrl(), expectedRisksPayload);
        expect(axios.put).toHaveBeenCalledWith(getDashboardFilters(), {
          filter: expectedFilter,
          basedOnFilterName: expectedFilterName,
        });

        expect(store.getActions().length).toBe(6);

        expect(store.getActions()[1]).toEqual({ type: 'RESET_ALL_TABS' });

        expect(store.getActions()[2]).toEqual({
          type: 'APPLY_FILTER_FULFILLED',
          payload: {
            filter: 'update filters response',
            basedOnFilterName: expectedFilterName,
          },
        });

        expect(store.getActions()[3]).toEqual({
          type: 'DASHBOARD_SET_PAGE',
          payload: { resultsType: 'violations', page: 0 },
        });

        expect(store.getActions()[4]).toEqual({
          type: 'LOAD_RESULTS_REQUESTED',
          payload: 'violations',
        });

        expect(store.getActions()[5]).toEqual({
          type: 'LOAD_RESULTS_FULFILLED',
          payload: {
            resultsType: 'violations',
            results: 'results',
            hasNextPage: true,
            classyBrew: undefined,
          },
        });

        done();
      });

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({ type: 'APPLY_FILTER_REQUESTED' });
    });
  }

  function testSuccessfullyUpdatesFiltersButFailsToLoadsResults(action, expectedFilter, expectedFilterName) {
    it('does not dispatch apply filter failed action if failed to load results', function (done) {
      mockAxiosCalls({
        put: {
          [getDashboardFilters()]: Promise.resolve({
            data: 'update filters response',
          }),
        },
        post: {
          [getNewestRisksUrl()]: () => Promise.reject('load results error'),
        },
      });

      store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(action).then(() => {
        expect(axios.post).toHaveBeenCalledWith(getNewestRisksUrl(), expectedRisksPayload);
        expect(axios.put).toHaveBeenCalledWith(getDashboardFilters(), {
          filter: expectedFilter,
          basedOnFilterName: expectedFilterName,
        });

        expect(store.getActions().length).toBe(6);

        expect(store.getActions()[1]).toEqual({ type: 'RESET_ALL_TABS' });

        expect(store.getActions()[2]).toEqual({
          type: 'APPLY_FILTER_FULFILLED',
          payload: {
            filter: 'update filters response',
            basedOnFilterName: expectedFilterName,
          },
        });

        expect(store.getActions()[3]).toEqual({
          type: 'DASHBOARD_SET_PAGE',
          payload: { resultsType: 'violations', page: 0 },
        });

        expect(store.getActions()[4]).toEqual({
          type: 'LOAD_RESULTS_REQUESTED',
          payload: 'violations',
        });

        expect(store.getActions()[5]).toEqual({
          type: 'LOAD_RESULTS_FAILED',
          payload: {
            error: 'load results error',
            resultsType: 'violations',
          },
        });

        done();
      });

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({ type: 'APPLY_FILTER_REQUESTED' });
    });
  }
});
