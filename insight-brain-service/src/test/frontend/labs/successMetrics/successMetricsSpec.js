describe('successMetrics component', function() {
  beforeEach(module('successMetricsModule'));

  var vm,
      $state,
      $scope,
      mockSystemConfigurationPropertyService = {
        checkSuccessMetricsEnabled: undefined
      },
      successMetricsDataService,
      checkSuccessMetricsEnabledDeferred,
      resetCheckSuccessMetricsEnabledPromise;

  beforeEach(inject(function(_$state_, $q, _$rootScope_, _successMetricsDataService_, $componentController) {
    $scope = _$rootScope_.$new();
    $state = _$state_;
    successMetricsDataService = _successMetricsDataService_;
    resetCheckSuccessMetricsEnabledPromise = function() {
      checkSuccessMetricsEnabledDeferred = $q.defer();
      mockSystemConfigurationPropertyService.checkSuccessMetricsEnabled = jasmine.createSpy().and.returnValue(
          checkSuccessMetricsEnabledDeferred.promise);
    };
    resetCheckSuccessMetricsEnabledPromise();

    vm = $componentController('successMetrics', {
      systemConfigurationPropertyService: mockSystemConfigurationPropertyService
    });
  }));

  afterEach(function() {
    $scope.$destroy();
  });


  describe('$onInit()', function() {
    it('properly loads on enabled success metrics', function() {
      vm.$onInit();
      checkSuccessMetricsEnabledDeferred.resolve(true);
      $scope.$digest();

      expect(vm.loaded).toBeTruthy();
      expect(vm.error).toBeUndefined();
    });

    it('properly loads on disabled success metrics', function() {
      vm.$onInit();
      checkSuccessMetricsEnabledDeferred.reject('disabled');
      $scope.$digest();

      expect(vm.loaded).toBeTruthy();
      expect(vm.error).toBe('disabled');
    });

    it('sets rootOrgAvailable', function() {
      spyOn(successMetricsDataService, 'isRootOrgAvailable').and.returnValue(true);
      expect(vm.rootOrgAvailable).toBeUndefined();
      vm.$onInit();
      expect(vm.rootOrgAvailable).toBe(true);
    });

    it('resets error on load', function() {
      vm.$onInit();
      checkSuccessMetricsEnabledDeferred.reject('disabled');
      $scope.$digest();
      expect(vm.error).toBeDefined();

      resetCheckSuccessMetricsEnabledPromise();
      vm.$onInit();
      checkSuccessMetricsEnabledDeferred.resolve(true);
      $scope.$digest();

      expect(vm.error).toBeUndefined();
    });
  });

  describe('goToRootOrgSuccessMetrics()', function() {
    it('does not call $state.go if root org is not available', function() {
      spyOn(successMetricsDataService, 'isRootOrgAvailable').and.returnValue(false);
      spyOn($state, 'go');
      vm.$onInit();
      vm.goToRootOrgSuccessMetrics();
      expect($state.go).not.toHaveBeenCalled();
    });

    it('calls $state.go if root org is available', function() {
      spyOn(successMetricsDataService, 'isRootOrgAvailable').and.returnValue(true);
      spyOn($state, 'go');
      vm.$onInit();
      vm.goToRootOrgSuccessMetrics();
      expect($state.go).toHaveBeenCalledWith('labs.rootOrg');
    });
  });
});
