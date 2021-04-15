/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';
import accessMockData from '../../stores/access/access.mock.data';

describe('role.mapping.service.spec', function () {
  var roleMappingService, loadedData, roleId;

  beforeEach(angular.mock.module(ownerManagerModule.name));

  beforeEach(inject([
    'role.mapping.service',
    'CLMContextLocations',
    '$httpBackend',
    function (RoleMappingService, CLMContextLocations, $httpBackend) {
      roleMappingService = RoleMappingService;

      $httpBackend.expectGET(CLMContextLocations.getRoleMappingUrl()).respond(accessMockData.getRoleMappings());
      roleMappingService.get().then(function (roleMappings) {
        loadedData = roleMappings;
        // Assertion in case test data is changed
        expect(loadedData.membersByRole[0].membersByOwner[0].members.length).toEqual(2);
        roleId = loadedData.membersByRole[0].roleId;
      });
      $httpBackend.flush();
    },
  ]));

  it('Set to empty', inject(function ($httpBackend, CLMContextLocations) {
    $httpBackend.expectPUT(CLMContextLocations.getRoleMappingUrl(roleId), []).respond(204);
    roleMappingService.put(roleId, []);
    $httpBackend.flush();

    expect(loadedData.membersByRole[0].membersByOwner[0].members).toEqual([]);
  }));

  it('Set with content', inject(function ($httpBackend, CLMContextLocations) {
    var member = {
      type: 'USER',
      internalName: 'userTest1',
      displayName: 'User Test1',
      email: 'userTest1@sonatype.com',
      realm: 'CLM',
    };
    $httpBackend.expectPUT(CLMContextLocations.getRoleMappingUrl(roleId), [member]).respond(204);
    roleMappingService.put(roleId, [member]);
    $httpBackend.flush();

    expect(loadedData.membersByRole[0].membersByOwner[0].members).toEqual([member]);
  }));
});
