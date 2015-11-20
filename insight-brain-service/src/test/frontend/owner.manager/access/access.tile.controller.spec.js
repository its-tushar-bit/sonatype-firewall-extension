describe('access.tile.controller.spec.js', function() {
  var vm,
      $httpBackend,
      CLMAppLocations;

  beforeEach(module('owner.manager.module', function($provide) {
    $provide.value('$cookies', {});
  }));

  beforeEach(inject(function($controller, _$httpBackend_, _CLMAppLocations_) {
        $httpBackend = _$httpBackend_;
        CLMAppLocations = _CLMAppLocations_;

        vm = $controller('AccessTileController');
      }
  ));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  it('Properly Loading Membership Mappings', function() {
    $httpBackend.expectGET(CLMAppLocations.getRoleMappingUrl()).respond(AccessMockData.getRoleMappings());
    $httpBackend.flush();

    expect(vm.ownerName).toEqual(AccessMockData.getRoleMappings().membersByRole[0].membersByOwner[0].ownerName);
    expect(vm.membersByRole.length).toEqual(AccessMockData.getRoleMappings().membersByRole.length);
    vm.membersByRole.forEach(function(role, roleIndex) {
      expect(role.roleName).toEqual(AccessMockData.getRoleMappings().membersByRole[roleIndex].roleName);
      role.membersByOwner.forEach(function(owner, ownerIndex) {
        expect(owner.members).toEqual(AccessMockData.getRoleMappings().membersByRole[roleIndex].membersByOwner[ownerIndex].members);
      });
    });
  });

  it('Missing Membership Mappings', function() {
    $httpBackend.expectGET(CLMAppLocations.getRoleMappingUrl()).respond(400, 'Bad Request');
    $httpBackend.flush();

    expect(vm.error).toBeDefined();

    vm.doLoad();
    $httpBackend.expectGET(CLMAppLocations.getRoleMappingUrl()).respond(AccessMockData.getRoleMappings());
    $httpBackend.flush();

    expect(vm.error).toBeUndefined();
  });
});
