describe('mainHeaderSpec', function() {

  beforeEach(module('mainHeader', 'legacyConfiguration'));

  var $scope,
      CLMAppLocations,
      CLMLocations,
      $rootScope,
      mockSystemConfigurationPropertyService,
      isSuccessMetricsEnabledDeferred,
      vm;

  beforeEach(inject(function(_$rootScope_, $q, _CLMAppLocations_, _CLMLocations_, $componentController) {
    $scope = _$rootScope_.$new();
    CLMAppLocations = _CLMAppLocations_;
    CLMLocations = _CLMLocations_;
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
});
