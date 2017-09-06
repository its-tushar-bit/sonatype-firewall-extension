/* global PolicyViolationAggregationResourceMockData */
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
      getApplicationCountsDataDeferred,
      getMttrDataDeferred,
      getAveragesDataDeferred,
      getComponentCountsDataDeferred,
      applicationCountsData = {
        totalApplications: 5,
        activeApplications: 4,
        total: {
          applicationsWithViolations: 3,
          applicationsWithCriticalViolations: 2
        },
        security: {
          applicationsWithViolations: 2,
          applicationsWithCriticalViolations: 2
        },
        license: {
          applicationsWithViolations: 1,
          applicationsWithCriticalViolations: 1
        },
        quality: {
          applicationsWithViolations: 1,
          applicationsWithCriticalViolations: 0
        },
        other: {
          applicationsWithViolations: 0,
          applicationsWithCriticalViolations: 0
        }
      },
      mttrData = [
        {timePeriodStart: 1483254000000, mttrInSeconds: null, criticalMttrInSeconds: null},
        {timePeriodStart: 1485932400000, mttrInSeconds: 1209714, criticalMttrInSeconds: 1209714},
        {timePeriodStart: 1488351600000, mttrInSeconds: 484000, criticalMttrInSeconds: 484000}
      ],
      averagesData = {
        activeApplicationCount: 3,
        totalEvaluations: 5,
        averagePolicyViolations: 5,
        averageCriticalPolicyViolations: 5,
        monthCount: 1,
        averageDiscoveredPolicyViolations: [
          {
            evaluationCount: 5,
            security: {
              low: 5,
              moderate: 8,
              severe: 2,
              critical: 0
            },
            license: {
              low: 0,
              moderate: 2,
              severe: 3,
              critical: 7
            },
            quality: {
              low: 15,
              moderate: 0,
              severe: 1,
              critical: 0
            },
            other: {
              low: 2,
              moderate: 2,
              severe: 2,
              critical: 2
            }
          }
        ]
      },
      componentCountsData = PolicyViolationAggregationResourceMockData.getComponentCountsData();

  beforeEach(inject(function($q, _$rootScope_, $componentController) {
    $scope = _$rootScope_.$new();
    resetPromise = function() {
      checkSuccessMetricsEnabledDeferred = $q.defer();
      mockSystemConfigurationPropertyService.checkSuccessMetricsEnabled = jasmine.createSpy().and.returnValue(
          checkSuccessMetricsEnabledDeferred.promise);
    };
    resetPromise();
    getApplicationCountsDataDeferred = $q.defer();
    getMttrDataDeferred = $q.defer();
    getAveragesDataDeferred = $q.defer();
    getComponentCountsDataDeferred = $q.defer();
    mockSuccessMetricsDataService = {
      getApplicationCountsData: jasmine.createSpy().and.returnValue(getApplicationCountsDataDeferred.promise),
      getMttrData: jasmine.createSpy().and.returnValue(getMttrDataDeferred.promise),
      getAveragesData: jasmine.createSpy().and.returnValue(getAveragesDataDeferred.promise),
      getComponentCountsData: jasmine.createSpy().and.returnValue(getComponentCountsDataDeferred.promise),
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
    getApplicationCountsDataDeferred.resolve(applicationCountsData);
    getMttrDataDeferred.resolve(mttrData);
    getAveragesDataDeferred.resolve(averagesData);
    getComponentCountsDataDeferred.resolve(componentCountsData);
    $scope.$digest();

    expect(vm.loaded).toBeTruthy();
    expect(vm.error).toBeUndefined();
    expect(vm.activeApplicationCount).toBe(4);
    expect(vm.applicationCountsData).toBe(applicationCountsData);
    expect(vm.mttrData).toBe(mttrData);
    expect(vm.averagesData).toBe(averagesData);
    expect(vm.componentCountsData).toBe(componentCountsData);
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

  describe('isMttrDisabled', function() {
    it('returns true before mttrData is bound', function() {
      expect(vm.isMttrDisabled()).toBe(true);
    });

    it('returns true if mttrData is an empty list', function() {
      checkSuccessMetricsEnabledDeferred.resolve(true);
      getMttrDataDeferred.resolve([]);

      $scope.$digest();

      expect(vm.isMttrDisabled()).toBe(true);
    });

    it('returns false if mttrData is a non-empty list', function() {
      checkSuccessMetricsEnabledDeferred.resolve(true);
      getMttrDataDeferred.resolve(mttrData);
      getAveragesDataDeferred.resolve(averagesData);
      getApplicationCountsDataDeferred.resolve(applicationCountsData);
      getComponentCountsDataDeferred.resolve(componentCountsData);

      $scope.$digest();

      expect(vm.isMttrDisabled()).toBe(false);
    });
  });
});
