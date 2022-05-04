/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from 'MainRoot/owner.manager/owner.manager.module';
import accessMockData from 'TestRoot/stores/access/access.mock.data';

describe('access.tile.controller', function () {
  var vm, $rootScope, $httpBackend, CLMContextLocations, EventNameConstant;

  beforeEach(
    angular.mock.module(ownerManagerModule.name, function ($provide) {
      SpecUtil.mockNgRedux($provide);
      $provide.value('$cookies', {
        get: angular.noop,
      });
    })
  );

  beforeEach(inject(function (_$rootScope_, $controller, $injector, _$httpBackend_, _CLMContextLocations_) {
    $httpBackend = _$httpBackend_;
    CLMContextLocations = _CLMContextLocations_;
    EventNameConstant = $injector.get('event.name.constant');
    $rootScope = _$rootScope_;

    vm = $controller('AccessTileController', {
      $scope: $rootScope.$new(),
    });
  }));

  afterEach(function () {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  describe('on $destroy()', () => {
    it('unsubscribes from redux store', () => {
      expect(vm.unsubscribe).not.toHaveBeenCalled();
      $rootScope.$destroy();
      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });

  it('Properly Loading Membership Mappings', function () {
    $httpBackend.expectGET(CLMContextLocations.getRoleMappingUrl()).respond(accessMockData.getRoleMappings());
    $httpBackend.flush();

    expect(vm.membersByRole.length).toEqual(accessMockData.getRoleMappings().membersByRole.length);
    vm.membersByRole.forEach(function (role, roleIndex) {
      expect(role.roleName).toEqual(accessMockData.getRoleMappings().membersByRole[roleIndex].roleName);
      role.membersByOwner.forEach(function (owner, ownerIndex) {
        expect(owner.members).toEqual(
          accessMockData.getRoleMappings().membersByRole[roleIndex].membersByOwner[ownerIndex].members
        );
      });
    });
  });

  it('Missing Membership Mappings', function () {
    $httpBackend.expectGET(CLMContextLocations.getRoleMappingUrl()).respond(400, 'Bad Request');
    $httpBackend.flush();

    expect(vm.error).toBeDefined();

    vm.doLoad();
    $httpBackend.expectGET(CLMContextLocations.getRoleMappingUrl()).respond(accessMockData.getRoleMappings());
    $httpBackend.flush();

    expect(vm.error).toBeUndefined();
  });

  it('Reloads on broadcasted owner summary reload event', function () {
    $httpBackend.expectGET(CLMContextLocations.getRoleMappingUrl()).respond(accessMockData.getRoleMappings());
    expect($httpBackend.flush).not.toThrow();

    $rootScope.$broadcast(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA);

    $httpBackend.expectGET(CLMContextLocations.getRoleMappingUrl()).respond(accessMockData.getRoleMappings());
    expect($httpBackend.flush).not.toThrow();
  });
});
