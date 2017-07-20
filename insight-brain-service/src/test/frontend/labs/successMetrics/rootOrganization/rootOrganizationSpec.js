describe('rootOrganizationSpec', function() {

  beforeEach(function() {
    module('utility.services');
    module('successMetricsModule');
  });

  var vm,
      $scope,
      mockSystemConfigurationPropertyService = {
        checkSuccessMetricsEnabled: undefined
      },
      checkSuccessMetricsEnabledDeferred,
      resetPromise,
      mockSuccessMetricsDataService,
      getApplicationCountsDataDeferred;

  beforeEach(inject(function($q, _$rootScope_, $componentController) {
    $scope = _$rootScope_.$new();
    resetPromise = function() {
      checkSuccessMetricsEnabledDeferred = $q.defer();
      mockSystemConfigurationPropertyService.checkSuccessMetricsEnabled = jasmine.createSpy().and.returnValue(
          checkSuccessMetricsEnabledDeferred.promise);
    };
    resetPromise();
    getApplicationCountsDataDeferred = $q.defer();
    mockSuccessMetricsDataService = {
      getApplicationCountsData: jasmine.createSpy().and.returnValue(getApplicationCountsDataDeferred.promise),
      isRootOrgAvailable: jasmine.createSpy().and.returnValue(true)
    };
    vm = $componentController('rootOrganization', {
      systemConfigurationPropertyService: mockSystemConfigurationPropertyService,
      successMetricsDataService: mockSuccessMetricsDataService
    });
  }));

  afterEach(function() {
    $scope.$destroy();
  });

  it('properly loads on enabled success metrics', function() {
    checkSuccessMetricsEnabledDeferred.resolve(true);
    getApplicationCountsDataDeferred.resolve({activeApplications: 1});
    $scope.$digest();

    expect(vm.loaded).toBeTruthy();
    expect(vm.error).toBeUndefined();
    expect(vm.activeApplicationCount).toBe(1);
  });

  it('properly loads on disabled success metrics', function() {
    checkSuccessMetricsEnabledDeferred.reject('disabled');
    $scope.$digest();

    expect(vm.loaded).toBeTruthy();
    expect(vm.error).toBe('disabled');
  });

  it('resets error on load', function() {
    checkSuccessMetricsEnabledDeferred.reject('disabled');
    $scope.$digest();
    expect(vm.error).toBeDefined();

    resetPromise();
    vm.doLoad();
    checkSuccessMetricsEnabledDeferred.resolve(true);
    $scope.$digest();

    expect(vm.error).toBeUndefined();
  });
});
