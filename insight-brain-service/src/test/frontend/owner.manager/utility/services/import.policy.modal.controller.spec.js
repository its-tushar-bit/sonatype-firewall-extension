describe('import.policy.modal.controller.spec.js', function() {
  var scope,
      vm,
      $httpBackend,
      $timeout,
      CLMAppLocations;

  beforeEach(module('owner.manager.module', function($provide) {
    $provide.value('$cookies', {});
  }));

  beforeEach(inject(function($rootScope, $q, $controller, _$httpBackend_, _$timeout_, _CLMAppLocations_) {
    scope = $rootScope.$new();
    $httpBackend = _$httpBackend_;
    $timeout = _$timeout_;
    CLMAppLocations = _CLMAppLocations_;

    vm = $controller('import.policy.modal.controller',
        {$scope: scope});
  }));

  it('Test Form validation', function() {
    expect(vm.importFile).toBeFalsy();
    vm.importFile = 'testfile';
  });

  describe('Policy Import', function() {
    beforeEach(inject(function($window) {
      
      $window.FileReader = function() {

        this.readAsText = function() {
          this.onload();
        };
      };
      var original = angular.element;
      spyOn(angular, 'element').andCallFake(function(selector) {
        if (selector === '#importFile') {
          return [
            {
              files: [
                {
                  name: 'testfile'
                }
              ],
              value: '{}'
            }
          ];
        }
        return original(selector);
      });
    }));

    function validateInitialState() {
      expect(vm.importFile).toBeUndefined();
      expect(vm.error).toBeUndefined();
    }

    it('Test import failure', function() {
      validateInitialState();

      $httpBackend.expectPUT(CLMAppLocations.getImportPolicyUrl()).respond(500, 'Some failure');

      vm.doSubmit();
      $httpBackend.flush();
      
      expect(vm.error).toBeDefined();
    });

    it('Test import success', function() {
      validateInitialState();

      $httpBackend.expectPUT(CLMAppLocations.getImportPolicyUrl()).respond({
        ownerName: 'test'
      });

      vm.doSubmit()
      $httpBackend.flush();

      expect(vm.error).toBeUndefined();
    });

    describe('IE9', function () {
      it('Error', inject(function ($window) {
        $window.FileReader = null;
        validateInitialState();

        vm.doSubmit();

        // Progress notification
        vm.uploaded('Please wait...', false);
        expect(vm.error).toBeFalsy();

        vm.uploaded('Error', true);
        scope.$apply();

        expect(vm.error).toEqual('Error');
      }));

      it('Successful', inject(function ($window) {
        scope.$close = jasmine.createSpy('close');
        $window.FileReader = null;
        validateInitialState();

        vm.doSubmit();

        // Progress notification
        vm.uploaded('Please wait...', false);
        expect(vm.error).toBeFalsy();

        vm.uploaded('', true);
        scope.$apply();
        $timeout.flush();

        expect(scope.$close).toHaveBeenCalled();
        expect(vm.error).toBeFalsy();
      }));
    });
  });
});
