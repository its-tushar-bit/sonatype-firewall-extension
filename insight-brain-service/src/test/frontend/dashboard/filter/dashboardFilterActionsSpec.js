describe('dashboardFilterActions', function() {
  var dashboardFilterActions, initialState, dashboardDataServiceMock, $q, $rootScope, CLMLocations, $httpBackend;

  beforeEach(module('dashboardFilterActionsModule'));

  beforeEach(module(function ($provide) {

    dashboardDataServiceMock = jasmine.createSpyObj('dashboardDataServiceMock', ['getNewestRisks']);

    $provide.service('dashboard.data.service', function() {
      return dashboardDataServiceMock;
    });
  }));

  beforeEach(inject(function($injector, _$q_, _$rootScope_) {
    $q = _$q_;
    $rootScope = _$rootScope_;
    dashboardFilterActions = $injector.get('dashboardFilterActions');
    CLMLocations = $injector.get('CLMLocations');
    $httpBackend = $injector.get('$httpBackend');

    initialState = {
      dashboard: {
        filters: 'current filters',
        currentTab: 'violations',
        violations: {sortFields: ['-time', '-threatLevel']},
        components: {sortFields: ['-score']},
        applications: {sortFields: ['-totalApplicationRisk.totalRisk']}
      }
    };
  }));

  describe('updateFiltersFulfilled', function() {
    it('simply fires the action if needsAcknowledgement is true', function() {
      var store = SpecUtil.mockReduxStore(initialState);
      var successSpy = jasmine.createSpy('successSpy');
      store.dispatch(dashboardFilterActions.updateFiltersFulfilled('updated filters', true, 'filterName'))
          .then(successSpy);
      $rootScope.$apply();
      expect(successSpy).toHaveBeenCalled();
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'UPDATE_FILTERS_FULFILLED',
        payload: {
          filters: 'updated filters',
          needsAcknowledgement: true,
          appliedFilterName: 'filterName'
        }
      });
    });

    it('fires the action and loadResults if needsAcknowledgement is false', function() {
      var expectedSortFields = initialState.dashboard.violations.sortFields;
      var successSpy = jasmine.createSpy('successSpy');
      var deferred = $q.defer();
      dashboardDataServiceMock.getNewestRisks.and.returnValue(deferred.promise);
      var store = SpecUtil.mockReduxStore(initialState);

      store.dispatch(dashboardFilterActions.updateFiltersFulfilled('updated filters', false, 'filterName'))
          .then(successSpy);
      expect(dashboardDataServiceMock.getNewestRisks).toHaveBeenCalledWith('current filters', expectedSortFields);
      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[0]).toEqual({
        type: 'UPDATE_FILTERS_FULFILLED',
        payload: {
          filters: 'updated filters',
          needsAcknowledgement: false,
          appliedFilterName: 'filterName'
        }
      });
      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_RESULTS_REQUESTED',
        payload: 'violations'
      });

      deferred.resolve({results: 'results', numResults: 3, classyBrew: 'classyBrew'});
      $rootScope.$apply();
      expect(successSpy).toHaveBeenCalled();
      expect(store.getActions().length).toBe(3);
      expect(store.getActions()[2]).toEqual({
        type: 'LOAD_RESULTS_FULFILLED',
        payload: {
          resultsType: 'violations',
          results: 'results',
          numResults: 3,
          classyBrew: 'classyBrew'
        }
      });
    });

    it('returns rejected promise if failed to load results', function() {
      var errorSpy = jasmine.createSpy('errorSpy');
      var deferred = $q.defer();
      dashboardDataServiceMock.getNewestRisks.and.returnValue(deferred.promise);
      var store = SpecUtil.mockReduxStore(initialState);

      store.dispatch(dashboardFilterActions.updateFiltersFulfilled('updated filters', false, 'filterName'))
          .catch(errorSpy);

      deferred.reject('load results error');
      $rootScope.$apply();
      expect(errorSpy).toHaveBeenCalledWith('load results error');
    });
  });

  describe('refreshViolationsDetails', function() {
    afterEach(function() {
      $httpBackend.verifyNoOutstandingExpectation(false);
      $httpBackend.verifyNoOutstandingRequest();
    });

    it('loads dashboard filters, loads results and dispatches REFRESH_VIOLATION_DETAILS action', function() {
      var expectedSortFields = initialState.dashboard.violations.sortFields;
      var deferred = $q.defer();
      var successSpy = jasmine.createSpy('successSpy');
      var store = SpecUtil.mockReduxStore(initialState);

      $httpBackend.expectGET(CLMLocations.getDashboardFilters()).respond({
        filter: 'new filters',
        needsAcknowledgement: false
      });
      dashboardDataServiceMock.getNewestRisks.and.returnValue(deferred.promise);

      store.dispatch(dashboardFilterActions.refreshViolationsDetails()).then(successSpy);
      expect(store.getActions().length).toBe(0);

      $httpBackend.flush();

      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[0]).toEqual({
        type: 'UPDATE_FILTERS_FULFILLED',
        payload: {
          filters: 'new filters',
          needsAcknowledgement: false,
          appliedFilterName: undefined
        }
      });
      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_RESULTS_REQUESTED',
        payload: 'violations'
      });
      expect(dashboardDataServiceMock.getNewestRisks).toHaveBeenCalledWith('current filters', expectedSortFields);

      deferred.resolve({results: 'new results', numResults: 3});
      $rootScope.$apply();

      expect(successSpy).toHaveBeenCalled();
      expect(store.getActions().length).toBe(4);
      expect(store.getActions()[2]).toEqual({
        type: 'LOAD_RESULTS_FULFILLED',
        payload: {
          resultsType: 'violations',
          results: 'new results',
          numResults: 3,
          classyBrew: undefined
        }
      });
      expect(store.getActions()[3]).toEqual({type: 'REFRESH_VIOLATION_DETAILS'});
    });

    it('in case of failure dispatches REFRESH_VIOLATION_DETAILS_FAILED and returns rejected promise', function() {
      var errorSpy = jasmine.createSpy('errorSpy');
      var store = SpecUtil.mockReduxStore(initialState);
      $httpBackend.expectGET(CLMLocations.getDashboardFilters()).respond(403);

      store.dispatch(dashboardFilterActions.refreshViolationsDetails()).catch(errorSpy);
      $httpBackend.flush();

      expect(errorSpy).toHaveBeenCalledWith(jasmine.objectContaining({status: 403}));
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toBe('REFRESH_VIOLATION_DETAILS_FAILED');
      expect(store.getActions()[0].payload.status).toBe(403);
    });
  });
});
