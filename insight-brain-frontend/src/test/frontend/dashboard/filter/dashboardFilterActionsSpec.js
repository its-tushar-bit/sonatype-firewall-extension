import dashboardFilterModule from '../../../../main/frontend/dashboard/filter/module';

describe('dashboardFilterActions', function() {
  var dashboardFilterActions, initialState, dashboardDataServiceMock, $q, $rootScope, CLMLocations, $httpBackend,
      OrganizationStoreMock, ApplicationStoreMock, StageTypeStoreMock;

  beforeEach(angular.mock.module(dashboardFilterModule.name, function ($provide) {

    dashboardDataServiceMock = jasmine.createSpyObj('dashboardDataServiceMock', ['getNewestRisks']);
    OrganizationStoreMock = jasmine.createSpyObj('OrganizationStoreMock', ['get']);
    ApplicationStoreMock = jasmine.createSpyObj('ApplicationStoreMock', ['get']);
    StageTypeStoreMock = jasmine.createSpyObj('StageTypeStoreMock', ['getDashboardStages']);

    $provide.service('dashboard.data.service', function() {
      return dashboardDataServiceMock;
    });
    $provide.service('OrganizationStore', function() {
      return OrganizationStoreMock;
    });
    $provide.service('ApplicationStore', function() {
      return ApplicationStoreMock;
    });
    $provide.service('StageTypeStore', function() {
      return StageTypeStoreMock;
    });
  }));

  beforeEach(inject(function($injector, _$q_, _$rootScope_) {
    $q = _$q_;
    $rootScope = _$rootScope_;
    dashboardFilterActions = $injector.get('dashboardFilterActions');
    CLMLocations = $injector.get('CLMLocations');
    $httpBackend = $injector.get('$httpBackend');

    initialState = {
      dashboardFilter: {
        appliedFilter: 'current filters'
      },
      dashboard: {
        currentTab: 'violations',
        violations: {sortFields: ['-time', '-threatLevel']},
        components: {sortFields: ['-score']},
        applications: {sortFields: ['-totalApplicationRisk.totalRisk']}
      }
    };
  }));

  describe('loadFilter', function() {
    var filterJson, deferredOrganizations, deferredApplications, deferredStages;

    function mockHttpCalls() {
      $httpBackend.expectGET(CLMLocations.getApplicationTagsUrl()).respond('tag data');
      $httpBackend.expectGET(CLMLocations.getDashboardFilters()).respond(filterJson);
      $httpBackend.expectGET(CLMLocations.getDashboardSavedFilters()).respond('saved filters data');
    }

    beforeEach(function() {
      filterJson = {
        name: '',
        basedOnFilterName: 'Test1',
        filter: 'filter data',
        needsAcknowledgement: false
      };

      deferredOrganizations = $q.defer();
      deferredApplications = $q.defer();
      deferredStages = $q.defer();
      OrganizationStoreMock.get.and.returnValue(deferredOrganizations.promise);
      ApplicationStoreMock.get.and.returnValue(deferredApplications.promise);
      StageTypeStoreMock.getDashboardStages.and.returnValue(deferredStages.promise);
    });

    afterEach(function() {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    });

    describe('when failed fetching filter data', function() {
      it('fires loadFiltersFailed action', function() {
        mockHttpCalls();

        var store = SpecUtil.mockReduxStore(initialState);
        var errorSpy = jasmine.createSpy('errorSpy');
        store.dispatch(dashboardFilterActions.loadFilter()).catch(errorSpy);

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0]).toEqual({
          type: 'LOAD_FILTER_REQUESTED'
        });

        expect(OrganizationStoreMock.get).toHaveBeenCalled();
        expect(ApplicationStoreMock.get).toHaveBeenCalled();
        expect(StageTypeStoreMock.getDashboardStages).toHaveBeenCalled();

        deferredOrganizations.resolve('organizations data');
        deferredApplications.resolve('applications data');
        deferredStages.reject('failed to get stages data');
        $httpBackend.flush();

        expect(errorSpy).toHaveBeenCalled();
        expect(store.getActions().length).toBe(3);
        expect(store.getActions()[1]).toEqual({
          type: 'LOAD_FILTER_FAILED',
          payload: 'failed to get stages data'
        });
        expect(store.getActions()[2]).toEqual({
          type: 'FETCH_SAVED_FILTERS_FULFILLED',
          payload: 'saved filters data'
        });

        expect(dashboardDataServiceMock.getNewestRisks).not.toHaveBeenCalled();
      });
    });

    describe('when needsAcknowledgement is true', function() {
      beforeEach(function() {
        filterJson.needsAcknowledgement = true;
        mockHttpCalls();
      });

      it('fires the action, fires fetchAvailableFilterOptionsFulfilled and fetchCurrentFilterFulfilled', function() {
        var store = SpecUtil.mockReduxStore(initialState);
        var successSpy = jasmine.createSpy('successSpy');
        store.dispatch(dashboardFilterActions.loadFilter()).then(successSpy);

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0]).toEqual({
          type: 'LOAD_FILTER_REQUESTED'
        });

        expect(OrganizationStoreMock.get).toHaveBeenCalled();
        expect(ApplicationStoreMock.get).toHaveBeenCalled();
        expect(StageTypeStoreMock.getDashboardStages).toHaveBeenCalled();

        deferredOrganizations.resolve('organizations data');
        deferredApplications.resolve('applications data');
        deferredStages.resolve('stages data');
        $httpBackend.flush();

        expect(successSpy).toHaveBeenCalled();
        expect(store.getActions().length).toBe(4);

        expect(store.getActions()[1]).toEqual({
          type: 'FETCH_SAVED_FILTERS_FULFILLED',
          payload: 'saved filters data'
        });
        expect(store.getActions()[2]).toEqual({
          type: 'FETCH_AVAILABLE_FILTER_OPTIONS_FULFILLED',
          payload: {
            organizations: 'organizations data',
            applications: 'applications data',
            stages: 'stages data',
            categories: 'tag data'
          }
        });

        expect(store.getActions()[3]).toEqual({
          type: 'FETCH_CURRENT_FILTER_FULFILLED',
          payload: {
            name: '',
            basedOnFilterName: 'Test1',
            filter: 'filter data',
            needsAcknowledgement: true
          }
        });

        expect(dashboardDataServiceMock.getNewestRisks).not.toHaveBeenCalled();
      });
    });

    describe('when needsAcknowledgement is false', function() {
      var deferredViolationResults;
      beforeEach(function() {
        deferredViolationResults = $q.defer();
        dashboardDataServiceMock.getNewestRisks.and.returnValue(deferredViolationResults.promise);
        filterJson.needsAcknowledgement = false;
        mockHttpCalls();
      });

      it('fires filter actions and also loads results', function() {
        var store = SpecUtil.mockReduxStore(initialState);
        var successSpy = jasmine.createSpy('successSpy');
        store.dispatch(dashboardFilterActions.loadFilter()).then(successSpy);

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0]).toEqual({
          type: 'LOAD_FILTER_REQUESTED'
        });

        expect(OrganizationStoreMock.get).toHaveBeenCalled();
        expect(ApplicationStoreMock.get).toHaveBeenCalled();
        expect(StageTypeStoreMock.getDashboardStages).toHaveBeenCalled();

        deferredOrganizations.resolve('organizations data');
        deferredApplications.resolve('applications data');
        deferredStages.resolve('stages data');
        $httpBackend.flush();

        expect(store.getActions().length).toBe(5);

        expect(store.getActions()[1]).toEqual({
          type: 'FETCH_SAVED_FILTERS_FULFILLED',
          payload: 'saved filters data'
        });

        expect(store.getActions()[2]).toEqual({
          type: 'FETCH_AVAILABLE_FILTER_OPTIONS_FULFILLED',
          payload: {
            organizations: 'organizations data',
            applications: 'applications data',
            stages: 'stages data',
            categories: 'tag data'
          }
        });

        expect(store.getActions()[3]).toEqual({
          type: 'FETCH_CURRENT_FILTER_FULFILLED',
          payload: {
            name: '',
            basedOnFilterName: 'Test1',
            filter: 'filter data',
            needsAcknowledgement: false
          }
        });

        expect(store.getActions()[4]).toEqual({
          type: 'LOAD_RESULTS_REQUESTED',
          payload: 'violations'
        });

        expect(dashboardDataServiceMock.getNewestRisks).toHaveBeenCalledWith('current filters',
            initialState.dashboard.violations.sortFields);

        deferredViolationResults.resolve({results: 'results', numResults: 3, classyBrew: 'classyBrew'});
        $rootScope.$apply();
        expect(successSpy).toHaveBeenCalled();
        expect(store.getActions().length).toBe(6);
        expect(store.getActions()[5]).toEqual({
          type: 'LOAD_RESULTS_FULFILLED',
          payload: {
            resultsType: 'violations',
            results: 'results',
            numResults: 3,
            classyBrew: 'classyBrew'
          }
        });
      });
    });
  });

  describe('applyFilter', function() {
    afterEach(function() {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    });

    it('updates filters and loads results', function() {
      // mock update filters
      $httpBackend.expectPUT(CLMLocations.getDashboardFilters()).respond('update filters response');

      // mock load results
      var expectedSortFields = initialState.dashboard.violations.sortFields;
      var deferred = $q.defer();
      dashboardDataServiceMock.getNewestRisks.and.returnValue(deferred.promise);

      var successSpy = jasmine.createSpy('successSpy');
      var store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(dashboardFilterActions.applyFilter('test filters', 'test filter name')).then(successSpy);

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({type: 'APPLY_FILTER_REQUESTED'});

      $httpBackend.flush();
      expect(dashboardDataServiceMock.getNewestRisks).toHaveBeenCalledWith('current filters', expectedSortFields);
      expect(store.getActions().length).toBe(3);
      expect(store.getActions()[1]).toEqual({
        type: 'APPLY_FILTER_FULFILLED',
        payload: {
          filter: 'update filters response',
          basedOnFilterName: 'test filter name'
        }
      });
      expect(store.getActions()[2]).toEqual({
        type: 'LOAD_RESULTS_REQUESTED',
        payload: 'violations'
      });

      deferred.resolve({results: 'results', numResults: 3, classyBrew: 'classyBrew'});
      $rootScope.$apply();
      expect(successSpy).toHaveBeenCalled();
      expect(store.getActions().length).toBe(4);
      expect(store.getActions()[3]).toEqual({
        type: 'LOAD_RESULTS_FULFILLED',
        payload: {
          resultsType: 'violations',
          results: 'results',
          numResults: 3,
          classyBrew: 'classyBrew'
        }
      });
    });

    it('dispatches APPLY_FILTER_FAILED if failed to update filters', function() {
      // mock update filters
      $httpBackend.expectPUT(CLMLocations.getDashboardFilters()).respond(403);

      var errorSpy = jasmine.createSpy('errorSpy');
      var store = SpecUtil.mockReduxStore(initialState);
      var actions = store.getActions();
      store.dispatch(dashboardFilterActions.applyFilter('test filters', 'test filter name')).catch(errorSpy);

      expect(actions.length).toBe(1);
      expect(actions[0]).toEqual({type: 'APPLY_FILTER_REQUESTED'});

      $httpBackend.flush();
      expect(dashboardDataServiceMock.getNewestRisks).not.toHaveBeenCalled();
      expect(actions.length).toBe(2);
      expect(actions[1].type).toBe('APPLY_FILTER_FAILED');
      expect(actions[1].payload.status).toEqual(403);
      expect(errorSpy).toHaveBeenCalledWith(jasmine.objectContaining({ status: 403 }));
    });

    it('returns rejected promise and does not dispatch APPLY_FILTER_FAILED if failed to load results', function() {
      // mock update filters
      $httpBackend.expectPUT(CLMLocations.getDashboardFilters()).respond('update filters response');

      // mock load results
      var deferred = $q.defer();
      dashboardDataServiceMock.getNewestRisks.and.returnValue(deferred.promise);

      var errorSpy = jasmine.createSpy('errorSpy');
      var store = SpecUtil.mockReduxStore(initialState);
      var actions = store.getActions();

      store.dispatch(dashboardFilterActions.applyFilter('test filters', 'test filter name')).catch(errorSpy);
      $httpBackend.flush();
      deferred.reject('load results error');
      $rootScope.$apply();
      expect(errorSpy).toHaveBeenCalledWith('load results error');
      expect(actions.length).toBe(4);
      expect(actions[3].type).toBe('LOAD_RESULTS_FAILED');
    });
  });

  describe('applySavedFilter', function() {
    var savedFilter = {
      filter: 'test filters',
      name: 'test filter name'
    };

    afterEach(function() {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    });

    it('updates filters and loads results', function() {
      // mock update filters
      $httpBackend.expectPUT(CLMLocations.getDashboardFilters()).respond('update filters response');

      // mock load results
      var expectedSortFields = initialState.dashboard.violations.sortFields;
      var deferred = $q.defer();
      dashboardDataServiceMock.getNewestRisks.and.returnValue(deferred.promise);

      var successSpy = jasmine.createSpy('successSpy');
      var store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(dashboardFilterActions.applySavedFilter(savedFilter)).then(successSpy);

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({type: 'APPLY_FILTER_REQUESTED'});

      $httpBackend.flush();
      expect(dashboardDataServiceMock.getNewestRisks).toHaveBeenCalledWith('current filters', expectedSortFields);
      expect(store.getActions().length).toBe(3);
      expect(store.getActions()[1]).toEqual({
        type: 'APPLY_FILTER_FULFILLED',
        payload: {
          filter: 'update filters response',
          basedOnFilterName: 'test filter name'
        }
      });
      expect(store.getActions()[2]).toEqual({
        type: 'LOAD_RESULTS_REQUESTED',
        payload: 'violations'
      });

      deferred.resolve({results: 'results', numResults: 3, classyBrew: 'classyBrew'});
      $rootScope.$apply();
      expect(successSpy).toHaveBeenCalled();
      expect(store.getActions().length).toBe(4);
      expect(store.getActions()[3]).toEqual({
        type: 'LOAD_RESULTS_FULFILLED',
        payload: {
          resultsType: 'violations',
          results: 'results',
          numResults: 3,
          classyBrew: 'classyBrew'
        }
      });
    });

    it('dispatches APPLY_SAVED_FILTER_FAILED if failed to update filters', function() {
      // mock update filters
      $httpBackend.expectPUT(CLMLocations.getDashboardFilters()).respond(403);

      var errorSpy = jasmine.createSpy('errorSpy');
      var store = SpecUtil.mockReduxStore(initialState);
      var actions = store.getActions();
      store.dispatch(dashboardFilterActions.applySavedFilter(savedFilter)).catch(errorSpy);

      expect(actions.length).toBe(1);
      expect(actions[0]).toEqual({type: 'APPLY_FILTER_REQUESTED'});

      $httpBackend.flush();
      expect(dashboardDataServiceMock.getNewestRisks).not.toHaveBeenCalled();
      expect(actions.length).toBe(2);
      expect(actions[1]).toEqual({
        type: 'APPLY_SAVED_FILTER_FAILED',
        payload: 'test filter name'
      });
      expect(errorSpy).toHaveBeenCalledWith(jasmine.objectContaining({ status: 403 }));
    });

    it('returns rejected promise and does not dispatch APPLY_SAVED_FILTER_FAILED if failed to load results', () => {
      // mock update filters
      $httpBackend.expectPUT(CLMLocations.getDashboardFilters()).respond('update filters response');

      // mock load results
      var deferred = $q.defer();
      dashboardDataServiceMock.getNewestRisks.and.returnValue(deferred.promise);

      var errorSpy = jasmine.createSpy('errorSpy');
      var store = SpecUtil.mockReduxStore(initialState);
      var actions = store.getActions();

      store.dispatch(dashboardFilterActions.applySavedFilter(savedFilter)).catch(errorSpy);
      $httpBackend.flush();
      deferred.reject('load results error');
      $rootScope.$apply();
      expect(errorSpy).toHaveBeenCalledWith('load results error');
      expect(actions.length).toBe(4);
      expect(actions[3].type).toBe('LOAD_RESULTS_FAILED');
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
        basedOnFilterName: 'test name'
      });
      dashboardDataServiceMock.getNewestRisks.and.returnValue(deferred.promise);

      store.dispatch(dashboardFilterActions.refreshViolationsDetails()).then(successSpy);
      expect(store.getActions().length).toBe(0);

      $httpBackend.flush();

      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[0]).toEqual({
        type: 'APPLY_FILTER_FULFILLED',
        payload: {
          filter: 'new filters',
          basedOnFilterName: 'test name'
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
