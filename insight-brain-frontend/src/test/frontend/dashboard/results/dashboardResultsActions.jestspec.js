/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import 'TestRoot/SpecUtil';
import * as dashboardActions from 'MainRoot/dashboard/results/dashboardResultsActions';
import axios from 'axios';
import {
  loadApplicationResults,
  loadComponentResults,
  loadResults,
  loadViolationResults,
  loadWaiverResults,
  setNextComponentsPage,
  setPreviousComponentsPage,
  setNextViolationsPage,
  setPreviousViolationsPage,
  setNextApplicationsPage,
  setPreviousApplicationsPage,
  setNextWaiversPage,
  setPreviousWaiversPage,
  setPage,
  sortApplicationResults,
  sortComponentResults,
  sortViolationResults,
  sortWaiversResults,
} from 'MainRoot/dashboard/results/dashboardResultsActions';
import * as dashboardDataServices from 'MainRoot/dashboard/services/dashboard.data.service';
import { getProductFeaturesUrl } from 'MainRoot/util/CLMLocation';

// Mock the dashboard data service module
jest.mock('MainRoot/dashboard/services/dashboard.data.service', () => ({
  getNewestRisks: jest.fn(),
  getApplicationRisks: jest.fn(),
  getComponentRisks: jest.fn(),
  getWaivers: jest.fn(),
  getWaiversAndAutoWaivers: jest.fn(),
  DASHBOARD_PAGE_SIZE: 25,
}));

describe('dashboardResultsActions', () => {
  const tabs = [
    {
      resultsType: 'violations',
      get serviceMethod() {
        return dashboardDataServices.getNewestRisks;
      },
    },
    {
      resultsType: 'components',
      get serviceMethod() {
        return dashboardDataServices.getComponentRisks;
      },
    },
    {
      resultsType: 'applications',
      get serviceMethod() {
        return dashboardDataServices.getApplicationRisks;
      },
    },
    {
      resultsType: 'waivers',
      get serviceMethod() {
        return dashboardDataServices.getWaivers;
      },
    },
  ];

  const getInitialState = () => ({
    dashboardFilter: {
      appliedFilter: 'current filters',
    },
    dashboard: {
      violations: { sortFields: ['-time', '-threatLevel'] },
      components: { sortFields: ['-score'] },
      applications: { sortFields: ['-totalApplicationRisk.totalRisk'] },
      waivers: { sortFields: ['expiryTime'] },
    },
  });

  let initialState;

  beforeEach(() => {
    // Reset initialState for each test
    initialState = getInitialState();

    // Reset all mocks
    jest.clearAllMocks();

    // Mock axios.get for product features URL (called by fetchResults for all result types)
    jest.spyOn(axios, 'get').mockResolvedValue({ data: [] });
  });

  const testLoadResultsAction = (tab) => {
    describe('loadResults for ' + tab.resultsType, () => {
      it('loads results', (done) => {
        const store = SpecUtil.mockReduxStore(initialState);
        const mockResults = Promise.resolve({
          results: 'results',
          hasNextPage: true,
          classyBrew: 'classyBrew',
        });
        tab.serviceMethod.mockReturnValue(mockResults);

        store.dispatch(loadResults(tab.resultsType)).then(() => {
          expect(tab.serviceMethod).toHaveBeenCalledWith(
            initialState.dashboardFilter.appliedFilter,
            initialState.dashboard[tab.resultsType].sortFields,
            0
          );

          expect(store.getActions().length).toBe(2);
          expect(store.getActions()[1]).toEqual({
            type: 'LOAD_RESULTS_FULFILLED',
            payload: {
              resultsType: tab.resultsType,
              results: 'results',
              hasNextPage: true,
              classyBrew: 'classyBrew',
            },
          });
          done();
        });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0]).toEqual({
          type: 'LOAD_RESULTS_REQUESTED',
          payload: tab.resultsType,
        });
      });

      it('handles failure to load results', (done) => {
        const store = SpecUtil.mockReduxStore(initialState);
        const mockRejection = Promise.reject('load results error');
        tab.serviceMethod.mockImplementation(() => mockRejection);

        store.dispatch(loadResults(tab.resultsType)).then(() => {
          const actions = store.getActions();
          // Waivers tab additionally dispatches setShowLimitedFirewallAccessAlert(false) on non-403 errors
          // so the limited-access banner clears when a real load error happens.
          if (tab.resultsType === 'waivers') {
            expect(actions.length).toBe(3);
            expect(actions[1]).toEqual({
              type: 'FIREWALL_SET_SHOW_LIMITED_FIREWALL_ACCESS_ALERT',
              payload: false,
            });
            expect(actions[2]).toEqual({
              type: 'LOAD_RESULTS_FAILED',
              payload: { resultsType: 'waivers', error: 'load results error' },
            });
          } else {
            expect(actions.length).toBe(2);
            expect(actions[1]).toEqual({
              type: 'LOAD_RESULTS_FAILED',
              payload: {
                resultsType: tab.resultsType,
                error: 'load results error',
              },
            });
          }
          done();
        });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0]).toEqual({
          type: 'LOAD_RESULTS_REQUESTED',
          payload: tab.resultsType,
        });
      });
    });
  };

  tabs.forEach(testLoadResultsAction);

  describe('loadResults 403 limited-access handling', () => {
    const make403 = () => {
      const err = new Error('Forbidden');
      err.response = { status: 403 };
      return err;
    };

    it('on WAIVERS 403, dispatches setShowLimitedFirewallAccessAlert(true) and an empty fulfilled, not failed', (done) => {
      const store = SpecUtil.mockReduxStore(initialState);
      dashboardDataServices.getWaivers.mockImplementation(() => Promise.reject(make403()));

      store.dispatch(loadResults('waivers')).then(() => {
        const types = store.getActions().map((a) => a.type);
        expect(types).toContain('FIREWALL_SET_SHOW_LIMITED_FIREWALL_ACCESS_ALERT');
        const flag = store.getActions().find((a) => a.type === 'FIREWALL_SET_SHOW_LIMITED_FIREWALL_ACCESS_ALERT');
        expect(flag.payload).toBe(true);
        expect(types).toContain('LOAD_RESULTS_FULFILLED');
        expect(types).not.toContain('LOAD_RESULTS_FAILED');
        done();
      });
    });

    it('on non-WAIVERS 403, behaves as before (LOAD_RESULTS_FAILED, no firewall flag)', (done) => {
      const store = SpecUtil.mockReduxStore(initialState);
      dashboardDataServices.getNewestRisks.mockImplementation(() => Promise.reject(make403()));

      store.dispatch(loadResults('violations')).then(() => {
        const types = store.getActions().map((a) => a.type);
        expect(types).toContain('LOAD_RESULTS_FAILED');
        expect(types).not.toContain('FIREWALL_SET_SHOW_LIMITED_FIREWALL_ACCESS_ALERT');
        done();
      });
    });

    it('on WAIVERS non-403 error, clears the firewall flag and dispatches LOAD_RESULTS_FAILED', (done) => {
      const store = SpecUtil.mockReduxStore(initialState);
      const err = new Error('Server error');
      err.response = { status: 500 };
      dashboardDataServices.getWaivers.mockImplementation(() => Promise.reject(err));

      store.dispatch(loadResults('waivers')).then(() => {
        const flagActions = store.getActions().filter((a) => a.type === 'FIREWALL_SET_SHOW_LIMITED_FIREWALL_ACCESS_ALERT');
        expect(flagActions).toHaveLength(1);
        expect(flagActions[0].payload).toBe(false);
        expect(store.getActions().some((a) => a.type === 'LOAD_RESULTS_FAILED')).toBe(true);
        done();
      });
    });
  });

  const testSetPageAction = (tab) => {
    describe('setPage for ' + tab.resultsType, () => {
      it('sets the page number', (done) => {
        const store = SpecUtil.mockReduxStore(initialState);
        const mockResults = Promise.resolve({
          results: 'results',
          hasNextPage: true,
          classyBrew: 'classyBrew',
        });
        tab.serviceMethod.mockReturnValue(mockResults);

        store.dispatch(setPage(tab.resultsType, 4)).then(() => {
          expect(tab.serviceMethod).toHaveBeenCalledWith(
            initialState.dashboardFilter.appliedFilter,
            initialState.dashboard[tab.resultsType].sortFields,
            0
          );

          expect(store.getActions().length).toBe(3);
          expect(store.getActions()[2]).toEqual({
            type: 'LOAD_RESULTS_FULFILLED',
            payload: {
              resultsType: tab.resultsType,
              results: 'results',
              hasNextPage: true,
              classyBrew: 'classyBrew',
            },
          });
          done();
        });

        expect(store.getActions().length).toBe(2);
        expect(store.getActions()[0]).toEqual({
          type: 'DASHBOARD_SET_PAGE',
          payload: {
            resultsType: tab.resultsType,
            page: 4,
          },
        });
        expect(store.getActions()[1]).toEqual({
          type: 'LOAD_RESULTS_REQUESTED',
          payload: tab.resultsType,
        });
      });
    });
  };

  tabs.forEach(testSetPageAction);

  describe('loadViolationResults', () => {
    let getNewestRisksSpy, getWaiverRiskySpy;

    beforeEach(() => {
      getNewestRisksSpy = jest.spyOn(dashboardDataServices, 'getNewestRisks').mockReturnValue(
        Promise.resolve({
          results: 'violationResults',
          classyBrew: 'classyBrew',
        })
      );
      getWaiverRiskySpy = jest.spyOn(dashboardDataServices, 'getWaivers').mockReturnValue(
        Promise.resolve({
          results: 'waiverResults',
          classyBrew: 'classyBrew',
        })
      );
    });

    it('calls loadResults with the violations resultsType', (done) => {
      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(loadViolationResults()).then(() => {
        expect(store.getActions()).toHaveActionsInOrder([
          {
            type: 'LOAD_RESULTS_REQUESTED',
            payload: 'violations',
          },
          {
            type: 'LOAD_RESULTS_FULFILLED',
            payload: {
              resultsType: 'violations',
              results: 'violationResults',
              classyBrew: 'classyBrew',
            },
          },
        ]);
        expect(getNewestRisksSpy).toHaveBeenCalledWith('current filters', ['-time', '-threatLevel'], 0);
        done();
      });
    });

    it('calls loadResults with the waivers resultsType', (done) => {
      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(loadWaiverResults()).then(() => {
        expect(store.getActions()).toHaveActionsInOrder([
          {
            type: 'LOAD_RESULTS_REQUESTED',
            payload: 'waivers',
          },
          {
            type: 'LOAD_RESULTS_FULFILLED',
            payload: {
              resultsType: 'waivers',
              results: 'waiverResults',
              classyBrew: 'classyBrew',
            },
          },
        ]);
        expect(getWaiverRiskySpy).toHaveBeenCalledWith('current filters', ['expiryTime'], 0);
        done();
      });
    });

    it('loads the current page number from the state', (done) => {
      const store = SpecUtil.mockReduxStore({ ...initialState, dashboard: { violations: { page: 10 } } });
      store.dispatch(loadViolationResults()).then(() => {
        expect(getNewestRisksSpy).toHaveBeenCalledWith('current filters', undefined, 10);
        done();
      });
    });

    it('loads the current page number from the state waivers', (done) => {
      const store = SpecUtil.mockReduxStore({ ...initialState, dashboard: { waivers: { page: 10 } } });
      store.dispatch(loadWaiverResults()).then(() => {
        expect(getWaiverRiskySpy).toHaveBeenCalledWith('current filters', undefined, 10);
        done();
      });
    });

    it('loads the current page number from route params as fallback', (done) => {
      const store = SpecUtil.mockReduxStore({ ...initialState, router: { currentParams: { page: 45 } } });
      store.dispatch(loadViolationResults()).then(() => {
        expect(getNewestRisksSpy).toHaveBeenCalledWith('current filters', ['-time', '-threatLevel'], 44);
        done();
      });
    });

    it('loads the current page number from route params as fallback waivers', (done) => {
      const store = SpecUtil.mockReduxStore({ ...initialState, router: { currentParams: { page: 45 } } });
      store.dispatch(loadWaiverResults()).then(() => {
        expect(getWaiverRiskySpy).toHaveBeenCalledWith('current filters', ['expiryTime'], 44);
        done();
      });
    });
  });

  describe('loadComponentResults', () => {
    let componentRisksSpy;

    beforeEach(() => {
      componentRisksSpy = jest.spyOn(dashboardDataServices, 'getComponentRisks').mockReturnValue(
        Promise.resolve({
          results: 'componentsResults',
          classyBrew: 'classyBrew',
        })
      );
    });

    it('calls loadResults with the components resultsType', (done) => {
      const store = SpecUtil.mockReduxStore(initialState);

      store.dispatch(loadComponentResults()).then(() => {
        expect(store.getActions()).toHaveActionsInOrder([
          {
            type: 'LOAD_RESULTS_REQUESTED',
            payload: 'components',
          },
          {
            type: 'LOAD_RESULTS_FULFILLED',
            payload: {
              resultsType: 'components',
              results: 'componentsResults',
              classyBrew: 'classyBrew',
            },
          },
        ]);
        expect(componentRisksSpy).toHaveBeenCalledWith('current filters', ['-score'], 0);
        done();
      });
    });

    it('loads the current page number from the state (components)', (done) => {
      const store = SpecUtil.mockReduxStore({ ...initialState, dashboard: { components: { page: 10 } } });
      store.dispatch(loadComponentResults()).then(() => {
        expect(componentRisksSpy).toHaveBeenCalledWith('current filters', undefined, 10);
        done();
      });
    });
  });

  describe('loadApplicationResults', () => {
    let getApplicationRisksSpy;

    beforeEach(() => {
      getApplicationRisksSpy = jest.spyOn(dashboardDataServices, 'getApplicationRisks').mockReturnValue(
        Promise.resolve({
          results: 'applicationResults',
          classyBrew: 'classyBrew',
        })
      );
    });

    it('calls loadResults with the applications resultsType', (done) => {
      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(loadApplicationResults()).then(() => {
        expect(store.getActions()).toHaveActionsInOrder([
          {
            type: 'LOAD_RESULTS_REQUESTED',
            payload: 'applications',
          },
          {
            type: 'LOAD_RESULTS_FULFILLED',
            payload: {
              resultsType: 'applications',
              results: 'applicationResults',
              classyBrew: 'classyBrew',
            },
          },
        ]);
        expect(getApplicationRisksSpy).toHaveBeenCalledWith('current filters', ['-totalApplicationRisk.totalRisk'], 0);
        done();
      });
    });

    it('loads the current page number from the state', (done) => {
      const store = SpecUtil.mockReduxStore({ ...initialState, dashboard: { applications: { page: 10 } } });
      store.dispatch(loadApplicationResults()).then(() => {
        expect(getApplicationRisksSpy).toHaveBeenCalledWith('current filters', undefined, 10);
        done();
      });
    });
  });

  describe('loadWaiverResults', () => {
    let getWaiversSpy, getWaiversAndAutoWaiversSpy, axiosGetSpy;

    beforeEach(() => {
      getWaiversSpy = jest.spyOn(dashboardDataServices, 'getWaivers').mockReturnValue(
        Promise.resolve({
          results: 'waiversResults',
        })
      );

      getWaiversAndAutoWaiversSpy = jest.spyOn(dashboardDataServices, 'getWaiversAndAutoWaivers').mockReturnValue(
        Promise.resolve({
          results: 'autoWaiversResults',
        })
      );

      axiosGetSpy = jest.spyOn(axios, 'get').mockReturnValue(Promise.resolve({ data: ['auto-waivers'] }));
    });

    it('calls loadResults with getWaiversAndAutoWaivers if auto-waivers feature is enabled', (done) => {
      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(loadWaiverResults()).then(() => {
        expect(axiosGetSpy).toHaveBeenCalledWith(getProductFeaturesUrl());
        expect(getWaiversAndAutoWaiversSpy).toHaveBeenCalledWith(
          initialState.dashboardFilter.appliedFilter,
          initialState.dashboard.waivers.sortFields,
          0
        );
        expect(store.getActions()).toHaveActionsInOrder([
          {
            type: 'LOAD_RESULTS_REQUESTED',
            payload: 'waivers',
          },
          {
            type: 'LOAD_RESULTS_FULFILLED',
            payload: {
              resultsType: 'waivers',
              results: 'autoWaiversResults',
            },
          },
        ]);
        done();
      });
    });

    it('calls loadResults with getWaivers if auto-waivers feature is not enabled', (done) => {
      axiosGetSpy.mockReturnValue(Promise.resolve({ data: [] }));

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(loadWaiverResults()).then(() => {
        expect(axiosGetSpy).toHaveBeenCalledWith(getProductFeaturesUrl());
        expect(getWaiversSpy).toHaveBeenCalledWith(
          initialState.dashboardFilter.appliedFilter,
          initialState.dashboard.waivers.sortFields,
          0
        );
        expect(store.getActions()).toHaveActionsInOrder([
          {
            type: 'LOAD_RESULTS_REQUESTED',
            payload: 'waivers',
          },
          {
            type: 'LOAD_RESULTS_FULFILLED',
            payload: {
              resultsType: 'waivers',
              results: 'waiversResults',
            },
          },
        ]);
        done();
      });
    });

    it('handles error from the service method', (done) => {
      const store = SpecUtil.mockReduxStore(initialState);
      getWaiversAndAutoWaiversSpy.mockReturnValue(Promise.reject('service error'));

      store.dispatch(loadWaiverResults()).then(() => {
        expect(store.getActions()).toHaveActionsInOrder([
          {
            type: 'LOAD_RESULTS_REQUESTED',
            payload: 'waivers',
          },
          {
            type: 'LOAD_RESULTS_FAILED',
            payload: {
              resultsType: 'waivers',
              error: 'service error',
            },
          },
        ]);
        done();
      });
    });
  });

  describe('setViolationsPages', () => {
    let violationsInitialState;
    beforeEach(() => {
      violationsInitialState = {
        ...initialState,
        dashboard: { ...initialState.dashboard, violations: { ...initialState.dashboard.violations, page: 2 } },
      };
    });
    const getExpectedViolationsActions = (page) => {
      return [
        {
          type: 'DASHBOARD_SET_PAGE',
          payload: { resultsType: 'violations', page: page },
        },
        {
          type: 'LOAD_RESULTS_REQUESTED',
          payload: 'violations',
        },
        {
          type: 'LOAD_RESULTS_FULFILLED',
          payload: {
            resultsType: 'violations',
            results: 'violationResults',
            hasNextPage: true,
            classyBrew: 'classyBrew',
          },
        },
      ];
    };
    beforeEach(() => {
      jest.spyOn(dashboardDataServices, 'getNewestRisks').mockReturnValue(
        Promise.resolve({
          results: 'violationResults',
          hasNextPage: true,
          classyBrew: 'classyBrew',
        })
      );
    });

    it('calls setNextPage with the violations resultsType', (done) => {
      const store = SpecUtil.mockReduxStore(violationsInitialState);
      store.dispatch(setNextViolationsPage()).then(() => {
        expect(store.getActions()).toHaveActionsInOrder(getExpectedViolationsActions(3));
        done();
      });
    });

    it('calls setPreviousPage with the violations resultsType', (done) => {
      const store = SpecUtil.mockReduxStore(violationsInitialState);
      store.dispatch(setPreviousViolationsPage()).then(() => {
        expect(store.getActions()).toHaveActionsInOrder(getExpectedViolationsActions(1));
        done();
      });
    });
  });

  describe('setWaiversPages', () => {
    let getWaiversSpy, getWaiversAndAutoWaiversSpy, axiosGetSpy, waiversInitialState;
    const getExpectedAutoWaiversActions = (page) => {
      return [
        {
          type: 'DASHBOARD_SET_PAGE',
          payload: { resultsType: 'waivers', page: page },
        },
        {
          type: 'LOAD_RESULTS_REQUESTED',
          payload: 'waivers',
        },
        {
          type: 'LOAD_RESULTS_FULFILLED',
          payload: {
            resultsType: 'waivers',
            results: 'autoWaiversResults',
            hasNextPage: true,
          },
        },
      ];
    };
    const getExpectedWaiversActions = (page) => {
      return [
        {
          type: 'DASHBOARD_SET_PAGE',
          payload: { resultsType: 'waivers', page: page },
        },
        {
          type: 'LOAD_RESULTS_REQUESTED',
          payload: 'waivers',
        },
        {
          type: 'LOAD_RESULTS_FULFILLED',
          payload: {
            resultsType: 'waivers',
            results: 'waiversResults',
            hasNextPage: true,
          },
        },
      ];
    };

    beforeEach(() => {
      waiversInitialState = {
        ...initialState,
        dashboard: { ...initialState.dashboard, waivers: { ...initialState.dashboard.waivers, page: 2 } },
      };

      getWaiversSpy = jest.spyOn(dashboardDataServices, 'getWaivers').mockReturnValue(
        Promise.resolve({
          results: 'waiversResults',
          hasNextPage: true,
        })
      );

      getWaiversAndAutoWaiversSpy = jest.spyOn(dashboardDataServices, 'getWaiversAndAutoWaivers').mockReturnValue(
        Promise.resolve({
          results: 'autoWaiversResults',
          hasNextPage: true,
        })
      );

      axiosGetSpy = jest.spyOn(axios, 'get').mockReturnValue(Promise.resolve({ data: ['auto-waivers'] }));
    });

    it('calls setNextPage with the correct resultsType and page number, and uses getWaiversAndAutoWaivers if feature is enabled', (done) => {
      const store = SpecUtil.mockReduxStore(waiversInitialState);
      store.dispatch(setNextWaiversPage()).then(() => {
        expect(axiosGetSpy).toHaveBeenCalledWith(getProductFeaturesUrl());
        expect(getWaiversAndAutoWaiversSpy).toHaveBeenCalledWith(
          waiversInitialState.dashboardFilter.appliedFilter,
          waiversInitialState.dashboard.waivers.sortFields,
          2
        );
        expect(store.getActions()).toHaveActionsInOrder(getExpectedAutoWaiversActions(3));
        done();
      });
    });

    it('calls setNextPage with the correct resultsType and page number, and uses getWaivers if feature is not enabled', (done) => {
      axiosGetSpy.mockReturnValue(Promise.resolve({ data: [] }));

      const store = SpecUtil.mockReduxStore(waiversInitialState);
      store.dispatch(setNextWaiversPage()).then(() => {
        expect(axiosGetSpy).toHaveBeenCalledWith(getProductFeaturesUrl());
        expect(getWaiversSpy).toHaveBeenCalledWith(
          waiversInitialState.dashboardFilter.appliedFilter,
          waiversInitialState.dashboard.waivers.sortFields,
          2
        );
        expect(store.getActions()).toHaveActionsInOrder(getExpectedWaiversActions(3));
        done();
      });
    });

    it('calls setPreviousPage with the correct resultsType and page number, and uses getWaiversAndAutoWaivers if feature is enabled', (done) => {
      const store = SpecUtil.mockReduxStore(waiversInitialState);
      store.dispatch(setPreviousWaiversPage()).then(() => {
        expect(axiosGetSpy).toHaveBeenCalledWith(getProductFeaturesUrl());
        expect(getWaiversAndAutoWaiversSpy).toHaveBeenCalledWith(
          waiversInitialState.dashboardFilter.appliedFilter,
          waiversInitialState.dashboard.waivers.sortFields,
          2
        );
        expect(store.getActions()).toHaveActionsInOrder(getExpectedAutoWaiversActions(1));
        done();
      });
    });

    it('calls setPreviousPage with the correct resultsType and page number, and uses getWaivers if feature is not enabled', (done) => {
      axiosGetSpy.mockReturnValue(Promise.resolve({ data: [] }));

      const store = SpecUtil.mockReduxStore(waiversInitialState);
      store.dispatch(setPreviousWaiversPage()).then(() => {
        expect(axiosGetSpy).toHaveBeenCalledWith(getProductFeaturesUrl());
        expect(getWaiversSpy).toHaveBeenCalledWith(
          waiversInitialState.dashboardFilter.appliedFilter,
          waiversInitialState.dashboard.waivers.sortFields,
          2
        );
        expect(store.getActions()).toHaveActionsInOrder(getExpectedWaiversActions(1));
        done();
      });
    });

    it('handles error from the service method', (done) => {
      const store = SpecUtil.mockReduxStore(waiversInitialState);
      getWaiversAndAutoWaiversSpy.mockReturnValue(Promise.reject('service error'));

      store.dispatch(setNextWaiversPage()).then(() => {
        expect(store.getActions()).toHaveActionsInOrder([
          {
            type: 'DASHBOARD_SET_PAGE',
            payload: { resultsType: 'waivers', page: 3 },
          },
          {
            type: 'LOAD_RESULTS_FAILED',
            payload: {
              resultsType: 'waivers',
              error: 'service error',
            },
          },
        ]);
        done();
      });
    });
  });

  describe('setApplicationsPages', () => {
    let applicationsInitialState;
    const getExpectedApplicationsActions = (page) => {
      return [
        {
          type: 'DASHBOARD_SET_PAGE',
          payload: { resultsType: 'applications', page: page },
        },
        {
          type: 'LOAD_RESULTS_REQUESTED',
          payload: 'applications',
        },
        {
          type: 'LOAD_RESULTS_FULFILLED',
          payload: {
            resultsType: 'applications',
            results: 'applicationResults',
            hasNextPage: true,
            classyBrew: 'classyBrew',
          },
        },
      ];
    };

    beforeEach(() => {
      applicationsInitialState = {
        ...initialState,
        dashboard: { ...initialState.dashboard, applications: { ...initialState.dashboard.applications, page: 2 } },
      };

      jest.spyOn(dashboardDataServices, 'getApplicationRisks').mockReturnValue(
        Promise.resolve({
          results: 'applicationResults',
          hasNextPage: true,
          classyBrew: 'classyBrew',
        })
      );
    });

    it('calls setNextPage with the applications resultsType', (done) => {
      const store = SpecUtil.mockReduxStore(applicationsInitialState);
      store.dispatch(setNextApplicationsPage()).then(() => {
        expect(store.getActions()).toHaveActionsInOrder(getExpectedApplicationsActions(3));
        done();
      });
    });

    it('calls setPreviousPage with the applications resultsType', (done) => {
      const store = SpecUtil.mockReduxStore(applicationsInitialState);
      store.dispatch(setPreviousApplicationsPage()).then(() => {
        expect(store.getActions()).toHaveActionsInOrder(getExpectedApplicationsActions(1));
        done();
      });
    });
  });

  describe('setComponentsPages', () => {
    let componentsInitialState;
    const getExpectedComponentsActions = (page) => {
      return [
        {
          type: 'DASHBOARD_SET_PAGE',
          payload: { resultsType: 'components', page: page },
        },
        {
          type: 'LOAD_RESULTS_REQUESTED',
          payload: 'components',
        },
        {
          type: 'LOAD_RESULTS_FULFILLED',
          payload: {
            resultsType: 'components',
            results: 'componentsResults',
            hasNextPage: true,
            classyBrew: 'classyBrew',
          },
        },
      ];
    };

    beforeEach(() => {
      componentsInitialState = {
        ...initialState,
        dashboard: { ...initialState.dashboard, components: { ...initialState.dashboard.components, page: 2 } },
      };

      jest.spyOn(dashboardDataServices, 'getComponentRisks').mockReturnValue(
        Promise.resolve({
          results: 'componentsResults',
          hasNextPage: true,
          classyBrew: 'classyBrew',
        })
      );
    });

    it('calls setNextPage with the components resultsType', (done) => {
      const store = SpecUtil.mockReduxStore(componentsInitialState);
      store.dispatch(setNextComponentsPage()).then(() => {
        expect(store.getActions()).toHaveActionsInOrder(getExpectedComponentsActions(3));
        done();
      });
    });

    it('calls setPreviousPage with the components resultsType', (done) => {
      const store = SpecUtil.mockReduxStore(componentsInitialState);
      store.dispatch(setPreviousComponentsPage()).then(() => {
        expect(store.getActions()).toHaveActionsInOrder(getExpectedComponentsActions(1));
        done();
      });
    });
  });

  describe('sortResults', () => {
    it('applications: updates sortFields and sorts on front end if results < 100', () => {
      initialState.dashboard.applications.results = [
        { foo: 1, bar: 2 },
        { foo: 1, bar: 1 },
        { foo: 3, bar: 3 },
      ];
      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(dashboardActions.sortApplicationResults(['-foo', 'bar']));

      expect(store.getActions().length).toBe(2);

      // this action will update sortFields in the state
      expect(store.getActions()[0]).toEqual({
        type: 'SORT_RESULTS_REQUESTED',
        payload: {
          resultsType: 'applications',
          sortFields: ['-foo', 'bar'],
        },
      });

      expect(store.getActions()[1]).toEqual({
        type: 'SORT_RESULTS_FULFILLED',
        payload: {
          resultsType: 'applications',
          results: [
            { foo: 3, bar: 3 },
            { foo: 1, bar: 1 },
            { foo: 1, bar: 2 },
          ],
        },
      });
    });

    it('applications: updates sortFields and sorts on front end if single page', () => {
      initialState.dashboard.applications.results = ['-foo', 'bar'];
      initialState.dashboard.applications.hasMultiplePages = false;
      initialState.dashboard.applications.hasNextPage = false;

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(dashboardActions.sortApplicationResults(['-foo', 'bar']));

      expect(store.getActions().length).toBe(2);

      // this action will update sortFields in the state
      expect(store.getActions()[0]).toEqual({
        type: 'SORT_RESULTS_REQUESTED',
        payload: {
          resultsType: 'applications',
          sortFields: ['-foo', 'bar'],
        },
      });

      expect(store.getActions()[1]).toEqual({
        type: 'SORT_RESULTS_FULFILLED',
        payload: {
          resultsType: 'applications',
          results: ['-foo', 'bar'],
        },
      });
    });

    it('components: updates sortFields and sorts on back end if multiple pages', (done) => {
      initialState.dashboard.components.results = ['-foo', 'bar'];
      initialState.dashboard.components.hasMultiplePages = true;
      initialState.dashboard.components.hasNextPage = true;

      const expectedSortFields = initialState.dashboard.components.sortFields;

      jest.spyOn(dashboardDataServices, 'getComponentRisks').mockReturnValue(
        Promise.resolve({
          results: 'sorted results',
          hasNextPage: true,
          hasMultiplePages: true,
          classyBrew: 'classyBrew',
        })
      );

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(dashboardActions.sortComponentResults(initialState.dashboard.components.results)).then(() => {
        expect(dashboardDataServices.getComponentRisks).toHaveBeenCalledWith('current filters', expectedSortFields, 0);
        expect(store.getActions().length).toBe(3);
        expect(store.getActions()[2]).toEqual({
          type: 'LOAD_RESULTS_FULFILLED',
          payload: {
            resultsType: 'components',
            results: 'sorted results',
            hasNextPage: true,
            classyBrew: 'classyBrew',
          },
        });
        done();
      });

      // this action will update sortFields in the state
      expect(store.getActions()[0]).toEqual({
        type: 'SORT_RESULTS_REQUESTED',
        payload: {
          resultsType: 'components',
          sortFields: ['-foo', 'bar'],
        },
      });

      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_RESULTS_REQUESTED',
        payload: 'components',
      });
    });

    it('components: updates sortFields and sorts on back end if results is not defined', (done) => {
      initialState.dashboard.components.results = null;
      const expectedSortFields = initialState.dashboard.components.sortFields;

      jest.spyOn(dashboardDataServices, 'getComponentRisks').mockReturnValue(
        Promise.resolve({
          results: 'sorted results',
          hasNextPage: true,
          classyBrew: 'classyBrew',
        })
      );

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(dashboardActions.sortComponentResults(['-foo', 'bar'])).then(() => {
        expect(dashboardDataServices.getComponentRisks).toHaveBeenCalledWith('current filters', expectedSortFields, 0);
        expect(store.getActions().length).toBe(3);
        expect(store.getActions()[2]).toEqual({
          type: 'LOAD_RESULTS_FULFILLED',
          payload: {
            resultsType: 'components',
            results: 'sorted results',
            hasNextPage: true,
            classyBrew: 'classyBrew',
          },
        });
        done();
      });

      // this action will update sortFields in the state
      expect(store.getActions()[0]).toEqual({
        type: 'SORT_RESULTS_REQUESTED',
        payload: {
          resultsType: 'components',
          sortFields: ['-foo', 'bar'],
        },
      });

      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_RESULTS_REQUESTED',
        payload: 'components',
      });
    });

    it('waivers: updates sortFields and sorts on back end if multiple pages', (done) => {
      initialState.dashboard.waivers.results = ['-foo', 'bar'];
      initialState.dashboard.waivers.hasMultiplePages = true;
      initialState.dashboard.waivers.hasNextPage = true;

      const expectedSortFields = initialState.dashboard.waivers.sortFields;

      jest.spyOn(dashboardDataServices, 'getWaivers').mockReturnValue(
        Promise.resolve({
          results: 'sorted results',
          hasNextPage: true,
          hasMultiplePages: true,
        })
      );

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(dashboardActions.sortWaiversResults(initialState.dashboard.waivers.results)).then(() => {
        expect(dashboardDataServices.getWaivers).toHaveBeenCalledWith('current filters', expectedSortFields, 0);
        expect(store.getActions().length).toBe(3);
        expect(store.getActions()[2]).toEqual({
          type: 'LOAD_RESULTS_FULFILLED',
          payload: {
            classyBrew: undefined,
            resultsType: 'waivers',
            results: 'sorted results',
            hasNextPage: true,
          },
        });
        done();
      });

      // this action will update sortFields in the state
      expect(store.getActions()[0]).toEqual({
        type: 'SORT_RESULTS_REQUESTED',
        payload: {
          resultsType: 'waivers',
          sortFields: ['-foo', 'bar'],
        },
      });

      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_RESULTS_REQUESTED',
        payload: 'waivers',
      });
    });

    it('waivers: updates sortFields and sorts on back end if results is not defined', (done) => {
      initialState.dashboard.waivers.results = null;
      const expectedSortFields = initialState.dashboard.waivers.sortFields;

      jest.spyOn(dashboardDataServices, 'getWaivers').mockReturnValue(
        Promise.resolve({
          results: 'sorted results',
          hasNextPage: true,
        })
      );

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(dashboardActions.sortWaiversResults(['-foo', 'bar'])).then(() => {
        expect(dashboardDataServices.getWaivers).toHaveBeenCalledWith('current filters', expectedSortFields, 0);
        expect(store.getActions().length).toBe(3);
        expect(store.getActions()[2]).toEqual({
          type: 'LOAD_RESULTS_FULFILLED',
          payload: {
            classyBrew: undefined,
            resultsType: 'waivers',
            results: 'sorted results',
            hasNextPage: true,
          },
        });
        done();
      });

      // this action will update sortFields in the state
      expect(store.getActions()[0]).toEqual({
        type: 'SORT_RESULTS_REQUESTED',
        payload: {
          resultsType: 'waivers',
          sortFields: ['-foo', 'bar'],
        },
      });

      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_RESULTS_REQUESTED',
        payload: 'waivers',
      });
    });

    it('waivers: updates sortFields and sorts on front end if single page', () => {
      initialState.dashboard.waivers.results = ['-foo', 'bar'];
      initialState.dashboard.waivers.hasMultiplePages = false;
      initialState.dashboard.waivers.hasNextPage = false;

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(dashboardActions.sortWaiversResults(['-foo', 'bar']));

      expect(store.getActions().length).toBe(2);

      // this action will update sortFields in the state
      expect(store.getActions()[0]).toEqual({
        type: 'SORT_RESULTS_REQUESTED',
        payload: {
          resultsType: 'waivers',
          sortFields: ['-foo', 'bar'],
        },
      });

      expect(store.getActions()[1]).toEqual({
        type: 'SORT_RESULTS_FULFILLED',
        payload: {
          resultsType: 'waivers',
          results: ['-foo', 'bar'],
        },
      });
    });
  });

  describe('sortViolationResults', () => {
    it('calls sortResults with the violations resultType', (done) => {
      initialState.dashboard.violations.results = ['-foo', 'bar'];
      initialState.dashboard.violations.hasMultiplePages = false;
      initialState.dashboard.violations.hasNextPage = false;
      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(sortViolationResults(['time', 'threatLevel'])).then(() => {
        expect(store.getActions()).toHaveActionsInOrder([
          {
            type: 'SORT_RESULTS_REQUESTED',
            payload: { resultsType: 'violations', sortFields: ['time', 'threatLevel'] },
          },
          {
            type: 'SORT_RESULTS_FULFILLED',
            payload: {
              resultsType: 'violations',
              results: ['-foo', 'bar'],
            },
          },
        ]);
        done();
      });
    });
  });

  describe('sortComponentResults', () => {
    it('calls sortResults with the components resultType', (done) => {
      initialState.dashboard.components.results = ['-foo', 'bar'];
      initialState.dashboard.components.hasMultiplePages = false;
      initialState.dashboard.components.hasNextPage = false;
      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(sortComponentResults(['score'])).then(() => {
        expect(store.getActions()).toHaveActionsInOrder([
          {
            type: 'SORT_RESULTS_REQUESTED',
            payload: { resultsType: 'components', sortFields: ['score'] },
          },
          {
            type: 'SORT_RESULTS_FULFILLED',
            payload: {
              resultsType: 'components',
              results: ['-foo', 'bar'],
            },
          },
        ]);
        done();
      });
    });
  });

  describe('sortApplicationResults', () => {
    it('calls sortResults with the applications resultType', (done) => {
      initialState.dashboard.applications.results = ['-foo', 'bar'];
      initialState.dashboard.applications.hasMultiplePages = false;
      initialState.dashboard.applications.hasNextPage = false;
      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(sortApplicationResults(['totalApplicationRisk.totalRisk'])).then(() => {
        expect(store.getActions()).toHaveActionsInOrder([
          {
            type: 'SORT_RESULTS_REQUESTED',
            payload: { resultsType: 'applications', sortFields: ['totalApplicationRisk.totalRisk'] },
          },
          {
            type: 'SORT_RESULTS_FULFILLED',
            payload: {
              resultsType: 'applications',
              results: ['-foo', 'bar'],
            },
          },
        ]);
        done();
      });
    });
  });

  describe('sortWaiversResults', () => {
    it('calls sortResults with the applications resultType', (done) => {
      initialState.dashboard.waivers.results = ['-foo', 'bar'];
      initialState.dashboard.waivers.hasMultiplePages = false;
      initialState.dashboard.waivers.hasNextPage = false;
      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(sortWaiversResults(['createTime'])).then(() => {
        expect(store.getActions()).toHaveActionsInOrder([
          {
            type: 'SORT_RESULTS_REQUESTED',
            payload: { resultsType: 'waivers', sortFields: ['createTime'] },
          },
          {
            type: 'SORT_RESULTS_FULFILLED',
            payload: {
              resultsType: 'waivers',
              results: ['-foo', 'bar'],
            },
          },
        ]);
        done();
      });
    });
  });
});
