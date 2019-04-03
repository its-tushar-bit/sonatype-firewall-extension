import { InitModule } from '../../../main/frontend/MainModule';

/* global beforeEach, module, jasmine, afterEach, inject, describe, it, expect, SpecUtil */
window.angularDebug = true;

describe('mainModuleSpec', function() {
  var scope, telemetryServiceMock, pendoServiceMock;

  beforeEach(angular.mock.module(InitModule.name, function($provide, $stateProvider) {
    $provide.value('$window', {
      location: {
        href: 'http://blah',
        replace: jasmine.createSpy()
      }
    });

    telemetryServiceMock = jasmine.createSpyObj('gettingStartedUsageTelemetryService', ['submitData']);
    $provide.service('gettingStartedUsageTelemetryService', function() {
      return telemetryServiceMock;
    });

    pendoServiceMock = jasmine.createSpyObj('pendoService', ['start']);
    $provide.service('pendoService', function() {
      return pendoServiceMock;
    });

    $stateProvider.state('someOtherState', {
      url: '/someOtherState'
    });
  }));

  beforeEach(inject(function($rootScope) {
    scope = $rootScope.$new();
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

  describe('Validate requests made on initService start', function() {
    it('validate state after all requests succeed',
        inject(function($httpBackend, CLMLocations, initService, $rootScope, ProductFeatures, $window) {
          $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({username: 'myname'});
          $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getValidateLicenseUrl())).respond({});
          $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getProductFeaturesUrl())).respond(
              ['dashboard', 'allow-external-hyperlinks']);
          $httpBackend.expectGET('dashboard/dashboard.view.html?').respond('<div></div>');

          initService.start();
          $httpBackend.flush();
          expect($rootScope.licensed).toEqual(true);
          expect($rootScope.username).toEqual('myname');
          expect(ProductFeatures.isDashboardLicensed()).toEqual(true);
          expect(ProductFeatures.isAvailable('allow-external-hyperlinks')).toEqual(true);
          expect($window.externalLinkClickHandler).not.toBeDefined();
          expect($rootScope.initialized).toEqual(true);

          expect(pendoServiceMock.start).toHaveBeenCalled();
        }));

    it('validate state after license check fails because unlicensed',
        inject(function($httpBackend, CLMLocations, initService, $rootScope, ProductFeatures, $window) {
          $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({username: 'myname'});
          $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getValidateLicenseUrl())).respond(402);
          $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getProductFeaturesUrl())).respond(['dashboard']);

          initService.start();
          $httpBackend.flush();
          expect($rootScope.licensed).toBeFalsy();
          expect($window.location.replace).toHaveBeenCalled();
          expect($rootScope.initialized).toBeFalsy();
          expect($rootScope.username).toBe('myname');

          expect(pendoServiceMock.start).toHaveBeenCalled();
        })
    );

    it('validate state after logged in check error',
        inject(function($httpBackend, CLMLocations, initService, $rootScope) {
          $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond(500);
          $rootScope.error = undefined;
          initService.start();
          $httpBackend.flush();
          expect($rootScope.error).toBeDefined();
          expect($rootScope.initialized).toBeFalsy();

          expect(pendoServiceMock.start).toHaveBeenCalled();
        })
    );

    it('validate state after license check error',
        inject(function($httpBackend, CLMLocations, initService, $rootScope) {
          $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({username: 'myname'});
          $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getValidateLicenseUrl())).respond(500);
          $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getProductFeaturesUrl())).respond(['dashboard']);
          $rootScope.error = undefined;
          initService.start();
          $httpBackend.flush();
          expect($rootScope.error).toBeDefined();
          expect($rootScope.initialized).toBeFalsy();

          expect(pendoServiceMock.start).toHaveBeenCalled();
        })
    );

    it('validate state after product feature error',
        inject(function($httpBackend, CLMLocations, initService, $rootScope) {
          $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({username: 'myname'});
          $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getValidateLicenseUrl())).respond({});
          $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getProductFeaturesUrl())).respond(500);
          $rootScope.error = undefined;
          initService.start();
          $httpBackend.flush();
          expect($rootScope.error).toBeDefined();
          expect($rootScope.initialized).toBeFalsy();

          expect(pendoServiceMock.start).toHaveBeenCalled();
        })
    );

    it('validate state after external hyperlinks are disabled',
        inject(function($httpBackend, CLMLocations, initService, $rootScope, ProductFeatures, $window) {
          $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({username: 'myname'});
          $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getValidateLicenseUrl())).respond({});
          $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getProductFeaturesUrl())).respond(['dashboard']);
          $httpBackend.expectGET('dashboard/dashboard.view.html?').respond('<div></div>');

          initService.start();
          $httpBackend.flush();
          expect($rootScope.licensed).toEqual(true);
          expect($rootScope.username).toEqual('myname');
          expect(ProductFeatures.isDashboardLicensed()).toEqual(true);
          expect(ProductFeatures.isAvailable('allow-external-hyperlinks')).toEqual(false);
          expect($window.externalLinkClickHandler).toBeDefined();
          expect($rootScope.initialized).toEqual(true);

          expect(pendoServiceMock.start).toHaveBeenCalled();
        }));
  });

  describe('on licenseInstalled event', function() {
    let $httpBackend;

    beforeEach(inject(function(_$httpBackend_, CLMLocations) {
      $httpBackend = _$httpBackend_;

      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({username: 'myname'});
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getValidateLicenseUrl())).respond({});
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getProductFeaturesUrl())).respond(['dashboard']);
      $httpBackend.expectGET('dashboard/dashboard.view.html?').respond('<div></div>');
    }));

    it('fires "REDIRECTED" telemetry event', inject(function(initService, $state) {
      initService.start();
      $httpBackend.flush();
      $state.go('someOtherState');
      scope.$digest();
      scope.$emit('licenseInstalled');
      expect(telemetryServiceMock.submitData).toHaveBeenCalledWith('REDIRECTED', {
        pageNavigatedFrom: 'someOtherState'
      });
    }));
  });

  describe('on beforeunload event', function() {
    let $httpBackend;

    beforeEach(inject(function(_$httpBackend_, CLMLocations) {
      $httpBackend = _$httpBackend_;

      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({username: 'myname'});
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getValidateLicenseUrl())).respond({});
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getProductFeaturesUrl())).respond(['dashboard']);
      $httpBackend.expectGET('dashboard/dashboard.view.html?').respond('<div></div>');
    }));

    it('fires synchronous "DEPARTED" telemetry event if current page is gettingStarted',
        inject(function(initService, $state) {
          initService.start();
          $httpBackend.flush();
          $state.go('gettingStarted');
          scope.$digest();
          window.dispatchEvent(new Event('beforeunload'));
          expect(telemetryServiceMock.submitData).toHaveBeenCalledWith('DEPARTED', null, true);
        })
    );

    it('does not fire "DEPARTED" telemetry event if current page is not gettingStarted', inject(function(initService) {
      initService.start();
      $httpBackend.flush();
      window.dispatchEvent(new Event('beforeunload'));
      expect(telemetryServiceMock.submitData).not.toHaveBeenCalled();
    }));
  });
});
