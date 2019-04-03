import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';

describe('policy.tile.controller.spec.js', function() {

  beforeEach(angular.mock.module(ownerManagerModule.name, function($provide) {
    $provide.value('$cookies', {
      get: angular.noop
    });
  }));

  var vm,
      scope,
      $httpBackend,
      $timeout,
      $rootScope,
      $controller,
      mockPolicyHierarchyStore = StoreUtils().createMockStore('PolicyHierarchyStore'),
      mockPolicyStoreData = StoreUtils().createMockHierarchyStoreData(PolicyTileMockData
          .getApplicablePolicies(), 'policiesByOwner'),
      stageTypeStoreDefer,
      EventNameConstant,
      MonitoredStageService,
      mockPolicyMonitoringStore = StoreUtils().createMockStore('PolicyMonitoringStore'),
      CLMLocations,
      CLMContextLocations,
      mockProprietaryConfigurationHierarchyStore = StoreUtils().createMockStore('ProprietaryConfigHierarchyStore'),
      mockProprietaryConfigurationHierarchyStoreData = StoreUtils().createMockHierarchyStoreData(ProprietaryMockData
          .getProprietaryConfigurationStoreMockData(), 'proprietaryConfigByOwners'),
      getGrandfatheringDefer,
      mockPolicyViolationGrandfatheringService;

  beforeEach(inject(['monitored.stage.service', function(_MonitoredStageService_) {
    MonitoredStageService = _MonitoredStageService_;
  }]));

  beforeEach(inject(function(_$rootScope_, $injector, $q, _$controller_, _$timeout_, _$httpBackend_, StageTypeStore,
                             _CLMLocations_, _CLMContextLocations_) {
    $rootScope = _$rootScope_;
    scope = $rootScope.$new();
    $httpBackend = _$httpBackend_;
    $timeout = _$timeout_;
    $controller = _$controller_;
    CLMLocations = _CLMLocations_;
    CLMContextLocations = _CLMContextLocations_;
    EventNameConstant = $injector.get('event.name.constant');
    stageTypeStoreDefer = $q.defer();
    getGrandfatheringDefer = $q.defer();
    mockPolicyViolationGrandfatheringService = {
      getGrandfathering: jasmine.createSpy().and.returnValue(getGrandfatheringDefer.promise),
      getStatusMessage: JSON.stringify
    };
    spyOn(stageTypeStoreDefer.promise, 'then').and.callThrough();
    spyOn(StageTypeStore, 'getActionStages').and.returnValue(stageTypeStoreDefer.promise);
    $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond(['policy-monitoring']);
  }));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  it('Properly Loading Owner Policies', function() {
    createController();
    mockPolicyHierarchyStore.resolveGet(mockPolicyStoreData);
    resolveStageTypeStore(MockData.getDashboardStageData());
    spyOn(MonitoredStageService, 'getMonitoredStage').and.returnValue({ stageName: 'Develop', stageTypeId: 'develop' });
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
        expect(policy.enforcementAction['build'][0].actionTypeId)
            .toEqual(mockPolicyStoreData[ownerIndex].policies[policyIndex].actions['build'][0].actionTypeId);
        expect(policy.enforcementAction['stage-release'][0].actionTypeId)
            .toEqual(mockPolicyStoreData[ownerIndex].policies[policyIndex].actions['stage-release'][0].actionTypeId);
      });
    });
    expect(vm.monitoredStage.stageName).toBe('Develop');
    expect(vm.isMonitoringSupported).toBe(true);
  });

  it('Uses the placeholder value for monitored stage if one is not inherited', function() {
    createController();
    spyOn(MonitoredStageService, 'getMonitoredStage').and.returnValue(undefined);
    spyOn(MonitoredStageService, 'createInheritOrNoMonitorOption').and.returnValue({stageName: 'Do not monitor'});
    mockPolicyHierarchyStore.resolveGet(mockPolicyStoreData);
    resolveStageTypeStore(MockData.getDashboardStageData());
    mockPolicyMonitoringStore.resolveGetApplicable(PolicyTileMockData.getPolicyMonitoring());
    mockProprietaryConfigurationHierarchyStore.resolveGet(mockProprietaryConfigurationHierarchyStoreData);
    $timeout.flush();
    $httpBackend.flush();

    expect(vm.monitoredStage.stageName).toBe('Do not monitor');
  });

  it('Missing Owner Policies', function() {
    createController();
    mockPolicyHierarchyStore.rejectGet('dagnabbit');
    resolveStageTypeStore(MockData.getDashboardStageData());
    mockPolicyMonitoringStore.resolveGetApplicable(PolicyTileMockData.getPolicyMonitoring());
    mockProprietaryConfigurationHierarchyStore.resolveGet(mockProprietaryConfigurationHierarchyStoreData);

    $timeout.flush();
    $httpBackend.flush();

    expect(vm.error).toBe('dagnabbit');

    vm.doLoad();
    mockPolicyHierarchyStore.resolveGet(mockPolicyStoreData);
    resolveStageTypeStore(MockData.getDashboardStageData());
    mockPolicyMonitoringStore.resolveGetApplicable(PolicyTileMockData.getPolicyMonitoring());
    mockProprietaryConfigurationHierarchyStore.resolveGet(mockProprietaryConfigurationHierarchyStoreData);
    $timeout.flush();

    expect(vm.error).toBeUndefined();
  });

  it('Reloads on broadcasted owner summary reload event', function() {
    createController();
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
    createController();
    mockPolicyHierarchyStore.resolveGet(mockPolicyStoreData);
    resolveStageTypeStore(MockData.getDashboardStageData());
    $timeout.flush();
    $httpBackend.flush();

    expect(vm.ownerName).not.toEqual('Bob');

    $rootScope.$broadcast(EventNameConstant.OWNER_UPDATED, {name: 'Bob'});

    expect(vm.ownerName).toEqual('Bob');
  });

  it('Proprietary config counts properly updated', function() {
    createController();
    mockPolicyHierarchyStore.resolveGet(mockPolicyStoreData);
    resolveStageTypeStore(MockData.getDashboardStageData());
    mockPolicyMonitoringStore.resolveGetApplicable(PolicyTileMockData.getPolicyMonitoring());
    mockProprietaryConfigurationHierarchyStore.resolveGet(mockProprietaryConfigurationHierarchyStoreData);
    $timeout.flush();
    $httpBackend.flush();

    expect(vm.localProprietaryCount).toEqual(3);
    expect(vm.inheritedProprietaryCount).toEqual(1);
  });

  it('does not load the grandfathering configuration if not an application or organization', function() {
    spyOn(CLMContextLocations, 'isApplication').and.returnValue(false);
    spyOn(CLMContextLocations, 'isOrganization').and.returnValue(false);

    createController();
    mockPolicyHierarchyStore.resolveGet(mockPolicyStoreData);
    resolveStageTypeStore(MockData.getDashboardStageData());
    mockPolicyMonitoringStore.resolveGetApplicable(PolicyTileMockData.getPolicyMonitoring());
    mockProprietaryConfigurationHierarchyStore.resolveGet(mockProprietaryConfigurationHierarchyStoreData);
    getGrandfatheringDefer.resolve({});

    $httpBackend.flush();

    expect(mockPolicyViolationGrandfatheringService.getGrandfathering).not.toHaveBeenCalled();
    expect(vm.grandfatheringStatusMessage).toBe(undefined);
  });

  it('loads and displays the grandfathering configuration for applications', function() {
    const config = {
      enabled: true,
      calculatedEnabled: true,
      inheritedFromOrganizationName: null,
      allowChange: true,
      allowOverride: true
    };

    spyOn(CLMContextLocations, 'isApplication').and.returnValue(true);
    spyOn(CLMContextLocations, 'isOrganization').and.returnValue(false);

    createController();
    mockPolicyHierarchyStore.resolveGet(mockPolicyStoreData);
    resolveStageTypeStore(MockData.getDashboardStageData());
    mockPolicyMonitoringStore.resolveGetApplicable(PolicyTileMockData.getPolicyMonitoring());
    mockProprietaryConfigurationHierarchyStore.resolveGet(mockProprietaryConfigurationHierarchyStoreData);
    getGrandfatheringDefer.resolve(config);

    $httpBackend.flush();

    expect(mockPolicyViolationGrandfatheringService.getGrandfathering).toHaveBeenCalled();
    expect(vm.grandfatheringStatusMessage).toBe(JSON.stringify(config));
  });

  it('loads and displays the grandfathering configuration for organizations', function() {
    const config = {
      enabled: true,
      calculatedEnabled: true,
      inheritedFromOrganizationName: null,
      allowChange: true,
      allowOverride: true
    };

    spyOn(CLMContextLocations, 'isApplication').and.returnValue(false);
    spyOn(CLMContextLocations, 'isOrganization').and.returnValue(true);

    createController();
    mockPolicyHierarchyStore.resolveGet(mockPolicyStoreData);
    resolveStageTypeStore(MockData.getDashboardStageData());
    mockPolicyMonitoringStore.resolveGetApplicable(PolicyTileMockData.getPolicyMonitoring());
    mockProprietaryConfigurationHierarchyStore.resolveGet(mockProprietaryConfigurationHierarchyStoreData);
    getGrandfatheringDefer.resolve(config);

    $httpBackend.flush();

    expect(mockPolicyViolationGrandfatheringService.getGrandfathering).toHaveBeenCalled();
    expect(vm.grandfatheringStatusMessage).toBe(JSON.stringify(config));
  });

  function resolveStageTypeStore(value) {
    expect(stageTypeStoreDefer.promise.then).toHaveBeenCalled();
    stageTypeStoreDefer.resolve(value);
  }

  function createController() {
    vm = $controller('policy.tile.controller', {
      $scope: scope,
      policyViolationGrandfatheringService: mockPolicyViolationGrandfatheringService
    });
    vm.$onInit();
  }
});
