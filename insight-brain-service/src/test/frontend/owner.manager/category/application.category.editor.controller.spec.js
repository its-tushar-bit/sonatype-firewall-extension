describe('application.category.editor.controller.spec.js', function() {
  beforeEach(module('owner.manager.module', function($provide) {
    $provide.value('$cookies', {});
  }));

  function createTests(type, owner) {
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

          vm = $controller('application.category.editor.controller', {
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
      it('Properly loading org categories, applied categories and application', function() {
        var mockOrgCategories = TagResourceMockData.getApplicableOrganizationTags();
        var mockAppliedCategories = TagResourceMockData.getApplicationTagUrl();

        mockApplicationStore.resolveGet([owner]);
        $httpBackend.expectGET(CLMLocations.getApplicableOrganizationTags(mockCLMAppLocations.getEntityId())).respond(mockOrgCategories);
        $httpBackend.expectGET(CLMLocations.getApplicationTagUrl(mockCLMAppLocations.getEntityId())).respond(mockAppliedCategories);
        $timeout.flush();
        $httpBackend.flush();

        expect(vm.ownerName).toEqual(owner.name);
        expect(vm.categories.length).toEqual(mockOrgCategories.length);

        var numAppliedCategories = 0;
        vm.categories.forEach(function(category, index) {
          expect(category.name).toEqual(mockOrgCategories[index].name);
          expect(category.id).toEqual(mockOrgCategories[index].id);
          expect(category.color).toEqual(mockOrgCategories[index].color);
          if (category.isApplied === true) {
            numAppliedCategories++;
          }
        });
        expect(numAppliedCategories).toEqual(mockAppliedCategories.length);
      });

      it('Missing app info', function() {
        mockApplicationStore.resolveGet([{}, {}]);
        $httpBackend.expectGET(CLMLocations.getApplicableOrganizationTags(mockCLMAppLocations.getEntityId())).respond(TagResourceMockData.getApplicableOrganizationTags());
        $httpBackend.expectGET(CLMLocations.getApplicationTagUrl(mockCLMAppLocations.getEntityId())).respond(TagResourceMockData.getApplicationTagUrl());
        $timeout.flush();
        $httpBackend.flush();

        expect(vm.loadError).toEqual('Could not find an application with ID ' + owner.publicId + '.');
      });

      it('Missing organization categories', function() {
            mockApplicationStore.resolveGet([owner]);
            $httpBackend.expectGET(CLMLocations.getApplicableOrganizationTags(mockCLMAppLocations.getEntityId())).respond(400,
                'Bad Request')
            $httpBackend.expectGET(CLMLocations.getApplicationTagUrl(mockCLMAppLocations.getEntityId())).respond(TagResourceMockData.getApplicationTagUrl());
            $timeout.flush();
            $httpBackend.flush();

            expect(vm.loadError).toBeDefined();
          }
      );

      it('Missing application categories', function() {
            mockApplicationStore.resolveGet([owner]);
            $httpBackend.expectGET(CLMLocations.getApplicableOrganizationTags(mockCLMAppLocations.getEntityId())).respond(TagResourceMockData.getApplicableOrganizationTags());
            $httpBackend.expectGET(CLMLocations.getApplicationTagUrl(mockCLMAppLocations.getEntityId())).respond(400,
                'Bad Request');
            $timeout.flush();
            $httpBackend.flush();

            expect(vm.loadError).toBeDefined();
          }
      );
    }
  }

  OwnerUtils.runTestsForOwnerTypes(createTests);
});
