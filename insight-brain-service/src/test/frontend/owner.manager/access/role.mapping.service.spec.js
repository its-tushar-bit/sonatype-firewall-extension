describe('role.mapping.service.spec', function() {
  'use strict';

  var roleMappingService, loadedData, roleId;

  beforeEach(module('owner.manager.module'));

  beforeEach(inject(['role.mapping.service', 'CLMAppLocations', '$httpBackend',
      function(RoleMappingService, CLMAppLocations, $httpBackend) {
        roleMappingService = RoleMappingService;

        $httpBackend.expectGET(CLMAppLocations.getRoleMappingUrl()).respond(AccessMockData.getRoleMappings());
        roleMappingService.get().then(function(roleMappings) {
          loadedData = roleMappings;
          expect(loadedData.membersByRole[0].membersByOwner[0].members.length).toEqual(2); // Assertion in case test
                                                                                            // data is changed
          roleId = loadedData.membersByRole[0].roleId;
        });
        $httpBackend.flush();
      }]));

  it('Set to empty', inject(function($httpBackend, CLMAppLocations) {
    $httpBackend.expectPUT(CLMAppLocations.getRoleMappingUrl(roleId), []).respond(204);
    roleMappingService.put(roleId, []);
    $httpBackend.flush();

    expect(loadedData.membersByRole[0].membersByOwner[0].members).toEqual([]);
  }));

  it('Set with content', inject(function($httpBackend, CLMAppLocations) {
    var member = {
      "type": "USER",
      "internalName": "userTest1",
      "displayName": "User Test1",
      "email": "userTest1@sonatype.com",
      "realm": "CLM"
    };
    $httpBackend.expectPUT(CLMAppLocations.getRoleMappingUrl(roleId), [member]).respond(204);
    roleMappingService.put(roleId, [member]);
    $httpBackend.flush();

    expect(loadedData.membersByRole[0].membersByOwner[0].members).toEqual([member]);
  }));
});
