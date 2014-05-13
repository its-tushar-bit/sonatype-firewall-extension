describe('Proprietary components', function() {
  'use strict';

  var scope,
    controller,
    proprietaryConfig = { packages: ['foo'], regexes: ['bar']};

  beforeEach(module('Configuration', 'CLMLocation'));
  afterEach(inject(function($httpBackend) {
    scope.$destroy();
    scope = null;
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

  describe('Load data from the server', function() {
    it('Success', inject(function($controller, $rootScope, $httpBackend, CLMLocations) {
      scope = $rootScope.$new();
      $httpBackend.expectGET(CLMLocations.getProprietaryConfig()).respond(proprietaryConfig);
      controller = $controller('ProprietaryConfigurationController', { $scope: scope });
      $httpBackend.flush();

      expect(scope.packages).toEqual(['foo']);
      expect(scope.regexes).toEqual(['bar']);
      expect(scope.loadError).toBeUndefined();
    }));

    it('Error from the server', inject(function($controller, $rootScope, $httpBackend, CLMLocations) {
      scope = $rootScope.$new();
      $httpBackend.expectGET(CLMLocations.getProprietaryConfig()).respond(500, 'A Random Error');
      controller = $controller('ProprietaryConfigurationController', { $scope: scope });
      $httpBackend.flush();
      expect(scope.packages).toBeUndefined();
      expect(scope.regexes).toBeUndefined();
      expect(scope.loadError).toEqual('A Random Error');
    }));
  });

  it('Reset local data before saving', inject(function($controller, $rootScope, $httpBackend, CLMLocations) {
    scope = $rootScope.$new();

    $httpBackend.expectGET(CLMLocations.getProprietaryConfig()).respond(proprietaryConfig);
    controller = $controller('ProprietaryConfigurationController', { $scope: scope });
    $httpBackend.flush();

    expect(scope.packages).toEqual(['foo']);
    expect(scope.regexes).toEqual(['bar']);
    scope.packages.push('bar');
    scope.regexes.push('foo');
    expect(scope.proprietary.packages).toEqual(['foo']);
    expect(scope.proprietary.regexes).toEqual(['bar']);

    scope.reset();
    expect(scope.packages).toEqual(['foo']);
    expect(scope.regexes).toEqual(['bar']);
  }));

  describe('Save local changes back to the server', function() {
    it('Success', inject(function($controller, $rootScope, $httpBackend, CLMLocations) {
      scope = $rootScope.$new();
      $httpBackend.expectGET(CLMLocations.getProprietaryConfig()).respond(proprietaryConfig);
      controller = $controller('ProprietaryConfigurationController', { $scope: scope });
      $httpBackend.flush();

      scope.packages.push('bar');
      scope.regexes.push('foo');

      $httpBackend.expectPUT(CLMLocations.getProprietaryConfig() + '/update').respond(204);
      scope.save();
      expect(scope.saving).toEqual(true);
      $httpBackend.flush();

      expect(scope.saving).toEqual(false);
      expect(scope.packages).toEqual(['foo', 'bar']);
      expect(scope.proprietary.packages).toEqual(['foo', 'bar']);
      expect(scope.regexes).toEqual(['bar', 'foo']);
      expect(scope.proprietary.regexes).toEqual(['bar', 'foo']);
    }));

    it('Error saving changes', inject(function($controller, $rootScope, $httpBackend, CLMLocations) {
      scope = $rootScope.$new();
      $httpBackend.expectGET(CLMLocations.getProprietaryConfig()).respond(proprietaryConfig);
      controller = $controller('ProprietaryConfigurationController', { $scope: scope });
      $httpBackend.flush();

      scope.packages.push('bar');
      scope.regexes.push('foo');

      $httpBackend.expectPUT(CLMLocations.getProprietaryConfig() + '/update').respond(500, 'A Random Error');
      scope.save();
      $httpBackend.flush();

      expect(scope.proprietary.packages).toEqual(['foo']);
      expect(scope.packages).toEqual(['foo', 'bar']);
      expect(scope.proprietary.regexes).toEqual(['bar']);
      expect(scope.regexes).toEqual(['bar', 'foo']);
      expect(scope.error).toEqual('A Random Error');
    }));
  });

  describe('Validation of inputs', function() {

    beforeEach(inject(function($controller, $rootScope, $httpBackend, CLMLocations){
      scope = $rootScope.$new();
      $httpBackend.expectGET(CLMLocations.getProprietaryConfig()).respond(proprietaryConfig);
      controller = $controller('ProprietaryConfigurationController', { $scope: scope });
      $httpBackend.flush();
    }));

    it('Good Inputs', function() {
      expect(scope.validatePackage('com.sonatype')).toBeTruthy();
      expect(scope.error).toBeNull();
    });

    //see CLM-1097
    it('Should treat an empty entry as valid', function(){
      expect(scope.validatePackage('')).toBeTruthy();
      expect(scope.error).toBeNull();
    });

    it('Bad package inputs', function() {
      expect(scope.validatePackage('com sonatype')).toBeFalsy();
      expect(scope.error).toMatch(/invalid.*/i);
      expect(scope.validatePackage('com/sonatype')).toBeFalsy();
      expect(scope.error).toMatch(/invalid.*/i);
      expect(scope.validatePackage('com.sonatype.')).toBeFalsy();
      expect(scope.error).toMatch(/invalid.*/i);
      expect(scope.validatePackage('.com.sonatype')).toBeFalsy();
      expect(scope.error).toMatch(/invalid.*/i);
      expect(scope.validatePackage('com.sonatype.*')).toBeFalsy();
      expect(scope.error).toMatch(/wildcards.*/i);
      expect(scope.validatePackage('com.sonatype.**')).toBeFalsy();
      expect(scope.error).toMatch(/wildcards.*/i);
      expect(scope.validatePackage('com.sona*')).toBeFalsy();
      expect(scope.error).toMatch(/wildcards.*/i);
      expect(scope.validatePackage('*.sonatype')).toBeFalsy();
      expect(scope.error).toMatch(/wildcards.*/i);
      expect(scope.validatePackage('foo')).toBeFalsy();
      expect(scope.error).toBe('Package already specified');
    });

    it('Bad regex inputs', function() {
      //presently only checking to ensure regexes are unique
      expect(scope.validateRegex('bar')).toBeFalsy();
      expect(scope.error).toBe('Regex already specified');
    });
  });

  describe('ProprietaryConfigurationController "isDirty"', function() {
    beforeEach(inject(function($controller, $rootScope, $httpBackend, CLMLocations) {
        scope = $rootScope.$new();
        $httpBackend.expectGET(CLMLocations.getProprietaryConfig()).respond(proprietaryConfig);
        controller = $controller('ProprietaryConfigurationController', { $scope: scope });
        $httpBackend.flush();
        expect(scope.isDirty()).toBeFalsy();
      }
    ));
    it('Should be true if we have added a proprietary package', function() {
      scope.packages.push('bar');
      expect(scope.isDirty()).toBeTruthy();
    });

    it('Should be true if we have added a proprietary regex', function() {
      scope.regexes.push('bar');
      expect(scope.isDirty()).toBeTruthy();
    });

    it('Should be true if we have deleted a proprietary package', function() {
      scope.packages.length = 0;
      expect(scope.isDirty()).toBeTruthy();
    });

    it('Should be true if we have deleted a proprietary reges', function() {
      scope.regexes.length = 0;
      expect(scope.isDirty()).toBeTruthy();
    });

    it('Should be false if we have both added and deleted the same proprietary regex', function() {
      scope.regexes.length = 0;
      expect(scope.isDirty()).toBeTruthy();

      scope.regexes.push('bar');
      expect(scope.isDirty()).toBeFalsy();
    });

    it('Should be false if we have reset', function() {
      scope.packages.length = 0;
      scope.regexes.length = 0;
      expect(scope.isDirty()).toBeTruthy();

      scope.reset();
      expect(scope.isDirty()).toBeFalsy();
      expect(scope.packages.length).toBeGreaterThan(0);
      expect(scope.regexes.length).toBeGreaterThan(0);
    });
  });
});
