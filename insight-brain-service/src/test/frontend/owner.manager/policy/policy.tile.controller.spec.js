describe('policy.tile.controller.spec.js', function() {
  var vm,
      $httpBackend,
      $timeout,
      CLMAppLocations,
      stageTypeStoreDefer;

  beforeEach(module('owner.manager.module', function($provide) {
    $provide.value('$cookies', {});
  }));

  beforeEach(inject(function($q, $controller, _$timeout_, _$httpBackend_, _CLMAppLocations_, StageTypeStore) {
        $httpBackend = _$httpBackend_;
        $timeout = _$timeout_;
        CLMAppLocations = _CLMAppLocations_;
        stageTypeStoreDefer = $q.defer();
        spyOn(stageTypeStoreDefer.promise, 'then').andCallThrough();
        spyOn(StageTypeStore, 'getActionStages').andReturn(stageTypeStoreDefer.promise);
        vm = $controller('policy.tile.controller');
      }
  ));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  it('Properly Loading Owner Policies', function() {
    $httpBackend.expectGET(CLMAppLocations.getApplicablePolicies()).respond(PolicyTileMockData.getApplicablePolicies());
    resolveStageTypeStore(MockData.getDashboardStageData());
    $httpBackend.flush();
    $timeout.flush();
    
    expect(vm.ownerName).toEqual(PolicyTileMockData.getApplicablePolicies().policiesByOwner[0].ownerName);
    expect(vm.policiesByOwner.length).toEqual(PolicyTileMockData.getApplicablePolicies().policiesByOwner.length);
    vm.policiesByOwner.forEach(function(owner, ownerIndex) {
      owner.policies.forEach(function(policy, policyIndex) {
        expect(policy.name).toEqual(PolicyTileMockData.getApplicablePolicies().policiesByOwner[ownerIndex].policies[policyIndex].name);
        expect(policy.threatLevel).toEqual(PolicyTileMockData.getApplicablePolicies().policiesByOwner[ownerIndex].policies[policyIndex].threatLevel);
        expect(policy.actions).toEqual(PolicyTileMockData.getApplicablePolicies().policiesByOwner[ownerIndex].policies[policyIndex].actions);
      });
    });
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
  
  function resolveStageTypeStore(value) {
    expect(stageTypeStoreDefer.promise.then).toHaveBeenCalled();
    stageTypeStoreDefer.resolve(value);
  }
});
