describe('application.category.tile.controller.org.spec.js', function() {
  beforeEach(module('owner.manager.module', function($provide) {
    $provide.value('$cookies', {});
  }));

  function createTests(type, storeName, owner) {
    var vm,
        $httpBackend,
        isOrg = type === 'organization',
        mockCLMAppLocations;

    beforeEach(inject(function($controller, _$httpBackend_, CLMAppLocations) {
      $httpBackend = _$httpBackend_;

      mockCLMAppLocations = {
        isOrganization: function() {
          return isOrg;
        },
        getTagsUrl: CLMAppLocations.getTagsUrl
      };

      vm = $controller('ApplicationCategoryTileControllerOrg', {
        CLMAppLocations: mockCLMAppLocations
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
    }
  }

  OwnerUtils.runTestsForOwnerTypes(createTests);
});
