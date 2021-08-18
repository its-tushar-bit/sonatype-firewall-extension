/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { InitModule } from '../../../main/frontend/MainModule';

/* global beforeEach, module, jasmine, afterEach, inject, describe, it, expect, SpecUtil */
window.angularDebug = true;

describe('mainModuleSpec', function () {
  var scope, telemetryServiceMock, pendoServiceMock;

  beforeEach(
    angular.mock.module(InitModule.name, function ($provide, $stateProvider) {
      // mock the window using anything on which events can be dispatched
      $provide.value('$window', document.createElement('div'));

      telemetryServiceMock = jasmine.createSpyObj('gettingStartedUsageTelemetryService', ['submitData']);
      $provide.service('gettingStartedUsageTelemetryService', function () {
        return telemetryServiceMock;
      });

      pendoServiceMock = jasmine.createSpyObj('pendoService', ['start']);
      $provide.service('pendoService', function () {
        return pendoServiceMock;
      });

      $stateProvider.state('someOtherState', {
        url: '/someOtherState',
      });
    })
  );

  beforeEach(inject(function ($rootScope, $state) {
    scope = $rootScope.$new();
    spyOn($state, 'go');
  }));

  afterEach(inject(function ($httpBackend) {
    if (scope) {
      scope.$destroy();
    }
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

  describe('Custom sanitation', function () {
    it('allows blob urls unsanitized', inject(function ($$sanitizeUri, $httpBackend, CLMLocations) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getProductFeaturesUrl())).respond(500);
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getValidateLicenseUrl())).respond({});
      $httpBackend.flush();

      var uri = 'blob:http%3A//127.0.0.1%3A8070/someuuid';
      var sanitized = $$sanitizeUri(uri, true);
      expect(sanitized).toBe(uri);
    }));
  });

  describe('Validate requests made on initService start', function () {
    it('validate state after all requests succeed', inject(function (
      $httpBackend,
      CLMLocations,
      initService,
      $rootScope,
      ProductFeatures,
      $window,
      $state
    ) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getValidateLicenseUrl())).respond({});
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({ username: 'myname' });
      $httpBackend
        .expectGET(SpecUtil.toRegExp(CLMLocations.getProductFeaturesUrl()))
        .respond(['dashboard', 'allow-external-hyperlinks']);

      initService.start();
      expect($state.go).not.toHaveBeenCalled();
      $httpBackend.flush();
      expect($rootScope.licensed).toEqual(true);
      expect($rootScope.username).toEqual('myname');
      expect(ProductFeatures.isDashboardAvailable()).toEqual(true);
      expect(ProductFeatures.isAvailable('allow-external-hyperlinks')).toEqual(true);
      expect($window.externalLinkClickHandler).not.toBeDefined();

      expect(pendoServiceMock.start).toHaveBeenCalled();
    }));

    it('validate state after license check fails because unlicensed', inject(function (
      $httpBackend,
      CLMLocations,
      initService,
      $rootScope,
      ProductFeatures,
      $state
    ) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getValidateLicenseUrl())).respond(402);
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({ username: 'myname' });
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getProductFeaturesUrl())).respond(402);

      initService.start();
      $httpBackend.flush();
      expect($rootScope.licensed).toBeFalsy();
      expect($state.go).toHaveBeenCalledTimes(1);
      expect($state.go).toHaveBeenCalledWith('productlicense');
      expect($rootScope.username).toBe('myname');

      expect(pendoServiceMock.start).toHaveBeenCalled();
    }));

    it('validate state after logged in check error', inject(function (
      $httpBackend,
      CLMLocations,
      initService,
      $rootScope
    ) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getValidateLicenseUrl())).respond({});
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond(500);
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getProductFeaturesUrl())).respond(['dashboard']);
      $rootScope.error = undefined;
      initService.start();
      $httpBackend.flush();
      expect($rootScope.error).toBeDefined();

      expect(pendoServiceMock.start).toHaveBeenCalled();
    }));

    it('validate state after license check error', inject(function (
      $httpBackend,
      CLMLocations,
      initService,
      $rootScope
    ) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getValidateLicenseUrl())).respond(500);
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({ username: 'myname' });
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getProductFeaturesUrl())).respond(['dashboard']);
      $rootScope.error = undefined;
      initService.start();
      $httpBackend.flush();
      expect($rootScope.error).toBeDefined();

      expect(pendoServiceMock.start).toHaveBeenCalled();
    }));

    it('validate state after product feature error', inject(function (
      $httpBackend,
      CLMLocations,
      initService,
      $rootScope
    ) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getValidateLicenseUrl())).respond({});
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({ username: 'myname' });
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getProductFeaturesUrl())).respond(500);
      $rootScope.error = undefined;
      initService.start();
      $httpBackend.flush();
      expect($rootScope.error).toBeDefined();

      expect(pendoServiceMock.start).toHaveBeenCalled();
    }));

    it('validate state after external hyperlinks are disabled', inject(function (
      $httpBackend,
      CLMLocations,
      initService,
      $rootScope,
      ProductFeatures,
      $window
    ) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getValidateLicenseUrl())).respond({});
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({ username: 'myname' });
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getProductFeaturesUrl())).respond(['dashboard']);

      initService.start();
      $httpBackend.flush();
      expect($rootScope.licensed).toEqual(true);
      expect($rootScope.username).toEqual('myname');
      expect(ProductFeatures.isDashboardAvailable()).toEqual(true);
      expect(ProductFeatures.isAvailable('allow-external-hyperlinks')).toEqual(false);
      expect($window.externalLinkClickHandler).toBeDefined();

      expect(pendoServiceMock.start).toHaveBeenCalled();
    }));

    it('validate state with only dashboard available', inject(function (
      $httpBackend,
      CLMLocations,
      initService,
      $rootScope,
      ProductFeatures,
      $window,
      $state
    ) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getValidateLicenseUrl())).respond({});
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({ username: 'myname' });
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getProductFeaturesUrl())).respond(['dashboard']);

      initService.start();
      $httpBackend.flush();
      expect($rootScope.licensed).toEqual(true);
      expect($rootScope.username).toEqual('myname');
      expect(ProductFeatures.isDashboardAvailable()).toBeTruthy();
      expect(ProductFeatures.isReportsListAvailable()).toBeFalsy();
      expect($window.externalLinkClickHandler).toBeDefined();
      expect($state.current.name).toBe('dashboard.overview.violations');

      expect(pendoServiceMock.start).toHaveBeenCalled();
    }));

    it('validate state with only reports-list available', inject(function (
      $httpBackend,
      CLMLocations,
      initService,
      $rootScope,
      ProductFeatures,
      $window,
      $state
    ) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getValidateLicenseUrl())).respond({});
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({ username: 'myname' });
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getProductFeaturesUrl())).respond(['reports-list']);

      initService.start();
      $httpBackend.flush();
      expect($rootScope.licensed).toEqual(true);
      expect($rootScope.username).toEqual('myname');
      expect(ProductFeatures.isDashboardAvailable()).toBeFalsy();
      expect(ProductFeatures.isReportsListAvailable()).toBeTruthy();
      expect($window.externalLinkClickHandler).toBeDefined();
      expect($state.current.name).toBe('violations');

      expect(pendoServiceMock.start).toHaveBeenCalled();
    }));

    it('validate state with dashboard and reports-list available', inject(function (
      $httpBackend,
      CLMLocations,
      initService,
      $rootScope,
      ProductFeatures,
      $window,
      $state
    ) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getValidateLicenseUrl())).respond({});
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({ username: 'myname' });
      $httpBackend
        .expectGET(SpecUtil.toRegExp(CLMLocations.getProductFeaturesUrl()))
        .respond(['dashboard', 'reports-list']);

      initService.start();
      $httpBackend.flush();
      expect($rootScope.licensed).toEqual(true);
      expect($rootScope.username).toEqual('myname');
      expect(ProductFeatures.isDashboardAvailable()).toBeTruthy();
      expect(ProductFeatures.isReportsListAvailable()).toBeTruthy();
      expect($window.externalLinkClickHandler).toBeDefined();
      expect($state.current.name).toBe('dashboard.overview.violations');

      expect(pendoServiceMock.start).toHaveBeenCalled();
    }));

    it('validate state with neither dashboard nor reports-list available', inject(function (
      $httpBackend,
      CLMLocations,
      initService,
      $rootScope,
      ProductFeatures,
      $window,
      $state
    ) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getValidateLicenseUrl())).respond({});
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({ username: 'myname' });
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getProductFeaturesUrl())).respond([]);

      initService.start();
      $httpBackend.flush();
      expect($rootScope.licensed).toEqual(true);
      expect($rootScope.username).toEqual('myname');
      expect(ProductFeatures.isDashboardAvailable()).toBeFalsy();
      expect(ProductFeatures.isReportsListAvailable()).toBeFalsy();
      expect($window.externalLinkClickHandler).toBeDefined();
      expect($state.current.name).toBe('gettingStarted');

      expect(pendoServiceMock.start).toHaveBeenCalled();
    }));
  });

  describe('on beforeunload event', function () {
    let $httpBackend, $window;

    beforeEach(inject(function (_$httpBackend_, CLMLocations, _$window_) {
      $httpBackend = _$httpBackend_;
      $window = _$window_;

      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getValidateLicenseUrl())).respond({});
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({ username: 'myname' });
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getProductFeaturesUrl())).respond(['dashboard']);
    }));

    it('fires synchronous "DEPARTED" telemetry event if current page is gettingStarted', inject(function (
      initService,
      $state
    ) {
      initService.start();
      $httpBackend.flush();
      $state.current.name = 'gettingStarted';
      scope.$digest();
      $window.dispatchEvent(new Event('beforeunload'));
      expect(telemetryServiceMock.submitData).toHaveBeenCalledWith('DEPARTED', null, true);
    }));

    it('does not fire "DEPARTED" telemetry event if current page is not gettingStarted', inject(function (initService) {
      initService.start();
      $httpBackend.flush();
      $window.dispatchEvent(new Event('beforeunload'));
      expect(telemetryServiceMock.submitData).not.toHaveBeenCalled();
    }));
  });

  describe('pendoService calls', function () {
    let $httpBackend, initService, pendoService, CLMLocations;

    beforeEach(inject(function (_$httpBackend_, _pendoService_, _initService_, _CLMLocations_) {
      $httpBackend = _$httpBackend_;
      pendoService = _pendoService_;
      initService = _initService_;
      CLMLocations = _CLMLocations_;
    }));

    it('calls pendoService.start before login', function () {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getValidateLicenseUrl())).respond({});
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({ username: 'myname' });
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getProductFeaturesUrl())).respond(['dashboard']);

      initService.start();

      expect(pendoService.start).toHaveBeenCalled();

      $httpBackend.flush();
    });

    it('calls pendoService a second time after login and license fetch', function () {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getValidateLicenseUrl())).respond({});
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({ username: 'myname' });
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getProductFeaturesUrl())).respond(['dashboard']);

      initService.start();

      expect(pendoService.start).toHaveBeenCalledTimes(1);

      $httpBackend.flush();
      expect(pendoService.start).toHaveBeenCalledTimes(2);
    });

    it('calls pendoService a second time after login if the license is not installed', function () {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getValidateLicenseUrl())).respond(402);
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({ username: 'myname' });
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getProductFeaturesUrl())).respond(['dashboard']);

      initService.start();

      expect(pendoService.start).toHaveBeenCalledTimes(1);

      $httpBackend.flush();
      expect(pendoService.start).toHaveBeenCalledTimes(2);
    });

    it('does not call pendoService a second time after failed login', function () {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getValidateLicenseUrl())).respond({});
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond(401);
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getProductFeaturesUrl())).respond(['dashboard']);

      initService.start();

      expect(pendoService.start).toHaveBeenCalledTimes(1);

      $httpBackend.flush();
      expect(pendoService.start).toHaveBeenCalledTimes(1);
    });
  });
});
