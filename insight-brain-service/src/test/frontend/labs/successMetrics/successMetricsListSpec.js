describe('successMetricsList component', function() {
  beforeEach(module('successMetricsModule'));

  var vm,
      $state,
      $scope,
      $q,
      mockSystemConfigurationPropertyService = {
        checkSuccessMetricsEnabled: undefined
      },
      mockSuccessMetricsDataService = {
        getSuccessMetricsForCurrentUser: undefined
      },
      checkSuccessMetricsEnabledDeferred,
      getSuccessMetricsForCurrentUserDeferred,
      resetCheckSuccessMetricsEnabledPromise,
      resetGetSuccessMetricsForCurrentUserPromise;

  beforeEach(inject(function(_$state_, _$q_, _$rootScope_, $componentController) {
    $scope = _$rootScope_.$new();
    $state = _$state_;
    $q = _$q_;

    resetCheckSuccessMetricsEnabledPromise = function() {
      checkSuccessMetricsEnabledDeferred = $q.defer();
      mockSystemConfigurationPropertyService.checkSuccessMetricsEnabled = jasmine.createSpy().and.returnValue(
          checkSuccessMetricsEnabledDeferred.promise);
    };

    resetGetSuccessMetricsForCurrentUserPromise = function() {
      getSuccessMetricsForCurrentUserDeferred = $q.defer();
      mockSuccessMetricsDataService.getSuccessMetricsForCurrentUser = jasmine.createSpy().and.returnValue(
          getSuccessMetricsForCurrentUserDeferred.promise);
    };

    resetCheckSuccessMetricsEnabledPromise();
    resetGetSuccessMetricsForCurrentUserPromise();

    vm = $componentController('successMetricsList', {
      systemConfigurationPropertyService: mockSystemConfigurationPropertyService,
      successMetricsDataService: mockSuccessMetricsDataService
    });
  }));

  afterEach(function() {
    $scope.$destroy();
  });

  describe('$onInit()', function() {
    it('properly loads on enabled success metrics', function() {
      vm.$onInit();
      checkSuccessMetricsEnabledDeferred.resolve(true);
      getSuccessMetricsForCurrentUserDeferred.resolve([]);
      $scope.$digest();

      expect(vm.loaded).toBeTruthy();
      expect(vm.error).toBeUndefined();
    });

    it('properly loads on disabled success metrics', function() {
      vm.$onInit();
      checkSuccessMetricsEnabledDeferred.reject('disabled');
      getSuccessMetricsForCurrentUserDeferred.resolve([]);
      $scope.$digest();

      expect(vm.loaded).toBeTruthy();
      expect(vm.error).toBe('disabled');
    });

    it('properly loads the successMetricsList', function() {
      const successMetricsList = [{
        name: 'Empty',
        scope: {}
      }];

      vm.$onInit();
      checkSuccessMetricsEnabledDeferred.resolve(true);
      getSuccessMetricsForCurrentUserDeferred.resolve(successMetricsList);
      $scope.$digest();

      expect(vm.successMetricsList).toBe(successMetricsList);
    });

    it('resets error on load', function() {
      vm.$onInit();
      checkSuccessMetricsEnabledDeferred.reject('disabled');
      getSuccessMetricsForCurrentUserDeferred.resolve([]);
      $scope.$digest();
      expect(vm.error).toBeDefined();

      resetCheckSuccessMetricsEnabledPromise();
      resetGetSuccessMetricsForCurrentUserPromise();
      vm.$onInit();
      checkSuccessMetricsEnabledDeferred.resolve(true);
      getSuccessMetricsForCurrentUserDeferred.resolve([]);
      $scope.$digest();

      expect(vm.error).toBeUndefined();
    });
  });

  describe('goToCharts()', function() {
    it('calls $state.go with the correct route and successMetricsId', function() {
      const id = '12345';

      spyOn($state, 'go');
      vm.$onInit();
      vm.goToCharts(id);
      expect($state.go).toHaveBeenCalledWith('labs.successMetricsChart', { successMetricsId: '12345' });
    });
  });

  describe('openAddSuccessMetricsModal', function() {
    it('opens a modal and then adds its result onto the end of the successMetricsList', inject(function(Modal) {
      const successMetricsList = {
            name: 'Empty',
            scope: {}
          },

          // NOTE: all we do with this object is check reference equality, so its contents don't matter
          modalResult = {},
          modalDeferred = $q.defer();

      spyOn(Modal, 'open').and.returnValue({ result: modalDeferred.promise });

      vm.$onInit();
      checkSuccessMetricsEnabledDeferred.resolve(true);
      getSuccessMetricsForCurrentUserDeferred.resolve([successMetricsList]);
      $scope.$digest();

      vm.openAddSuccessMetricsModal();

      expect(Modal.open).toHaveBeenCalled();

      modalDeferred.resolve(modalResult);
      $scope.$digest();

      expect(vm.successMetricsList.length).toBe(2);
      expect(vm.successMetricsList[0]).toBe(successMetricsList);
      expect(vm.successMetricsList[1]).toBe(modalResult);
    }));
  });
});
