/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { InitModule } from 'MainRoot/MainModule';
import * as gettingStartedTelemetryServiceHelper from 'MainRoot/configuration/gettingStarted/gettingStartedTelemetryServiceHelper';
import * as RouteProductLicenseValidator from 'MainRoot/routeProductLicenseValidator/RouteProductLicenseValidator';
window.angularDebug = true;

describe('mainModuleSpec', function () {
  let scope, pendoServiceMock, $ngRedux, productLicenseLoadDefer;

  beforeEach(
    angular.mock.module(InitModule.name, function ($provide, $stateProvider) {
      SpecUtil.mockNgRedux($provide);
      // mock the window using anything on which events can be dispatched
      $provide.value('$window', document.createElement('div'));

      pendoServiceMock = jasmine.createSpyObj('pendoService', ['start']);
      $provide.service('pendoService', function () {
        return pendoServiceMock;
      });

      $provide.service('routeStateUtilService', function () {
        function stateRequiresAuthentication() {
          return Promise.resolve(true);
        }

        function stateRequiresAuthenticationSync() {
          return true;
        }

        return { stateRequiresAuthentication, stateRequiresAuthenticationSync };
      });
      $stateProvider.state('someOtherState', {
        url: '/someOtherState',
      });
    })
  );

  beforeEach(inject(function ($q, $rootScope, _$ngRedux_, _ProductLicense_) {
    scope = $rootScope.$new();
    $ngRedux = _$ngRedux_;
    $ngRedux.dispatch = jasmine.createSpy('dispatch').and.returnValue({ payload: [] });

    productLicenseLoadDefer = $q.defer();
    _ProductLicense_.load = jasmine.createSpy('load').and.returnValue(productLicenseLoadDefer.promise);

    spyOn(RouteProductLicenseValidator, 'default').and.returnValue(true);
  }));

  afterEach(inject(function ($httpBackend) {
    if (scope) {
      scope.$destroy();
    }
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

  describe('initService start', function () {
    beforeEach(inject(function ($state) {
      spyOn($state, 'go');
    }));

    describe('Validates requests made', function () {
      beforeEach(function () {
        $ngRedux.getState = jasmine.createSpy('getState').and.returnValue({
          productFeatures: { productFeatures: {} },
          firewallOnboarding: {
            unconfiguredRepoManagers: {
              repoManagers: [],
              loading: false,
              loadError: null,
            },
          },
        });
      });

      it('validate state after all requests succeed', inject(function (
        $httpBackend,
        CLMLocations,
        initService,
        $rootScope,
        $window,
        $state
      ) {
        $rootScope.isAllowExternalHyperlinks = true;
        $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({ username: 'myname' });
        productLicenseLoadDefer.resolve({});

        initService.start();

        $httpBackend.flush();
        expect($rootScope.licensed).toEqual(true);
        expect($state.go).not.toHaveBeenCalled();
        expect($rootScope.licensed).toEqual(true);
        expect($rootScope.username).toEqual('myname');
        expect($window.externalLinkClickHandler).not.toBeDefined();
        expect(pendoServiceMock.start).toHaveBeenCalled();
      }));

      it('validate state after license check fails because unlicensed', inject(function (
        $httpBackend,
        CLMLocations,
        initService,
        $rootScope,
        $state
      ) {
        $rootScope.isAllowExternalHyperlinks = true;
        productLicenseLoadDefer.reject({ response: { status: 402 } });
        $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({ username: 'myname' });

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
        $rootScope.isAllowExternalHyperlinks = true;
        productLicenseLoadDefer.resolve({});
        $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond(500);

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
        $rootScope.isAllowExternalHyperlinks = true;
        $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({ username: 'myname' });
        $rootScope.error = undefined;

        initService.start();
        $rootScope.$digest();
        productLicenseLoadDefer.reject({ response: { status: 500 } });
        $httpBackend.flush();

        expect($rootScope.error).toBeDefined();
        expect(pendoServiceMock.start).toHaveBeenCalled();
      }));

      it('validate state after license check 403 error', inject(function (
        $httpBackend,
        CLMLocations,
        initService,
        $rootScope
      ) {
        $rootScope.error = undefined;
        const errorMsg = 'Access from this IP is not allowed, please contact an administrator.';
        $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({ username: 'myname' });

        initService.start();
        $rootScope.$digest();
        productLicenseLoadDefer.reject(errorMsg);
        $httpBackend.flush();

        expect($rootScope.error).toEqual(errorMsg);
      }));

      it('validate state after waitForLogin 403 error', inject(function (
        $httpBackend,
        CLMLocations,
        initService,
        $rootScope
      ) {
        const errorMsg = 'Access from this IP is not allowed, please contact an administrator.';
        productLicenseLoadDefer.resolve({});
        $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond(403, errorMsg);

        $rootScope.error = undefined;
        initService.start();
        $httpBackend.flush();
        expect($rootScope.error).toEqual(errorMsg);
      }));

      it('validate state after external hyperlinks are disabled', inject(function (
        $httpBackend,
        CLMLocations,
        initService,
        $rootScope,
        $window
      ) {
        $rootScope.isAllowExternalHyperlinks = false;
        productLicenseLoadDefer.resolve({});
        $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({ username: 'myname' });

        initService.start();
        $httpBackend.flush();

        expect($rootScope.licensed).toEqual(true);
        expect($rootScope.username).toEqual('myname');
        expect($window.externalLinkClickHandler).toBeDefined();
        expect(pendoServiceMock.start).toHaveBeenCalled();
      }));

      it('validate state with only dashboard available', inject(function (
        $httpBackend,
        CLMLocations,
        initService,
        $rootScope,
        $window,
        $state
      ) {
        $rootScope.isAllowExternalHyperlinks = false;
        $ngRedux.getState = jasmine.createSpy('getState').and.returnValue({
          productFeatures: {
            productFeatures: {
              dashboard: true,
            },
          },
          firewallOnboarding: {
            unconfiguredRepoManagers: {
              repoManagers: [],
              loading: false,
              loadError: null,
            },
          },
        });

        productLicenseLoadDefer.resolve({});
        $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({ username: 'myname' });

        initService.start();
        $httpBackend.flush();

        expect($rootScope.licensed).toEqual(true);
        expect($rootScope.username).toEqual('myname');
        expect($window.externalLinkClickHandler).toBeDefined();
        expect($state.current.name).toBe('dashboard.overview.violations');

        expect(pendoServiceMock.start).toHaveBeenCalled();
      }));

      it('validate state with only reports-list available', inject(function (
        $httpBackend,
        CLMLocations,
        initService,
        $rootScope,
        $window,
        $state
      ) {
        $rootScope.isAllowExternalHyperlinks = false;
        $ngRedux.getState = jasmine.createSpy('getState').and.returnValue({
          productFeatures: {
            productFeatures: {
              'reports-list': true,
            },
          },
          firewallOnboarding: {
            unconfiguredRepoManagers: {
              repoManagers: [],
              loading: false,
              loadError: null,
            },
          },
        });

        productLicenseLoadDefer.resolve({});
        $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({ username: 'myname' });

        initService.start();
        $httpBackend.flush();

        expect($rootScope.licensed).toEqual(true);
        expect($rootScope.username).toEqual('myname');
        expect($window.externalLinkClickHandler).toBeDefined();
        expect($state.current.name).toBe('violations');

        expect(pendoServiceMock.start).toHaveBeenCalled();
      }));

      it('validate state with dashboard and reports-list available', inject(function (
        $httpBackend,
        CLMLocations,
        initService,
        $rootScope,
        $window,
        $state
      ) {
        $rootScope.isAllowExternalHyperlinks = false;
        $ngRedux.getState = jasmine.createSpy('getState').and.returnValue({
          productFeatures: {
            productFeatures: {
              dashboard: true,
              'reports-list': true,
            },
          },
          firewallOnboarding: {
            unconfiguredRepoManagers: {
              repoManagers: [],
              loading: false,
              loadError: null,
            },
          },
        });
        productLicenseLoadDefer.resolve({});
        $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({ username: 'myname' });

        initService.start();
        $httpBackend.flush();

        expect($rootScope.licensed).toEqual(true);
        expect($rootScope.username).toEqual('myname');
        expect($window.externalLinkClickHandler).toBeDefined();
        expect($state.current.name).toBe('dashboard.overview.violations');

        expect(pendoServiceMock.start).toHaveBeenCalled();
      }));

      it('validate state with neither dashboard nor reports-list available', inject(function (
        $httpBackend,
        CLMLocations,
        initService,
        $rootScope,
        $window,
        $state
      ) {
        $rootScope.isAllowExternalHyperlinks = false;
        productLicenseLoadDefer.resolve({});
        $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({ username: 'myname' });

        initService.start();
        $httpBackend.flush();

        expect($rootScope.licensed).toEqual(true);
        expect($rootScope.username).toEqual('myname');
        expect($window.externalLinkClickHandler).toBeDefined();
        expect($state.current.name).toBe('gettingStarted');
        expect(pendoServiceMock.start).toHaveBeenCalled();
      }));
    });

    describe('on beforeunload event', function () {
      let $httpBackend, $window, $rootScope;

      beforeEach(() => {
        spyOn(gettingStartedTelemetryServiceHelper, 'submitData');

        return inject(function (_$httpBackend_, CLMLocations, _$window_, _$rootScope_, _$ngRedux_) {
          $httpBackend = _$httpBackend_;
          $window = _$window_;
          $rootScope = _$rootScope_;
          $ngRedux = _$ngRedux_;
          $ngRedux.getState = jasmine.createSpy('getState').and.returnValue({
            router: {
              currentState: {
                data: {
                  isDirty: false,
                },
              },
            },
            productFeatures: {
              productFeatures: {
                dashboard: true,
              },
            },
          });

          $rootScope.isAllowExternalHyperlinks = true;
          productLicenseLoadDefer.resolve({});
          $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({ username: 'myname' });
        });
      });

      it('fires synchronous "DEPARTED" telemetry event if current page is gettingStarted', inject(function (
        initService,
        $state
      ) {
        initService.start();
        $httpBackend.flush();
        $state.current.name = 'gettingStarted';
        scope.$digest();
        $window.dispatchEvent(new Event('beforeunload'));

        expect(gettingStartedTelemetryServiceHelper.submitData).toHaveBeenCalledWith('DEPARTED', null, true);
      }));

      it('does not fire "DEPARTED" telemetry event if current page is not gettingStarted', inject(function (
        initService
      ) {
        initService.start();
        $httpBackend.flush();
        $window.dispatchEvent(new Event('beforeunload'));

        expect(gettingStartedTelemetryServiceHelper.submitData).not.toHaveBeenCalled();
      }));
    });
  });

  describe('pendoService calls', function () {
    let $httpBackend, initService, pendoService, CLMLocations, $rootScope;

    beforeEach(inject(function (_$httpBackend_, _pendoService_, _initService_, _CLMLocations_, _$rootScope_) {
      $httpBackend = _$httpBackend_;
      pendoService = _pendoService_;
      initService = _initService_;
      CLMLocations = _CLMLocations_;
      $rootScope = _$rootScope_;
    }));

    it('calls pendoService.start before login', function () {
      $rootScope.isAllowExternalHyperlinks = true;
      productLicenseLoadDefer.resolve({});
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({ username: 'myname' });

      initService.start();

      expect(pendoService.start).toHaveBeenCalled();

      $httpBackend.flush();
    });

    it('calls pendoService a second time after login and license fetch', function () {
      $rootScope.isAllowExternalHyperlinks = true;
      productLicenseLoadDefer.resolve({});
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({ username: 'myname' });

      initService.start();

      expect(pendoService.start).toHaveBeenCalledTimes(1);

      $httpBackend.flush();
      expect(pendoService.start).toHaveBeenCalledTimes(2);
    });

    it('calls pendoService a second time after login if the license is not installed', function () {
      $rootScope.isAllowExternalHyperlinks = true;
      productLicenseLoadDefer.reject({ response: { status: 402 } });
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({ username: 'myname' });

      initService.start();

      expect(pendoService.start).toHaveBeenCalledTimes(1);

      $httpBackend.flush();
      expect(pendoService.start).toHaveBeenCalledTimes(2);
    });

    it('does not call pendoService a second time after failed login', function () {
      $rootScope.isAllowExternalHyperlinks = true;
      productLicenseLoadDefer.resolve({});
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond(401);

      initService.start();

      expect(pendoService.start).toHaveBeenCalledTimes(1);

      $httpBackend.flush();
      expect(pendoService.start).toHaveBeenCalledTimes(1);
    });
  });
});
