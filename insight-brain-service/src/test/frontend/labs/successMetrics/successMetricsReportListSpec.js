describe('successMetricsReportList component', function() {
  beforeEach(module('successMetricsModule'));

  var vm,
      $state,
      $scope,
      $q,
      mockSystemConfigurationPropertyService = {
        checkSuccessMetricsEnabled: undefined
      },
      mockSuccessMetricsDataService = {
        getSuccessMetricsReportsForCurrentUser: undefined
      },
      checkSuccessMetricsEnabledDeferred,
      getSuccessMetricsReportsForCurrentUserDeferred,
      resetCheckSuccessMetricsEnabledPromise,
      resetGetSuccessMetricsReportsForCurrentUserPromise;

  beforeEach(inject(function(_$state_, _$q_, _$rootScope_, $componentController) {
    $scope = _$rootScope_.$new();
    $state = _$state_;
    $q = _$q_;

    resetCheckSuccessMetricsEnabledPromise = function() {
      checkSuccessMetricsEnabledDeferred = $q.defer();
      mockSystemConfigurationPropertyService.checkSuccessMetricsEnabled = jasmine.createSpy().and.returnValue(
          checkSuccessMetricsEnabledDeferred.promise);
    };

    resetGetSuccessMetricsReportsForCurrentUserPromise = function() {
      getSuccessMetricsReportsForCurrentUserDeferred = $q.defer();
      mockSuccessMetricsDataService.getSuccessMetricsReportsForCurrentUser = jasmine.createSpy().and.returnValue(
          getSuccessMetricsReportsForCurrentUserDeferred.promise);
    };

    resetCheckSuccessMetricsEnabledPromise();
    resetGetSuccessMetricsReportsForCurrentUserPromise();

    vm = $componentController('successMetricsReportList', {
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
      getSuccessMetricsReportsForCurrentUserDeferred.resolve([]);
      $scope.$digest();

      expect(vm.loaded).toBeTruthy();
      expect(vm.error).toBeUndefined();
    });

    it('properly loads on disabled success metrics', function() {
      vm.$onInit();
      checkSuccessMetricsEnabledDeferred.reject('disabled');
      getSuccessMetricsReportsForCurrentUserDeferred.resolve([]);
      $scope.$digest();

      expect(vm.loaded).toBeTruthy();
      expect(vm.error).toBe('disabled');
    });

    it('properly loads the successMetricsReports', function() {
      const successMetricsReports = [{
        name: 'Empty',
        scope: {}
      }];

      vm.$onInit();
      checkSuccessMetricsEnabledDeferred.resolve(true);
      getSuccessMetricsReportsForCurrentUserDeferred.resolve(successMetricsReports);
      $scope.$digest();

      expect(vm.successMetricsReports).toBe(successMetricsReports);
    });

    it('resets error on load', function() {
      vm.$onInit();
      checkSuccessMetricsEnabledDeferred.reject('disabled');
      getSuccessMetricsReportsForCurrentUserDeferred.resolve([]);
      $scope.$digest();
      expect(vm.error).toBeDefined();

      resetCheckSuccessMetricsEnabledPromise();
      resetGetSuccessMetricsReportsForCurrentUserPromise();
      vm.$onInit();
      checkSuccessMetricsEnabledDeferred.resolve(true);
      getSuccessMetricsReportsForCurrentUserDeferred.resolve([]);
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
      expect($state.go).toHaveBeenCalledWith('labs.successMetricsReport', { successMetricsReportId: '12345' });
    });
  });

  describe('openAddSuccessMetricsModal', function() {
    it('opens a modal and then adds its result onto the end of the successMetricsReports', inject(function(Modal) {
      const successMetricsReports = {
            name: 'Empty',
            scope: {}
          },

          // NOTE: all we do with this object is check reference equality, so its contents don't matter
          modalResult = {},
          modalDeferred = $q.defer();

      spyOn(Modal, 'open').and.returnValue({ result: modalDeferred.promise });

      vm.$onInit();
      checkSuccessMetricsEnabledDeferred.resolve(true);
      getSuccessMetricsReportsForCurrentUserDeferred.resolve([successMetricsReports]);
      $scope.$digest();

      vm.openAddSuccessMetricsReportModal();

      expect(Modal.open).toHaveBeenCalled();

      modalDeferred.resolve(modalResult);
      $scope.$digest();

      expect(vm.successMetricsReports.length).toBe(2);
      expect(vm.successMetricsReports[0]).toBe(successMetricsReports);
      expect(vm.successMetricsReports[1]).toBe(modalResult);
    }));
  });
});
