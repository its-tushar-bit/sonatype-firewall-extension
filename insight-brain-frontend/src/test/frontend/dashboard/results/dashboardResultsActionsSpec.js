/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import dashboardResultsModule from '../../../../main/frontend/dashboard/results/module';

describe('dashboardResultsActions', function() {
  var dashboardResultsActions, initialState, dashboardDataServiceMock, $q, $rootScope;

  var tabs = [
    {
      resultsType: 'violations',
      serviceMethod: 'getNewestRisks'
    },
    {
      resultsType: 'components',
      serviceMethod: 'getComponentRisks'
    },
    {
      resultsType: 'applications',
      serviceMethod: 'getApplicationRisks'
    }
  ];

  beforeEach(angular.mock.module(dashboardResultsModule.name, function ($provide) {

    dashboardDataServiceMock = jasmine.createSpyObj('dashboardDataServiceMock',
        ['getNewestRisks', 'getApplicationRisks', 'getComponentRisks']);
    dashboardDataServiceMock.MAX_RESULTS = 100;

    $provide.service('dashboard.data.service', function() {
      return dashboardDataServiceMock;
    });
  }));

  beforeEach(inject(function($injector, _$q_, _$rootScope_) {
    $q = _$q_;
    $rootScope = _$rootScope_;
    dashboardResultsActions = $injector.get('dashboardResultsActions');

    initialState = {
      dashboardFilter: {
        appliedFilter: 'current filters'
      },
      dashboard: {
        violations: {sortFields: ['-time', '-threatLevel']},
        components: {sortFields: ['-score']},
        applications: {sortFields: ['-totalApplicationRisk.totalRisk']}
      }
    };
  }));

  angular.forEach(tabs, testLoadResultsAction);

  function testLoadResultsAction(tab) {
    describe('loadResults for ' + tab.resultsType, function() {
      var deferred, store, expectedSortFields;

      beforeEach(function() {
        deferred = $q.defer();
        dashboardDataServiceMock[tab.serviceMethod].and.returnValue(deferred.promise);
        store = SpecUtil.mockReduxStore(initialState);
        expectedSortFields = initialState.dashboard[tab.resultsType].sortFields;
      });

      it('loads results', function() {
        var successSpy = jasmine.createSpy('successSpy');
        store.dispatch(dashboardResultsActions.loadResults(tab.resultsType)).then(successSpy);
        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0]).toEqual({
          type: 'LOAD_RESULTS_REQUESTED',
          payload: tab.resultsType
        });
        expect(dashboardDataServiceMock[tab.serviceMethod]).toHaveBeenCalledWith('current filters', expectedSortFields);

        deferred.resolve({results: 'results', numResults: 3, classyBrew: 'classyBrew'});
        $rootScope.$apply();
        expect(successSpy).toHaveBeenCalled();
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
      });

      it('handles failure to load results', function() {
        var errorSpy = jasmine.createSpy('errorSpy');
        store.dispatch(dashboardResultsActions.loadResults(tab.resultsType)).catch(errorSpy);
        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0]).toEqual({
          type: 'LOAD_RESULTS_REQUESTED',
          payload: tab.resultsType
        });
        expect(dashboardDataServiceMock[tab.serviceMethod]).toHaveBeenCalledWith('current filters', expectedSortFields);

        deferred.reject('load results error');
        $rootScope.$apply();
        expect(errorSpy).toHaveBeenCalledWith('load results error');
        expect(store.getActions().length).toBe(2);
        expect(store.getActions()[1]).toEqual({
          type: 'LOAD_RESULTS_FAILED',
          payload: {
            resultsType: tab.resultsType,
            error: 'load results error'
          }
        });
      });
    });
  }

  describe('sortResults', function() {
    it('updates sortFields and sorts on front end if results < 100', function() {
      initialState.dashboard.applications.results = [
        {foo: 1, bar: 2},
        {foo: 1, bar: 1},
        {foo: 3, bar: 3}
      ];
      var store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(dashboardResultsActions.sortResults('applications', ['-foo', 'bar']));

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
      store.dispatch(dashboardResultsActions.sortResults('applications', ['-foo', 'bar']));

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

    it('updates sortFields and sorts on back end if results > 100', function() {
      initialState.dashboard.components.results = [];
      for (var i = 0; i < 101; i++) {
        initialState.dashboard.components.results.push({ foo: i, bar: i});
      }
      var expectedSortFields = initialState.dashboard.components.sortFields;
      var deferred = $q.defer();
      dashboardDataServiceMock.getComponentRisks.and.returnValue(deferred.promise);

      var store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(dashboardResultsActions.sortResults('components', ['-foo', 'bar']));
      expect(dashboardDataServiceMock.getComponentRisks).toHaveBeenCalledWith('current filters', expectedSortFields);

      expect(store.getActions().length).toBe(2);

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

      deferred.resolve({results: 'sorted results', numResults: 3, classyBrew: 'classyBrew'});
      $rootScope.$apply();
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
    });

    it('updates sortFields and sorts on back end if results is not defined', function() {
      initialState.dashboard.components.results = null;

      var expectedSortFields = initialState.dashboard.components.sortFields;
      var deferred = $q.defer();
      dashboardDataServiceMock.getComponentRisks.and.returnValue(deferred.promise);

      var store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(dashboardResultsActions.sortResults('components', ['-foo', 'bar']));
      expect(dashboardDataServiceMock.getComponentRisks).toHaveBeenCalledWith('current filters', expectedSortFields);

      expect(store.getActions().length).toBe(2);

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

      deferred.resolve({results: 'sorted results', numResults: 3, classyBrew: 'classyBrew'});
      $rootScope.$apply();
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
    });
  });
});
