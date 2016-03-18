describe('access.tile.controller.spec.js', function() {
  var vm,
      $rootScope,
      $httpBackend,
      CLMAppLocations,
      EventNameConstant;

  beforeEach(module('owner.manager.module', function($provide) {
    $provide.value('$cookies', {
      get: angular.noop
    });
  }));

  beforeEach(inject(function(_$rootScope_, $controller, $injector, _$httpBackend_, _CLMAppLocations_) {
        $httpBackend = _$httpBackend_;
        CLMAppLocations = _CLMAppLocations_;
        EventNameConstant = $injector.get('event.name.constant');
        $rootScope = _$rootScope_;

        vm = $controller('AccessTileController', {
          $scope: $rootScope.$new()
        });
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

  it('Reloads on broadcasted owner summary reload event', function() {
    $httpBackend.expectGET(CLMAppLocations.getRoleMappingUrl()).respond(AccessMockData.getRoleMappings());
    $httpBackend.flush();

    $rootScope.$broadcast(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA);

    $httpBackend.expectGET(CLMAppLocations.getRoleMappingUrl()).respond(AccessMockData.getRoleMappings());
    $httpBackend.flush();
  });

  it('Updates Owner name on broadcasted updated owner event', function() {
    $httpBackend.expectGET(CLMAppLocations.getRoleMappingUrl()).respond(AccessMockData.getRoleMappings());
    $httpBackend.flush();

    expect(vm.ownerName).not.toEqual('Bob');

    $rootScope.$broadcast(EventNameConstant.OWNER_UPDATED, {name: 'Bob'});

    expect(vm.ownerName).toEqual('Bob');
  });
});
