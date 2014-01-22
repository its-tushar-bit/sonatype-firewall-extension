var clmBuildTimestamp = '';

describe('PolicyController tests', function() {
  var scope;

  beforeEach(module('Policy', function($provide) {
    $provide.value('ApplicationId', {
      encoded: function() {
        return 'bom1-12345678';
      }
    });
    $provide.value('OrganizationId', {
      encoded: function() {
        return null;
      }
    });
  }));

  afterEach(inject(function($httpBackend) {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
    scope.$destroy();
  }));

  // setup our http backend to return what we want
  beforeEach(inject(function($httpBackend, $rootScope, $controller, CLMLocations, CLMAppLocations, $state) {
    $state.current.name = "management.application";

    $httpBackend.expectGET(CLMLocations.getActionTypeUrl()).respond(PolicyMockData.getActionTypeData());
    $httpBackend.expectGET(CLMLocations.getActionStageUrl()).respond(MockData.getActionStageData());
    $httpBackend.expectGET(SpecUtil.toRegExp(CLMAppLocations.getPolicyUrl())).respond(PolicyMockData.getPolicyData());
    $httpBackend.expectGET(SpecUtil.toRegExp(CLMAppLocations.getApplicablePolicies())).respond(ApplicationMockData.getApplicablePolicies());
    $httpBackend.expectGET(CLMAppLocations.getApplicablePolicyMonitoring()).respond(PolicyMonitoringMockData);
    $httpBackend.whenGET(SpecUtil.toRegExp(CLMLocations.getConditionTypeUrl())).respond(PolicyMockData.getConditionTypeData());
    $httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getConditionValueTypeUrl())).respond(PolicyMockData.getConditionValueTypeData());
    // inject the controller
    scope = $rootScope.$new();

    $controller('PolicyController', {
      $scope: scope,
      global: {}
    });
    $httpBackend.flush();
  }));

  it('Test initial data state', function() {
    expect(scope.applicablePolicies[0].policies.length).toEqual(PolicyMockData.getPolicyData().length);
  });

  it('Test Summary', inject(function($compile, $httpBackend) {
    $httpBackend.expectGET('../policy-assets/components/policy/policy-items.html?').respond('');
    var sc = $compile('<div policy-items></div>')(scope).scope();
    $httpBackend.flush();
    expect(sc.$$childTail.getActionCount(scope.applicablePolicies[0].policies[0])).toEqual(1);
    expect(sc.$$childTail.getActionCount(scope.applicablePolicies[0].policies[1])).toEqual(1);
    expect(sc.$$childTail.getActionCount(scope.applicablePolicies[0].policies[2])).toEqual(0);
    expect(sc.$$childTail.getActionCount(scope.applicablePolicies[0].policies[3])).toEqual(0);
    expect(sc.$$childTail.getActionCount(scope.applicablePolicies[0].policies[4])).toEqual(0);
  }));

  it('Test remove policy', inject(function(CLMAppLocations, $httpBackend, Dialog) {
    expect(scope.applicablePolicies[0].policies.length).toEqual(5);
    spyOn(Dialog, 'open');
    scope.viewRemovePolicy(scope.applicablePolicies[0].policies[0]);

    expect(scope.applicablePolicies[0].policies[0].id).toEqual('053e89a476b34d7dac5d97665d2d241e');
    expect(Dialog.open).toHaveBeenCalledWith({
      title : 'Delete Policy',
      body : 'Are you sure you want to delete the Policy named "asdffffrfff"? This action is not reversible.',
      buttons : [{
        name : 'Cancel'
      }, {
        name : 'Delete',
        type : 'danger',
        click : jasmine.any(Function)
      }]
    });

    $httpBackend.expectDELETE(
            CLMAppLocations.getPolicyUrl() + '/' + scope.applicablePolicies[0].policies[0].id).respond(200);

    Dialog.open.mostRecentCall.args[0].buttons[1].click();

    $httpBackend.flush();

    expect(scope.applicablePolicies[0].policies.length).toEqual(4);
    expect(scope.applicablePolicies[0].policies[0].id).toEqual('ec21b3ee9f31447c9e40913d91776593');
  }));

  it('Editability', function() {
    expect(scope.applicablePolicies[0].editable).toEqual(true);
    expect(scope.applicablePolicies[1].editable).toEqual(false);
  });

  it('handles owner changes', inject(function($httpBackend, CLMAppLocations) {
    $httpBackend.expectGET(SpecUtil.toRegExp(CLMAppLocations.getPolicyUrl())).respond(PolicyMockData.getPolicyData());
    $httpBackend.expectGET(SpecUtil.toRegExp(CLMAppLocations.getApplicablePolicies())).respond(ApplicationMockData.getApplicablePolicies());
    $httpBackend.expectGET(CLMAppLocations.getApplicablePolicyMonitoring()).respond(PolicyMonitoringMockData);
    scope.$broadcast('ownerChanged', {
      ownerId : ApplicationMockData.getApplicablePolicies().policiesByOwner[0].ownerId,
      changes : [ { field : 'organizationId', newValue : 'new_org_id' } ]
    });

    $httpBackend.flush();

    scope.$broadcast('ownerChanged', {
      ownerId : ApplicationMockData.getApplicablePolicies().policiesByOwner[0].ownerId,
      changes : [ { field : 'name', newValue : 'NEW NAME' } ]
    });
    expect(scope.applicablePolicies[0].ownerName).toBe("NEW NAME");
  }));

  describe('Policy Monitoring', function() {

    it('Handles inherited Org policy monitoring properly', function(){
      expect(scope.policyMonitoringPlaceHolder).toBe("Develop (inherited from parent)")
    });

    it('Can save policy monitoring stage choice', inject(function($httpBackend, CLMAppLocations) {
      $httpBackend.expectPUT(CLMAppLocations.getPolicyMonitoringUrl()).respond(204);
      scope.savePolicyMonitoring();
      $httpBackend.flush();
      expect(scope.policyMonitoring.ownerId).toEqual(CLMAppLocations.getEntityId());
    }));

    it('Can delete policy monitoring stage choice', inject(function($httpBackend, CLMAppLocations) {
      scope.policyMonitoring.stageTypeId = null;
      $httpBackend.expectDELETE(CLMAppLocations.getPolicyMonitoringUrl()).respond(204);
      scope.savePolicyMonitoring();
      $httpBackend.flush();
    }));
  });

});
