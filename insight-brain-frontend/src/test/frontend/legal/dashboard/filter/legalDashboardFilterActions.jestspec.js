/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { DASHBOARD } from '../../../../../main/frontend/legal/advancedLegalConstants';
import {
  loadFilter,
  applyFilter,
  applyDefaultFilter,
  applyFilterCancelled,
} from '../../../../../main/frontend/legal/dashboard/filter/legalDashboardFilterActions';
import {
  getApplicationsUrl,
  getOrganizationsUrl,
  getApplicationTagsUrl,
  getLegalDashboardApplicationsUrl,
  getLegalDashboardComponentsUrl,
  getLegalDashboardFilters,
  getLegalDashboardSavedFilters,
} from '../../../../../main/frontend/util/CLMLocation';
import { filterToJson } from '../../../../../main/frontend/legal/dashboard/filter/legalDashboardFilterService';
import defaultFilter from '../../../../../main/frontend/legal/dashboard/filter/defaultFilter';
import * as specUtilFunctions from 'TestRoot/SpecUtil';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';

describe('legalDashboardFilterActions', function () {
  const SpecUtil = specUtilFunctions.getSpecUtil();
  let store;

  const filterJson = {
    name: '',
    basedOnFilterName: 'Test1',
    filter: 'filter data',
  };

  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  const mockGetData = {
    [getApplicationsUrl()]: Promise.resolve({ data: 'applications data' }),
    [getOrganizationsUrl()]: Promise.resolve({ data: 'organizations data' }),
    [getApplicationTagsUrl()]: Promise.resolve({ data: 'tag data' }),
    [getLegalDashboardFilters()]: Promise.resolve({ data: filterJson }),
    [getLegalDashboardSavedFilters()]: Promise.resolve({ data: 'saved filters data' }),
  };

  const initialState = {
    router: {
      currentState: {
        data: {
          activeTab: 'applications',
        },
      },
    },
    stages: {
      dashboard: {
        stageTypes: [{ stageTypeId: 1, stageName: 'stage' }],
      },
    },
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
      currentTab: 'applications',
      components: { sortFields: ['-score'] },
      applications: { sortField: null },
    },
  };

  function getRequestPayload(activeTab) {
    const payload = {
      applicationIds: [],
      organizationIds: [],
      stageTypeIds: [],
      tagIds: [],
      reviewStatus: [],
      page: 1,
      pageSize: DASHBOARD[activeTab].itemsPerPage * DASHBOARD[activeTab].pagesToFill,
      order: null,
    };

    return activeTab === 'components' ? { ...payload, componentName: undefined, order: undefined } : payload;
  }

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

        store.dispatch(loadFilter()).catch(() => {
          expect(axios.get).toHaveBeenCalledWith(getApplicationsUrl());
          expect(axios.get).toHaveBeenCalledWith(getOrganizationsUrl());
          expect(axios.get).toHaveBeenCalledWith(getApplicationTagsUrl());
          expect(axios.get).toHaveBeenCalledWith(getLegalDashboardFilters());

          expect(store.getActions().length).toBe(5);

          expect(store.getActions()[3]).toEqual({
            type: 'LEGAL_DASHBOARD_FETCH_SAVE_FILTERS_FULFILLED',
            payload: 'saved filters data',
          });

          expect(store.getActions()[4]).toEqual({
            type: 'LEGAL_DASHBOARD_LOAD_FILTER_FAILED',
            payload: 'failed to get applications data',
          });
          done();
        });

        expect(store.getActions()[0]).toEqual({
          type: 'LEGAL_DASHBOARD_LOAD_FILTER_REQUESTED',
        });
      });

      it('will not fire getApplicationTagsUrl() when isSbomManager is true', function (done) {
        jest.spyOn(routerSelectors, 'selectIsSbomManager').mockImplementation(() => true);
        mockAxiosCalls({
          get: {
            ...mockGetData,
            [getApplicationsUrl()]: () => Promise.reject('failed to get applications data'),
          },
        });

        store = SpecUtil.mockReduxStore(initialState);

        store.dispatch(loadFilter()).catch(() => {
          expect(axios.get).toHaveBeenCalledWith(getApplicationsUrl());
          expect(axios.get).toHaveBeenCalledWith(getOrganizationsUrl());
          expect(axios.get).not.toHaveBeenCalledWith(getApplicationTagsUrl());
          expect(axios.get).toHaveBeenCalledWith(getLegalDashboardFilters());

          expect(store.getActions().length).toBe(5);

          expect(store.getActions()[3]).toEqual({
            type: 'LEGAL_DASHBOARD_FETCH_SAVE_FILTERS_FULFILLED',
            payload: 'saved filters data',
          });

          expect(store.getActions()[4]).toEqual({
            type: 'LEGAL_DASHBOARD_LOAD_FILTER_FAILED',
            payload: 'failed to get applications data',
          });
          done();
        });

        expect(store.getActions()[0]).toEqual({
          type: 'LEGAL_DASHBOARD_LOAD_FILTER_REQUESTED',
        });
      });
    });
  });

  describe('applyFilter for applications tab', function () {
    const expectedFailAction = {
      type: 'LEGAL_DASHBOARD_APPLY_FILTER_FAILED',
      payload: 'Error 403',
    };

    const action = applyFilter('test filters', 'test filter name');

    testSuccessfullyUpdatesFiltersAndLoadsResults(action, 'test filters', 'test filter name');
    testSuccessfullyUpdatesFiltersButFailsToLoadsResults(action, 'test filters', 'test filter name');
    testFailedToUpdateFilter(action, 'test filters', 'test filter name', expectedFailAction);
  });

  describe('applyFilter for components tab', function () {
    const expectedFailAction = {
      type: 'LEGAL_DASHBOARD_APPLY_FILTER_FAILED',
      payload: 'Error 403',
    };

    const action = applyFilter('test filters', 'test filter name');

    testSuccessfullyUpdatesFiltersAndLoadsResults(action, 'test filters', 'test filter name', 'components');
    testSuccessfullyUpdatesFiltersButFailsToLoadsResults(action, 'test filters', 'test filter name', 'components');
    testFailedToUpdateFilter(action, 'test filters', 'test filter name', expectedFailAction, 'components');
  });

  describe('applyDefaultFilter for applications tab', function () {
    const filter = filterToJson(defaultFilter);

    const expectedFailAction = {
      type: 'LEGAL_DASHBOARD_APPLY_SAVED_FILTER_FAILED',
      payload: 'Default filter',
    };

    const action = applyDefaultFilter();

    testSuccessfullyUpdatesFiltersAndLoadsResults(action, filter, null);
    testSuccessfullyUpdatesFiltersButFailsToLoadsResults(action, filter, null);
    testFailedToUpdateFilter(action, filter, null, expectedFailAction);
  });

  describe('applyDefaultFilter for components tab', function () {
    const filter = filterToJson(defaultFilter);

    const expectedFailAction = {
      type: 'LEGAL_DASHBOARD_APPLY_SAVED_FILTER_FAILED',
      payload: 'Default filter',
    };

    const action = applyDefaultFilter();

    testSuccessfullyUpdatesFiltersAndLoadsResults(action, filter, null, 'components');
    testSuccessfullyUpdatesFiltersButFailsToLoadsResults(action, filter, null, 'components');
    testFailedToUpdateFilter(action, filter, null, expectedFailAction, 'components');
  });

  describe('applyFilterCancelled', function () {
    it('dispatches an LEGAL_DASHBOARD_APPLY_FILTER_CANCELLED function', () => {
      store = SpecUtil.mockReduxStore(initialState);

      store.dispatch(applyFilterCancelled());
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'LEGAL_DASHBOARD_APPLY_FILTER_CANCELLED',
      });
    });
  });

  function testFailedToUpdateFilter(
    action,
    expectedFilter,
    expectedFilterName,
    expectedFailAction,
    activeTab = 'applications'
  ) {
    const endpointUrl = getEndpointUrl(activeTab);
    it(`dispatches ${expectedFailAction.type} if failed to update filters`, function (done) {
      mockAxiosCalls({
        put: {
          [getLegalDashboardFilters()]: () => Promise.reject({ status: 403 }),
        },
        post: {
          [endpointUrl]: Promise.resolve({
            data: { dashboardResults: 'results' },
          }),
        },
      });

      initialState.router.currentState.data.activeTab = activeTab;
      store = SpecUtil.mockReduxStore(initialState);

      store.dispatch(action).catch(() => {
        expect(axios.put).toHaveBeenCalledWith(getLegalDashboardFilters(), {
          filter: expectedFilter,
          basedOnFilterName: expectedFilterName,
          type: 'ADVANCED_LEGAL_PACK_DASHBOARD',
        });
        expect(axios.post).not.toHaveBeenCalledWith(endpointUrl);

        expect(store.getActions().length).toBe(2);

        expect(store.getActions()[1]).toEqual(expectedFailAction);

        done();
      });

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'LEGAL_DASHBOARD_APPLY_FILTER_REQUESTED',
      });
    });
  }

  function getEndpointUrl(activeTab) {
    return activeTab === 'components' ? getLegalDashboardComponentsUrl() : getLegalDashboardApplicationsUrl();
  }

  function testSuccessfullyUpdatesFiltersAndLoadsResults(
    action,
    expectedFilter,
    expectedFilterName,
    activeTab = 'applications'
  ) {
    const endpointUrl = getEndpointUrl(activeTab);
    it('updates filters and loads results', function (done) {
      mockAxiosCalls({
        put: {
          [getLegalDashboardFilters()]: Promise.resolve({
            data: { filter: 'update filters response' },
          }),
        },
        post: {
          [endpointUrl]: Promise.resolve({
            data: { [activeTab]: { results: [] } },
          }),
        },
      });

      initialState.router.currentState.data.activeTab = activeTab;
      store = SpecUtil.mockReduxStore(initialState);

      store.dispatch(action).then(() => {
        expect(axios.post).toHaveBeenCalledWith(endpointUrl, getRequestPayload(activeTab));
        expect(axios.put).toHaveBeenCalledWith(getLegalDashboardFilters(), {
          filter: expectedFilter,
          basedOnFilterName: expectedFilterName,
          type: 'ADVANCED_LEGAL_PACK_DASHBOARD',
        });

        expect(store.getActions().length).toBe(4);

        expect(store.getActions()[1]).toEqual({
          type: 'LEGAL_DASHBOARD_APPLY_FILTER_FULFILLED',
          payload: {
            filter: 'update filters response',
            basedOnFilterName: expectedFilterName,
          },
        });
        expect(store.getActions()[2]).toEqual({
          type: 'LEGAL_DASHBOARD_LOAD_RESULTS_REQUESTED',
          payload: activeTab,
        });

        expect(store.getActions()[3]).toEqual({
          type: 'LEGAL_DASHBOARD_LOAD_RESULTS_FULFILLED',
          payload: {
            resultsType: activeTab,
            results: { [activeTab]: { results: [] } },
          },
        });

        done();
      });

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'LEGAL_DASHBOARD_APPLY_FILTER_REQUESTED',
      });
    });
  }

  function testSuccessfullyUpdatesFiltersButFailsToLoadsResults(
    action,
    expectedFilter,
    expectedFilterName,
    activeTab = 'applications'
  ) {
    const endpointUrl = getEndpointUrl(activeTab);
    it('returns rejected promise and does not dispatch apply filter failed action if failed to load results', function (done) {
      mockAxiosCalls({
        put: {
          [getLegalDashboardFilters()]: Promise.resolve({
            data: { filter: 'update filters response' },
          }),
        },
        post: {
          [endpointUrl]: () => Promise.reject('load results error'),
        },
      });

      initialState.router.currentState.data.activeTab = activeTab;
      store = SpecUtil.mockReduxStore(initialState);

      store.dispatch(action).catch(() => {
        expect(axios.post).toHaveBeenCalledWith(endpointUrl, getRequestPayload(activeTab));
        expect(axios.put).toHaveBeenCalledWith(getLegalDashboardFilters(), {
          filter: expectedFilter,
          basedOnFilterName: expectedFilterName,
          type: 'ADVANCED_LEGAL_PACK_DASHBOARD',
        });

        expect(store.getActions().length).toBe(4);

        expect(store.getActions()[1]).toEqual({
          type: 'LEGAL_DASHBOARD_APPLY_FILTER_FULFILLED',
          payload: {
            filter: 'update filters response',
            basedOnFilterName: expectedFilterName,
          },
        });

        expect(store.getActions()[2]).toEqual({
          type: 'LEGAL_DASHBOARD_LOAD_RESULTS_REQUESTED',
          payload: activeTab,
        });

        expect(store.getActions()[3]).toEqual({
          type: 'LEGAL_DASHBOARD_LOAD_RESULTS_FAILED',
          payload: {
            error: 'load results error',
            resultsType: activeTab,
          },
        });

        done();
      });

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'LEGAL_DASHBOARD_APPLY_FILTER_REQUESTED',
      });
    });
  }
});
