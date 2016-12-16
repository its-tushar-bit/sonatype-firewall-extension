/* global beforeEach, module, jasmine, afterEach, inject, describe, it, expect, SpecUtil */
window.angularDebug = true;

describe('mainModuleSpec', function() {
  'use strict';
  var scope, state;

  beforeEach(module('InitModule', function($provide){
    $provide.value('$window', {
      location: {
        href: 'http://blah',
        replace: jasmine.createSpy()
      }
    });
  }));

  beforeEach(inject(function($rootScope, $state) {
    scope = $rootScope.$new();
    state = $state;
  }));

  afterEach(inject(function($httpBackend) {
    if (scope) {
      scope.$destroy();
    }
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

  describe('Custom sanitation', function() {
    it('allows blob urls unsanitized', inject(function($$sanitizeUri) {
      var uri = 'blob:http%3A//127.0.0.1%3A8070/someuuid';
      var sanitized = $$sanitizeUri(uri, true);
      expect(sanitized).toBe(uri);
    }));
  });

  describe('Validate requests made on initService start', function(){
    it('validate state after all requests succeed', inject(function($httpBackend, CLMLocations, initService, $rootScope, ProductFeatures){
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({username:'myname'});
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getValidateLicenseUrl())).respond({});
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getProductFeaturesUrl())).respond(['dashboard']);
      $httpBackend.expectGET('dashboard/dashboard.view.html?').respond('<div></div>');
      $httpBackend.expectGET('dashboard/results/dashboard.results.html?').respond('<div></div>');
      $httpBackend.expectGET('dashboard/dashboard.filter.html?').respond('<div></div>');
      $httpBackend.expectGET('dashboard/results/violations.html?').respond('<div></div>');

      initService.start();
      $httpBackend.flush();
      expect($rootScope.licensed).toEqual(true);
      expect($rootScope.username).toEqual('myname');
      expect(ProductFeatures.isDashboardLicensed()).toEqual(true);
      expect($rootScope.initialized).toEqual(true);
    }));

    it('validate state after license check fails because unlicensed', inject(function($httpBackend, CLMLocations, initService, $rootScope, ProductFeatures, $window){
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({username:'myname'});
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getValidateLicenseUrl())).respond(402);
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getProductFeaturesUrl())).respond(['dashboard']);

      initService.start();
      $httpBackend.flush();
      expect($rootScope.licensed).toBeFalsy();
      expect($window.location.replace).toHaveBeenCalled();
      expect($rootScope.initialized).toBeFalsy();
    }));

    it('validate state after logged in check error', inject(function($httpBackend, CLMLocations, initService, $rootScope){
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond(500);
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getValidateLicenseUrl())).respond({});
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getProductFeaturesUrl())).respond(['dashboard']);
      $rootScope.error = undefined;
      initService.start();
      $httpBackend.flush();
      expect($rootScope.error).toBeDefined();
      expect($rootScope.initialized).toBeFalsy();
    }));

    it('validate state after license check error', inject(function($httpBackend, CLMLocations, initService, $rootScope){
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({username:'myname'});
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getValidateLicenseUrl())).respond(500);
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getProductFeaturesUrl())).respond(['dashboard']);
      $rootScope.error = undefined;
      initService.start();
      $httpBackend.flush();
      expect($rootScope.error).toBeDefined();
      expect($rootScope.initialized).toBeFalsy();
    }));

    it('validate state after product feature error', inject(function($httpBackend, CLMLocations, initService, $rootScope){
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({username:'myname'});
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getValidateLicenseUrl())).respond({});
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getProductFeaturesUrl())).respond(500);
      $rootScope.error = undefined;
      initService.start();
      $httpBackend.flush();
      expect($rootScope.error).toBeDefined();
      expect($rootScope.initialized).toBeFalsy();
    }));
  });
});
