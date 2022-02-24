/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';
import OwnerUtils from '../owner.utils';

describe('owner.detail.tree.view.directive.spec.js', function () {
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
      CLMLocations,
      CLMContextLocations,
      mockOwnerStore = storeName ? StoreUtils().createMockStore(storeName) : null;

    beforeEach(inject(function (
      $rootScope,
      $controller,
      _$timeout_,
      _$httpBackend_,
      _CLMLocations_,
      _CLMContextLocations_
    ) {
      $scope = $rootScope.$new();
      $timeout = _$timeout_;
      $httpBackend = _$httpBackend_;
      CLMLocations = _CLMLocations_;
      CLMContextLocations = _CLMContextLocations_;

      spyOn(CLMContextLocations, 'isApplication').and.returnValue(type === 'application');
      spyOn(CLMContextLocations, 'isRepositories').and.returnValue(type === 'repositories');
      spyOn(CLMContextLocations, 'getEntityId').and.returnValue(owner[type === 'application' ? 'publicId' : 'id']);

      vm = $controller('OwnerDetailTreeViewController', {
        $scope: $scope,
        $state: {
          $current: { name: '' },
        },
      });

      $scope.vm = vm;
    }));

    afterEach(function () {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    });

    it('Properly Loading Data', function () {
      resolveGet(owner, [SidebarResourceMockData.getOwnerDetailsUrl()]);

      expect(vm.ownerName).toBe(owner.name);
      expect(vm.details).toEqual(SidebarResourceMockData.getOwnerDetailsUrl());
      expect(vm.error).toBeUndefined();

      if (vm.isApp) {
        expect(vm.areAnyCategoriesDefined).toBeFalsy();
      }

      expect(vm.isMonitoringSupported).toBe(true);
    });

    it('Properly Detecting Details Loading Error', function () {
      resolveGet(owner, [400, 'Bad Request']);

      expect(vm.details).toBeUndefined();
      expect(vm.error).toBeDefined();
      expect(vm.error.data).toEqual('Bad Request');
    });

    it('Properly Displaying Owner Name Loading Error', function () {
      resolveGet(null, [SidebarResourceMockData.getOwnerDetailsUrl()]);

      if (!vm.isRepositories) {
        expect(vm.ownerName).toBeUndefined();
        expect(vm.error).toBe('Could not find an ' + type + ' with ID ' + CLMContextLocations.getEntityId() + '.');
      } else {
        expect(vm.ownerName).toBe('Repositories');
        expect(vm.error).toBeFalsy();
      }
    });

    it('Properly Updating Data via broadcast', inject(function ($rootScope) {
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

      if (vm.isApp) {
        $httpBackend
          .expectGET(CLMLocations.getApplicableOrganizationTags(CLMContextLocations.getEntityId()))
          .respond([]);
      }

      $httpBackend.flush();
      $timeout.flush();

      expect(vm.ownerName).toBe(owner.name);
      expect(vm.details).toEqual(SidebarResourceMockData.getOwnerDetailsUrl());
      expect(vm.error).toBeUndefined();
    }));

    it('watches vm.labels and calls vm.doLoad on change', function () {
      resolveGet(owner, [400, 'Bad Request']);

      expect(vm.details).toBeUndefined();
      expect(vm.error).toBeDefined();
      expect(vm.error.data).toEqual('Bad Request');

      $httpBackend
        .expectGET(CLMContextLocations.getOwnerDetailsUrl())
        .respond(SidebarResourceMockData.getOwnerDetailsUrl());

      if (vm.isApp) {
        $httpBackend
          .expectGET(CLMLocations.getApplicableOrganizationTags(CLMContextLocations.getEntityId()))
          .respond([]);
      }

      vm.labels = 'test';
      $scope.$digest();

      if (mockOwnerStore) {
        mockOwnerStore.resolveGetById(owner);
      }

      $httpBackend.flush();
      $timeout.flush();

      expect(vm.ownerName).toBe(owner.name);
      expect(vm.details).toEqual(SidebarResourceMockData.getOwnerDetailsUrl());
      expect(vm.error).toBeUndefined();
    });

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

      if (vm.isApp) {
        $httpBackend
          .expectGET(CLMLocations.getApplicableOrganizationTags(CLMContextLocations.getEntityId()))
          .respond([]);
      }

      $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond(['policy-monitoring']);

      $httpBackend.flush();
      $timeout.flush();
    }
  }

  OwnerUtils.runTestsForAllOwnerTypes(createTests);
});
