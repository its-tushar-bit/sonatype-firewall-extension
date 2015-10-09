describe('application.category.tile.controller.spec.js', function() {
  beforeEach(module('owner.manager.module', function($provide) {
    $provide.value('$cookies', {});
  }));

  function createTests(type, storeName, owner) {
    var vm,
        $httpBackend,
        $timeout,
        isApp = type === 'application',
        CLMLocations,
        mockCLMAppLocations,
        mockApplicationStore = StoreUtils().createMockStore('ApplicationStore');

    beforeEach(inject(function($controller, _$httpBackend_, _$timeout_, _CLMLocations_) {
          $httpBackend = _$httpBackend_;
          $timeout = _$timeout_;
          CLMLocations = _CLMLocations_;

          mockCLMAppLocations = {
            isApplication: function() {
              return isApp;
            },
            getEntityId: function() {
              return isApp ? owner.publicId : owner.id;
            }
          };

          vm = $controller('ApplicationCategoryTileController', {
            CLMAppLocations: mockCLMAppLocations
          });
        }
    ));

    afterEach(function() {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    });

    if (isApp) {
      it('Properly Loading Applied Categories and Application', function() {
        var mockAppliedTags = TagResourceMockData.getApplicationTagUrl();

        mockApplicationStore.resolveGet([owner]);
        $httpBackend.expectGET(CLMLocations.getApplicationTagUrl(mockCLMAppLocations.getEntityId())).respond(mockAppliedTags);
        $timeout.flush();
        $httpBackend.flush();

        expect(vm.ownerName).toEqual(owner.name);
        expect(vm.appliedCategories.length).toEqual(mockAppliedTags.length);
        vm.appliedCategories.forEach(function(category, index) {
          expect(category).toEqual(mockAppliedTags[index]);
        });
      });

      it('Missing App Info', function() {
        mockApplicationStore.resolveGet([{}, {}]);
        $httpBackend.expectGET(CLMLocations.getApplicationTagUrl(mockCLMAppLocations.getEntityId())).respond(TagResourceMockData.getApplicationTagUrl());
        $timeout.flush();
        $httpBackend.flush();

        expect(vm.error).toEqual('Could not find an application with ID ' + owner.publicId + '.');
      });

      it('Missing Categories', function() {
            mockApplicationStore.resolveGet([owner]);
            $httpBackend.expectGET(CLMLocations.getApplicationTagUrl(mockCLMAppLocations.getEntityId())).respond(400,
                'Bad Request');
            $timeout.flush();
            $httpBackend.flush();

            expect(vm.error).toBeDefined();
          }
      );
    }
  }

  OwnerUtils.runTestsForOwnerTypes(createTests);
});
