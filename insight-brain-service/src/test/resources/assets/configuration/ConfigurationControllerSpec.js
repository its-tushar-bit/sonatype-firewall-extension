describe('Proprietary components', function() {
  'use strict';

  var scope,
    controller,
    proprietaryConfig = { packages: ['foo'], regexes: ['bar']};

  beforeEach(module('Configuration', 'CLMLocation', function($provide){
    SpecUtil.mockPermissionService($provide);
  }));

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
      controller = $controller('ProprietaryConfigurationController', { $scope: scope, hasAdminPermission : true });
      $httpBackend.flush();

      expect(scope.packages).toEqual(['foo']);
      expect(scope.regexes).toEqual(['bar']);
      expect(scope.loadError).toBeUndefined();
    }));

    it('Error from the server', inject(function($controller, $rootScope, $httpBackend, CLMLocations) {
      scope = $rootScope.$new();
      $httpBackend.expectGET(CLMLocations.getProprietaryConfig()).respond(500, 'A Random Error');
      controller = $controller('ProprietaryConfigurationController', { $scope: scope, hasAdminPermission : true });
      $httpBackend.flush();
      expect(scope.packages).toBeUndefined();
      expect(scope.regexes).toBeUndefined();
      expect(scope.loadError).toEqual('A Random Error');
    }));
  });

  it('Reset local data before saving', inject(function($controller, $rootScope, $httpBackend, CLMLocations) {
    scope = $rootScope.$new();

    $httpBackend.expectGET(CLMLocations.getProprietaryConfig()).respond(proprietaryConfig);
    controller = $controller('ProprietaryConfigurationController', { $scope: scope, hasAdminPermission : true });
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
      controller = $controller('ProprietaryConfigurationController', { $scope: scope, hasAdminPermission : true });
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
      controller = $controller('ProprietaryConfigurationController', { $scope: scope, hasAdminPermission : true });
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
      expect(scope.error[0].msg).toEqual('A Random Error');
    }));
  });

  describe('ProprietaryConfigurationController "isDirty"', function() {
    beforeEach(inject(function($controller, $rootScope, $httpBackend, CLMLocations) {
        scope = $rootScope.$new();
        $httpBackend.expectGET(CLMLocations.getProprietaryConfig()).respond(proprietaryConfig);
        controller = $controller('ProprietaryConfigurationController', { $scope: scope, hasAdminPermission : true });
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

describe('proprietaryConfigEditor', function() {
  var scope, element, editorScope;

  beforeEach(module('Configuration'));

  beforeEach(inject(function($rootScope, $compile, $httpBackend) {
    scope = angular.extend($rootScope.$new(), {
      packages: ['foo'],
      regexes: ['bar']
    });
    $httpBackend.expectGET('config-editor')
      .respond('<form name="foo"><input name="bar" type="text" ng-model="qux" input-validator="validatePackage"></form>');
    element = $compile('<div proprietary-config-editor prefixes="packages" regexes="regexes"></div>')(scope);
    $httpBackend.flush();
    editorScope = scope.$$childHead;
    scope.$apply(function () {
      editorScope.qux = 'mcgenius';
    });
  }));

  afterEach(inject(function($httpBackend) {
    scope.$destroy();
    scope = null;
    editorScope.$destroy();
    editorScope = null;
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

  describe('Validation of Inputs', function() {
    it('Good package inputs', function() {
      expect(editorScope.validatePackage('com.sonatype')).toEqual({ invalidPrefix : true, wildcards : true });
    });

    //see CLM-1097
    it('Should treat an empty entry as valid', function(){
      expect(editorScope.validatePackage('')).toEqual({ invalidPrefix : true, wildcards : true });
    });

    it('Bad package inputs', function() {
      expect(editorScope.validatePackage('com sonatype')).toEqual({ invalidPrefix : false, wildcards : true });
      expect(editorScope.validatePackage('com/sonatype')).toEqual({ invalidPrefix : false, wildcards : true });
      expect(editorScope.validatePackage('com.sonatype.')).toEqual({ invalidPrefix : false, wildcards : true });
      expect(editorScope.validatePackage('.com.sonatype')).toEqual({ invalidPrefix : false, wildcards : true });
      expect(editorScope.validatePackage('com.sonatype.*')).toEqual({ invalidPrefix : true, wildcards : false });
      expect(editorScope.validatePackage('com.sonatype.**')).toEqual({ invalidPrefix : true, wildcards : false });
      expect(editorScope.validatePackage('com.sona*')).toEqual({ invalidPrefix : true, wildcards : false });
      expect(editorScope.validatePackage('*.sonatype')).toEqual({ invalidPrefix : true, wildcards : false });
    });
  });

  // See https://issues.sonatype.org/browse/CLM-844
  it('Reruns validation when source array resets', function() {
    spyOn(editorScope, 'validatePackage').andCallThrough();
    scope.$apply(function () {
      scope.packages = ['baz'];
    });
    expect(editorScope.validatePackage).toHaveBeenCalled();
  });
});
