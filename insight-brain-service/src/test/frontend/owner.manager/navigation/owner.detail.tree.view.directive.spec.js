describe('owner.detail.tree.view.directive.spec.js', function() {
  beforeEach(module('owner.manager.module', function($provide) {
    $provide.value('$cookies', {
      get: angular.noop
    });
  }));

  function createTests(type, storeName, owner) {
    var vm,
        $scope,
        $timeout,
        $httpBackend,
        CLMAppLocations,
        mockOwnerStore = StoreUtils().createMockStore(storeName);

    beforeEach(inject(function($rootScope, $controller, _$timeout_, _$httpBackend_, _CLMAppLocations_) {
      $scope = $rootScope.$new();
      $timeout = _$timeout_;
      $httpBackend = _$httpBackend_;
      CLMAppLocations = _CLMAppLocations_;

      spyOn(CLMAppLocations, 'isApplication').andReturn(type === 'application');
      spyOn(CLMAppLocations, 'getEntityId').andReturn(owner[type === 'application' ? 'publicId' : 'id']);

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
      mockOwnerStore.resolveGet([owner]);
      $httpBackend.expectGET(CLMAppLocations.getOwnerDetailsUrl()).respond(SidebarResourceMockData.getOwnerDetailsUrl());
      $httpBackend.flush();
      $timeout.flush();

      expect(vm.ownerName).toBe(owner.name);
      expect(vm.details).toEqual(SidebarResourceMockData.getOwnerDetailsUrl());
      expect(vm.error).toBeUndefined();
    });

    it('Properly Detecting Details Loading Error', function() {
      mockOwnerStore.resolveGet([owner]);
      $httpBackend.expectGET(CLMAppLocations.getOwnerDetailsUrl()).respond(400, 'Bad Request');
      $httpBackend.flush();
      $timeout.flush();

      expect(vm.details).toBeUndefined();
      expect(vm.error).toBeDefined();
    });

    it('Properly Displaying Owner Name Loading Error', function() {
      mockOwnerStore.resolveGet([{}, {}]);
      $httpBackend.expectGET(CLMAppLocations.getOwnerDetailsUrl()).respond(SidebarResourceMockData.getOwnerDetailsUrl());
      $httpBackend.flush();
      $timeout.flush();

      expect(vm.ownerName).toBeUndefined();
      expect(vm.error).toBe('Could not find an ' + type + ' with ID ' +
          CLMAppLocations.getEntityId() + '.');
    });

    it('Properly Updating Data via broadcast', inject(function($rootScope) {
      mockOwnerStore.resolveGet([owner]);
      $httpBackend.expectGET(CLMAppLocations.getOwnerDetailsUrl()).respond(400, 'Bad Request');
      $httpBackend.flush();
      $timeout.flush();

      expect(vm.details).toBeUndefined();
      expect(vm.error).toBeDefined();

      $rootScope.$broadcast('resource.data.modified');
      mockOwnerStore.resolveRefresh([owner]);
      $httpBackend.expectGET(CLMAppLocations.getOwnerDetailsUrl()).respond(SidebarResourceMockData.getOwnerDetailsUrl());
      $httpBackend.flush();
      $timeout.flush();

      expect(vm.ownerName).toBe(owner.name);
      expect(vm.details).toEqual(SidebarResourceMockData.getOwnerDetailsUrl());
      expect(vm.error).toBeUndefined();
    }));
  }

  OwnerUtils.runTestsForOwnerTypes(createTests);
});
