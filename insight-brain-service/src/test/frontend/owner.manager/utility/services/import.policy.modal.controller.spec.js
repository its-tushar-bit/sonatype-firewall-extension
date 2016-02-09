describe('import.policy.modal.controller.spec.js', function() {
  var scope,
      vm,
      $httpBackend,
      $timeout,
      CLMAppLocations;

  beforeEach(module('owner.manager.module', function($provide) {
    $provide.value('$cookies', {
      get: angular.noop
    });
  }));

  beforeEach(inject(function($rootScope, $q, $controller, _$httpBackend_, _$timeout_, _CLMAppLocations_) {
    scope = $rootScope.$new();
    $httpBackend = _$httpBackend_;
    $timeout = _$timeout_;
    CLMAppLocations = _CLMAppLocations_;

    vm = $controller('import.policy.modal.controller',
        {$scope: scope});
    vm.importPolicyMask = {wrap: SpecUtil.promiseWrapper($q)};
  }));

  it('Test Form validation', function() {
    expect(vm.importFile).toBeFalsy();
    vm.importFile = 'testfile';
  });

  describe('Policy Import', function() {
    beforeEach(inject(function($window) {
      $window.FormData = angular.noop;
    }));

    function validateInitialState() {
      expect(vm.importFile).toBeUndefined();
      expect(vm.error).toBeUndefined();
    }

    it('Test import failure', function() {
      validateInitialState();

      $httpBackend.expectPOST(CLMAppLocations.getImportPolicyUrl()).respond(500, 'Some failure');

      vm.doSubmit();
      $httpBackend.flush();
      
      expect(vm.error).toBeDefined();
    });

    it('Test import success', function() {
      validateInitialState();
      scope.$close = jasmine.createSpy('close');

      $httpBackend.expectPOST(CLMAppLocations.getImportPolicyUrl()).respond({
        ownerName: 'test'
      });

      vm.doSubmit()
      $httpBackend.flush();

      expect(scope.$close).toHaveBeenCalled();
      expect(vm.error).toBeUndefined();
    });

    describe('IE9', function () {
      it('Error', inject(function ($window) {
        $window.FormData = null;
        validateInitialState();

        vm.doSubmit();

        vm.uploaded('Error');
        scope.$apply();

        expect(vm.error).toEqual('Error');
      }));

      it('Successful', inject(function ($window) {
        scope.$close = jasmine.createSpy('close');
        $window.FormData = null;
        validateInitialState();

        vm.doSubmit();

        vm.uploaded();
        scope.$apply();
        $timeout.flush();

        expect(scope.$close).toHaveBeenCalled();
        expect(vm.error).toBeFalsy();
      }));
    });
  });
});
