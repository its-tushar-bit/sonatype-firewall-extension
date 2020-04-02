/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

describe('dashboardResultsActions', function() {
  let loadResults, sortResults;

  const newRisksSpy = jasmine.createSpy('getNewestRisks'),
      applicationsRiskSpy = jasmine.createSpy('getApplicationRisks'),
      componentRisksSpy = jasmine.createSpy('getComponentRisks');

  const tabs = [{
    resultsType: 'violations',
    serviceMethod: newRisksSpy
  }, {
    resultsType: 'components',
    serviceMethod: componentRisksSpy
  }, {
    resultsType: 'applications',
    serviceMethod: applicationsRiskSpy
  }];

  beforeEach(function() {
    const module = require('inject-loader!../../../../main/frontend/dashboard/results/dashboardResultsActions')({
      '../services/dashboard.data.service': {
        getNewestRisks: newRisksSpy,
        getApplicationRisks: applicationsRiskSpy,
        getComponentRisks: componentRisksSpy,
        MAX_RESULTS: 100
      }
    });
    loadResults = module.loadResults;
    sortResults = module.sortResults;
  });

  const initialState = {
    dashboardFilter: {
      appliedFilter: 'current filters'
    },
    dashboard: {
      violations: {sortFields: ['-time', '-threatLevel']},
      components: {sortFields: ['-score']},
      applications: {sortFields: ['-totalApplicationRisk.totalRisk']}
    }
  };

  function testLoadResultsAction(tab) {
    describe('loadResults for ' + tab.resultsType, function() {
      it('loads results', function(done) {
        const store = SpecUtil.mockReduxStore(initialState);
        const mockResults = Promise.resolve({ results: 'results', numResults: 3, classyBrew: 'classyBrew' });
        tab.serviceMethod.and.returnValue(mockResults);

        store.dispatch(loadResults(tab.resultsType))
            .then(() => {
              expect(tab.serviceMethod).toHaveBeenCalledWith(initialState.dashboardFilter.appliedFilter,
                  initialState.dashboard[tab.resultsType].sortFields);

              expect(store.getActions().length).toBe(2);
              expect(store.getActions()[1]).toEqual({
                type: 'LOAD_RESULTS_FULFILLED',
                payload: {
                  resultsType: tab.resultsType,
                  results: 'results',
                  numResults: 3,
                  classyBrew: 'classyBrew'
                }
              });
              done();
            });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0]).toEqual({
          type: 'LOAD_RESULTS_REQUESTED',
          payload: tab.resultsType
        });
      });

      it('handles failure to load results', function(done) {
        const store = SpecUtil.mockReduxStore(initialState);
        const mockRejection = Promise.reject('load results error');
        tab.serviceMethod.and.returnValue(mockRejection);

        store.dispatch(loadResults(tab.resultsType))
            .catch(() => {
              expect(store.getActions().length).toBe(2);
              expect(store.getActions()[1]).toEqual({
                type: 'LOAD_RESULTS_FAILED',
                payload: {
                  resultsType: tab.resultsType,
                  error: 'load results error'
                }
              });
              done();
            });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0]).toEqual({
          type: 'LOAD_RESULTS_REQUESTED',
          payload: tab.resultsType
        });
      });
    });
  }

  tabs.forEach(testLoadResultsAction);

  describe('sortResults', function() {
    it('updates sortFields and sorts on front end if results < 100', function() {
      initialState.dashboard.applications.results = [
        {foo: 1, bar: 2},
        {foo: 1, bar: 1},
        {foo: 3, bar: 3}
      ];
      var store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(sortResults('applications', ['-foo', 'bar']));

      expect(store.getActions().length).toBe(2);

      // this action will update sortFields in the state
      expect(store.getActions()[0]).toEqual({
        type: 'SORT_RESULTS_REQUESTED',
        payload: {
          resultsType: 'applications',
          sortFields: ['-foo', 'bar']
        }
      });

      expect(store.getActions()[1]).toEqual({
        type: 'SORT_RESULTS_FULFILLED',
        payload: {
          resultsType: 'applications',
          results: [
            {foo: 3, bar: 3},
            {foo: 1, bar: 1},
            {foo: 1, bar: 2}
          ]
        }
      });
    });

    it('updates sortFields and sorts on front end if results === 100', function() {
      var results = [];
      for (var i = 0; i < 100; i++) {
        results.push({foo: i, bar: i});
      }
      initialState.dashboard.applications.results = results;
      expect(initialState.dashboard.applications.results.length).toBe(100);

      var store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(sortResults('applications', ['-foo', 'bar']));

      expect(store.getActions().length).toBe(2);

      // this action will update sortFields in the state
      expect(store.getActions()[0]).toEqual({
        type: 'SORT_RESULTS_REQUESTED',
        payload: {
          resultsType: 'applications',
          sortFields: ['-foo', 'bar']
        }
      });

      expect(store.getActions()[1].type).toBe('SORT_RESULTS_FULFILLED');
      expect(store.getActions()[1].payload.resultsType).toBe('applications');
      expect(store.getActions()[1].payload.results.length).toBe(100);
      expect(store.getActions()[1].payload.results[0]).toEqual({foo: 99, bar: 99});
      expect(store.getActions()[1].payload.results[99]).toEqual({foo: 0, bar: 0});
    });

    it('updates sortFields and sorts on back end if results > 100', function(done) {
      initialState.dashboard.components.results = [];
      for (var i = 0; i < 101; i++) {
        initialState.dashboard.components.results.push({ foo: i, bar: i });
      }
      const expectedSortFields = initialState.dashboard.components.sortFields;

      componentRisksSpy.and.returnValue(
          Promise.resolve({ results: 'sorted results', numResults: 3, classyBrew: 'classyBrew' }));

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(sortResults('components', ['-foo', 'bar']))
          .then(() => {
            expect(componentRisksSpy).toHaveBeenCalledWith('current filters', expectedSortFields);
            expect(store.getActions().length).toBe(3);
            expect(store.getActions()[2]).toEqual({
              type: 'LOAD_RESULTS_FULFILLED',
              payload: {
                resultsType: 'components',
                results: 'sorted results',
                numResults: 3,
                classyBrew: 'classyBrew'
              }
            });
            done();
          });

      // this action will update sortFields in the state
      expect(store.getActions()[0]).toEqual({
        type: 'SORT_RESULTS_REQUESTED',
        payload: {
          resultsType: 'components',
          sortFields: ['-foo', 'bar']
        }
      });

      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_RESULTS_REQUESTED',
        payload: 'components'
      });
    });

    it('updates sortFields and sorts on back end if results is not defined', function(done) {
      initialState.dashboard.components.results = null;
      const expectedSortFields = initialState.dashboard.components.sortFields;

      componentRisksSpy.and.returnValue(
          Promise.resolve({ results: 'sorted results', numResults: 3, classyBrew: 'classyBrew' }));

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(sortResults('components', ['-foo', 'bar']))
          .then(() => {
            expect(componentRisksSpy).toHaveBeenCalledWith('current filters', expectedSortFields);
            expect(store.getActions().length).toBe(3);
            expect(store.getActions()[2]).toEqual({
              type: 'LOAD_RESULTS_FULFILLED',
              payload: {
                resultsType: 'components',
                results: 'sorted results',
                numResults: 3,
                classyBrew: 'classyBrew'
              }
            });
            done();
          });

      // this action will update sortFields in the state
      expect(store.getActions()[0]).toEqual({
        type: 'SORT_RESULTS_REQUESTED',
        payload: {
          resultsType: 'components',
          sortFields: ['-foo', 'bar']
        }
      });

      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_RESULTS_REQUESTED',
        payload: 'components'
      });
    });
  });
});
