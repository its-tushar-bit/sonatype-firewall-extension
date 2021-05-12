/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import navigationContainerModule from '../../../main/frontend/navigationContainer/module';
import legacyConfigurationModule from '../../../main/frontend/LegacyConfigurationModule';

describe('navigationContainerSpec', function () {
  var $scope,
    $rootScope,
    $ngRedux,
    unsubscribeSpy,
    mockSystemConfigurationPropertyService,
    mockCurrentUser,
    mockProductFeatures,
    loginDeferred,
    productFeaturesDeferred,
    vm,
    clmServerVersion;

  beforeEach(
    angular.mock.module(navigationContainerModule.name, legacyConfigurationModule.name, function ($provide) {
      unsubscribeSpy = SpecUtil.mockNgRedux($provide);
    })
  );

  beforeEach(inject(function (_$rootScope_, $q, $componentController, _$ngRedux_) {
    clmServerVersion = window.clmServerVersion;
    $scope = _$rootScope_.$new();
    $rootScope = _$rootScope_;
    $ngRedux = _$ngRedux_;
    loginDeferred = $q.defer();
    productFeaturesDeferred = $q.defer();

    mockCurrentUser = {
      fetch: jasmine.createSpy('fetch'),
      waitForLogin: jasmine.createSpy('waitForLogin').and.returnValue(loginDeferred.promise),
    };

    mockProductFeatures = jasmine.createSpyObj('mockProductFeatures', ['isAvailable', 'load']);
    mockProductFeatures.load.and.returnValue(productFeaturesDeferred.promise);
    mockProductFeatures.isAvailable.and.callFake(function () {
      return false;
    });

    $ngRedux.dispatch = jasmine.createSpy('dispatch');

    vm = $componentController('navigationContainer', {
      systemConfigurationPropertyService: mockSystemConfigurationPropertyService,
      CurrentUser: mockCurrentUser,
      ProductFeatures: mockProductFeatures,
      $scope: $scope,
    });
  }));

  afterEach(function () {
    window.clmServerVersion = clmServerVersion;
    $scope.$destroy();
  });

  it('properly loads on supported firewall', function () {
    mockProductFeatures.isAvailable.and.callFake(function (feature) {
      return feature === 'firewall-auto-unquarantine' || feature === 'release-integrity';
    });
    vm.$onInit();
    loginDeferred.resolve();
    productFeaturesDeferred.resolve();
    $scope.$digest();

    expect(vm.isFirewallSupported).toBe(true);
  });

  it('properly loads on not supported firewall', function () {
    vm.$onInit();
    loginDeferred.resolve();
    productFeaturesDeferred.resolve();
    $scope.$digest();

    expect(vm.isFirewallSupported).toBe(false);
  });

  it('properly loads on supported advanced legal pack', function () {
    mockProductFeatures.isAvailable.and.callFake(function (feature) {
      return feature === 'advanced-legal-pack';
    });
    vm.$onInit();
    loginDeferred.resolve();
    productFeaturesDeferred.resolve();
    $scope.$digest();

    expect(vm.isAdvancedLegalPackSupported).toBe(true);
  });

  it('properly loads on not supported advanced legal pack', function () {
    vm.$onInit();
    loginDeferred.resolve();
    productFeaturesDeferred.resolve();
    $scope.$digest();

    expect(vm.isAdvancedLegalPackSupported).toBe(false);
  });

  describe('mapStateToThis', function () {
    let mapStateToThis;

    beforeEach(function () {
      vm.$onInit();
      loginDeferred.resolve();
      $scope.$digest();
      mapStateToThis = $ngRedux.connect.calls.first().args[0];
    });

    it('returns an object with isAdvancedSearchEnabled set to false given a state with no server data', function () {
      let mockStateNoServerData = {
        advancedSearchConfig: {
          serverData: null,
        },
      };

      expect(mapStateToThis(mockStateNoServerData).isAdvancedSearchEnabled).toBeFalsy();
    });

    it('returns an object with isAdvancedSearchEnabled set to true given a state with server data and isEnabled true', function () {
      let mockStateWithServerDataAndIsEnabledTrue = {
        advancedSearchConfig: {
          serverData: {
            isEnabled: true,
          },
        },
      };

      expect(mapStateToThis(mockStateWithServerDataAndIsEnabledTrue).isAdvancedSearchEnabled).toBe(true);
    });

    it('returns an object with isAdvancedSearchEnabled set to false given a state with server data and isEnabled false', function () {
      let mockStateWithServerDataAndIsEnabledFalse = {
        advancedSearchConfig: {
          serverData: {
            isEnabled: false,
          },
        },
      };

      expect(mapStateToThis(mockStateWithServerDataAndIsEnabledFalse).isAdvancedSearchEnabled).toBe(false);
    });

    it('returns an object with isSuccessMetricsEnabled set to false given a state with no server data', function () {
      let mockStateNoServerData = {
        successMetricsConfiguration: {
          serverData: null,
        },
      };

      expect(mapStateToThis(mockStateNoServerData).isSuccessMetricsEnabled).toBeFalsy();
    });

    it('returns an object with isSuccessMetricsEnabled set to true given a state with server data and enabled true', function () {
      let mockStateWithServerDataAndIsEnabledTrue = {
        successMetricsConfiguration: {
          serverData: {
            enabled: true,
          },
        },
      };

      expect(mapStateToThis(mockStateWithServerDataAndIsEnabledTrue).isSuccessMetricsEnabled).toBe(true);
    });

    it('returns an object with isSuccessMetricsEnabled set to false given a state with server data and enabled false', function () {
      let mockStateWithServerDataAndIsEnabledFalse = {
        successMetricsConfiguration: {
          serverData: {
            enabled: false,
          },
        },
      };

      expect(mapStateToThis(mockStateWithServerDataAndIsEnabledFalse).isSuccessMetricsEnabled).toBe(false);
    });
  });

  it('calls unsubscribe when the $scope is destroyed', function () {
    vm.$onInit();
    loginDeferred.resolve();
    $scope.$digest();

    expect(unsubscribeSpy).not.toHaveBeenCalled();
    $scope.$destroy();
    expect(unsubscribeSpy).toHaveBeenCalled();
  });

  it('properly determines the displayed release version number', function () {
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

  describe('isLicensed()', function () {
    it('Not Loaded', function () {
      expect(vm.isLicensed()).toBeFalsy();
    });
    it('Licensed', function () {
      $rootScope.licensed = true;
      vm.$onInit();
      expect(vm.isLicensed()).toBeTruthy();
    });
    it('Not Licensed', function () {
      vm.$onInit();
      expect(vm.isLicensed()).toBeFalsy();
    });
  });
});
