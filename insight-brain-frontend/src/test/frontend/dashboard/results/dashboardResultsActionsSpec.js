/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as dashboardActions from 'MainRoot/dashboard/results/dashboardResultsActions';
import axios from 'axios';
import {
  loadApplicationResults,
  loadComponentResults,
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
  sortApplicationResults,
  sortComponentResults,
  sortViolationResults,
  sortWaiversResults,
} from 'MainRoot/dashboard/results/dashboardResultsActions';
import * as dashboardDataServices from 'MainRoot/dashboard/services/dashboard.data.service';
import { getProductFeaturesUrl } from 'MainRoot/util/CLMLocation';

describe('dashboardResultsActions', function () {
  let loadResults, setPage;

  const newRisksSpy = jasmine.createSpy('getNewestRisks'),
    applicationsRiskSpy = jasmine.createSpy('getApplicationRisks'),
    componentRisksSpy = jasmine.createSpy('getComponentRisks'),
    getWaiversSpy = jasmine.createSpy('getWaivers'),
    getWaiversAndAutoWaiversSpy = jasmine.createSpy('getWaiversAndAutoWaivers');

  const tabs = [
    {
      resultsType: 'violations',
      serviceMethod: newRisksSpy,
    },
    {
      resultsType: 'components',
      serviceMethod: componentRisksSpy,
    },
    {
      resultsType: 'applications',
      serviceMethod: applicationsRiskSpy,
    },
    {
      resultsType: 'waivers',
      serviceMethod: getWaiversSpy,
    },
  ];

  beforeEach(function () {
    const module = require('inject-loader!../../../../main/frontend/dashboard/results/dashboardResultsActions')({
      '../services/dashboard.data.service': {
        getNewestRisks: newRisksSpy,
        getApplicationRisks: applicationsRiskSpy,
        getComponentRisks: componentRisksSpy,
        getWaivers: getWaiversSpy,
        getWaiversAndAutoWaivers: getWaiversAndAutoWaiversSpy,
        DASHBOARD_PAGE_SIZE: 25,
      },
    });
    loadResults = module.loadResults;
    setPage = module.setPage;
  });

  const initialState = {
    dashboardFilter: {
      appliedFilter: 'current filters',
    },
    dashboard: {
      violations: { sortFields: ['-time', '-threatLevel'] },
      components: { sortFields: ['-score'] },
      applications: { sortFields: ['-totalApplicationRisk.totalRisk'] },
      waivers: { sortFields: ['expiryTime'] },
    },
  };

  function testLoadResultsAction(tab) {
    describe('loadResults for ' + tab.resultsType, function () {
      it('loads results', function (done) {
        const store = SpecUtil.mockReduxStore(initialState);
        const mockResults = Promise.resolve({
          results: 'results',
          numResults: 3,
          hasNextPage: true,
          classyBrew: 'classyBrew',
        });
        tab.serviceMethod.and.returnValue(mockResults);

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
              numResults: 3,
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

      it('handles failure to load results', function (done) {
        const store = SpecUtil.mockReduxStore(initialState);
        const mockRejection = Promise.reject('load results error');
        tab.serviceMethod.and.callFake(() => mockRejection);

        store.dispatch(loadResults(tab.resultsType)).then(() => {
          expect(store.getActions().length).toBe(2);
          expect(store.getActions()[1]).toEqual({
            type: 'LOAD_RESULTS_FAILED',
            payload: {
              resultsType: tab.resultsType,
              error: 'load results error',
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
    });
  }

  tabs.forEach(testLoadResultsAction);

  function testSetPageAction(tab) {
    describe('setPage for ' + tab.resultsType, function () {
      it('sets the page number', function (done) {
        const store = SpecUtil.mockReduxStore(initialState);
        const mockResults = Promise.resolve({
          results: 'results',
          numResults: 3,
          hasNextPage: true,
          classyBrew: 'classyBrew',
        });
        tab.serviceMethod.and.returnValue(mockResults);

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
              numResults: 3,
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
  }

  tabs.forEach(testSetPageAction);

  describe('loadViolationResults', () => {
    let getNewestRisksSpy, getWaiverRiskySpy;

    beforeEach(function () {
      getNewestRisksSpy = spyOn(dashboardDataServices, 'getNewestRisks').and.returnValue(
        Promise.resolve({
          results: 'violationResults',
          numResults: 3,
          classyBrew: 'classyBrew',
        })
      );
      getWaiverRiskySpy = spyOn(dashboardDataServices, 'getWaivers').and.returnValue(
        Promise.resolve({
          results: 'waiverResults',
          numResults: 3,
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
              numResults: 3,
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
              numResults: 3,
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

    beforeEach(function () {
      componentRisksSpy = spyOn(dashboardDataServices, 'getComponentRisks').and.returnValue(
        Promise.resolve({
          results: 'componentsResults',
          numResults: 3,
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
              numResults: 3,
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

    beforeEach(function () {
      getApplicationRisksSpy = spyOn(dashboardDataServices, 'getApplicationRisks').and.returnValue(
        Promise.resolve({
          results: 'applicationResults',
          numResults: 3,
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
              numResults: 3,
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
      getWaiversSpy = spyOn(dashboardDataServices, 'getWaivers').and.returnValue(
        Promise.resolve({
          results: 'waiversResults',
          numResults: 3,
        })
      );

      getWaiversAndAutoWaiversSpy = spyOn(dashboardDataServices, 'getWaiversAndAutoWaivers').and.returnValue(
        Promise.resolve({
          results: 'autoWaiversResults',
          numResults: 3,
        })
      );

      axiosGetSpy = spyOn(axios, 'get').and.returnValue(Promise.resolve({ data: ['auto-waivers'] }));
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
              numResults: 3,
            },
          },
        ]);
        done();
      });
    });

    it('calls loadResults with getWaivers if auto-waivers feature is not enabled', (done) => {
      axiosGetSpy.and.returnValue(Promise.resolve({ data: [] }));

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
              numResults: 3,
            },
          },
        ]);
        done();
      });
    });

    it('handles error from the service method', (done) => {
      const store = SpecUtil.mockReduxStore(initialState);
      getWaiversAndAutoWaiversSpy.and.returnValue(Promise.reject('service error'));

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
    const violationsInitialState = { ...initialState, dashboard: { violations: { page: 2 } } }; // 2 is the current page
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
            numResults: 3,
            hasNextPage: true,
            classyBrew: 'classyBrew',
          },
        },
      ];
    };
    beforeEach(() => {
      spyOn(dashboardDataServices, 'getNewestRisks').and.returnValue(
        Promise.resolve({
          results: 'violationResults',
          numResults: 3,
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
    let getWaiversSpy, getWaiversAndAutoWaiversSpy, axiosGetSpy;
    const waiversInitialState = { ...initialState, dashboard: { waivers: { page: 2 } } }; // 2 is the current page
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
            numResults: 3,
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
            numResults: 3,
            hasNextPage: true,
          },
        },
      ];
    };

    beforeEach(() => {
      getWaiversSpy = spyOn(dashboardDataServices, 'getWaivers').and.returnValue(
        Promise.resolve({
          results: 'waiversResults',
          numResults: 3,
          hasNextPage: true,
        })
      );

      getWaiversAndAutoWaiversSpy = spyOn(dashboardDataServices, 'getWaiversAndAutoWaivers').and.returnValue(
        Promise.resolve({
          results: 'autoWaiversResults',
          numResults: 3,
          hasNextPage: true,
        })
      );

      axiosGetSpy = spyOn(axios, 'get').and.returnValue(Promise.resolve({ data: ['auto-waivers'] }));
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
      axiosGetSpy.and.returnValue(Promise.resolve({ data: [] }));

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
      axiosGetSpy.and.returnValue(Promise.resolve({ data: [] }));

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
      getWaiversAndAutoWaiversSpy.and.returnValue(Promise.reject('service error'));

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
    const applicationsInitialState = { ...initialState, dashboard: { applications: { page: 2 } } }; // 2 is the current page
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
            numResults: 3,
            hasNextPage: true,
            classyBrew: 'classyBrew',
          },
        },
      ];
    };

    beforeEach(() => {
      spyOn(dashboardDataServices, 'getApplicationRisks').and.returnValue(
        Promise.resolve({
          results: 'applicationResults',
          numResults: 3,
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
    const componentsInitialState = { ...initialState, dashboard: { components: { page: 2 } } }; // 2 is the current page
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
            numResults: 3,
            hasNextPage: true,
            classyBrew: 'classyBrew',
          },
        },
      ];
    };

    beforeEach(() => {
      spyOn(dashboardDataServices, 'getComponentRisks').and.returnValue(
        Promise.resolve({
          results: 'componentsResults',
          numResults: 3,
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

  describe('sortResults', function () {
    it('updates sortFields and sorts on front end if results < 100', function () {
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

    it('updates sortFields and sorts on front end if numResults === DASHBOARD_PAGE_SIZE (25)', function () {
      initialState.dashboard.applications.results = ['-foo', 'bar'];
      initialState.dashboard.components.numResults = 100;

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

    it('updates sortFields and sorts on back end if numResults > DASHBOARD_PAGE_SIZE (25)', function (done) {
      initialState.dashboard.components.results = ['-foo', 'bar'];
      initialState.dashboard.components.numResults = 101;

      const expectedSortFields = initialState.dashboard.components.sortFields;

      spyOn(dashboardDataServices, 'getComponentRisks').and.returnValue(
        Promise.resolve({
          results: 'sorted results',
          numResults: 3,
          hasNextPage: true,
          classyBrew: 'classyBrew',
        })
      );

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(dashboardActions.sortComponentResults(initialState.dashboard.components.results)).then(() => {
        expect(componentRisksSpy).toHaveBeenCalledWith('current filters', expectedSortFields, 0);
        expect(store.getActions().length).toBe(3);
        expect(store.getActions()[2]).toEqual({
          type: 'LOAD_RESULTS_FULFILLED',
          payload: {
            resultsType: 'components',
            results: 'sorted results',
            numResults: 3,
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

    it('updates sortFields and sorts on back end if results is not defined', function (done) {
      initialState.dashboard.components.results = null;
      const expectedSortFields = initialState.dashboard.components.sortFields;

      spyOn(dashboardDataServices, 'getComponentRisks').and.returnValue(
        Promise.resolve({
          results: 'sorted results',
          numResults: 3,
          hasNextPage: true,
          classyBrew: 'classyBrew',
        })
      );

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(dashboardActions.sortComponentResults(['-foo', 'bar'])).then(() => {
        expect(componentRisksSpy).toHaveBeenCalledWith('current filters', expectedSortFields, 0);
        expect(store.getActions().length).toBe(3);
        expect(store.getActions()[2]).toEqual({
          type: 'LOAD_RESULTS_FULFILLED',
          payload: {
            resultsType: 'components',
            results: 'sorted results',
            numResults: 3,
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

    it('updates sortFields and sorts on back end if numResults > DASHBOARD_PAGE_SIZE (25)', function (done) {
      initialState.dashboard.waivers.results = ['-foo', 'bar'];
      initialState.dashboard.waivers.numResults = 101;

      const expectedSortFields = initialState.dashboard.waivers.sortFields;

      spyOn(dashboardDataServices, 'getWaivers').and.returnValue(
        Promise.resolve({
          results: 'sorted results',
          numResults: 3,
          hasNextPage: true,
        })
      );

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(dashboardActions.sortWaiversResults(initialState.dashboard.waivers.results)).then(() => {
        expect(getWaiversSpy).toHaveBeenCalledWith('current filters', expectedSortFields, 0);
        expect(store.getActions().length).toBe(3);
        expect(store.getActions()[2]).toEqual({
          type: 'LOAD_RESULTS_FULFILLED',
          payload: {
            classyBrew: undefined,
            resultsType: 'waivers',
            results: 'sorted results',
            numResults: 3,
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

    it('updates sortFields and sorts on back end if results is not defined', function (done) {
      initialState.dashboard.waivers.results = null;
      const expectedSortFields = initialState.dashboard.waivers.sortFields;

      spyOn(dashboardDataServices, 'getWaivers').and.returnValue(
        Promise.resolve({
          results: 'sorted results',
          numResults: 3,
          hasNextPage: true,
        })
      );

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(dashboardActions.sortWaiversResults(['-foo', 'bar'])).then(() => {
        expect(getWaiversSpy).toHaveBeenCalledWith('current filters', expectedSortFields, 0);
        expect(store.getActions().length).toBe(3);
        expect(store.getActions()[2]).toEqual({
          type: 'LOAD_RESULTS_FULFILLED',
          payload: {
            classyBrew: undefined,
            resultsType: 'waivers',
            results: 'sorted results',
            numResults: 3,
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

    it('updates sortFields and sorts on front end if numResults === DASHBOARD_PAGE_SIZE (25)', function () {
      initialState.dashboard.waivers.results = ['-foo', 'bar'];
      initialState.dashboard.waivers.numResults = 25;

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
      initialState.dashboard.violations.numResults = 10;
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
      initialState.dashboard.components.numResults = 10;
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
      initialState.dashboard.applications.numResults = 10;
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
      initialState.dashboard.waivers.numResults = 10;
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
