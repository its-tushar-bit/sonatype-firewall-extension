describe('license.threat.group.tile.controller.js', function() {
  var vm,
      $httpBackend,
      CLMAppLocations,
      $rootScope;

  var responseMock = {
    licenseThreatGroupsByOwner: [
      { ownerName: 'Foo'}
    ]
  };

  beforeEach(module('owner.manager.module', function($provide) {
    $provide.value('$cookies', {
      get: angular.noop
    });
  }));

  beforeEach(inject(function(_$rootScope_, $controller, _$httpBackend_, _CLMAppLocations_) {
    $rootScope = _$rootScope_;
    $httpBackend = _$httpBackend_;
    CLMAppLocations = _CLMAppLocations_;

    vm = $controller('LicenseThreatGroupTileController', {
      $scope: $rootScope.$new()
    });
  }));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  it('Reloads on broadcasted owner summary reload event', inject(function($rootScope, $injector) {
    var EventNameConstant = $injector.get('event.name.constant');

    $httpBackend.expectGET(CLMAppLocations.getApplicableLicenseGroupsUrl()).respond(responseMock);
    $httpBackend.flush();

    $rootScope.$broadcast(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA);

    $httpBackend.expectGET(CLMAppLocations.getApplicableLicenseGroupsUrl()).respond(responseMock);
    $httpBackend.flush();
  }));
});
