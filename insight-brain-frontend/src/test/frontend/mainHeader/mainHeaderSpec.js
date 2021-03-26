/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import mainHeaderModule from '../../../main/frontend/mainHeader/module';
import legacyConfigurationModule from '../../../main/frontend/LegacyConfigurationModule';

describe('mainHeaderSpec', function() {

  var $scope,
      $rootScope,
      $ngRedux,
      unsubscribeSpy,
      mockSystemConfigurationPropertyService,
      mockCurrentUser,
      mockPermissionService,
      mockProductFeatures,
      isSuccessMetricsEnabledDeferred,
      loginDeferred,
      productFeaturesDeferred,
      vm,
      clmServerVersion;

  beforeEach(angular.mock.module(mainHeaderModule.name, legacyConfigurationModule.name, function($provide) {
    unsubscribeSpy = SpecUtil.mockNgRedux($provide);
  }));

  beforeEach(inject(function(_$rootScope_, $q, $componentController, _$ngRedux_) {
    clmServerVersion = window.clmServerVersion;
    $scope = _$rootScope_.$new();
    $rootScope = _$rootScope_;
    $ngRedux = _$ngRedux_;
    isSuccessMetricsEnabledDeferred = $q.defer();
    loginDeferred = $q.defer();
    productFeaturesDeferred = $q.defer();
    mockSystemConfigurationPropertyService = {
      isSuccessMetricsEnabled: jasmine.createSpy().and.returnValue(isSuccessMetricsEnabledDeferred.promise)
    };

    mockCurrentUser = {
      fetch: jasmine.createSpy('fetch'),
      waitForLogin: jasmine.createSpy('waitForLogin').and.returnValue(loginDeferred.promise)
    };

    mockPermissionService = {getValidPermissions: jasmine.createSpy().and.returnValue($q.resolve())};

    mockProductFeatures = jasmine.createSpyObj('mockProductFeatures', ['isAvailable', 'load']);
    mockProductFeatures.load.and.returnValue(productFeaturesDeferred.promise);
    mockProductFeatures.isAvailable.and.callFake(function() {
      return false;
    });

    $ngRedux.dispatch = jasmine.createSpy('dispatch');

    vm = $componentController('mainHeader', {
      PermissionService: mockPermissionService,
      CurrentUser: mockCurrentUser,
      systemConfigurationPropertyService: mockSystemConfigurationPropertyService,
      ProductFeatures: mockProductFeatures,
      $scope: $scope
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

  it('properly loads on supported firewall', function() {
    mockProductFeatures.isAvailable.and.callFake(function(feature) {
      return feature === 'firewall' || feature === 'release-integrity';
    });
    vm.$onInit();
    loginDeferred.resolve();
    productFeaturesDeferred.resolve();
    $scope.$digest();

    expect(vm.isFirewallSupported).toBe(true);
  });

  it('properly loads on not supported firewall', function() {
    vm.$onInit();
    loginDeferred.resolve();
    productFeaturesDeferred.resolve();
    $scope.$digest();

    expect(vm.isFirewallSupported).toBe(false);
  });

  it('properly loads on supported advanced legal pack', function() {
    mockProductFeatures.isAvailable.and.callFake(function(feature) {
      return feature === 'advanced-legal-pack';
    });
    vm.$onInit();
    loginDeferred.resolve();
    productFeaturesDeferred.resolve();
    $scope.$digest();

    expect(vm.isAdvancedLegalPackSupported).toBe(true);
  });

  it('properly loads on not supported advanced legal pack', function() {
    vm.$onInit();
    loginDeferred.resolve();
    productFeaturesDeferred.resolve();
    $scope.$digest();

    expect(vm.isAdvancedLegalPackSupported).toBe(false);
  });

  it('does not load success metrics, permissions, or features until after login', function() {
    vm.$onInit();

    isSuccessMetricsEnabledDeferred.reject('disabled');
    expect(mockSystemConfigurationPropertyService.isSuccessMetricsEnabled).not.toHaveBeenCalled();
    expect($ngRedux.dispatch).not.toHaveBeenCalled();
    expect(mockPermissionService.getValidPermissions).not.toHaveBeenCalled();
    expect(mockProductFeatures.load).not.toHaveBeenCalled();

    loginDeferred.resolve();
    $scope.$digest();

    expect(mockSystemConfigurationPropertyService.isSuccessMetricsEnabled).toHaveBeenCalled();
    expect($ngRedux.dispatch).toHaveBeenCalledTimes(2);
    expect(mockPermissionService.getValidPermissions).toHaveBeenCalled();
    expect(mockProductFeatures.load).toHaveBeenCalled();
  });

  describe('mapStateToThis', function() {
    let mapStateToThis;

    beforeEach(function() {
      vm.$onInit();
      loginDeferred.resolve();
      $scope.$digest();
      mapStateToThis = $ngRedux.connect.calls.first().args[0];
    });

    it('returns an object with isAdvancedSearchEnabled set to false given a state with no server data', function() {
      let mockStateNoServerData = {
        advancedSearchConfig: {
          serverData: null
        }
      };

      expect(mapStateToThis(mockStateNoServerData).isAdvancedSearchEnabled).toBeFalsy();
    });

    it('returns an object with isAdvancedSearchEnabled set to true given a state with server data and isEnabled true',
        function() {
          let mockStateWithServerDataAndIsEnabledTrue = {
            advancedSearchConfig: {
              serverData: {
                isEnabled: true
              }
            }
          };

          expect(mapStateToThis(mockStateWithServerDataAndIsEnabledTrue).isAdvancedSearchEnabled).toBe(true);
        });

    it('returns an object with isAdvancedSearchEnabled set to false given a state with server data and isEnabled false',
        function() {
          let mockStateWithServerDataAndIsEnabledFalse = {
            advancedSearchConfig: {
              serverData: {
                isEnabled: false
              }
            }
          };

          expect(mapStateToThis(mockStateWithServerDataAndIsEnabledFalse).isAdvancedSearchEnabled).toBe(false);
        });

    it('returns an object with isFirewallEnabled set to false given a state with no firewall state statusState',
        function() {
          let mockStateNoFirewallStatusState = {
            firewall: {}
          };

          expect(mapStateToThis(mockStateNoFirewallStatusState).isFirewallEnabled).toBeFalsy();
        });

    it('returns an object with isFirewallEnabled true given state with firewall statusState and isEnabled true',
        function() {
          let mockStateWithIsEnabledTrue = {
            firewall: {
              statusState: {
                isEnabled: true
              }
            }
          };

          expect(mapStateToThis(mockStateWithIsEnabledTrue).isFirewallEnabled).toBe(true);
        });

    it('returns object with isFirewallEnabled false given state with firewall statusState and isEnabled false',
        function() {
          let mockStateWithIsEnabledFalse = {
            firewall: {
              statusState: {
                isEnabled: false
              }
            }
          };

          expect(mapStateToThis(mockStateWithIsEnabledFalse).isFirewallEnabled).toBe(false);
        });
  });

  it('calls unsubscribe when the $scope is destroyed', function() {
    vm.$onInit();
    loginDeferred.resolve();
    $scope.$digest();

    expect(unsubscribeSpy).not.toHaveBeenCalled();
    $scope.$destroy();
    expect(unsubscribeSpy).toHaveBeenCalled();
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
