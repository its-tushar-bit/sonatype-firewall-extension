import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';

describe('license.threat.group.tile.controller.js', function() {
  var vm,
      $httpBackend,
      CLMAppLocations,
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

  beforeEach(inject(function(_$rootScope_, $injector, $controller, _$httpBackend_, _CLMAppLocations_) {
    $rootScope = _$rootScope_;
    $httpBackend = _$httpBackend_;
    CLMAppLocations = _CLMAppLocations_;
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

    $httpBackend.expectGET(CLMAppLocations.getApplicableLicenseGroupsUrl()).respond(responseMock);
    $httpBackend.flush();

    $rootScope.$broadcast(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA);

    $httpBackend.expectGET(CLMAppLocations.getApplicableLicenseGroupsUrl()).respond(responseMock);
    $httpBackend.flush();
  });

  it('Updates Owner name on broadcasted updated owner event', function() {
    $httpBackend.expectGET(CLMAppLocations.getApplicableLicenseGroupsUrl()).respond(responseMock);
    $httpBackend.flush();

    expect(vm.ownerName).not.toEqual('Bob');

    $rootScope.$broadcast(EventNameConstant.OWNER_UPDATED, {name: 'Bob'});

    expect(vm.ownerName).toEqual('Bob');
  });
});
