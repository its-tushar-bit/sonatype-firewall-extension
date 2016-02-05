describe('Proprietary components', function() {
  'use strict';

  var vm,
      proprietaryConfig = { packages: ['foo'], regexes: ['bar']};

  beforeEach(module('proprietary.configuration.module', 'CLMLocation', function($provide){
    SpecUtil.mockPermissionService($provide);
  }));

  afterEach(inject(function($httpBackend) {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

  function getViewModel($controller) {
    return $controller('proprietary.configuration.controller', { $scope: {$on: function(){}}, isAuthorized : true });
  }

  describe('Load data from the server', function() {
    it('Success', inject(function($controller, $httpBackend, CLMLocations) {
      $httpBackend.expectGET(CLMLocations.getProprietaryConfig()).respond(proprietaryConfig);
      vm = getViewModel($controller);
      $httpBackend.flush();

      expect(vm.packages).toEqual(['foo']);
      expect(vm.regexes).toEqual(['bar']);
      expect(vm.loadError).toBeUndefined();
    }));

    it('Error from the server', inject(function($controller, $httpBackend, CLMLocations) {
      $httpBackend.expectGET(CLMLocations.getProprietaryConfig()).respond(500, 'A Random Error');
      vm = getViewModel($controller);
      $httpBackend.flush();
      expect(vm.packages).toBeUndefined();
      expect(vm.regexes).toBeUndefined();
      expect(vm.loadError).toEqual('A Random Error');
    }));
  });

  it('Reset local data before saving', inject(function($controller, $httpBackend, CLMLocations) {
    $httpBackend.expectGET(CLMLocations.getProprietaryConfig()).respond(proprietaryConfig);
    vm = getViewModel($controller);
    $httpBackend.flush();

    expect(vm.packages).toEqual(['foo']);
    expect(vm.regexes).toEqual(['bar']);
    vm.packages.push('bar');
    vm.regexes.push('foo');
    expect(vm.proprietary.packages).toEqual(['foo']);
    expect(vm.proprietary.regexes).toEqual(['bar']);

    vm.reset();
    expect(vm.packages).toEqual(['foo']);
    expect(vm.regexes).toEqual(['bar']);
  }));

  describe('Save local changes back to the server', function() {
    it('Success', inject(function($controller, $httpBackend, CLMLocations) {
      $httpBackend.expectGET(CLMLocations.getProprietaryConfig()).respond(proprietaryConfig);
      vm = getViewModel($controller);
      $httpBackend.flush();

      vm.packages.push('bar');
      vm.regexes.push('foo');

      $httpBackend.expectPUT(CLMLocations.getProprietaryConfig() + '/update').respond(204);
      vm.save();
      expect(vm.saving).toEqual(true);
      $httpBackend.flush();

      expect(vm.saving).toEqual(false);
      expect(vm.packages).toEqual(['foo', 'bar']);
      expect(vm.proprietary.packages).toEqual(['foo', 'bar']);
      expect(vm.regexes).toEqual(['bar', 'foo']);
      expect(vm.proprietary.regexes).toEqual(['bar', 'foo']);
    }));

    it('Error saving changes', inject(function($controller, $httpBackend, CLMLocations) {
      $httpBackend.expectGET(CLMLocations.getProprietaryConfig()).respond(proprietaryConfig);
      vm = getViewModel($controller);
      $httpBackend.flush();

      vm.packages.push('bar');
      vm.regexes.push('foo');

      $httpBackend.expectPUT(CLMLocations.getProprietaryConfig() + '/update').respond(500, 'A Random Error');
      vm.save();
      $httpBackend.flush();

      expect(vm.proprietary.packages).toEqual(['foo']);
      expect(vm.packages).toEqual(['foo', 'bar']);
      expect(vm.proprietary.regexes).toEqual(['bar']);
      expect(vm.regexes).toEqual(['bar', 'foo']);
      expect(vm.error[0].msg).toEqual('A Random Error');
    }));
  });

  describe('ProprietaryConfigurationController "isDirty"', function() {
    beforeEach(inject(function($controller, $httpBackend, CLMLocations) {
          $httpBackend.expectGET(CLMLocations.getProprietaryConfig()).respond(proprietaryConfig);
          vm = getViewModel($controller);
          $httpBackend.flush();
          expect(vm.isDirty()).toBeFalsy();
        }
    ));
    it('Should be true if we have added a proprietary package', function() {
      vm.packages.push('bar');
      expect(vm.isDirty()).toBeTruthy();
    });

    it('Should be true if we have added a proprietary regex', function() {
      vm.regexes.push('bar');
      expect(vm.isDirty()).toBeTruthy();
    });

    it('Should be true if we have deleted a proprietary package', function() {
      vm.packages.length = 0;
      expect(vm.isDirty()).toBeTruthy();
    });

    it('Should be true if we have deleted a proprietary reges', function() {
      vm.regexes.length = 0;
      expect(vm.isDirty()).toBeTruthy();
    });

    it('Should be false if we have both added and deleted the same proprietary regex', function() {
      vm.regexes.length = 0;
      expect(vm.isDirty()).toBeTruthy();

      vm.regexes.push('bar');
      expect(vm.isDirty()).toBeFalsy();
    });

    it('Should be false if we have reset', function() {
      vm.packages.length = 0;
      vm.regexes.length = 0;
      expect(vm.isDirty()).toBeTruthy();

      vm.reset();
      expect(vm.isDirty()).toBeFalsy();
      expect(vm.packages.length).toBeGreaterThan(0);
      expect(vm.regexes.length).toBeGreaterThan(0);
    });
  });
});
