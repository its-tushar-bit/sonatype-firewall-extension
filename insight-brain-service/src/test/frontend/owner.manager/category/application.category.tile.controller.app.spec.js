describe('application.category.tile.controller.app.spec.js', function() {
  beforeEach(module('owner.manager.module', function($provide) {
    $provide.value('$cookies', {
      get: angular.noop
    });
  }));

  function createTests(type, storeName, owner) {
    var vm,
        scope,
        $httpBackend,
        $timeout,
        isApp = type === 'application',
        CLMLocations,
        mockCLMAppLocations,
        mockApplicationStore = StoreUtils().createMockStore('ApplicationStore');

    beforeEach(inject(function($rootScope, $controller, _$httpBackend_, _$timeout_, _CLMLocations_) {
          scope = $rootScope.$new();
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

          vm = $controller('ApplicationCategoryTileControllerApp', {
            CLMAppLocations: mockCLMAppLocations,
            $scope: scope
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
        $httpBackend.expectGET(CLMLocations.getApplicableOrganizationTags(mockCLMAppLocations.getEntityId())).respond([]);
        $timeout.flush();
        $httpBackend.flush();

        expect(vm.ownerName).toEqual(owner.name);
        expect(vm.appliedCategories.length).toEqual(mockAppliedTags.length);
        expect(vm.areAnyCategoriesDefined).toBeFalsy();
        vm.appliedCategories.forEach(function(category, index) {
          expect(category).toEqual(mockAppliedTags[index]);
        });
      });

      it('Missing App Info', function() {
        mockApplicationStore.resolveGet([{}, {}]);
        $httpBackend.expectGET(CLMLocations.getApplicationTagUrl(mockCLMAppLocations.getEntityId())).respond(TagResourceMockData.getApplicationTagUrl());
        $httpBackend.expectGET(CLMLocations.getApplicableOrganizationTags(mockCLMAppLocations.getEntityId())).respond([]);
        $timeout.flush();
        $httpBackend.flush();

        expect(vm.error).toEqual('Could not find an application with ID ' + owner.publicId + '.');
      });

      it('Missing Categories', function() {
            mockApplicationStore.resolveGet([owner]);
            $httpBackend.expectGET(CLMLocations.getApplicationTagUrl(mockCLMAppLocations.getEntityId())).respond(400,
                'Bad Request');
            $httpBackend.expectGET(CLMLocations.getApplicableOrganizationTags(mockCLMAppLocations.getEntityId())).respond([]);
            $timeout.flush();
            $httpBackend.flush();

            expect(vm.error).toBeDefined();
          }
      );

      it('Reloads on broadcasted owner summary reload event', inject(function($rootScope, $injector) {
        var EventNameConstant = $injector.get('event.name.constant'),
            mockAppliedTags = TagResourceMockData.getApplicationTagUrl();

        mockApplicationStore.resolveGet([owner]);
        $httpBackend.expectGET(CLMLocations.getApplicationTagUrl(mockCLMAppLocations.getEntityId())).respond(mockAppliedTags);
        $httpBackend.expectGET(CLMLocations.getApplicableOrganizationTags(mockCLMAppLocations.getEntityId())).respond([]);
        $timeout.flush();
        $httpBackend.flush();

        $rootScope.$broadcast(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA);

        mockApplicationStore.resolveGet([owner]);
        $httpBackend.expectGET(CLMLocations.getApplicationTagUrl(mockCLMAppLocations.getEntityId())).respond(mockAppliedTags);
        $httpBackend.expectGET(CLMLocations.getApplicableOrganizationTags(mockCLMAppLocations.getEntityId())).respond([]);
        $timeout.flush();
        $httpBackend.flush();
      }));
    }
  }

  OwnerUtils.runTestsForOwnerTypes(createTests);
});
