/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';
import OwnerUtils from '../owner.utils';
import TagResourceMockData from '../mock.data/tag.resource.mock.data';

describe('application.category.editor.controller.spec.js', function() {
  beforeEach(angular.mock.module(ownerManagerModule.name, function($provide) {
    $provide.value('$cookies', {
      get: angular.noop
    });
  }));

  function createTests(type, owner) {
    var vm,
        scope,
        $httpBackend,
        $timeout,
        isApp = type === 'application',
        CLMLocations,
        mockCLMContextLocations,
        mockApplicationStore = StoreUtils().createMockStore('ApplicationStore');

    beforeEach(inject(function($rootScope, $controller, _$httpBackend_, _$timeout_, _CLMLocations_) {
      scope = $rootScope.$new();
      $httpBackend = _$httpBackend_;
      $timeout = _$timeout_;
      CLMLocations = _CLMLocations_;

      mockCLMContextLocations = {
        isApplication: function() {
          return isApp;
        },
        getEntityId: function() {
          return isApp ? owner.publicId : owner.id;
        }
      };

      vm = $controller('application.category.editor.controller', {
        CLMContextLocations: mockCLMContextLocations,
        $scope: scope
      });
    }));

    afterEach(function() {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    });

    if (isApp) {
      it('Properly loading org categories, applied categories and application', function() {
        var mockOrgCategories = TagResourceMockData.getApplicableOrganizationTags();
        var mockAppliedCategories = TagResourceMockData.getApplicationTagUrl();

        mockApplicationStore.resolveGet([owner]);
        $httpBackend.expectGET(CLMLocations.getApplicableOrganizationTags(mockCLMContextLocations.getEntityId()))
            .respond(mockOrgCategories);
        $httpBackend.expectGET(CLMLocations.getApplicationTagUrl(mockCLMContextLocations.getEntityId()))
            .respond(mockAppliedCategories);
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
        $httpBackend.expectGET(CLMLocations.getApplicableOrganizationTags(mockCLMContextLocations.getEntityId()))
            .respond(TagResourceMockData.getApplicableOrganizationTags());
        $httpBackend.expectGET(CLMLocations.getApplicationTagUrl(mockCLMContextLocations.getEntityId()))
            .respond(TagResourceMockData.getApplicationTagUrl());
        $timeout.flush();
        $httpBackend.flush();

        expect(vm.loadError).toEqual('Could not find an application with ID ' + owner.publicId + '.');
      });

      it('Missing organization categories', function() {
        mockApplicationStore.resolveGet([owner]);
        $httpBackend.expectGET(CLMLocations.getApplicableOrganizationTags(mockCLMContextLocations.getEntityId()))
            .respond(400, 'Bad Request');
        $httpBackend.expectGET(CLMLocations.getApplicationTagUrl(mockCLMContextLocations.getEntityId())).respond(
            TagResourceMockData.getApplicationTagUrl());
        $timeout.flush();
        $httpBackend.flush();

        expect(vm.loadError).toBeDefined();
      });

      it('Missing application categories', function() {
        mockApplicationStore.resolveGet([owner]);
        $httpBackend.expectGET(CLMLocations.getApplicableOrganizationTags(mockCLMContextLocations.getEntityId()))
            .respond(TagResourceMockData.getApplicableOrganizationTags());
        $httpBackend.expectGET(CLMLocations.getApplicationTagUrl(mockCLMContextLocations.getEntityId())).respond(400,
            'Bad Request');
        $timeout.flush();
        $httpBackend.flush();

        expect(vm.loadError).toBeDefined();
      });

      it('Save refreshes policy store', inject(function($q, PolicyHierarchyStore) {
        mockApplicationStore.resolveGet([owner]);
        $httpBackend.expectGET(CLMLocations.getApplicableOrganizationTags(mockCLMContextLocations.getEntityId()))
            .respond(TagResourceMockData.getApplicableOrganizationTags());
        $httpBackend.expectGET(CLMLocations.getApplicationTagUrl(mockCLMContextLocations.getEntityId())).respond(
            TagResourceMockData.getApplicationTagUrl());

        $timeout.flush();
        $httpBackend.flush();
        vm.categoryEditorMask = {wrap: SpecUtil.promiseWrapper($q)};
        vm.categoryEditor = {$setPristine: angular.noop};
        spyOn(PolicyHierarchyStore, 'refresh');

        vm.save();
        $httpBackend.expectPUT(CLMLocations.getApplicationTagUrl(owner.id)).respond();
        $httpBackend.flush();
        expect(PolicyHierarchyStore.refresh).toHaveBeenCalled();
      }));

      describe('Page Changes', function() {
        beforeEach(function() {
          mockApplicationStore.resolveGet([owner]);
          $httpBackend.expectGET(CLMLocations.getApplicableOrganizationTags(mockCLMContextLocations.getEntityId()))
              .respond(TagResourceMockData.getApplicableOrganizationTags());
          $httpBackend.expectGET(CLMLocations.getApplicationTagUrl(mockCLMContextLocations.getEntityId())).respond(
              TagResourceMockData.getApplicationTagUrl());

          $timeout.flush();
          $httpBackend.flush();
        });

        it('clean', function() {
          spyOn(vm, 'areCategoriesDirty').and.returnValue(false);

          SpecUtil.expectStateChangeNotPrevented(scope);
          expect(vm.areCategoriesDirty).toHaveBeenCalled();
        });

        it('dirty', function() {
          spyOn(vm, 'areCategoriesDirty').and.returnValue(true);

          SpecUtil.expectStateChangePrevented(scope);
          expect(vm.areCategoriesDirty).toHaveBeenCalled();
        });
      });
    }
  }

  OwnerUtils.runTestsForOwnerTypes(createTests);
});
