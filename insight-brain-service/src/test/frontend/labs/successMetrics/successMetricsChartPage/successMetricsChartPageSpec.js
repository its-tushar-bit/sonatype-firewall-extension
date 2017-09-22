/* global PolicyViolationAggregationResourceMockData */
describe('successMetricsChartPageSpec', function() {

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
      mockStateParams = { successMetricsId: 'SuccessMetrics1' },
      mockState = jasmine.createSpyObj('state', ['go']),
      getApplicationCountsDataDeferred,
      getMttrDataDeferred,
      getAveragesDataDeferred,
      getComponentCountsDataDeferred,
      getSuccessMetricsForCurrentUserDeferred,
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
      componentCountsData = PolicyViolationAggregationResourceMockData.getComponentCountsData(),
      getSuccessMetricsForCurrentUserData = [{
        id: 'SuccessMetrics1',
        name: 'Success Metrics 1',
        scope: {
          organizationIds: ['1234', '5678'],
          applicationIds: ['asdf', 'qwerty']
        }
      }, {
        id: 'SuccessMetrics2',
        name: 'Success Metrics 2',
        scope: {
          organizationIds: null,
          applicationIds: null
        }
      }],
      applicationStoreDeferred;

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
    getSuccessMetricsForCurrentUserDeferred = $q.defer();
    applicationStoreDeferred = $q.defer();

    mockSuccessMetricsDataService = {
      getApplicationCountsData: jasmine.createSpy().and.returnValue(getApplicationCountsDataDeferred.promise),
      getMttrData: jasmine.createSpy().and.returnValue(getMttrDataDeferred.promise),
      getAveragesData: jasmine.createSpy().and.returnValue(getAveragesDataDeferred.promise),
      getComponentCountsData: jasmine.createSpy().and.returnValue(getComponentCountsDataDeferred.promise),
      getSuccessMetricsForCurrentUser:
          jasmine.createSpy().and.returnValue(getSuccessMetricsForCurrentUserDeferred.promise),
      getApplicationByInternalId: jasmine.createSpy().and.returnValue(applicationStoreDeferred.promise)
    };

    vm = $componentController('successMetricsChartPage', {
      systemConfigurationPropertyService: mockSystemConfigurationPropertyService,
      successMetricsDataService: mockSuccessMetricsDataService,
      $stateParams: mockStateParams,
      $state: mockState
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
    getSuccessMetricsForCurrentUserDeferred.resolve(getSuccessMetricsForCurrentUserData);
    $scope.$digest();

    expect(vm.loaded).toBeTruthy();
    expect(vm.error).toBeUndefined();
    expect(vm.activeApplicationCount).toBe(4);
    expect(vm.applicationCountsData).toBe(applicationCountsData);
    expect(vm.mttrData).toBe(mttrData);
    expect(vm.averagesData).toBe(averagesData);
    expect(vm.componentCountsData).toBe(componentCountsData);
  });

  it('uses the `successMetricsId` state param to find the right ids to send to the chart endpoints', function() {
    checkSuccessMetricsEnabledDeferred.resolve(true);
    getApplicationCountsDataDeferred.resolve(applicationCountsData);
    getMttrDataDeferred.resolve(mttrData);
    getAveragesDataDeferred.resolve(averagesData);
    getComponentCountsDataDeferred.resolve(componentCountsData);
    getSuccessMetricsForCurrentUserDeferred.resolve(getSuccessMetricsForCurrentUserData);
    $scope.$digest();

    expect(mockSuccessMetricsDataService.getApplicationCountsData)
        .toHaveBeenCalledWith(getSuccessMetricsForCurrentUserData[0].scope);
    expect(mockSuccessMetricsDataService.getMttrData)
        .toHaveBeenCalledWith(getSuccessMetricsForCurrentUserData[0].scope);
    expect(mockSuccessMetricsDataService.getAveragesData)
        .toHaveBeenCalledWith(getSuccessMetricsForCurrentUserData[0].scope);
    expect(mockSuccessMetricsDataService.getComponentCountsData)
        .toHaveBeenCalledWith(getSuccessMetricsForCurrentUserData[0].scope);
  });

  it('sets the successMetricsChartName from the matched SuccessMetrics', function() {
    checkSuccessMetricsEnabledDeferred.resolve(true);
    getApplicationCountsDataDeferred.resolve(applicationCountsData);
    getMttrDataDeferred.resolve(mttrData);
    getAveragesDataDeferred.resolve(averagesData);
    getComponentCountsDataDeferred.resolve(componentCountsData);
    getSuccessMetricsForCurrentUserDeferred.resolve(getSuccessMetricsForCurrentUserData);
    $scope.$digest();

    expect(vm.successMetrics.name).toBe('Success Metrics 1');
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

  it('properly loads when the successMetricsId could not be found', function() {
    checkSuccessMetricsEnabledDeferred.resolve(true);
    getApplicationCountsDataDeferred.resolve(applicationCountsData);
    getMttrDataDeferred.resolve(mttrData);
    getAveragesDataDeferred.resolve(averagesData);
    getComponentCountsDataDeferred.resolve(componentCountsData);
    getSuccessMetricsForCurrentUserDeferred.resolve(angular.copy(getSuccessMetricsForCurrentUserData).slice(1));
    $scope.$digest();

    expect(mockSuccessMetricsDataService.getApplicationCountsData).not.toHaveBeenCalled();
    expect(mockSuccessMetricsDataService.getMttrData).not.toHaveBeenCalled();
    expect(mockSuccessMetricsDataService.getAveragesData).not.toHaveBeenCalled();
    expect(mockSuccessMetricsDataService.getComponentCountsData).not.toHaveBeenCalled();
    expect(vm.error).toBe('Could not find Success Metrics with id SuccessMetrics1');
    expect(vm.loaded).toBe(true);
  });

  describe('isMttrDisabled', function() {
    it('returns true before mttrData is bound', function() {
      expect(vm.isMttrDisabled()).toBe(true);
    });

    it('returns true if mttrData is an empty list', function() {
      checkSuccessMetricsEnabledDeferred.resolve(true);
      getSuccessMetricsForCurrentUserDeferred.resolve(getSuccessMetricsForCurrentUserData);
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
      getSuccessMetricsForCurrentUserDeferred.resolve(getSuccessMetricsForCurrentUserData);

      $scope.$digest();

      expect(vm.isMttrDisabled()).toBe(false);
    });
  });

  describe('vm.goToList', function() {
    it('redirects to SuccessMetrics list page', function() {
      vm.goToList();

      expect(mockState.go).toHaveBeenCalledWith('labs.successMetrics');
    })
  })

  describe('single application', function() {

    it('sets singleApplicationName as undefined and isSingleApplicationReport is false before successMetrics ' +
        'is bound', function() {
      expect(vm.isSingleApplicationReport).toBeUndefined();
      expect(vm.singleApplicationName).toBeUndefined();
      expect(vm.successMetrics).toBeUndefined();
    });

    it('sets singleApplicationName as undefined and isSingleApplicationReport is false if applicationIds is null',
        function() {
          checkSuccessMetricsEnabledDeferred.resolve(true);
          getSuccessMetricsForCurrentUserDeferred.resolve(
              [{id: 'SuccessMetrics1', scope: {organizationIds: null, applicationIds: null}}]);
          getApplicationCountsDataDeferred.resolve({singleApplicationName: 'Test'});
          getMttrDataDeferred.resolve(mttrData);
          getAveragesDataDeferred.resolve(averagesData);
          getComponentCountsDataDeferred.resolve(componentCountsData);
          getSuccessMetricsForCurrentUserDeferred.resolve(getSuccessMetricsForCurrentUserData);

          $scope.$digest();

          expect(vm.isSingleApplicationReport).toBe(false);
          expect(vm.singleApplicationName).toBeUndefined();
        })

    it('sets singleApplicationName as undefined and isSingleApplicationReport is false if applicationIds is empty',
        function() {
          checkSuccessMetricsEnabledDeferred.resolve(true);
          getSuccessMetricsForCurrentUserDeferred.resolve(
              [{id: 'SuccessMetrics1', scope: {organizationIds: null, applicationIds: []}}]);
          getApplicationCountsDataDeferred.resolve({singleApplicationName: 'Test'});
          getMttrDataDeferred.resolve(mttrData);
          getAveragesDataDeferred.resolve(averagesData);
          getComponentCountsDataDeferred.resolve(componentCountsData);
          getSuccessMetricsForCurrentUserDeferred.resolve(getSuccessMetricsForCurrentUserData);

          $scope.$digest();

          expect(vm.isSingleApplicationReport).toBe(false);
          expect(vm.singleApplicationName).toBeUndefined();
        })

    it('sets singleApplicationName as undefined and isSingleApplicationReport is false if applicationIds array ' +
        'length is more than 1',
        function() {
          checkSuccessMetricsEnabledDeferred.resolve(true);
          getSuccessMetricsForCurrentUserDeferred.resolve(getSuccessMetricsForCurrentUserData);
          getApplicationCountsDataDeferred.resolve(applicationCountsData);
          getMttrDataDeferred.resolve(mttrData);
          getAveragesDataDeferred.resolve(averagesData);
          getComponentCountsDataDeferred.resolve(componentCountsData);
          getSuccessMetricsForCurrentUserDeferred.resolve(getSuccessMetricsForCurrentUserData);

          $scope.$digest();

          expect(vm.isSingleApplicationReport).toBe(false);
          expect(vm.singleApplicationName).toBeUndefined();
        });

    it('sets singleApplicationName and isSingleApplicationReport is true if applicationIds array length is 1',
        function() {
          checkSuccessMetricsEnabledDeferred.resolve(true);
          getSuccessMetricsForCurrentUserDeferred.resolve(
              [{id: 'SuccessMetrics1', scope: {organizationIds: null, applicationIds: ['app1']}}]);
          getApplicationCountsDataDeferred.resolve(applicationCountsData);
          getMttrDataDeferred.resolve(mttrData);
          getAveragesDataDeferred.resolve(averagesData);
          getComponentCountsDataDeferred.resolve(componentCountsData);
          getSuccessMetricsForCurrentUserDeferred.resolve(getSuccessMetricsForCurrentUserData);
          applicationStoreDeferred.resolve({id: 'app1', name: 'app 1'});

          $scope.$digest();

          expect(vm.isSingleApplicationReport).toBe(true);
          expect(vm.singleApplicationName).toBe('app 1');
        });

    it('sets singleApplicationName and isSingleApplicationReport is true if applicationIds array length is 1 and ' +
        'organizationIds array length is 1',
        function() {
          checkSuccessMetricsEnabledDeferred.resolve(true);
          getSuccessMetricsForCurrentUserDeferred.resolve(
              [{id: 'SuccessMetrics1', scope: {organizationIds: ['org1'], applicationIds: ['app1']}}]);
          getApplicationCountsDataDeferred.resolve(applicationCountsData);
          getMttrDataDeferred.resolve(mttrData);
          getAveragesDataDeferred.resolve(averagesData);
          getComponentCountsDataDeferred.resolve(componentCountsData);
          getSuccessMetricsForCurrentUserDeferred.resolve(getSuccessMetricsForCurrentUserData);
          applicationStoreDeferred.resolve({id: 'app1', name: 'app 1'});

          $scope.$digest();

          expect(vm.isSingleApplicationReport).toBe(true);
          expect(vm.singleApplicationName).toBe('app 1');
        });

    it('sets singleApplicationName as undefined and isSingleApplicationReport is false if applicationIds array ' +
         'length is 1 and organizationIds array length is more than 1',
        function() {
          checkSuccessMetricsEnabledDeferred.resolve(true);
          getSuccessMetricsForCurrentUserDeferred.resolve(
              [{id: 'SuccessMetrics1', scope: {organizationIds: ['org1', 'org2'], applicationIds: ['app1']}}]);
          getApplicationCountsDataDeferred.resolve(applicationCountsData);
          getMttrDataDeferred.resolve(mttrData);
          getAveragesDataDeferred.resolve(averagesData);
          getComponentCountsDataDeferred.resolve(componentCountsData);
          getSuccessMetricsForCurrentUserDeferred.resolve(getSuccessMetricsForCurrentUserData);

          $scope.$digest();

          expect(vm.isSingleApplicationReport).toBe(false);
          expect(vm.singleApplicationName).toBeUndefined();
        });

    it('sets error when singleApplicationName not found', function() {
      checkSuccessMetricsEnabledDeferred.resolve(true);
      getSuccessMetricsForCurrentUserDeferred.resolve(
          [{id: 'SuccessMetrics1', scope: {organizationIds: null, applicationIds: ['app1']}}]);
      getApplicationCountsDataDeferred.resolve(applicationCountsData);
      getMttrDataDeferred.resolve(mttrData);
      getAveragesDataDeferred.resolve(averagesData);
      getComponentCountsDataDeferred.resolve(componentCountsData);
      getSuccessMetricsForCurrentUserDeferred.resolve(getSuccessMetricsForCurrentUserData);
      applicationStoreDeferred.reject('Could not find Application with internal id app1');

      $scope.$digest();

      expect(vm.isSingleApplicationReport).toBe(true);
      expect(vm.singleApplicationName).toBeUndefined();
      expect(vm.error).toBe('Could not find Application with internal id app1');
    });

    it('does not call getApplicationByInternalId if activeApplicationCount is 0', function() {
      checkSuccessMetricsEnabledDeferred.resolve(true);
      getSuccessMetricsForCurrentUserDeferred.resolve(
          [{id: 'SuccessMetrics1', scope: {organizationIds: null, applicationIds: ['app1']}}]);
      getApplicationCountsDataDeferred.resolve({activeApplications: 0});
      getMttrDataDeferred.resolve(mttrData);
      getAveragesDataDeferred.resolve(averagesData);
      getComponentCountsDataDeferred.resolve(componentCountsData);
      getSuccessMetricsForCurrentUserDeferred.resolve(getSuccessMetricsForCurrentUserData);

      $scope.$digest();

      expect(vm.activeApplicationCount).toBe(0);
      expect(mockSuccessMetricsDataService.getApplicationByInternalId).not.toHaveBeenCalled();
      expect(vm.error).toBeUndefined();
      expect(vm.singleApplicationName).toBeUndefined();
      expect(vm.isSingleApplicationReport).toBe(true);
    });
  });
});
