/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';

describe('license.threat.group.tile.controller.js', function() {
  var vm,
      $httpBackend,
      CLMContextLocations,
      EventNameConstant,
      $rootScope;

  var responseMock = {
    licenseThreatGroupsByOwner: [
      { ownerName: 'Foo'}
    ]
  };

  beforeEach(angular.mock.module(ownerManagerModule.name, function($provide) {
    $provide.value('$cookies', {
      get: angular.noop
    });
  }));

  beforeEach(inject(function(_$rootScope_, $injector, $controller, _$httpBackend_, _CLMContextLocations_) {
    $rootScope = _$rootScope_;
    $httpBackend = _$httpBackend_;
    CLMContextLocations = _CLMContextLocations_;
    EventNameConstant = $injector.get('event.name.constant');

    vm = $controller('LicenseThreatGroupTileController', {
      $scope: $rootScope.$new()
    });
  }));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  it('Reloads on broadcasted owner summary reload event', function() {

    $httpBackend.expectGET(CLMContextLocations.getApplicableLicenseGroupsUrl()).respond(responseMock);
    expect($httpBackend.flush).not.toThrow();

    $rootScope.$broadcast(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA);

    $httpBackend.expectGET(CLMContextLocations.getApplicableLicenseGroupsUrl()).respond(responseMock);
    expect($httpBackend.flush).not.toThrow();
  });

  it('Updates Owner name on broadcasted updated owner event', function() {
    $httpBackend.expectGET(CLMContextLocations.getApplicableLicenseGroupsUrl()).respond(responseMock);
    $httpBackend.flush();

    expect(vm.ownerName).not.toEqual('Bob');

    $rootScope.$broadcast(EventNameConstant.OWNER_UPDATED, {name: 'Bob'});

    expect(vm.ownerName).toEqual('Bob');
  });
});
