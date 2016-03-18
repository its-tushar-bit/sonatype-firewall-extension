describe('policy.tile.controller.spec.js', function() {

  beforeEach(module('Policy'));

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
      stageTypeStoreDefer,
      EventNameConstant,
      MonitoredStageService,
      mockPolicyMonitoringStore = StoreUtils().createMockStore('PolicyMonitoringStore');

  beforeEach(inject(['monitored.stage.service', function(_MonitoredStageService_) {
        MonitoredStageService = _MonitoredStageService_;
      }]
  ));

  beforeEach(inject(function(_$rootScope_, $injector, $q, $controller, _$timeout_, _$httpBackend_, _CLMAppLocations_,
                             StageTypeStore)
      {
        $rootScope = _$rootScope_;
        scope = $rootScope.$new();
        $httpBackend = _$httpBackend_;
        $timeout = _$timeout_;
        CLMAppLocations = _CLMAppLocations_;
        EventNameConstant = $injector.get('event.name.constant');
        stageTypeStoreDefer = $q.defer();
        spyOn(stageTypeStoreDefer.promise, 'then').andCallThrough();
        spyOn(StageTypeStore, 'getActionStages').andReturn(stageTypeStoreDefer.promise);
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
    $httpBackend.expectGET(CLMAppLocations.getApplicablePolicies()).respond(PolicyTileMockData.getApplicablePolicies());
    resolveStageTypeStore(MockData.getDashboardStageData());
    spyOn(MonitoredStageService, 'getMonitoredStage').andReturn({ stageName: 'Develop', stageTypeId: 'develop' });
    mockPolicyMonitoringStore.resolveGetApplicable(PolicyTileMockData.getPolicyMonitoring());
    $httpBackend.flush();
    $timeout.flush();

    expect(vm.ownerName).toEqual(PolicyTileMockData.getApplicablePolicies().policiesByOwner[0].ownerName);
    expect(vm.policiesByOwner.length).toEqual(PolicyTileMockData.getApplicablePolicies().policiesByOwner.length);
    vm.policiesByOwner.forEach(function(owner, ownerIndex) {
      owner.policies.forEach(function(policy, policyIndex) {
        expect(policy.name).toEqual(PolicyTileMockData.getApplicablePolicies().policiesByOwner[ownerIndex].policies[policyIndex].name);
        expect(policy.threatLevel).toEqual(PolicyTileMockData.getApplicablePolicies().policiesByOwner[ownerIndex].policies[policyIndex].threatLevel);
        expect(policy.actions).toEqual(PolicyTileMockData.getApplicablePolicies().policiesByOwner[ownerIndex].policies[policyIndex].actions);
        expect(policy.enforcementAction).toBeDefined();
        expect(policy.enforcementAction['build']).toEqual(PolicyTileMockData.getApplicablePolicies().policiesByOwner[ownerIndex].policies[policyIndex].actions['build'][0].actionTypeId);
        expect(policy.enforcementAction['stage-release']).toEqual(PolicyTileMockData.getApplicablePolicies().policiesByOwner[ownerIndex].policies[policyIndex].actions['stage-release'][0].actionTypeId);
      });
    });
    expect(vm.monitoredStage.stageName).toBe('Develop');
  });

  it('Uses the placeholder value for monitored stage if one is not inherited', function() {
    spyOn(MonitoredStageService, 'getMonitoredStage').andReturn(undefined);
    spyOn(MonitoredStageService, 'createInheritOrNoMonitorOption').andReturn({stageName: 'Do not monitor'});
    $httpBackend.expectGET(CLMAppLocations.getApplicablePolicies()).respond(PolicyTileMockData.getApplicablePolicies());
    resolveStageTypeStore(MockData.getDashboardStageData());
    mockPolicyMonitoringStore.resolveGetApplicable(PolicyTileMockData.getPolicyMonitoring());
    $httpBackend.flush();
    $timeout.flush();

    expect(vm.monitoredStage.stageName).toBe('Do not monitor');
  });

  it('Missing Owner Policies', function() {
    resolveStageTypeStore(MockData.getDashboardStageData());
    $httpBackend.expectGET(CLMAppLocations.getApplicablePolicies()).respond(400, 'Bad Request');
    $httpBackend.flush();
    $timeout.flush();

    expect(vm.error).toBeDefined();

    vm.doLoad();
    $httpBackend.expectGET(CLMAppLocations.getApplicablePolicies()).respond(PolicyTileMockData.getApplicablePolicies());
    resolveStageTypeStore(MockData.getDashboardStageData());
    $httpBackend.flush();
    $timeout.flush();

    expect(vm.error).toBeUndefined();

  });

  it('Reloads on broadcasted owner summary reload event', function() {
    $httpBackend.expectGET(CLMAppLocations.getApplicablePolicies()).respond(PolicyTileMockData.getApplicablePolicies());
    resolveStageTypeStore(MockData.getDashboardStageData());
    $httpBackend.flush();
    $timeout.flush();

    $rootScope.$broadcast(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA);

    $httpBackend.expectGET(CLMAppLocations.getApplicablePolicies()).respond(PolicyTileMockData.getApplicablePolicies());
    resolveStageTypeStore(MockData.getDashboardStageData());
    $httpBackend.flush();
    $timeout.flush();
  });

  it('Updates Owner name on broadcasted updated owner event', function() {
    $httpBackend.expectGET(CLMAppLocations.getApplicablePolicies()).respond(PolicyTileMockData.getApplicablePolicies());
    resolveStageTypeStore(MockData.getDashboardStageData());
    $httpBackend.flush();
    $timeout.flush();

    expect(vm.ownerName).not.toEqual('Bob');

    $rootScope.$broadcast(EventNameConstant.OWNER_UPDATED, {name: 'Bob'});

    expect(vm.ownerName).toEqual('Bob');
  });

  function resolveStageTypeStore(value) {
    expect(stageTypeStoreDefer.promise.then).toHaveBeenCalled();
    stageTypeStoreDefer.resolve(value);
  }
});
