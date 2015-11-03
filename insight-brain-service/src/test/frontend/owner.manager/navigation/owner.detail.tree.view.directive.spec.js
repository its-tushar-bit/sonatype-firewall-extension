describe('owner.detail.tree.view.directive.spec.js', function() {
  var vm,
      $scope,
      $httpBackend,
      CLMAppLocations;

  beforeEach(module('owner.manager.module', function($provide) {
    $provide.value('$cookies', {});
  }));

  beforeEach(inject(function($rootScope, $controller, _$httpBackend_, _CLMAppLocations_) {
    $scope = $rootScope.$new();
    $httpBackend = _$httpBackend_;
    CLMAppLocations = _CLMAppLocations_;

    vm = $controller('OwnerDetailTreeViewController', {
      $scope: $scope,
      $state: {
        $current: {name: ""}
      }
    });
  }));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  it('Properly Loading Data', function() {
    $httpBackend.expectGET(CLMAppLocations.getOwnerDetailsUrl()).respond(SidebarResourceMockData.getOwnerDetailsUrl());
    $httpBackend.flush();

    expect(vm.details).toEqual(SidebarResourceMockData.getOwnerDetailsUrl());
    expect(vm.error).toBeUndefined();
  });

  it('Properly Displaying Error', function() {
    $httpBackend.expectGET(CLMAppLocations.getOwnerDetailsUrl()).respond(400, 'Bad Request');
    $httpBackend.flush();

    expect(vm.details).toBeUndefined();
    expect(vm.error).toBeDefined();
  });

  it('Properly Updating Data via broadcast', inject(function($rootScope) {
    $httpBackend.expectGET(CLMAppLocations.getOwnerDetailsUrl()).respond(400, 'Bad Request');
    $httpBackend.flush();

    expect(vm.details).toBeUndefined();
    expect(vm.error).toBeDefined();

    $rootScope.$broadcast('resource.data.modified');
    $httpBackend.expectGET(CLMAppLocations.getOwnerDetailsUrl()).respond(SidebarResourceMockData.getOwnerDetailsUrl());
    $httpBackend.flush();

    expect(vm.details).toEqual(SidebarResourceMockData.getOwnerDetailsUrl());
    expect(vm.error).toBeUndefined();
  }));
});
