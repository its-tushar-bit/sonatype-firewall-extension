/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';
import OwnerUtils from '../owner.utils';
import TagResourceMockData from '../mock.data/tag.resource.mock.data';

describe('application.category.tile.controller.org.spec.js', function() {
  var $state;

  beforeEach(angular.mock.module(ownerManagerModule.name, function($provide) {
    $provide.value('$cookies', {
      get: angular.noop
    });

    $state = {
      current: {
        name: ''
      },
      params: {}
    };
    $provide.value('$state', $state);
    $provide.value('$stateParams', $state.params);
  }));

  function createTests(type, storeName, owner) {
    var vm,
        scope,
        $httpBackend,
        $rootScope,
        EventNameConstant,
        isOrg = type === 'organization';

    beforeEach(inject(function(_$rootScope_, $controller, $injector, _$httpBackend_) {
      $rootScope = _$rootScope_;
      scope = $rootScope.$new();
      $httpBackend = _$httpBackend_;
      EventNameConstant = $injector.get('event.name.constant');

      $state.current.name = type;
      if (type === 'application') {
        $state.params.applicationPublicId = owner.publicId;
      }
      else if (type === 'organization') {
        $state.params.organizationId = owner.id;
      }

      vm = $controller('ApplicationCategoryTileControllerOrg', {
        $scope: scope
      });
    }));

    afterEach(function() {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    });

    if (isOrg) {
      it('Properly Loading Applicable Categories and Org Name', inject(function(CLMContextLocations) {
        var mockAppCategoryOwners = TagResourceMockData.getApplicationCategoriesUrl();

        $httpBackend.expectGET(CLMContextLocations.getApplicationCategoriesUrl()).respond(mockAppCategoryOwners);
        $httpBackend.flush();

        expect(vm.ownerName).toEqual(mockAppCategoryOwners.applicationCategoriesByOwner[0].ownerName);
        expect(vm.appCategoryOwners.length).toEqual(mockAppCategoryOwners.applicationCategoriesByOwner.length);
        vm.appCategoryOwners.forEach(function(owner, index) {
          expect(angular.equals(owner.applicationCategories,
              mockAppCategoryOwners.applicationCategoriesByOwner[index].applicationCategories)).toBeTruthy();
        });
      }));

      it('Missing Categories', inject(function(CLMContextLocations) {
        $httpBackend.expectGET(CLMContextLocations.getApplicationCategoriesUrl()).respond(400, 'Bad Request');
        $httpBackend.flush();

        expect(vm.error).toBeDefined();
        expect(vm.ownerName).toBeUndefined();
        expect(vm.appCategoryOwners).toEqual([]);
      }));

      it('Reloads on broadcasted owner summary reload event', inject(function(CLMContextLocations) {
        $httpBackend.expectGET(CLMContextLocations.getApplicationCategoriesUrl()).respond(
            TagResourceMockData.getApplicationCategoriesUrl());
        $httpBackend.flush();

        $rootScope.$broadcast(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA);

        $httpBackend.expectGET(CLMContextLocations.getApplicationCategoriesUrl()).respond(
            TagResourceMockData.getApplicationCategoriesUrl());
        $httpBackend.flush();
      }));

      it('Updates Owner name on broadcasted updated owner event', inject(function(CLMContextLocations) {
        $httpBackend.expectGET(CLMContextLocations.getApplicationCategoriesUrl()).respond(
            TagResourceMockData.getApplicationCategoriesUrl());
        $httpBackend.flush();

        expect(vm.ownerName).not.toEqual('Bob');

        $rootScope.$broadcast(EventNameConstant.OWNER_UPDATED, {name: 'Bob'});

        expect(vm.ownerName).toEqual('Bob');
      }));
    }
  }

  OwnerUtils.runTestsForOwnerTypes(createTests);
});
