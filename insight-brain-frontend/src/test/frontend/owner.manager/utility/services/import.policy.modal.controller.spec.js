/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from 'MainRoot/owner.manager/owner.manager.module';

describe('import.policy.modal.controller.spec.js', function () {
  var scope, vm, $httpBackend, CLMContextLocations;

  beforeEach(
    angular.mock.module(ownerManagerModule.name, function ($provide) {
      $provide.value('$cookies', {
        get: angular.noop,
      });
      SpecUtil.mockNgRedux($provide);
    })
  );

  beforeEach(inject(function ($rootScope, $q, $controller, _$httpBackend_, _CLMContextLocations_) {
    scope = $rootScope.$new();
    scope.$dismiss = jasmine.createSpy('$dismiss');
    scope.$close = jasmine.createSpy('$close ');

    $httpBackend = _$httpBackend_;
    CLMContextLocations = _CLMContextLocations_;

    vm = $controller('import.policy.modal.controller', { $scope: scope });
    vm.importPolicyMask = { wrap: SpecUtil.promiseWrapper($q) };
  }));

  it('subscribes to the redux store', () => {
    expect(vm.unsubscribe).toBeDefined();
  });

  it('unsubscribes from the redux store', () => {
    expect(vm.unsubscribe).not.toHaveBeenCalled();
    scope.$destroy();
    expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
  });

  it('Test Form validation', function () {
    expect(vm.importFile).toBeFalsy();
    vm.importFile = 'testfile';
  });

  it('dismisses on navigating away', inject(function ($rootScope) {
    $rootScope.$broadcast('pageChangeAccepted');
    expect(scope.$dismiss).toHaveBeenCalled();
  }));

  describe('Policy Import', function () {
    let originalFormData, submitEvent;

    beforeEach(inject(function ($window) {
      originalFormData = $window.FormData;
      $window.FormData = angular.noop;
      submitEvent = jasmine.createSpyObj(['preventDefault']);
    }));

    afterEach(inject(function ($window) {
      $window.FormData = originalFormData;
    }));

    function validateInitialState() {
      expect(vm.importFile).toBeUndefined();
      expect(vm.error).toBeUndefined();
    }

    it('Test import failure', function () {
      validateInitialState();

      $httpBackend.expectPOST(CLMContextLocations.getImportPolicyUrl()).respond(500, 'Some failure');

      vm.doSubmit(submitEvent);
      $httpBackend.flush();

      expect(vm.error).toBeDefined();
      expect(submitEvent.preventDefault).toHaveBeenCalled();
    });

    it('Test import success', function () {
      validateInitialState();
      $httpBackend.expectPOST(CLMContextLocations.getImportPolicyUrl()).respond({ data: ['foo'], id: 'bar' });

      vm.doSubmit(submitEvent);
      $httpBackend.flush();

      expect(vm.loadApplicableLabels).toHaveBeenCalled();
      expect(vm.loadApplicableCategories).toHaveBeenCalled();
    });
  });
});
