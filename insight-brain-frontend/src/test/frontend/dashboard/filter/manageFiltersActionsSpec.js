/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import dashboardFilterModule from '../../../../main/frontend/dashboard/filter/module';

describe('manageFiltersActions', function() {
  var manageFiltersActions,
      $httpBackend,
      $rootScope,
      $q,
      CLMLocations,
      dashboardFilterService,
      initialState = {
        manageFilters: {
          savedFilters: null
        }
      };

  beforeEach(angular.mock.module(dashboardFilterModule.name));

  beforeEach(inject(function(_manageFiltersActions_, _$httpBackend_, _$rootScope_, _CLMLocations_, _$q_,
                             _dashboardFilterService_) {
    manageFiltersActions = _manageFiltersActions_;
    $httpBackend = _$httpBackend_;
    CLMLocations = _CLMLocations_;
    $rootScope = _$rootScope_;
    dashboardFilterService = _dashboardFilterService_;
    $q = _$q_;
  }));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingExpectation(false);
    $httpBackend.verifyNoOutstandingRequest();
  });

  describe('fetchSavedFilters', function() {
    it('gets the saved filters from the backend and then dispatches FETCH_SAVED_FILTERS_FULFILLED', function() {
      var response = [{ name: 'foo' }],
          mockReduxStore = SpecUtil.mockReduxStore(),
          resolveSpy = jasmine.createSpy('resolve');

      $httpBackend.expectGET(CLMLocations.getDashboardSavedFilters()).respond(response);

      mockReduxStore.dispatch(manageFiltersActions.fetchSavedFilters()).then(resolveSpy);

      var actions = mockReduxStore.getActions();

      expect(actions.length).toBe(0);
      expect(resolveSpy).not.toHaveBeenCalled();

      $httpBackend.flush();

      actions = mockReduxStore.getActions();

      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('FETCH_SAVED_FILTERS_FULFILLED');
      expect(actions[0].payload).toEqual(response);
      expect(resolveSpy).toHaveBeenCalled();
    });

    it('dispatches FETCH_SAVED_FILTERS_FAILED when the backend call fails', function() {
      var mockReduxStore = SpecUtil.mockReduxStore(),
          rejectSpy = jasmine.createSpy('reject');

      $httpBackend.expectGET(CLMLocations.getDashboardSavedFilters()).respond(403);

      mockReduxStore.dispatch(manageFiltersActions.fetchSavedFilters()).catch(rejectSpy);

      var actions = mockReduxStore.getActions();

      expect(actions.length).toBe(0);
      expect(rejectSpy).not.toHaveBeenCalled();

      $httpBackend.flush();

      actions = mockReduxStore.getActions();

      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('FETCH_SAVED_FILTERS_FAILED');
      expect(actions[0].payload.status).toEqual(403);
      expect(rejectSpy).toHaveBeenCalledWith(jasmine.objectContaining({ status: 403 }));
    });
  });

  describe('saveFilter', function() {
    var initialState = {
          dashboardFilter: {
            filterJson: { applications: ['1234'] }
          }
        },
        expectedPUTBody = {
          name: 'foo',
          filter: { applicationFilters: ['1234'] }
        };

    it('immediately sends a SAVE_FILTER_REQUESTED action', function() {
      spyOn(dashboardFilterService, 'filterToJson').and.returnValue(expectedPUTBody.filter);
      var mockReduxStore = SpecUtil.mockReduxStore(initialState);

      $httpBackend.expectPUT(CLMLocations.getDashboardSavedFilters(), expectedPUTBody).respond({});
      $httpBackend.expectGET(CLMLocations.getDashboardSavedFilters()).respond({});

      mockReduxStore.dispatch(manageFiltersActions.saveFilter('foo'));

      var actions = mockReduxStore.getActions();

      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('SAVE_FILTER_REQUESTED');
      expect(actions[0].payload).toBeUndefined();

      $httpBackend.flush();
    });

    it('PUTs the filter to the server and then dispatches SAVE_FILTERS_FULFILLED and fetches the saved filters',
        function() {
          spyOn(dashboardFilterService, 'filterToJson').and.returnValue(expectedPUTBody.filter);
          var mockReduxStore = SpecUtil.mockReduxStore(initialState),
              putSavedFilterResponse = { foo: 'bar' },
              getSavedFiltersResponse = { baz: 'buzz' },
              successSpy = jasmine.createSpy('success');

          $httpBackend.expectPUT(CLMLocations.getDashboardSavedFilters(), expectedPUTBody)
              .respond(putSavedFilterResponse);
          $httpBackend.expectGET(CLMLocations.getDashboardSavedFilters()).respond(getSavedFiltersResponse);

          mockReduxStore.dispatch(manageFiltersActions.saveFilter('foo')).then(successSpy);
          $rootScope.$digest();

          expect(mockReduxStore.getActions().length).toBe(1);
          expect(successSpy).not.toHaveBeenCalled();

          $httpBackend.flush(1);
          $rootScope.$digest();

          var actions = mockReduxStore.getActions();

          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe('SAVE_FILTER_FULFILLED');
          expect(actions[1].payload).toEqual(putSavedFilterResponse);
          expect(successSpy).not.toHaveBeenCalled();

          $httpBackend.flush();
          $rootScope.$digest();

          actions = mockReduxStore.getActions();

          expect(actions.length).toBe(3);
          expect(actions[2].type).toBe('FETCH_SAVED_FILTERS_FULFILLED');
          expect(actions[2].payload).toEqual(getSavedFiltersResponse);
          expect(successSpy).toHaveBeenCalled();
        }
    );

    it('dispatches SAVE_FILTER_FAILED and does not fetch the saved filters if the PUT fails', function() {
      spyOn(dashboardFilterService, 'filterToJson').and.returnValue(expectedPUTBody.filter);
      var mockReduxStore = SpecUtil.mockReduxStore(initialState),
          failureSpy = jasmine.createSpy('failure');

      $httpBackend.expectPUT(CLMLocations.getDashboardSavedFilters()).respond(403);

      mockReduxStore.dispatch(manageFiltersActions.saveFilter('foo')).catch(failureSpy);
      $rootScope.$digest();

      expect(mockReduxStore.getActions().length).toBe(1);
      expect(failureSpy).not.toHaveBeenCalled();

      $httpBackend.flush(1);
      $rootScope.$digest();

      var actions = mockReduxStore.getActions();

      expect(actions.length).toBe(2);
      expect(actions[1].type).toBe('SAVE_FILTER_FAILED');
      expect(actions[1].payload.status).toBe(403);
      expect(failureSpy).toHaveBeenCalledWith(jasmine.objectContaining({ status: 403 }));
    });

    it('rejects the returned promise if the saved filter fetching fails', function() {
      spyOn(dashboardFilterService, 'filterToJson').and.returnValue(expectedPUTBody.filter);
      var mockReduxStore = SpecUtil.mockReduxStore(initialState),
          putSavedFilterResponse = { foo: 'bar' },
          failureSpy = jasmine.createSpy('failure');

      $httpBackend.expectPUT(CLMLocations.getDashboardSavedFilters(), expectedPUTBody)
          .respond(putSavedFilterResponse);
      $httpBackend.expectGET(CLMLocations.getDashboardSavedFilters()).respond(403);

      mockReduxStore.dispatch(manageFiltersActions.saveFilter('foo')).catch(failureSpy);
      $rootScope.$digest();

      expect(mockReduxStore.getActions().length).toBe(1);
      expect(failureSpy).not.toHaveBeenCalled();

      $httpBackend.flush();
      $rootScope.$digest();

      var actions = mockReduxStore.getActions();

      expect(actions.length).toBe(3);
      expect(actions[2].type).toBe('FETCH_SAVED_FILTERS_FAILED');
      expect(actions[2].payload.status).toBe(403);
      expect(failureSpy).toHaveBeenCalledWith(jasmine.objectContaining({ status: 403 }));
    });
  });

  describe('deleteSpecifiedFilters', function() {
    it('immediately dispatches a DELETE_SPECIFIED_FILTERS_REQUESTED action with no payload', function() {
      var mockReduxStore = SpecUtil.mockReduxStore(initialState),
          serviceDeferred = $q.defer(),
          servicePromise = serviceDeferred.promise;

      spyOn(dashboardFilterService, 'deleteSavedFilters').and.returnValue(servicePromise);

      mockReduxStore.dispatch(manageFiltersActions.deleteSpecifiedFilters());

      var actions = mockReduxStore.getActions();

      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('DELETE_SPECIFIED_FILTERS_REQUESTED');
      expect(actions[0].payload).toBeUndefined();
    });

    it('calls filterService.deleteSavedFilters with its parameter and then dispatches ' +
        'DELETE_SPECIFIED_FILTERS_FULFILLED after that completes', function() {
      var mockReduxStore = SpecUtil.mockReduxStore(initialState),
          serviceDeferred = $q.defer(),
          servicePromise = serviceDeferred.promise,
          filtersToDelete = ['foo', 'bar'];

      spyOn(dashboardFilterService, 'deleteSavedFilters').and.returnValue(servicePromise);
      $httpBackend.expectGET(CLMLocations.getDashboardSavedFilters()).respond({});

      mockReduxStore.dispatch(manageFiltersActions.deleteSpecifiedFilters(filtersToDelete));

      var actions = mockReduxStore.getActions();

      expect(actions.length).toBe(1);
      expect(dashboardFilterService.deleteSavedFilters).toHaveBeenCalledWith(filtersToDelete);

      serviceDeferred.resolve();
      $rootScope.$digest();

      actions = mockReduxStore.getActions();

      expect(actions.length).toBe(2);
      expect(actions[1].type).toBe('DELETE_SPECIFIED_FILTERS_FULFILLED');
      expect(actions[1].payload).toBe(filtersToDelete);

      $httpBackend.flush();
    });

    it('fetches the saved filters after deleteSavedFilters completes', function() {
      var mockReduxStore = SpecUtil.mockReduxStore(initialState),
          serviceDeferred = $q.defer(),
          servicePromise = serviceDeferred.promise,
          getSavedFiltersResponse = { foo: 'bar' },
          successSpy = jasmine.createSpy('success');

      spyOn(dashboardFilterService, 'deleteSavedFilters').and.returnValue(servicePromise);
      $httpBackend.expectGET(CLMLocations.getDashboardSavedFilters()).respond(getSavedFiltersResponse);

      mockReduxStore.dispatch(manageFiltersActions.deleteSpecifiedFilters()).then(successSpy);

      var actions = mockReduxStore.getActions();

      expect(actions.length).toBe(1);
      expect(successSpy).not.toHaveBeenCalled();

      serviceDeferred.resolve();
      $rootScope.$digest();

      actions = mockReduxStore.getActions();

      expect(actions.length).toBe(2);
      expect(successSpy).not.toHaveBeenCalled();

      $httpBackend.flush();

      actions = mockReduxStore.getActions();

      expect(actions.length).toBe(3);
      expect(actions[2].type).toBe('FETCH_SAVED_FILTERS_FULFILLED');
      expect(actions[2].payload).toEqual(getSavedFiltersResponse);
      expect(successSpy).toHaveBeenCalled();
    });

    it('dispatches DELETE_SPECIFIED_FILTERS_FAILED and rejects the promise if deleteSavedFilters fails', function() {
      var mockReduxStore = SpecUtil.mockReduxStore(initialState),
          serviceDeferred = $q.defer(),
          servicePromise = serviceDeferred.promise,
          failureSpy = jasmine.createSpy('failure');

      spyOn(dashboardFilterService, 'deleteSavedFilters').and.returnValue(servicePromise);

      mockReduxStore.dispatch(manageFiltersActions.deleteSpecifiedFilters()).catch(failureSpy);

      var actions = mockReduxStore.getActions();

      expect(actions.length).toBe(1);
      expect(failureSpy).not.toHaveBeenCalled();

      serviceDeferred.reject('error!');
      $rootScope.$digest();

      actions = mockReduxStore.getActions();

      expect(actions.length).toBe(2);
      expect(actions[1].type).toBe('DELETE_SPECIFIED_FILTERS_FAILED');
      expect(actions[1].payload).toEqual('error!');
      expect(failureSpy).toHaveBeenCalledWith('error!');
    });

    it('rejects the promise if fetching the saved filters fails but does not dispatch DELETE_SPECIFIED_FILTERS_FAILED',
        function() {
          var mockReduxStore = SpecUtil.mockReduxStore(initialState),
              serviceDeferred = $q.defer(),
              servicePromise = serviceDeferred.promise,
              failureSpy = jasmine.createSpy('failure');

          spyOn(dashboardFilterService, 'deleteSavedFilters').and.returnValue(servicePromise);
          $httpBackend.expectGET(CLMLocations.getDashboardSavedFilters()).respond(403);

          mockReduxStore.dispatch(manageFiltersActions.deleteSpecifiedFilters()).catch(failureSpy);

          var actions = mockReduxStore.getActions();

          expect(actions.length).toBe(1);
          expect(failureSpy).not.toHaveBeenCalled();

          serviceDeferred.resolve();
          $rootScope.$digest();

          actions = mockReduxStore.getActions();

          expect(actions.length).toBe(2);
          expect(failureSpy).not.toHaveBeenCalled();

          $httpBackend.flush();

          actions = mockReduxStore.getActions();

          expect(actions.length).toBe(3);
          expect(actions[2].type).toBe('FETCH_SAVED_FILTERS_FAILED');
          expect(actions[2].payload.status).toEqual(403);
          expect(failureSpy).toHaveBeenCalledWith(jasmine.objectContaining({ status: 403 }));
        }
    );
  });

  describe('resetDeleteFiltersStatus', function() {
    it('immediately sends a RESET_DELETE_FILTERS_STATUS action with no payload', function() {
      var payload = {},
          mockReduxStore = SpecUtil.mockReduxStore(initialState);

      mockReduxStore.dispatch(manageFiltersActions.resetDeleteFiltersStatus(payload));

      var actions = mockReduxStore.getActions();

      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('RESET_DELETE_FILTERS_STATUS');
      expect(actions[0].payload).toBeUndefined();
    });
  });
});
