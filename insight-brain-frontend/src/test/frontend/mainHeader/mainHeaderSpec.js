import mainHeaderModule from '../../../main/frontend/mainHeader/module';
import legacyConfigurationModule from '../../../main/frontend/LegacyConfigurationModule';

describe('mainHeaderSpec', function() {

  beforeEach(angular.mock.module(mainHeaderModule.name, legacyConfigurationModule.name));

  var $scope,
      $rootScope,
      mockSystemConfigurationPropertyService,
      isSuccessMetricsEnabledDeferred,
      vm,
      clmServerVersion;

  beforeEach(inject(function(_$rootScope_, $q, $componentController) {
    clmServerVersion = window.clmServerVersion;
    $scope = _$rootScope_.$new();
    $rootScope = _$rootScope_;
    isSuccessMetricsEnabledDeferred = $q.defer();
    mockSystemConfigurationPropertyService = {
      isSuccessMetricsEnabled: jasmine.createSpy().and.returnValue(isSuccessMetricsEnabledDeferred.promise)
    };

    vm = $componentController('mainHeader', {
      PermissionService: { getValidPermissions: jasmine.createSpy().and.returnValue($q.resolve())},
      systemConfigurationPropertyService: mockSystemConfigurationPropertyService
    });
  }));

  afterEach(function() {
    window.clmServerVersion = clmServerVersion;
    $scope.$destroy();
  });

  it('properly loads on enabled success metrics', function() {
    vm.$onInit();
    isSuccessMetricsEnabledDeferred.resolve(true);
    $scope.$digest();

    expect(vm.isSuccessMetricsEnabled).toBe(true);
  });

  it('properly loads on disabled success metrics', function() {
    vm.$onInit();
    isSuccessMetricsEnabledDeferred.reject('disabled');
    $scope.$digest();

    expect(vm.isSuccessMetricsEnabled).toBe(false);
  });

  it('resets isSuccessMetricsEnabled on successMetricsConfigurationUpdated event', function() {
    vm.$onInit();
    isSuccessMetricsEnabledDeferred.resolve(false);
    $scope.$digest();

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
});
