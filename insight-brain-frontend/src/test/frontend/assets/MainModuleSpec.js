/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { InitModule } from 'MainRoot/MainModule';
import * as gettingStartedTelemetryServiceHelper from 'MainRoot/configuration/gettingStarted/gettingStartedTelemetryServiceHelper';
import * as RouteProductLicenseValidator from 'MainRoot/routeProductLicenseValidator/RouteProductLicenseValidator';
import { axiosMockAdapter, waitFor } from 'TestRoot/SpecUtil';
import * as userSession from 'MainRoot/user/userSession';
window.angularDebug = true;

describe('mainModuleSpec', function () {
  let scope, pendoServiceMock, $ngRedux, productLicenseLoadDefer, axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  afterEach(() => {
    userSession._resetForTest();
  });

  beforeEach(
    angular.mock.module(InitModule.name, function ($provide, $stateProvider) {
      SpecUtil.mockNgRedux($provide);
      // mock the window using anything on which events can be dispatched
      const mockWindow = document.createElement('div');
      mockWindow.top = {
        sessionExpired: jasmine.createSpy('sessionExpired'),
      };
      $provide.value('$window', mockWindow);

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
      let $rootScope, $state, $window, CLMLocations, initService;

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

      beforeEach(inject(function (_CLMLocations_, _initService_, _$rootScope_, _$window_, _$state_) {
        CLMLocations = _CLMLocations_;
        initService = _initService_;
        $rootScope = _$rootScope_;
        $window = _$window_;
        $state = _$state_;
      }));

      it('validate state after all requests succeed', async function () {
        $rootScope.isAllowExternalHyperlinks = true;
        $rootScope.$digest();
        axiosMock.onGet(CLMLocations.getSessionUrl()).reply(200, { username: 'myname' });
        productLicenseLoadDefer.resolve({});

        initService.start();

        await userSession.waitForLogin();
        $rootScope.$digest();
        expect($rootScope.licensed).toEqual(true);
        expect($state.go).not.toHaveBeenCalled();
        expect($rootScope.licensed).toEqual(true);
        expect($rootScope.username).toEqual('myname');
        expect($window.externalLinkClickHandler).not.toBeDefined();
        expect(pendoServiceMock.start).toHaveBeenCalled();
      });

      it('validate state after license check fails because unlicensed', async function () {
        $rootScope.isAllowExternalHyperlinks = true;
        $rootScope.$digest();
        productLicenseLoadDefer.reject({ response: { status: 402 } });
        axiosMock.onGet(CLMLocations.getSessionUrl()).reply(200, { username: 'myname' });

        initService.start();

        await userSession.waitForLogin();
        $rootScope.$digest();
        expect($rootScope.licensed).toBeFalsy();
        expect($state.go).toHaveBeenCalledTimes(1);
        expect($state.go).toHaveBeenCalledWith('productlicense');
        expect($rootScope.username).toBe('myname');

        expect(pendoServiceMock.start).toHaveBeenCalled();
      });

      it('validate state after logged in check error', async function () {
        $rootScope.isAllowExternalHyperlinks = true;
        $rootScope.error = undefined;
        $rootScope.$digest();
        productLicenseLoadDefer.resolve({});
        axiosMock.onGet(CLMLocations.getSessionUrl()).reply(500);

        initService.start();

        try {
          await userSession.waitForLogin();
        } catch (error) {
          // ignore
        }
        await waitFor(() => {
          $rootScope.$digest();
          return $rootScope.error;
        });
        expect($rootScope.error).toBeDefined();
        expect(pendoServiceMock.start).toHaveBeenCalled();
      });

      it('validate state after license check error', async function () {
        $rootScope.isAllowExternalHyperlinks = true;
        $rootScope.error = undefined;
        $rootScope.$digest();
        axiosMock.onGet(CLMLocations.getSessionUrl()).reply(200, { username: 'myname' });

        initService.start();

        $rootScope.$digest();
        productLicenseLoadDefer.reject({ response: { status: 500 } });
        try {
          await userSession.waitForLogin();
        } catch (error) {
          // ignore
        }
        await waitFor(() => {
          $rootScope.$digest();
          return $rootScope.error;
        });
        expect($rootScope.error).toBeDefined();
        expect(pendoServiceMock.start).toHaveBeenCalled();
      });

      it('validate state after license check 403 error', async function () {
        $rootScope.error = undefined;
        $rootScope.$digest();
        const errorMsg = 'Access from this IP is not allowed, please contact an administrator.';
        axiosMock.onGet(CLMLocations.getSessionUrl()).reply(200, { username: 'myname' });

        initService.start();

        $rootScope.$digest();
        productLicenseLoadDefer.reject(errorMsg);
        try {
          await userSession.waitForLogin();
        } catch (error) {
          // ignore
        }
        await waitFor(() => {
          $rootScope.$digest();
          return $rootScope.error;
        });
        expect($rootScope.error).toEqual(errorMsg);
      });

      it('validate state after waitForLogin 403 error', async function () {
        $rootScope.error = undefined;
        $rootScope.$digest();
        const errorMsg = 'Access from this IP is not allowed, please contact an administrator.';
        productLicenseLoadDefer.resolve({});
        axiosMock.onGet(CLMLocations.getSessionUrl()).reply(403, errorMsg);

        initService.start();

        try {
          await userSession.waitForLogin();
        } catch (error) {
          // ignore
        }
        await waitFor(() => {
          $rootScope.$digest();
          return $rootScope.error;
        });
        expect($rootScope.error).toEqual(errorMsg);
      });

      it('validate state after external hyperlinks are disabled', async function () {
        $rootScope.isAllowExternalHyperlinks = false;
        $rootScope.$digest();
        productLicenseLoadDefer.resolve({});
        axiosMock.onGet(CLMLocations.getSessionUrl()).reply(200, { username: 'myname' });

        initService.start();

        await userSession.waitForLogin();
        $rootScope.$digest();
        expect($rootScope.licensed).toEqual(true);
        expect($rootScope.username).toEqual('myname');
        expect($window.externalLinkClickHandler).toBeDefined();
        expect(pendoServiceMock.start).toHaveBeenCalled();
      });

      it('validate state with only dashboard available', async function () {
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
        $rootScope.$digest();
        productLicenseLoadDefer.resolve({});
        axiosMock.onGet(CLMLocations.getSessionUrl()).reply(200, { username: 'myname' });

        initService.start();

        await userSession.waitForLogin();
        $rootScope.$digest();
        expect($rootScope.licensed).toEqual(true);
        expect($rootScope.username).toEqual('myname');
        expect($window.externalLinkClickHandler).toBeDefined();
        expect($state.current.name).toBe('dashboard.overview.violations');

        expect(pendoServiceMock.start).toHaveBeenCalled();
      });

      it('validate state with dashboard unavailable and reports-list available', async function () {
        $rootScope.isAllowExternalHyperlinks = false;
        $ngRedux.getState = jasmine.createSpy('getState').and.returnValue({
          productFeatures: {
            productFeatures: {
              dashboard: false,
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
        $rootScope.$digest();
        productLicenseLoadDefer.resolve({});
        axiosMock.onGet(CLMLocations.getSessionUrl()).reply(200, { username: 'myname' });

        initService.start();

        await userSession.waitForLogin();
        $rootScope.$digest();
        expect($rootScope.licensed).toEqual(true);
        expect($rootScope.username).toEqual('myname');
        expect($window.externalLinkClickHandler).toBeDefined();
        expect($state.current.name).toBe('violations');

        expect(pendoServiceMock.start).toHaveBeenCalled();
      });

      it('validate state with only reports-list available', async function () {
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
        $rootScope.$digest();
        productLicenseLoadDefer.resolve({});
        axiosMock.onGet(CLMLocations.getSessionUrl()).reply(200, { username: 'myname' });

        initService.start();

        await userSession.waitForLogin();
        $rootScope.$digest();
        expect($rootScope.licensed).toEqual(true);
        expect($rootScope.username).toEqual('myname');
        expect($window.externalLinkClickHandler).toBeDefined();
        expect($state.current.name).toBe('violations');

        expect(pendoServiceMock.start).toHaveBeenCalled();
      });

      it('validate state with dashboard and reports-list available', async function () {
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
        $rootScope.$digest();
        productLicenseLoadDefer.resolve({});
        axiosMock.onGet(CLMLocations.getSessionUrl()).reply(200, { username: 'myname' });

        initService.start();

        await userSession.waitForLogin();
        $rootScope.$digest();
        expect($rootScope.licensed).toEqual(true);
        expect($rootScope.username).toEqual('myname');
        expect($window.externalLinkClickHandler).toBeDefined();
        expect($state.current.name).toBe('dashboard.overview.violations');

        expect(pendoServiceMock.start).toHaveBeenCalled();
      });

      it('validate state with neither dashboard nor reports-list available', async function () {
        $rootScope.isAllowExternalHyperlinks = false;
        $rootScope.$digest();
        productLicenseLoadDefer.resolve({});
        axiosMock.onGet(CLMLocations.getSessionUrl()).reply(200, { username: 'myname' });

        initService.start();

        await userSession.waitForLogin();
        $rootScope.$digest();
        expect($rootScope.licensed).toEqual(true);
        expect($rootScope.username).toEqual('myname');
        expect($window.externalLinkClickHandler).toBeDefined();
        expect($state.current.name).toBe('gettingStarted');
        expect(pendoServiceMock.start).toHaveBeenCalled();
      });
    });

    describe('on beforeunload event', function () {
      let $window, $rootScope, initService, $state;

      beforeEach(inject(function (
        _$httpBackend_,
        CLMLocations,
        _$window_,
        _$rootScope_,
        _$ngRedux_,
        _initService_,
        _$state_
      ) {
        $window = _$window_;
        $rootScope = _$rootScope_;
        $ngRedux = _$ngRedux_;
        initService = _initService_;
        $state = _$state_;
        spyOn(gettingStartedTelemetryServiceHelper, 'submitData');
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
        axiosMock.onGet(CLMLocations.getSessionUrl()).reply(200, { username: 'myname' });
      }));

      it('fires synchronous "DEPARTED" telemetry event if current page is gettingStarted', async function () {
        initService.start();

        await userSession.waitForLogin();
        $state.current.name = 'gettingStarted';
        scope.$digest();
        $window.dispatchEvent(new Event('beforeunload'));
        expect(gettingStartedTelemetryServiceHelper.submitData).toHaveBeenCalledWith('DEPARTED', null, true);
      });

      it('does not fire "DEPARTED" telemetry event if current page is not gettingStarted', async function () {
        initService.start();

        await userSession.waitForLogin();
        $window.dispatchEvent(new Event('beforeunload'));
        expect(gettingStartedTelemetryServiceHelper.submitData).not.toHaveBeenCalled();
      });
    });
  });

  describe('pendoService calls', function () {
    let initService, pendoService, CLMLocations, $rootScope;

    beforeEach(inject(function (_pendoService_, _initService_, _CLMLocations_, _$rootScope_) {
      pendoService = _pendoService_;
      initService = _initService_;
      CLMLocations = _CLMLocations_;
      $rootScope = _$rootScope_;
    }));

    it('calls pendoService.start before login', async function () {
      $rootScope.isAllowExternalHyperlinks = true;
      $rootScope.$digest();
      productLicenseLoadDefer.resolve({});
      axiosMock.onGet(CLMLocations.getSessionUrl()).reply(200, { username: 'myname' });

      initService.start();

      await userSession.waitForLogin();
      expect(pendoService.start).toHaveBeenCalled();
    });

    it('calls pendoService a second time after login and license fetch', async function () {
      $rootScope.isAllowExternalHyperlinks = true;
      $rootScope.$digest();
      productLicenseLoadDefer.resolve({});
      axiosMock.onGet(CLMLocations.getSessionUrl()).reply(200, { username: 'myname' });

      initService.start();

      expect(pendoService.start).toHaveBeenCalledTimes(1);

      await userSession.waitForLogin();
      $rootScope.$digest();
      expect(pendoService.start).toHaveBeenCalledTimes(2);
    });

    it('calls pendoService a second time after login if the license is not installed', async function () {
      $rootScope.isAllowExternalHyperlinks = true;
      $rootScope.$digest();
      productLicenseLoadDefer.reject({ response: { status: 402 } });
      axiosMock.onGet(CLMLocations.getSessionUrl()).reply(200, { username: 'myname' });

      initService.start();

      expect(pendoService.start).toHaveBeenCalledTimes(1);

      await userSession.waitForLogin();
      $rootScope.$digest();
      expect(pendoService.start).toHaveBeenCalledTimes(2);
    });

    it('does not call pendoService a second time after failed login', async function () {
      $rootScope.isAllowExternalHyperlinks = true;
      $rootScope.$digest();
      productLicenseLoadDefer.resolve({});
      axiosMock.onGet(CLMLocations.getSessionUrl()).replyOnce(401);

      initService.start();

      expect(pendoService.start).toHaveBeenCalledTimes(1);

      await waitFor(() => {
        $rootScope.$digest();
        return axiosMock.history['get'].length > 0;
      });
      $rootScope.$digest();
      expect(pendoService.start).toHaveBeenCalledTimes(1);
    });
  });
});
