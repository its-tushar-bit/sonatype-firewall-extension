/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import dashboardFilterModule from '../../../../../main/frontend/dashboard/filter/module';
import legacyConfigurationModule from '../../../../../main/frontend/LegacyConfigurationModule';

describe('deleteFiltersModalController', function() {

  var unsubscribeSpy;

  beforeEach(angular.mock.module(dashboardFilterModule.name, legacyConfigurationModule.name, function($provide) {
    unsubscribeSpy = SpecUtil.mockNgRedux($provide);
  }));

  var vm,
      scope,
      actions;

  beforeEach(inject(function($rootScope, $controller) {
    actions = jasmine.createSpyObj('actions', ['deleteSpecifiedFilters', 'fetchSavedFilters']);

    scope = $rootScope.$new();
    vm = $controller('deleteFiltersModalController as vm', { $scope: scope, manageFiltersActions: actions }, {
      savedFilters: []
    });
  }));

  describe('$destroy event', function() {
    it('unsubcribes from the redux store', function() {
      expect(unsubscribeSpy).not.toHaveBeenCalled();

      scope.$destroy();

      expect(unsubscribeSpy).toHaveBeenCalled();
    });
  });

  describe('isDirty', function() {
    it('returns whether or not vm.filters has changed', function() {
      expect(vm.isDirty()).toBe(false);

      vm.filters['foo'] = true;
      scope.$digest();

      expect(vm.isDirty()).toBe(true);
    });
  });

  describe('deleteFilters', function() {
    var $q,
        DeleteModalService;

    beforeEach(inject(function(_$q_, _DeleteModalService_) {
      $q = _$q_;
      DeleteModalService = _DeleteModalService_;
    }));

    it('sets isLoading and deleteMode and calls DeleteModalService.deleteRedux if there are filters to delete',
        function() {
          vm.filters['foo'] = true;
          vm.isLoading = false;
          vm.deleteMode = false;
          scope.$digest();

          spyOn(DeleteModalService, 'deleteRedux').and.returnValue($q.defer().promise);

          vm.deleteFilters();

          expect(DeleteModalService.deleteRedux).toHaveBeenCalled();
          expect(vm.isLoading).toBe(true);
          expect(vm.deleteMode).toBe(true);
        }
    );

    it('does not call DeleteModalService.deleteRedux or set isLoading and deleteMode if there are no filters to delete',
        function() {
          vm.filters['foo'] = false;
          vm.isLoading = false;
          vm.deleteMode = false;

          spyOn(DeleteModalService, 'deleteRedux').and.returnValue($q.defer().promise);

          vm.deleteFilters();

          expect(DeleteModalService.deleteRedux).not.toHaveBeenCalled();
          expect(vm.isLoading).toBe(false);
          expect(vm.deleteMode).toBe(false);
        }
    );

    it('passes a continueAction which calls deleteSpecifiedFilters with the list of filters to delete', function() {
      vm.filters['foo'] = true;
      vm.filters['bar'] = false;
      scope.$digest();

      spyOn(DeleteModalService, 'deleteRedux').and.returnValue($q.defer().promise);

      vm.deleteFilters();

      var continueAction = DeleteModalService.deleteRedux.calls.first().args[3];

      expect(continueAction).toEqual(jasmine.any(Function));
      expect(actions.deleteSpecifiedFilters).not.toHaveBeenCalled();

      continueAction();

      expect(actions.deleteSpecifiedFilters).toHaveBeenCalledWith(['foo']);
    });

    it('passes a stateMapper which maps the errorState, deleting, and success from the manageFilters', function() {
      var stateToMap = Object.freeze({
        manageFilters: Object.freeze({
          deleteFiltersError: 'error!',
          deleteFiltersSaving: true,
          deleteFiltersSuccess: false
        })
      });

      vm.filters['foo'] = true;
      scope.$digest();

      spyOn(DeleteModalService, 'deleteRedux').and.returnValue($q.defer().promise);

      vm.deleteFilters();

      var stateMapper = DeleteModalService.deleteRedux.calls.first().args[4];

      expect(stateMapper).toEqual(jasmine.any(Function));

      var results = stateMapper(stateToMap);
      expect(results.errorState).toBe('error!');
      expect(results.deleting).toBe(true);
      expect(results.success).toBe(false);
    });

    it('calls $scope.$close when the service\'s promise is resolved', function() {
      var servicedDeferred = $q.defer(),
          servicePromise = servicedDeferred.promise;

      scope.$close = jasmine.createSpy('$close');
      spyOn(DeleteModalService, 'deleteRedux').and.returnValue(servicePromise);

      vm.filters['foo'] = true;
      scope.$digest();

      vm.deleteFilters();

      expect(scope.$close).not.toHaveBeenCalled();

      servicedDeferred.resolve();
      scope.$digest();

      expect(scope.$close).toHaveBeenCalled();
    });

    it('sets vm.deleteError, vm.deleteMode, and vm.isLoading, and calls fetchSavedFilters when the promise is rejected',
        function() {
          var servicedDeferred = $q.defer(),
              servicePromise = servicedDeferred.promise;

          spyOn(DeleteModalService, 'deleteRedux').and.returnValue(servicePromise);

          vm.filters['foo'] = true;
          scope.$digest();

          vm.deleteFilters();

          expect(actions.fetchSavedFilters).not.toHaveBeenCalled();

          servicedDeferred.reject('error!');
          scope.$digest();

          expect(vm.deleteError).toBe('error!');
          expect(vm.isLoading).toBe(false);
          expect(vm.deleteMode).toBe(false);
          expect(actions.fetchSavedFilters).toHaveBeenCalled();
        }
    );
  });

  describe('pageChangeStarted', function() {
    it('does not preventDefault when not dirty', function() {
      spyOn(vm, 'isDirty').and.returnValue(false);

      SpecUtil.expectStateChangeNotPrevented(scope);
      expect(vm.isDirty).toHaveBeenCalled();
    });

    it('prevents default and sets unsavedModalVisible to true when dirty', function() {
      spyOn(vm, 'isDirty').and.returnValue(true);
      vm.unsavedModalVisible = false;

      SpecUtil.expectStateChangePrevented(scope);
      expect(vm.isDirty).toHaveBeenCalled();
      expect(vm.unsavedModalVisible).toBe(true);
    });
  });

  describe('pageChangeCanceled', function() {
    it('sets unsavedModalVisible to false', function() {
      vm.unsavedModalVisible = true;

      scope.$broadcast('pageChangeCanceled');

      expect(vm.unsavedModalVisible).toBe(false);
    });
  });

  describe('pageChangeAccepted', function() {
    it('calls scope.$dismiss', function() {
      scope.$dismiss = jasmine.createSpy('$dismiss');

      scope.$broadcast('pageChangeAccepted');

      expect(scope.$dismiss).toHaveBeenCalled();
    });
  });

  describe('toggleSelected', function() {
    it('sets the specified property of vm.filters to true if it is falsy', function() {
      vm.filters['foo'] = false;
      vm.filters['bar'] = undefined;

      vm.toggleSelected('foo');

      expect(vm.filters['foo']).toBe(true);
      expect(vm.filters['bar']).toBeFalsy();

      vm.toggleSelected('bar');

      expect(vm.filters['foo']).toBe(true);
      expect(vm.filters['bar']).toBe(true);
    });

    it('sets the specified property of vm.filters to false if it is true', function() {
      vm.filters['foo'] = true;
      vm.filters['bar'] = undefined;

      vm.toggleSelected('foo');

      expect(vm.filters['foo']).toBe(false);
      expect(vm.filters['bar']).toBeFalsy();
    });
  });
});
