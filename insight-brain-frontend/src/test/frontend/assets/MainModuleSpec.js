/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as gettingStartedTelemetryServiceHelper from 'MainRoot/configuration/gettingStarted/gettingStartedTelemetryServiceHelper';
import * as RouteProductLicenseValidator from 'MainRoot/routeProductLicenseValidator/RouteProductLicenseValidator';
import { waitFor } from 'TestRoot/SpecUtil';
import * as userSession from 'MainRoot/user/userSessionUtils';
import * as routeStateUtilService from 'MainRoot/utility/services/routeStateUtilService';
import * as ProductLicense from 'MainRoot/utility/services/ProductLicense';
import MockAdapter from 'axios-mock-adapter';
import axios from 'axios';
window.angularDebug = true;

describe('mainModuleSpec', function () {
  let scope, $ngRedux, productLicenseLoadDefer, mockPendoService, InitModule, axiosMock;

  beforeAll(() => {
    // Create axios mock adapter for HTTP mocking
    axiosMock = new MockAdapter(axios);
  });

  afterEach(() => {
    if ($ngRedux) {
      userSession._resetForTest($ngRedux);
    }
    // Reset axios mocks after each test
    if (axiosMock) {
      axiosMock.reset();
    }
  });

  afterAll(() => {
    // Restore axios after all tests
    if (axiosMock) {
      axiosMock.restore();
    }
  });

  beforeEach(() => {
    // Create fresh mock for each test
    mockPendoService = {
      start: jasmine.createSpy('start'),
    };

    // Mock the ES6 module functions
    spyOn(routeStateUtilService, 'initialize');
    spyOn(routeStateUtilService, 'stateRequiresAuthenticationSync').and.returnValue(true);
    spyOn(routeStateUtilService, 'stateRequiresAuthentication').and.returnValue(Promise.resolve(true));

    // Use inject-loader to mock the pendoService dependency
    const MainModuleInjector = require('inject-loader!MainRoot/MainModule');
    const moduleExports = MainModuleInjector({
      './pendo/mainBundlePendoService': {
        default: mockPendoService,
        setUrlService: jasmine.createSpy('setUrlService'),
        // NOTE: this is a hack to get around an apparent bug in inject-loader where it ends up exporting the entire
        // exports object as the default export
        ...mockPendoService,
      },
    });
    InitModule = moduleExports.InitModule;
  });

  beforeEach(function () {
    angular.mock.module(InitModule.name, function ($provide, $stateProvider) {
      const unsubscribeSpy = jasmine.createSpy('unsubscribe');

      $provide.service('$ngRedux', function () {
        let mockReduxState = {
          productFeatures: { productFeatures: {} },
          firewallOnboarding: {
            unconfiguredRepoManagers: {
              repoManagers: [],
              loading: false,
              loadError: null,
            },
          },
          userSession: {
            data: null,
          },
          appError: {
            error: null,
          },
        };

        let connectedTarget = null;
        let mapStateToThisFunc = null;

        this.actions = [];
        this.connect = jasmine.createSpy('connect').and.callFake(function (mapStateToThis) {
          mapStateToThisFunc = mapStateToThis;
          return function (target) {
            connectedTarget = target;
            // Immediately sync Redux state to target (usually $rootScope)
            const mappedState = mapStateToThis(mockReduxState);
            Object.assign(target, mappedState);
            return unsubscribeSpy;
          };
        });

        this.getState = jasmine.createSpy('getState').and.callFake(() => mockReduxState);
        this.subscribe = jasmine.createSpy('subscribe').and.returnValue(unsubscribeSpy);

        // Mock dispatch to actually update state for relevant actions
        this.dispatch = jasmine.createSpy('dispatch').and.callFake((action) => {
          if (typeof action === 'function') {
            // Thunk actions return promises. Execute the thunk and ensure we return its result.
            const result = action(this.dispatch, this.getState);

            // If the thunk returns a promise, return it so callers can use .then()
            if (result && typeof result.then === 'function') {
              return result;
            }

            // Otherwise wrap in a resolved promise to maintain consistent API
            return Promise.resolve(result);
          } else {
            if (action && action.type === 'appError/setError') {
              mockReduxState.appError.error = action.payload;
            } else if (action && action.type === 'appError/clearError') {
              mockReduxState.appError.error = null;
            } else if (action && action.type === 'userSession/fetchUserSession/fulfilled') {
              // When user session is fetched successfully, update the Redux state
              mockReduxState.userSession.data = action.payload;
            } else if (action && action.type === 'LOAD_USER_FULFILLED') {
              // Also handle the legacy action for backward compatibility
              if (action.payload && action.payload.currentUser) {
                mockReduxState.userSession.data = action.payload.currentUser;
              }
            } else if (action && action.type === 'productFeatures/fetchProductFeaturesIfNeeded/fulfilled') {
              // Update product features state when fulfilled
              if (action.payload) {
                mockReduxState.productFeatures.productFeatures = {
                  ...mockReduxState.productFeatures.productFeatures,
                  ...action.payload,
                };
              }
            }

            // Re-sync state to $rootScope after any action (if connect was called)
            if (connectedTarget && mapStateToThisFunc) {
              const newMappedState = mapStateToThisFunc(mockReduxState);
              Object.assign(connectedTarget, newMappedState);
            }

            this.actions.push(action);
            return action;
          }
        });
      });

      // mock the window using anything on which events can be dispatched
      const mockWindow = document.createElement('div');
      mockWindow.top = {
        sessionExpired: jasmine.createSpy('sessionExpired'),
      };
      $provide.value('$window', mockWindow);

      $stateProvider.state('someOtherState', {
        url: '/someOtherState',
      });
    });
  });

  beforeEach(inject(function ($q, $rootScope, _$ngRedux_) {
    scope = $rootScope.$new();
    $ngRedux = _$ngRedux_;

    productLicenseLoadDefer = $q.defer();
    spyOn(ProductLicense, 'loadIfNotYetLoaded').and.returnValue(productLicenseLoadDefer.promise);

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
      let $rootScope, $state, $window, initService, waitForLoginDeferred;

      // Helper to wait for handler with proper async timing
      async function waitForHandler() {
        const maxAttempts = 50;
        let attempts = 0;
        while (!$window.externalLinkClickHandler && attempts < maxAttempts) {
          await new Promise((resolve) => setTimeout(resolve, 50));
          $rootScope.$digest();
          attempts++;
        }
        return !!$window.externalLinkClickHandler;
      }

      beforeEach(inject(function ($q) {
        // Mock waitForLogin with a deferred promise (same approach as mainHeaderSpec and primaryNavSpec)
        waitForLoginDeferred = $q.defer();

        // Wrap the original resolve to also dispatch the Redux action
        const originalResolve = waitForLoginDeferred.resolve;
        waitForLoginDeferred.resolve = function (authStatus) {
          // Dispatch Redux action to update userSession state
          $ngRedux.dispatch({
            type: 'userSession/fetchUserSession/fulfilled',
            payload: authStatus,
          });
          // Call original resolve
          return originalResolve.call(waitForLoginDeferred, authStatus);
        };

        spyOn(userSession, 'waitForLogin').and.returnValue(waitForLoginDeferred.promise);
      }));

      beforeEach(inject(function (_initService_, _$rootScope_, _$window_, _$state_) {
        initService = _initService_;
        $rootScope = _$rootScope_;
        $window = _$window_;
        $state = _$state_;
      }));

      it('validate state after all requests succeed', function () {
        // Set isAllowExternalHyperlinksSupported in Redux state
        $ngRedux.getState().productFeatures.productFeatures.isAllowExternalHyperlinksSupported = true;
        $rootScope.$digest();
        productLicenseLoadDefer.resolve({});

        initService.start();

        waitForLoginDeferred.resolve({ username: 'myname' });
        $rootScope.$digest();
        expect($state.go).not.toHaveBeenCalled();
        expect($window.externalLinkClickHandler).not.toBeDefined();
        expect(mockPendoService.start).toHaveBeenCalled();
      });

      it('validate state after license check fails because unlicensed', function () {
        // Set isAllowExternalHyperlinksSupported in Redux state
        $ngRedux.getState().productFeatures.productFeatures.isAllowExternalHyperlinksSupported = true;
        $rootScope.$digest();
        productLicenseLoadDefer.reject({ response: { status: 402 } });

        initService.start();

        waitForLoginDeferred.resolve({ username: 'myname' });
        $rootScope.$digest();
        expect($state.go).toHaveBeenCalledTimes(1);
        expect($state.go).toHaveBeenCalledWith('productlicense');

        expect(mockPendoService.start).toHaveBeenCalled();
      });

      it('validate state after logged in check error', async function () {
        // Set isAllowExternalHyperlinksSupported in Redux state
        $ngRedux.getState().productFeatures.productFeatures.isAllowExternalHyperlinksSupported = true;
        $rootScope.$digest();
        productLicenseLoadDefer.resolve({});

        initService.start();

        waitForLoginDeferred.reject(new Error('Login failed'));
        await waitFor(() => {
          $rootScope.$digest();
          return $ngRedux.getState().appError.error;
        });
        expect($ngRedux.getState().appError.error).toBeDefined();
        expect(mockPendoService.start).toHaveBeenCalled();
      });

      it('validate state after license check error', async function () {
        // Set isAllowExternalHyperlinksSupported in Redux state
        $ngRedux.getState().productFeatures.productFeatures.isAllowExternalHyperlinksSupported = true;
        $rootScope.$digest();

        initService.start();

        $rootScope.$digest();
        productLicenseLoadDefer.reject({ response: { status: 500 } });
        try {
          waitForLoginDeferred.resolve({ username: 'myname' });
        } catch (error) {
          // ignore
        }
        await waitFor(() => {
          $rootScope.$digest();
          return $ngRedux.getState().appError.error;
        });
        expect($ngRedux.getState().appError.error).toBeDefined();
        expect(mockPendoService.start).toHaveBeenCalled();
      });

      it('validate state after license check 403 error', async function () {
        $rootScope.$digest();
        const errorMsg = 'Access from this IP is not allowed, please contact an administrator.';

        initService.start();

        $rootScope.$digest();
        productLicenseLoadDefer.reject(errorMsg);
        try {
          waitForLoginDeferred.resolve({ username: 'myname' });
        } catch (error) {
          // ignore
        }
        await waitFor(() => {
          $rootScope.$digest();
          return $ngRedux.getState().appError.error;
        });
        expect($ngRedux.getState().appError.error).toEqual(errorMsg);
      });

      it('validate state after waitForLogin 403 error', async function () {
        $rootScope.$digest();
        const errorMsg = 'Access from this IP is not allowed, please contact an administrator.';
        productLicenseLoadDefer.resolve({});

        initService.start();

        waitForLoginDeferred.reject(errorMsg);
        await waitFor(() => {
          $rootScope.$digest();
          // Check Redux state instead of $rootScope
          return $ngRedux.getState().appError.error;
        });
        expect($ngRedux.getState().appError.error).toEqual(errorMsg);
      });

      it('validate state after external hyperlinks are disabled', async function () {
        // Mock product features HTTP call - return empty array (no 'allow-external-hyperlinks' feature)
        axiosMock.onGet('/rest/product/features').reply(200, []);

        // Set isAllowExternalHyperlinksSupported in Redux state
        $ngRedux.getState().productFeatures.productFeatures.isAllowExternalHyperlinksSupported = false;
        $rootScope.$digest();
        productLicenseLoadDefer.resolve({});

        initService.start();
        waitForLoginDeferred.resolve({ username: 'myname' });

        await waitForHandler();

        expect($window.externalLinkClickHandler).toBeDefined();
        expect(mockPendoService.start).toHaveBeenCalled();
      });

      it('validate state with only dashboard available', async function () {
        // Set isAllowExternalHyperlinksSupported in Redux state
        $ngRedux.getState().productFeatures.productFeatures.isAllowExternalHyperlinksSupported = false;
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
          userSession: {
            data: null,
          },
          appError: {
            error: null,
          },
        });
        $rootScope.$digest();
        productLicenseLoadDefer.resolve({});

        initService.start();

        waitForLoginDeferred.resolve({ username: 'myname' });
        await waitForHandler();

        expect($window.externalLinkClickHandler).toBeDefined();
        expect($state.current.name).toBe('dashboard.overview.violations');

        expect(mockPendoService.start).toHaveBeenCalled();
      });

      it('validate state with dashboard unavailable and reports-list available', async function () {
        // Set isAllowExternalHyperlinksSupported in Redux state
        $ngRedux.getState().productFeatures.productFeatures.isAllowExternalHyperlinksSupported = false;
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
          userSession: {
            data: null,
          },
          appError: {
            error: null,
          },
        });
        $rootScope.$digest();
        productLicenseLoadDefer.resolve({});

        initService.start();

        waitForLoginDeferred.resolve({ username: 'myname' });
        await waitForHandler();

        expect($window.externalLinkClickHandler).toBeDefined();
        expect($state.current.name).toBe('violations');

        expect(mockPendoService.start).toHaveBeenCalled();
      });

      it('validate state with only reports-list available', async function () {
        // Set isAllowExternalHyperlinksSupported in Redux state
        $ngRedux.getState().productFeatures.productFeatures.isAllowExternalHyperlinksSupported = false;
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
          userSession: {
            data: null,
          },
          appError: {
            error: null,
          },
        });
        $rootScope.$digest();
        productLicenseLoadDefer.resolve({});

        initService.start();

        waitForLoginDeferred.resolve({ username: 'myname' });
        await waitForHandler();

        expect($window.externalLinkClickHandler).toBeDefined();
        expect($state.current.name).toBe('violations');

        expect(mockPendoService.start).toHaveBeenCalled();
      });

      it('validate state with dashboard and reports-list available', async function () {
        // Set isAllowExternalHyperlinksSupported in Redux state
        $ngRedux.getState().productFeatures.productFeatures.isAllowExternalHyperlinksSupported = false;
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
          userSession: {
            data: null,
          },
          appError: {
            error: null,
          },
        });
        $rootScope.$digest();
        productLicenseLoadDefer.resolve({});

        initService.start();

        waitForLoginDeferred.resolve({ username: 'myname' });
        await waitForHandler();

        expect($window.externalLinkClickHandler).toBeDefined();
        expect($state.current.name).toBe('dashboard.overview.violations');

        expect(mockPendoService.start).toHaveBeenCalled();
      });

      it('validate state with neither dashboard nor reports-list available', async function () {
        // Mock product features HTTP call - return empty array
        axiosMock.onGet('/rest/product/features').reply(200, []);

        // Set isAllowExternalHyperlinksSupported in Redux state
        $ngRedux.getState().productFeatures.productFeatures.isAllowExternalHyperlinksSupported = false;
        $rootScope.$digest();
        productLicenseLoadDefer.resolve({});

        initService.start();

        waitForLoginDeferred.resolve({ username: 'myname' });
        await waitForHandler();

        expect($window.externalLinkClickHandler).toBeDefined();
        expect($state.current.name).toBe('gettingStarted');
        expect(mockPendoService.start).toHaveBeenCalled();
      });
    });

    describe('on beforeunload event', function () {
      let $window, initService, $state, waitForLoginDeferred;

      beforeEach(inject(function ($q) {
        // Mock waitForLogin with a deferred promise (same approach as mainHeaderSpec and primaryNavSpec)
        waitForLoginDeferred = $q.defer();
        spyOn(userSession, 'waitForLogin').and.returnValue(waitForLoginDeferred.promise);
      }));

      beforeEach(inject(function (_$httpBackend_, _$window_, _$ngRedux_, _initService_, _$state_) {
        $window = _$window_;
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
          userSession: {
            data: null,
          },
          appError: {
            error: null,
          },
        });

        // Set isAllowExternalHyperlinksSupported in Redux state
        $ngRedux.getState().productFeatures.productFeatures.isAllowExternalHyperlinksSupported = true;
        productLicenseLoadDefer.resolve({});
      }));

      it('fires synchronous "DEPARTED" telemetry event if current page is gettingStarted', function () {
        initService.start();

        waitForLoginDeferred.resolve({ username: 'myname' });
        $state.current.name = 'gettingStarted';
        scope.$digest();
        $window.dispatchEvent(new Event('beforeunload'));
        expect(gettingStartedTelemetryServiceHelper.submitData).toHaveBeenCalledWith('DEPARTED', null, true);
      });

      it('does not fire "DEPARTED" telemetry event if current page is not gettingStarted', function () {
        initService.start();

        waitForLoginDeferred.resolve({ username: 'myname' });
        $window.dispatchEvent(new Event('beforeunload'));
        expect(gettingStartedTelemetryServiceHelper.submitData).not.toHaveBeenCalled();
      });
    });
  });

  describe('pendoService calls', function () {
    let initService, $rootScope, waitForLoginDeferred;

    beforeEach(inject(function ($q) {
      // Mock waitForLogin with a deferred promise (same approach as mainHeaderSpec and primaryNavSpec)
      waitForLoginDeferred = $q.defer();
      spyOn(userSession, 'waitForLogin').and.returnValue(waitForLoginDeferred.promise);
    }));

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
        userSession: {
          data: null,
        },
        appError: {
          error: null,
        },
      });
    });

    beforeEach(inject(function (_initService_, _$rootScope_) {
      initService = _initService_;
      $rootScope = _$rootScope_;
    }));

    it('calls pendoService.start before login', function () {
      // Set isAllowExternalHyperlinksSupported in Redux state
      $ngRedux.getState().productFeatures.productFeatures.isAllowExternalHyperlinksSupported = true;
      $rootScope.$digest();
      productLicenseLoadDefer.resolve({});

      initService.start();

      waitForLoginDeferred.resolve({ username: 'myname' });
      expect(mockPendoService.start).toHaveBeenCalled();
    });

    it('calls pendoService a second time after login and license fetch', function () {
      // Set isAllowExternalHyperlinksSupported in Redux state
      $ngRedux.getState().productFeatures.productFeatures.isAllowExternalHyperlinksSupported = true;
      $rootScope.$digest();
      productLicenseLoadDefer.resolve({});

      initService.start();

      expect(mockPendoService.start).toHaveBeenCalledTimes(1);

      waitForLoginDeferred.resolve({ username: 'myname' });
      $rootScope.$digest();
      expect(mockPendoService.start).toHaveBeenCalledTimes(2);
    });

    it('calls pendoService a second time after login if the license is not installed', function () {
      // Set isAllowExternalHyperlinksSupported in Redux state
      $ngRedux.getState().productFeatures.productFeatures.isAllowExternalHyperlinksSupported = true;
      $rootScope.$digest();
      productLicenseLoadDefer.reject({ response: { status: 402 } });

      initService.start();

      expect(mockPendoService.start).toHaveBeenCalledTimes(1);

      waitForLoginDeferred.resolve({ username: 'myname' });
      $rootScope.$digest();
      expect(mockPendoService.start).toHaveBeenCalledTimes(2);
    });

    it('does not call pendoService a second time after failed login', function () {
      // Set isAllowExternalHyperlinksSupported in Redux state
      $ngRedux.getState().productFeatures.productFeatures.isAllowExternalHyperlinksSupported = true;
      $rootScope.$digest();
      productLicenseLoadDefer.resolve({});

      initService.start();

      expect(mockPendoService.start).toHaveBeenCalledTimes(1);

      // Simulate failed login
      waitForLoginDeferred.reject(new Error('Login failed'));
      $rootScope.$digest();

      // pendoService.start should still only be called once (not a second time after failed login)
      expect(mockPendoService.start).toHaveBeenCalledTimes(1);
    });
  });
});
