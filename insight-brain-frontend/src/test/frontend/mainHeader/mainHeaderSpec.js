/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import mainHeaderModule from '../../../main/frontend/mainHeader/module';
import legacyConfigurationModule from '../../../main/frontend/LegacyConfigurationModule';

describe('mainHeaderSpec', function() {

  beforeEach(angular.mock.module(mainHeaderModule.name, legacyConfigurationModule.name));

  var $scope,
      $rootScope,
      mockSystemConfigurationPropertyService,
      mockCurrentUser,
      mockPermissionService,
      mockProductFeatures,
      isSuccessMetricsEnabledDeferred,
      isFullTextSearchEnabledDeferred,
      loginDeferred,
      productFeaturesDeferred,
      vm,
      clmServerVersion;

  beforeEach(inject(function(_$rootScope_, $q, $componentController) {
    clmServerVersion = window.clmServerVersion;
    $scope = _$rootScope_.$new();
    $rootScope = _$rootScope_;
    isSuccessMetricsEnabledDeferred = $q.defer();
    isFullTextSearchEnabledDeferred = $q.defer();
    loginDeferred = $q.defer();
    productFeaturesDeferred = $q.defer();
    mockSystemConfigurationPropertyService = {
      isSuccessMetricsEnabled: jasmine.createSpy().and.returnValue(isSuccessMetricsEnabledDeferred.promise),
      isFullTextSearchEnabled: jasmine.createSpy().and.returnValue(isFullTextSearchEnabledDeferred.promise)
    };

    mockCurrentUser = {
      fetch: jasmine.createSpy('fetch'),
      waitForLogin: jasmine.createSpy('waitForLogin').and.returnValue(loginDeferred.promise)
    };

    mockPermissionService = { getValidPermissions: jasmine.createSpy().and.returnValue($q.resolve())};

    mockProductFeatures = {
      load: jasmine.createSpy('load').and.returnValue(productFeaturesDeferred.promise)
    };

    vm = $componentController('mainHeader', {
      PermissionService: mockPermissionService,
      CurrentUser: mockCurrentUser,
      systemConfigurationPropertyService: mockSystemConfigurationPropertyService,
      ProductFeatures: mockProductFeatures
    });
  }));

  afterEach(function() {
    window.clmServerVersion = clmServerVersion;
    $scope.$destroy();
  });

  it('properly loads on enabled success metrics', function() {
    vm.$onInit();
    loginDeferred.resolve();
    isSuccessMetricsEnabledDeferred.resolve(true);
    $scope.$digest();

    expect(vm.isSuccessMetricsEnabled).toBe(true);
  });

  it('properly loads on disabled success metrics', function() {
    vm.$onInit();
    loginDeferred.resolve();
    isSuccessMetricsEnabledDeferred.reject('disabled');
    $scope.$digest();

    expect(vm.isSuccessMetricsEnabled).toBe(false);
  });

  it('properly loads on enabled full text search', function() {
    vm.$onInit();
    loginDeferred.resolve();
    isFullTextSearchEnabledDeferred.resolve(true);
    $scope.$digest();

    expect(vm.isFullTextSearchEnabled).toBe(true);
  });

  it('properly loads on disabled full text search', function() {
    vm.$onInit();
    loginDeferred.resolve();
    isFullTextSearchEnabledDeferred.resolve(false);
    $scope.$digest();

    expect(vm.isFullTextSearchEnabled).toBe(false);
  });

  it('does not load success metrics, full text search, permissions, or features until after login', function() {
    vm.$onInit();

    isSuccessMetricsEnabledDeferred.reject('disabled');
    isFullTextSearchEnabledDeferred.resolve(false);
    expect(mockSystemConfigurationPropertyService.isSuccessMetricsEnabled).not.toHaveBeenCalled();
    expect(mockSystemConfigurationPropertyService.isFullTextSearchEnabled).not.toHaveBeenCalled();
    expect(mockPermissionService.getValidPermissions).not.toHaveBeenCalled();
    expect(mockProductFeatures.load).not.toHaveBeenCalled();

    loginDeferred.resolve();
    $scope.$digest();

    expect(mockSystemConfigurationPropertyService.isSuccessMetricsEnabled).toHaveBeenCalled();
    expect(mockSystemConfigurationPropertyService.isFullTextSearchEnabled).toHaveBeenCalled();
    expect(mockPermissionService.getValidPermissions).toHaveBeenCalled();
    expect(mockProductFeatures.load).toHaveBeenCalled();
  });

  it('resets isSuccessMetricsEnabled on successMetricsConfigurationUpdated event', function() {
    vm.$onInit();
    isSuccessMetricsEnabledDeferred.resolve(false);

    expect(vm.isSuccessMetricsEnabled).toBe(false);

    $rootScope.$broadcast('successMetricsConfigurationUpdated', true);

    expect(vm.isSuccessMetricsEnabled).toBe(true);

    $rootScope.$broadcast('successMetricsConfigurationUpdated', false);

    expect(vm.isSuccessMetricsEnabled).toBe(false);
  });

  it('resets isFullTextSearchEnabled on fullTextSearchConfigurationUpdated event', function() {
    vm.$onInit();
    isFullTextSearchEnabledDeferred.resolve(false);

    expect(vm.isFullTextSearchEnabled).toBe(false);

    $rootScope.$broadcast('fullTextSearchConfigurationUpdated', true);

    expect(vm.isFullTextSearchEnabled).toBe(true);

    $rootScope.$broadcast('fullTextSearchConfigurationUpdated', false);

    expect(vm.isFullTextSearchEnabled).toBe(false);
  });

  it('properly determines the displayed release version number', function() {
    vm.$onInit();

    window.clmServerVersion = '1.50.0-SNAPSHOT';
    expect(vm.getReleaseVersion()).toEqual('50');

    window.clmServerVersion = '1.50.0-01';
    expect(vm.getReleaseVersion()).toEqual('50');

    window.clmServerVersion = '1.50.1-SNAPSHOT';
    expect(vm.getReleaseVersion()).toEqual('50.1');

    window.clmServerVersion = '1.50.1-01';
    expect(vm.getReleaseVersion()).toEqual('50.1');

    window.clmServerVersion = '50.0-SNAPSHOT';
    expect(vm.getReleaseVersion()).toEqual('50');

    window.clmServerVersion = '50.0-01';
    expect(vm.getReleaseVersion()).toEqual('50');

    window.clmServerVersion = '50.1-SNAPSHOT';
    expect(vm.getReleaseVersion()).toEqual('50.1');

    window.clmServerVersion = '50.1-01';
    expect(vm.getReleaseVersion()).toEqual('50.1');
  });

  describe('isLoggedIn()', function() {
    it('Not Loaded', function() {
      expect(vm.isLoggedIn()).toBeFalsy();
    });
    it('Logged In', function() {
      $rootScope.username = 'user';
      vm.$onInit();
      expect(vm.isLoggedIn()).toBeTruthy();
    });
    it('Not LoggedIn', function() {
      vm.$onInit();
      expect(vm.isLoggedIn()).toBeFalsy();
    });
  });

  describe('isLicensed()', function() {
    it('Not Loaded', function() {
      expect(vm.isLicensed()).toBeFalsy();
    });
    it('Licensed', function() {
      $rootScope.licensed = true;
      vm.$onInit();
      expect(vm.isLicensed()).toBeTruthy();
    });
    it('Not Licensed', function() {
      vm.$onInit();
      expect(vm.isLicensed()).toBeFalsy();
    });
  });

  describe('login', function() {
    it('calls CurrentUser.fetch', function() {
      vm.$onInit();

      expect(mockCurrentUser.fetch).not.toHaveBeenCalled();

      vm.login();

      expect(mockCurrentUser.fetch).toHaveBeenCalled();
    });
  });

  describe('shouldShowLoginButton', function() {
    let routeStateUtilService;

    beforeEach(inject(function(_routeStateUtilService_) {
      routeStateUtilService = _routeStateUtilService_;
    }));

    it('returns false if the user is logged in already', function() {
      vm.$onInit();
      $rootScope.username = 'user';
      $scope.$digest();
      spyOn(routeStateUtilService, 'stateRequiresAuthentication').and.returnValue(true);

      expect(vm.shouldShowLoginButton()).toBe(false);

      // whether auth is required makes no difference
      routeStateUtilService.stateRequiresAuthentication.and.returnValue(false);
      expect(vm.shouldShowLoginButton()).toBe(false);
    });

    it('returns false if the user is not logged in but the current page requires authentication', function() {
      vm.$onInit();
      spyOn(routeStateUtilService, 'stateRequiresAuthentication').and.returnValue(true);

      expect(vm.shouldShowLoginButton()).toBe(false);
    });

    it('returns true if the user is not logged in and the current page does not require authentication', function() {
      vm.$onInit();
      spyOn(routeStateUtilService, 'stateRequiresAuthentication').and.returnValue(false);

      expect(vm.shouldShowLoginButton()).toBe(true);
    });
  });
});
