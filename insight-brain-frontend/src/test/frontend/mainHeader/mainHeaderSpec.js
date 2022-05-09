/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import mainHeaderModule from 'MainRoot/mainHeader/module';
import legacyConfigurationModule from 'MainRoot/LegacyConfigurationModule';
import { actions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { mapStateToThis } from 'MainRoot/mainHeader/mainHeader';

describe('mainHeaderSpec', function () {
  var $scope,
    $rootScope,
    mockSystemConfigurationPropertyService,
    mockCurrentUser,
    mockPermissionService,
    mockRouteStateUtilService,
    routeStateUtilServiceDeferred,
    isSuccessMetricsEnabledDeferred,
    loginDeferred,
    unsubscribeSpy,
    vm,
    clmServerVersion,
    fetchProductFeaturesSpy;

  beforeEach(
    angular.mock.module(mainHeaderModule.name, legacyConfigurationModule.name, function ($provide) {
      unsubscribeSpy = SpecUtil.mockNgRedux($provide);
    })
  );

  beforeEach(inject(function (_$rootScope_, $q, $componentController) {
    clmServerVersion = window.clmServerVersion;
    $scope = _$rootScope_.$new();
    $rootScope = _$rootScope_;
    isSuccessMetricsEnabledDeferred = $q.defer();
    loginDeferred = $q.defer();
    routeStateUtilServiceDeferred = $q.defer();
    mockSystemConfigurationPropertyService = {
      isSuccessMetricsEnabled: jasmine.createSpy().and.returnValue(isSuccessMetricsEnabledDeferred.promise),
    };

    mockCurrentUser = {
      fetch: jasmine.createSpy('fetch'),
      waitForLogin: jasmine.createSpy('waitForLogin').and.returnValue(loginDeferred.promise),
    };

    mockPermissionService = {
      getValidPermissions: jasmine.createSpy().and.returnValue($q.resolve()),
    };

    fetchProductFeaturesSpy = spyOn(actions, 'fetchProductFeaturesIfNeeded').and.returnValue({ payload: [] });

    mockRouteStateUtilService = jasmine.createSpyObj('mockRouteStateUtilService', [
      'stateRequiresAuthenticationSync',
      'stateRequiresAuthentication',
    ]);
    mockRouteStateUtilService.stateRequiresAuthentication.and.returnValue(routeStateUtilServiceDeferred.promise);

    vm = $componentController('mainHeader', {
      PermissionService: mockPermissionService,
      CurrentUser: mockCurrentUser,
      systemConfigurationPropertyService: mockSystemConfigurationPropertyService,
      $scope: $scope,
      routeStateUtilService: mockRouteStateUtilService,
    });
  }));

  afterEach(function () {
    window.clmServerVersion = clmServerVersion;
    $scope.$destroy();
  });

  describe('mapStateToThis', () => {
    it('sets applicableLabels, error, loading and ownerName', () => {
      const state = {
        productFeatures: {
          productFeatures: {
            'webhooks-for-applications': true,
            'data-insights': true,
            automation: true,
          },
        },
      };

      const output = mapStateToThis(state);

      expect(output.isWebhooksSupported).toBeTrue();
      expect(output.isLabsDataInsightsEnabled).toBeTrue();
      expect(output.isSourceControlSupported).toBeTrue();
    });
  });

  it('calls fetchProductFeaturesIfNeeded action on init', function () {
    vm.$onInit();
    loginDeferred.resolve();
    $scope.$digest();

    expect(fetchProductFeaturesSpy).toHaveBeenCalled();
  });

  it('calls unsubscribe when the $scope is destroyed', function () {
    vm.$onInit();
    loginDeferred.resolve();
    $scope.$digest();

    expect(unsubscribeSpy).not.toHaveBeenCalled();
    $scope.$destroy();
    expect(unsubscribeSpy).toHaveBeenCalled();
  });

  it('does not load permissions or features until after login', function () {
    vm.$onInit();

    expect(mockPermissionService.getValidPermissions).not.toHaveBeenCalled();

    loginDeferred.resolve();
    $scope.$digest();

    expect(mockPermissionService.getValidPermissions).toHaveBeenCalled();
  });

  describe('isLoggedIn()', function () {
    it('Not Loaded', function () {
      expect(vm.isLoggedIn()).toBeFalsy();
    });
    it('Logged In', function () {
      $rootScope.username = 'user';
      vm.$onInit();
      expect(vm.isLoggedIn()).toBeTruthy();
    });
    it('Not LoggedIn', function () {
      vm.$onInit();
      expect(vm.isLoggedIn()).toBeFalsy();
    });
  });

  describe('login', function () {
    it('calls CurrentUser.fetch', function () {
      vm.$onInit();

      expect(mockCurrentUser.fetch).not.toHaveBeenCalled();

      vm.login();

      expect(mockCurrentUser.fetch).toHaveBeenCalled();
    });
  });

  describe('shouldShowLoginButton', function () {
    it('returns false if the user is logged in already and page requires authentication', function () {
      vm.$onInit();
      $rootScope.username = 'user';
      $scope.$digest();
      routeStateUtilServiceDeferred.resolve(true);

      $rootScope.$digest();
      expect(vm.shouldShowLoginButton).toBe(false);
    });

    it('returns false if the user is logged in already and page does not require authentication', function () {
      vm.$onInit();
      $rootScope.username = 'user';
      $scope.$digest();
      routeStateUtilServiceDeferred.resolve(false);

      $rootScope.$digest();
      expect(vm.shouldShowLoginButton).toBe(false);
    });

    it('returns false if the user is not logged in but the current page requires authentication', function () {
      routeStateUtilServiceDeferred.resolve(true);
      vm.$onInit();

      $rootScope.$digest();
      expect(vm.shouldShowLoginButton).toBe(false);
    });

    it('returns true if the user is not logged in and the current page does not require authentication', function () {
      routeStateUtilServiceDeferred.resolve(false);
      vm.$onInit();

      $rootScope.$digest();
      expect(vm.shouldShowLoginButton).toBe(true);
    });
  });
});
