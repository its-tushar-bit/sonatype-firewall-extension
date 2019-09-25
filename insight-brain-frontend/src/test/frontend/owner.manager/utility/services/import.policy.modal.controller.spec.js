import ownerManagerModule from '../../../../../main/frontend/owner.manager/owner.manager.module';

describe('import.policy.modal.controller.spec.js', function() {
  var scope,
      vm,
      $httpBackend,
      $timeout,
      CLMContextLocations;

  beforeEach(angular.mock.module(ownerManagerModule.name, function($provide) {
    $provide.value('$cookies', {
      get: angular.noop
    });
  }));

  beforeEach(inject(function($rootScope, $q, $controller, _$httpBackend_, _$timeout_, _CLMContextLocations_) {
    scope = $rootScope.$new();
    scope.$dismiss = jasmine.createSpy('$dismiss');

    $httpBackend = _$httpBackend_;
    $timeout = _$timeout_;
    CLMContextLocations = _CLMContextLocations_;

    vm = $controller('import.policy.modal.controller',
        {$scope: scope});
    vm.importPolicyMask = {wrap: SpecUtil.promiseWrapper($q)};
  }));

  it('Test Form validation', function() {
    expect(vm.importFile).toBeFalsy();
    vm.importFile = 'testfile';
  });

  it('dismisses on navigating away', inject(function ($rootScope) {
    $rootScope.$broadcast('pageChangeAccepted');
    expect(scope.$dismiss).toHaveBeenCalled();
  }));

  describe('Policy Import', function() {
    let originalFormData;

    beforeEach(inject(function($window) {
      originalFormData = $window.FormData;
      $window.FormData = angular.noop;
    }));

    afterEach(inject(function($window) {
      $window.FormData = originalFormData;
    }));

    function validateInitialState() {
      expect(vm.importFile).toBeUndefined();
      expect(vm.error).toBeUndefined();
    }

    it('Test import failure', function() {
      validateInitialState();

      $httpBackend.expectPOST(CLMContextLocations.getImportPolicyUrl()).respond(500, 'Some failure');

      vm.doSubmit();
      $httpBackend.flush();

      expect(vm.error).toBeDefined();
    });

    it('Test import success', inject(function(PolicyHierarchyStore) {
      validateInitialState();
      scope.$close = jasmine.createSpy('close');

      $httpBackend.expectPOST(CLMContextLocations.getImportPolicyUrl()).respond({
        ownerName: 'test'
      });
      spyOn(PolicyHierarchyStore, 'refresh');

      vm.doSubmit();
      $httpBackend.flush();

      expect(scope.$close).toHaveBeenCalled();
      expect(PolicyHierarchyStore.refresh).toHaveBeenCalled();
      expect(vm.error).toBeUndefined();
    }));

    describe('IE9', function () {
      it('Error', inject(function ($window) {
        $window.FormData = null;
        validateInitialState();

        vm.doSubmit();

        vm.uploaded('Error');
        scope.$apply();

        expect(vm.error).toEqual('Error');
      }));

      it('Successful', inject(function ($window, PolicyHierarchyStore) {
        scope.$close = jasmine.createSpy('close');
        $window.FormData = null;
        validateInitialState();
        spyOn(PolicyHierarchyStore, 'refresh');

        vm.doSubmit();

        vm.uploaded();
        scope.$apply();
        $timeout.flush();

        expect(scope.$close).toHaveBeenCalled();
        expect(PolicyHierarchyStore.refresh).toHaveBeenCalled();
        expect(vm.error).toBeFalsy();
      }));
    });
  });
});
