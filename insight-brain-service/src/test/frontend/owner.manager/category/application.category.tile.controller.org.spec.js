describe('application.category.tile.controller.org.spec.js', function() {
  beforeEach(module('owner.manager.module', function($provide) {
    $provide.value('$cookies', {
      get: angular.noop
    });
  }));

  function createTests(type, storeName, owner) {
    var vm,
        scope,
        $httpBackend,
        $rootScope,
        EventNameConstant,
        isOrg = type === 'organization',
        mockCLMAppLocations;

    beforeEach(inject(function(_$rootScope_, $controller, $injector, _$httpBackend_, CLMAppLocations) {
      $rootScope = _$rootScope_;
      scope = $rootScope.$new();
      $httpBackend = _$httpBackend_;
      EventNameConstant = $injector.get('event.name.constant');

      mockCLMAppLocations = {
        isOrganization: function() {
          return isOrg;
        },
        getTagsUrl: CLMAppLocations.getTagsUrl
      };

      vm = $controller('ApplicationCategoryTileControllerOrg', {
        CLMAppLocations: mockCLMAppLocations,
        $scope: scope
      });
    }));

    afterEach(function() {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    });

    if (isOrg) {
      it('Properly Loading Applicable Categories and Org Name', function() {
        var mockAppCategoryOwners = TagResourceMockData.getTagsUrl();

        $httpBackend.expectGET(mockCLMAppLocations.getTagsUrl()).respond(mockAppCategoryOwners);
        $httpBackend.flush();

        expect(vm.ownerName).toEqual(mockAppCategoryOwners.tagsByOwner[0].ownerName);
        expect(vm.appCategoryOwners.length).toEqual(mockAppCategoryOwners.tagsByOwner.length);
        vm.appCategoryOwners.forEach(function(owner, index) {
          expect(owner.tags).toEqual(mockAppCategoryOwners.tagsByOwner[index].tags);
        });
      });

      it('Missing Categories', function() {
        $httpBackend.expectGET(mockCLMAppLocations.getTagsUrl()).respond(400, 'Bad Request');
        $httpBackend.flush();

        expect(vm.error).toBeDefined();
        expect(vm.ownerName).toBeUndefined();
        expect(vm.appCategoryOwners).toEqual([]);
      });

      it('Reloads on broadcasted owner summary reload event', function() {
        $httpBackend.expectGET(mockCLMAppLocations.getTagsUrl()).respond(TagResourceMockData.getTagsUrl());
        $httpBackend.flush();

        $rootScope.$broadcast(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA);

        $httpBackend.expectGET(mockCLMAppLocations.getTagsUrl()).respond(TagResourceMockData.getTagsUrl());
        $httpBackend.flush();
      });

      it('Updates Owner name on broadcasted updated owner event', function() {
        $httpBackend.expectGET(mockCLMAppLocations.getTagsUrl()).respond(TagResourceMockData.getTagsUrl());
        $httpBackend.flush();

        expect(vm.ownerName).not.toEqual('Bob');

        $rootScope.$broadcast(EventNameConstant.OWNER_UPDATED, {name: 'Bob'});

        expect(vm.ownerName).toEqual('Bob');
      });
    }
  }

  OwnerUtils.runTestsForOwnerTypes(createTests);
});
