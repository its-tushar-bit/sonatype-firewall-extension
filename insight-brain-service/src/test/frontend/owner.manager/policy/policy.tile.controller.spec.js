describe('policy.tile.controller.spec.js', function() {

  beforeEach(module('Policy'));

  beforeEach(module('ProductFeaturesModule'));

  beforeEach(module('owner.manager.module', function($provide) {
    $provide.value('$cookies', {
      get: angular.noop
    });
  }));

  var vm,
      scope,
      $httpBackend,
      $timeout,
      $rootScope,
      CLMAppLocations,
      mockPolicyHierarchyStore = StoreUtils().createMockStore('PolicyHierarchyStore'),
      mockPolicyStoreData = StoreUtils().createMockHierarchyStoreData(PolicyTileMockData
          .getApplicablePolicies(), 'policiesByOwner'),
      stageTypeStoreDefer,
      EventNameConstant,
      MonitoredStageService,
      mockPolicyMonitoringStore = StoreUtils().createMockStore('PolicyMonitoringStore'),
      CLMLocations,
      ProductFeatures,
      mockProprietaryConfigurationHierarchyStore = StoreUtils().createMockStore('ProprietaryConfigHierarchyStore'),
      mockProprietaryConfigurationHierarchyStoreData = StoreUtils().createMockHierarchyStoreData(ProprietaryMockData
          .getProprietaryConfigurationStoreMockData(), 'proprietaryConfigByOwners');

  beforeEach(inject(['monitored.stage.service', function(_MonitoredStageService_) {
        MonitoredStageService = _MonitoredStageService_;
      }]
  ));

  beforeEach(inject(function(_$rootScope_, $injector, $q, $controller, _$timeout_, _$httpBackend_, _CLMAppLocations_,
                             StageTypeStore, _ProductFeatures_, _CLMLocations_)
      {
        $rootScope = _$rootScope_;
        scope = $rootScope.$new();
        $httpBackend = _$httpBackend_;
        $timeout = _$timeout_;
        CLMAppLocations = _CLMAppLocations_;
        CLMLocations = _CLMLocations_;
        ProductFeatures = _ProductFeatures_;
        EventNameConstant = $injector.get('event.name.constant');
        stageTypeStoreDefer = $q.defer();
        spyOn(stageTypeStoreDefer.promise, 'then').andCallThrough();
        spyOn(StageTypeStore, 'getActionStages').andReturn(stageTypeStoreDefer.promise);
        $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond(['policy-monitoring']);
        vm = $controller('policy.tile.controller', {
          $scope: scope
        });
      }
  ));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  it('Properly Loading Owner Policies', function() {
    mockPolicyHierarchyStore.resolveGet(mockPolicyStoreData);
    resolveStageTypeStore(MockData.getDashboardStageData());
    spyOn(MonitoredStageService, 'getMonitoredStage').andReturn({ stageName: 'Develop', stageTypeId: 'develop' });
    mockPolicyMonitoringStore.resolveGetApplicable(PolicyTileMockData.getPolicyMonitoring());
    mockProprietaryConfigurationHierarchyStore.resolveGet(mockProprietaryConfigurationHierarchyStoreData);
    $timeout.flush();
    $httpBackend.flush();

    expect(vm.ownerName).toEqual(mockPolicyStoreData[0].ownerName);
    expect(vm.policiesByOwner.length).toEqual(mockPolicyStoreData.length);
    vm.policiesByOwner.forEach(function(owner, ownerIndex) {
      owner.policies.forEach(function(policy, policyIndex) {
        expect(policy.name).toEqual(mockPolicyStoreData[ownerIndex].policies[policyIndex].name);
        expect(policy.threatLevel).toEqual(mockPolicyStoreData[ownerIndex].policies[policyIndex].threatLevel);
        expect(policy.actions).toEqual(mockPolicyStoreData[ownerIndex].policies[policyIndex].actions);
        expect(policy.enforcementAction).toBeDefined();
        expect(policy.enforcementAction['build'][0].actionTypeId).toEqual(mockPolicyStoreData[ownerIndex].policies[policyIndex].actions['build'][0].actionTypeId);
        expect(policy.enforcementAction['stage-release'][0].actionTypeId).toEqual(mockPolicyStoreData[ownerIndex].policies[policyIndex].actions['stage-release'][0].actionTypeId);
      });
    });
    expect(vm.monitoredStage.stageName).toBe('Develop');
    expect(vm.isMonitoringSupported).toBe(true);
  });

  it('Uses the placeholder value for monitored stage if one is not inherited', function() {
    spyOn(MonitoredStageService, 'getMonitoredStage').andReturn(undefined);
    spyOn(MonitoredStageService, 'createInheritOrNoMonitorOption').andReturn({stageName: 'Do not monitor'});
    mockPolicyHierarchyStore.resolveGet(mockPolicyStoreData);
    resolveStageTypeStore(MockData.getDashboardStageData());
    mockPolicyMonitoringStore.resolveGetApplicable(PolicyTileMockData.getPolicyMonitoring());
    mockProprietaryConfigurationHierarchyStore.resolveGet(mockProprietaryConfigurationHierarchyStoreData);
    $timeout.flush();
    $httpBackend.flush();

    expect(vm.monitoredStage.stageName).toBe('Do not monitor');
  });

  it('Missing Owner Policies', function() {
    mockPolicyHierarchyStore.rejectGet("dagnabbit");
    resolveStageTypeStore(MockData.getDashboardStageData());
    mockPolicyMonitoringStore.resolveGetApplicable(PolicyTileMockData.getPolicyMonitoring());
    mockProprietaryConfigurationHierarchyStore.resolveGet(mockProprietaryConfigurationHierarchyStoreData);

    $timeout.flush();
    $httpBackend.flush();

    expect(vm.error).toBe("dagnabbit");

    vm.doLoad();
    mockPolicyHierarchyStore.resolveGet(mockPolicyStoreData);
    resolveStageTypeStore(MockData.getDashboardStageData());
    mockPolicyMonitoringStore.resolveGetApplicable(PolicyTileMockData.getPolicyMonitoring());
    mockProprietaryConfigurationHierarchyStore.resolveGet(mockProprietaryConfigurationHierarchyStoreData);
    $timeout.flush();

    expect(vm.error).toBeUndefined();
  });

  it('Reloads on broadcasted owner summary reload event', function() {
    mockPolicyHierarchyStore.resolveGet(mockPolicyStoreData);
    resolveStageTypeStore(MockData.getDashboardStageData());
    $timeout.flush();
    $httpBackend.flush();

    $rootScope.$broadcast(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA);

    mockPolicyHierarchyStore.resolveGet(mockPolicyStoreData);
    resolveStageTypeStore(MockData.getDashboardStageData());
    $timeout.flush();
  });

  it('Updates Owner name on broadcasted updated owner event', function() {
    mockPolicyHierarchyStore.resolveGet(mockPolicyStoreData);
    resolveStageTypeStore(MockData.getDashboardStageData());
    $timeout.flush();
    $httpBackend.flush();

    expect(vm.ownerName).not.toEqual('Bob');

    $rootScope.$broadcast(EventNameConstant.OWNER_UPDATED, {name: 'Bob'});

    expect(vm.ownerName).toEqual('Bob');
  });

  it('Proprietary config counts properly updated', function() {
    mockPolicyHierarchyStore.resolveGet(mockPolicyStoreData);
    resolveStageTypeStore(MockData.getDashboardStageData());
    mockPolicyMonitoringStore.resolveGetApplicable(PolicyTileMockData.getPolicyMonitoring());
    mockProprietaryConfigurationHierarchyStore.resolveGet(mockProprietaryConfigurationHierarchyStoreData);
    $timeout.flush();
    $httpBackend.flush();

    expect(vm.localProprietaryCount).toEqual(3);
    expect(vm.inheritedProprietaryCount).toEqual(1);
  });

  function resolveStageTypeStore(value) {
    expect(stageTypeStoreDefer.promise.then).toHaveBeenCalled();
    stageTypeStoreDefer.resolve(value);
  }
});
