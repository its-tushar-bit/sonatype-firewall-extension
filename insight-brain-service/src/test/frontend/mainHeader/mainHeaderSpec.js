describe('mainHeaderSpec', function() {

  beforeEach(module('mainHeader', 'legacyConfiguration'));

  var $scope,
      $rootScope,
      mockSystemConfigurationPropertyService,
      isSuccessMetricsEnabledDeferred,
      vm;

  beforeEach(inject(function(_$rootScope_, $q, $componentController) {
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
