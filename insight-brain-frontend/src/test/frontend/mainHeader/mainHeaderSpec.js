/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import mainHeaderModule from '../../../main/frontend/mainHeader/module';
import legacyConfigurationModule from '../../../main/frontend/LegacyConfigurationModule';

describe('mainHeaderSpec', function () {
  var $scope,
    $rootScope,
    mockSystemConfigurationPropertyService,
    mockCurrentUser,
    mockPermissionService,
    mockProductFeatures,
    mockRouteStateUtilService,
    isSuccessMetricsEnabledDeferred,
    loginDeferred,
    productFeaturesDeferred,
    routeStateUtilServiceDeferred,
    vm,
    clmServerVersion;

  beforeEach(angular.mock.module(mainHeaderModule.name, legacyConfigurationModule.name));

  beforeEach(inject(function (_$rootScope_, $q, $componentController) {
    clmServerVersion = window.clmServerVersion;
    $scope = _$rootScope_.$new();
    $rootScope = _$rootScope_;
    isSuccessMetricsEnabledDeferred = $q.defer();
    loginDeferred = $q.defer();
    productFeaturesDeferred = $q.defer();
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

    mockProductFeatures = jasmine.createSpyObj('mockProductFeatures', ['isAvailable', 'load']);
    mockProductFeatures.load.and.returnValue(productFeaturesDeferred.promise);
    mockProductFeatures.isAvailable.and.callFake(function () {
      return false;
    });

    mockRouteStateUtilService = jasmine.createSpyObj('mockRouteStateUtilService', [
      'stateRequiresAuthenticationSync',
      'stateRequiresAuthentication',
    ]);
    mockRouteStateUtilService.stateRequiresAuthentication.and.returnValue(routeStateUtilServiceDeferred.promise);

    vm = $componentController('mainHeader', {
      PermissionService: mockPermissionService,
      CurrentUser: mockCurrentUser,
      systemConfigurationPropertyService: mockSystemConfigurationPropertyService,
      ProductFeatures: mockProductFeatures,
      $scope: $scope,
      routeStateUtilService: mockRouteStateUtilService,
    });
  }));

  afterEach(function () {
    window.clmServerVersion = clmServerVersion;
    $scope.$destroy();
  });

  it('properly loads on not supported labs data insights', function () {
    vm.$onInit();
    loginDeferred.resolve();
    productFeaturesDeferred.resolve();
    $scope.$digest();

    expect(vm.isLabsDataInsightsEnabled).toBe(false);
  });

  it('properly loads on supported labs data insights', function () {
    mockProductFeatures.isAvailable.and.callFake(function (feature) {
      return feature === 'data-insights';
    });
    vm.$onInit();
    loginDeferred.resolve();
    productFeaturesDeferred.resolve();
    $scope.$digest();

    expect(vm.isLabsDataInsightsEnabled).toBe(true);
  });

  it('does not load permissions or features until after login', function () {
    vm.$onInit();

    expect(mockPermissionService.getValidPermissions).not.toHaveBeenCalled();
    expect(mockProductFeatures.load).not.toHaveBeenCalled();

    loginDeferred.resolve();
    $scope.$digest();

    expect(mockPermissionService.getValidPermissions).toHaveBeenCalled();
    expect(mockProductFeatures.load).toHaveBeenCalled();
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
