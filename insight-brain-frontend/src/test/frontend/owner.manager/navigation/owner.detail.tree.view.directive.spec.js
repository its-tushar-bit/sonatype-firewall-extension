/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from 'MainRoot/owner.manager/owner.manager.module';
import OwnerUtils from '../owner.utils';
import { actions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { actions as applicationActions } from 'MainRoot/OrgsAndPolicies/applicationsSlice';
import { actions as applicationCategoriesActions } from 'MainRoot/OrgsAndPolicies/assignApplicationCategoriesSlice';

describe('owner.detail.tree.view.directive', function () {
  beforeEach(
    angular.mock.module(ownerManagerModule.name, function ($provide) {
      $provide.value('$cookies', {
        get: angular.noop,
      });
      SpecUtil.mockNgRedux($provide);
    })
  );

  function createTests(type, storeName, owner) {
    var vm,
      $scope,
      $timeout,
      $httpBackend,
      CLMContextLocations,
      loadApplicationsIfNeededMock,
      mockOwnerStore = storeName ? StoreUtils().createMockStore(storeName) : null;

    beforeEach(inject(function ($rootScope, $controller, _$timeout_, _$httpBackend_, _CLMContextLocations_) {
      $scope = $rootScope.$new();
      $timeout = _$timeout_;
      $httpBackend = _$httpBackend_;
      CLMContextLocations = _CLMContextLocations_;

      spyOn(CLMContextLocations, 'isApplication').and.returnValue(type === 'application');
      spyOn(CLMContextLocations, 'isRepositories').and.returnValue(type === 'repositories');
      spyOn(CLMContextLocations, 'getEntityId').and.returnValue(owner[type === 'application' ? 'publicId' : 'id']);
      spyOn(actions, 'fetchProductFeaturesIfNeeded').and.returnValue({ payload: [] });
      spyOn(applicationCategoriesActions, 'loadApplicableCategories').and.returnValue({ payload: [] });

      loadApplicationsIfNeededMock = spyOn(applicationActions, 'loadApplications').and.returnValue({
        payload: [
          {
            contact: null,
            id: '635618f9560042fb80608592c568041d',
            name: 'PublicId',
            organizationId: '0a4ca3e6b672406892170481ef79799e',
            organizationName: 'org',
            publicId: owner.publicId,
          },
        ],
      });

      vm = $controller('OwnerDetailTreeViewController', {
        $scope: $scope,
        $state: {
          $current: { name: '' },
        },
      });

      vm.isMonitoringSupported = true;
      vm.isGrandfatheringSupported = true;
      $scope.vm = vm;
    }));

    afterEach(function () {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    });

    it('Properly Loading Data', function () {
      vm.$onInit();
      resolveGet(owner, [SidebarResourceMockData.getOwnerDetailsUrl()]);

      if (vm.isApp) {
        expect(vm.ownerName).toBe('PublicId');
      } else {
        expect(vm.ownerName).toBe(owner.name);
      }

      expect(vm.details).toEqual(SidebarResourceMockData.getOwnerDetailsUrl());
      expect(vm.error).toBeUndefined();

      if (vm.isApp) {
        expect(vm.areAnyCategoriesDefined).toBeFalsy();
      }

      expect(vm.isMonitoringSupported).toBe(true);
    });

    it('Properly Detecting Details Loading Error', function () {
      vm.$onInit();
      resolveGet(owner, [400, 'Bad Request']);

      expect(vm.details).toBeUndefined();
      expect(vm.error).toBeDefined();
      expect(vm.error.data).toEqual('Bad Request');
    });

    it('Properly Displaying Owner Name Loading Error', function () {
      loadApplicationsIfNeededMock.and.returnValue({ payload: [] });
      vm.$onInit();
      resolveGet(null, [SidebarResourceMockData.getOwnerDetailsUrl()]);

      if (!vm.isRepositories) {
        if (!vm.isApp) {
          expect(vm.ownerName).toBeUndefined();
        }

        expect(vm.error).toBe('Could not find an ' + type + ' with ID ' + CLMContextLocations.getEntityId() + '.');
      } else {
        expect(vm.ownerName).toBe('Repositories');
        expect(vm.error).toBeFalsy();
      }
    });

    it('Properly Updating Data via broadcast', inject(function ($rootScope) {
      vm.$onInit();
      resolveGet(owner, [400, 'Bad Request']);

      expect(vm.details).toBeUndefined();
      expect(vm.error).toBeDefined();
      expect(vm.error.data).toEqual('Bad Request');

      $rootScope.$broadcast('resource.data.modified');
      if (mockOwnerStore) {
        mockOwnerStore.resolveGetById(owner);
      }
      $httpBackend
        .expectGET(CLMContextLocations.getOwnerDetailsUrl())
        .respond(SidebarResourceMockData.getOwnerDetailsUrl());

      $httpBackend.flush();
      $timeout.flush();

      if (vm.isApp) {
        expect(vm.ownerName).toBe('PublicId');
      } else {
        expect(vm.ownerName).toBe(owner.name);
      }

      expect(vm.details).toEqual(SidebarResourceMockData.getOwnerDetailsUrl());
      expect(vm.error).toBeUndefined();
    }));

    it('watches vm.labels and calls vm.doLoad on change', function () {
      vm.$onInit();
      resolveGet(owner, [400, 'Bad Request']);

      expect(vm.details).toBeUndefined();
      expect(vm.error).toBeDefined();
      expect(vm.error.data).toEqual('Bad Request');

      $httpBackend
        .expectGET(CLMContextLocations.getOwnerDetailsUrl())
        .respond(SidebarResourceMockData.getOwnerDetailsUrl());

      vm.labels = [
        {
          color: 'dark-green',
          description: 'description',
          label: 'Dark Green',
          id: '1242345',
        },
      ];

      $scope.$digest();

      if (mockOwnerStore) {
        mockOwnerStore.resolveGetById(owner);
      }

      $httpBackend.flush();
      $timeout.flush();

      if (vm.isApp) {
        expect(vm.ownerName).toBe('PublicId');
      } else {
        expect(vm.ownerName).toBe(owner.name);
      }

      expect(vm.details).toEqual(SidebarResourceMockData.getOwnerDetailsUrl());
      expect(vm.error).toBeUndefined();
    });

    it('watches vm.categories and calls vm.doLoad on change', inject(function () {
      vm.$onInit();
      resolveGet(owner, [400, 'Bad Request']);

      expect(vm.details).toBeUndefined();
      expect(vm.error).toBeDefined();
      expect(vm.error.data).toEqual('Bad Request');

      $httpBackend
        .expectGET(CLMContextLocations.getOwnerDetailsUrl())
        .respond(SidebarResourceMockData.getOwnerDetailsUrl());

      vm.categories = 'test';
      $scope.$digest();

      if (mockOwnerStore) {
        mockOwnerStore.resolveGetById(owner);
      }

      $httpBackend.flush();
      $timeout.flush();

      if (vm.isApp) {
        expect(vm.ownerName).toBe('PublicId');
      } else {
        expect(vm.ownerName).toBe(owner.name);
      }

      expect(vm.details).toEqual(SidebarResourceMockData.getOwnerDetailsUrl());
      expect(vm.error).toBeUndefined();
    }));

    it('watches vm.policies and calls vm.doLoad on change', inject(function () {
      vm.$onInit();
      resolveGet(owner, [400, 'Bad Request']);

      expect(vm.details).toBeUndefined();
      expect(vm.error).toBeDefined();
      expect(vm.error.data).toEqual('Bad Request');

      $httpBackend
        .expectGET(CLMContextLocations.getOwnerDetailsUrl())
        .respond(SidebarResourceMockData.getOwnerDetailsUrl());

      vm.policies = 'test';
      $scope.$digest();

      if (mockOwnerStore) {
        mockOwnerStore.resolveGetById(owner);
      }

      $httpBackend.flush();
      $timeout.flush();

      if (vm.isApp) {
        expect(vm.ownerName).toBe('PublicId');
      } else {
        expect(vm.ownerName).toBe(owner.name);
      }

      expect(vm.details).toEqual(SidebarResourceMockData.getOwnerDetailsUrl());
      expect(vm.error).toBeUndefined();
    }));

    function resolveGet(ownerData, detailsDataArray) {
      if (mockOwnerStore) {
        if (ownerData) {
          mockOwnerStore.resolveGetById(ownerData);
        } else {
          mockOwnerStore.rejectGetById(
            'Could not find an ' + type + ' with ID ' + CLMContextLocations.getEntityId() + '.'
          );
        }
      }
      $httpBackend.expectGET(CLMContextLocations.getOwnerDetailsUrl()).respond.apply(this, detailsDataArray);

      $httpBackend.flush();
      $timeout.flush();
    }
  }

  OwnerUtils.runTestsForAllOwnerTypes(createTests);
});
