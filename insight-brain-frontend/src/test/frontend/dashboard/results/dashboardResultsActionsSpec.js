/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as dashboardActions from 'MainRoot/dashboard/results/dashboardResultsActions';
import {
  loadApplicationResults,
  loadComponentResults,
  loadViolationResults,
  loadWaiverResults,
  sortApplicationResults,
  sortComponentResults,
  sortViolationResults,
  sortWaiversResults,
} from 'MainRoot/dashboard/results/dashboardResultsActions';
import * as dashboardDataServices from 'MainRoot/dashboard/services/dashboard.data.service';

describe('dashboardResultsActions', function () {
  let loadResults;

  const newRisksSpy = jasmine.createSpy('getNewestRisks'),
    applicationsRiskSpy = jasmine.createSpy('getApplicationRisks'),
    componentRisksSpy = jasmine.createSpy('getComponentRisks'),
    getWaiversSpy = jasmine.createSpy('getWaivers');

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
        MAX_RESULTS: 100,
      },
    });
    loadResults = module.loadResults;
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
          classyBrew: 'classyBrew',
        });
        tab.serviceMethod.and.returnValue(mockResults);

        store.dispatch(loadResults(tab.resultsType)).then(() => {
          expect(tab.serviceMethod).toHaveBeenCalledWith(
            initialState.dashboardFilter.appliedFilter,
            initialState.dashboard[tab.resultsType].sortFields
          );

          expect(store.getActions().length).toBe(2);
          expect(store.getActions()[1]).toEqual({
            type: 'LOAD_RESULTS_FULFILLED',
            payload: {
              resultsType: tab.resultsType,
              results: 'results',
              numResults: 3,
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

        store.dispatch(loadResults(tab.resultsType)).catch(() => {
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

  describe('loadViolationResults', () => {
    it('calls loadResults with the violations resultsType', (done) => {
      spyOn(dashboardDataServices, 'getNewestRisks').and.returnValue(
        Promise.resolve({
          results: 'violationResults',
          numResults: 3,
          classyBrew: 'classyBrew',
        })
      );

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
        done();
      });
    });
  });

  describe('loadComponentResults', () => {
    it('calls loadResults with the components resultsType', (done) => {
      spyOn(dashboardDataServices, 'getComponentRisks').and.returnValue(
        Promise.resolve({
          results: 'componentResults',
          numResults: 3,
          classyBrew: 'classyBrew',
        })
      );

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
              results: 'componentResults',
              numResults: 3,
              classyBrew: 'classyBrew',
            },
          },
        ]);
        done();
      });
    });
  });

  describe('loadApplicationResults', () => {
    it('calls loadResults with the applications resultsType', (done) => {
      spyOn(dashboardDataServices, 'getApplicationRisks').and.returnValue(
        Promise.resolve({
          results: 'applicationResults',
          numResults: 3,
          classyBrew: 'classyBrew',
        })
      );

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
        done();
      });
    });
  });

  describe('loadWaiverResults', () => {
    it('calls loadResults with the applications resultsType', (done) => {
      spyOn(dashboardDataServices, 'getWaivers').and.returnValue(
        Promise.resolve({
          results: 'waiversResults',
          numResults: 3,
        })
      );

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
              results: 'waiversResults',
              numResults: 3,
            },
          },
        ]);
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

    it('updates sortFields and sorts on front end if numResults === MAX_RESULTS (100)', function () {
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

    it('updates sortFields and sorts on back end if numResults > MAX_RESULTS (100)', function (done) {
      initialState.dashboard.components.results = ['-foo', 'bar'];
      initialState.dashboard.components.numResults = 101;

      const expectedSortFields = initialState.dashboard.components.sortFields;

      spyOn(dashboardDataServices, 'getComponentRisks').and.returnValue(
        Promise.resolve({
          results: 'sorted results',
          numResults: 3,
          classyBrew: 'classyBrew',
        })
      );

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(dashboardActions.sortComponentResults(initialState.dashboard.components.results)).then(() => {
        expect(componentRisksSpy).toHaveBeenCalledWith('current filters', expectedSortFields);
        expect(store.getActions().length).toBe(3);
        expect(store.getActions()[2]).toEqual({
          type: 'LOAD_RESULTS_FULFILLED',
          payload: {
            resultsType: 'components',
            results: 'sorted results',
            numResults: 3,
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
          classyBrew: 'classyBrew',
        })
      );

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(dashboardActions.sortComponentResults(['-foo', 'bar'])).then(() => {
        expect(componentRisksSpy).toHaveBeenCalledWith('current filters', expectedSortFields);
        expect(store.getActions().length).toBe(3);
        expect(store.getActions()[2]).toEqual({
          type: 'LOAD_RESULTS_FULFILLED',
          payload: {
            resultsType: 'components',
            results: 'sorted results',
            numResults: 3,
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

    it('updates sortFields and sorts on back end if numResults > MAX_RESULTS (100)', function (done) {
      initialState.dashboard.waivers.results = ['-foo', 'bar'];
      initialState.dashboard.waivers.numResults = 101;

      const expectedSortFields = initialState.dashboard.waivers.sortFields;

      spyOn(dashboardDataServices, 'getWaivers').and.returnValue(
        Promise.resolve({
          results: 'sorted results',
          numResults: 3,
        })
      );

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(dashboardActions.sortWaiversResults(initialState.dashboard.waivers.results)).then(() => {
        expect(getWaiversSpy).toHaveBeenCalledWith('current filters', expectedSortFields);
        expect(store.getActions().length).toBe(3);
        expect(store.getActions()[2]).toEqual({
          type: 'LOAD_RESULTS_FULFILLED',
          payload: {
            classyBrew: undefined,
            resultsType: 'waivers',
            results: 'sorted results',
            numResults: 3,
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
        })
      );

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(dashboardActions.sortWaiversResults(['-foo', 'bar'])).then(() => {
        expect(getWaiversSpy).toHaveBeenCalledWith('current filters', expectedSortFields);
        expect(store.getActions().length).toBe(3);
        expect(store.getActions()[2]).toEqual({
          type: 'LOAD_RESULTS_FULFILLED',
          payload: {
            classyBrew: undefined,
            resultsType: 'waivers',
            results: 'sorted results',
            numResults: 3,
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

    it('updates sortFields and sorts on front end if numResults === MAX_RESULTS (100)', function () {
      initialState.dashboard.waivers.results = ['-foo', 'bar'];
      initialState.dashboard.waivers.numResults = 100;

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
