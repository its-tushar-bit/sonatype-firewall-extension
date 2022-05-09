/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import innerSourceRepositoryModule from 'MainRoot/owner.manager/innersource.repository/module';
import utilityModule from 'MainRoot/utility/utility.module';
import ownerManagerModule from 'MainRoot/owner.manager/owner.manager.module';
import { actions } from 'MainRoot/productFeatures/productFeaturesSlice';

describe('innerSourceRepositoryTile', function () {
  let $rootScope,
    $scope,
    vm,
    $componentController,
    $q,
    mockInnerSourceRepositoryService,
    getRepositoryConnectionsDeferred;

  beforeEach(
    angular.mock.module(ownerManagerModule.name, function ($provide) {
      $provide.value('$cookies', {
        get: angular.noop,
      });
      SpecUtil.mockNgRedux($provide);
    })
  );

  beforeEach(
    angular.mock.module(innerSourceRepositoryModule.name, function ($provide) {
      SpecUtil.mockNgRedux($provide);
    })
  );

  beforeEach(angular.mock.module(utilityModule.name));

  beforeEach(inject(function (_$rootScope_, $injector, _$componentController_, _$q_) {
    $rootScope = _$rootScope_;
    $componentController = _$componentController_;
    $scope = $rootScope.$new();
    spyOn(actions, 'fetchProductFeaturesIfNeeded').and.returnValue({ payload: [] });

    mockInnerSourceRepositoryService = jasmine.createSpyObj('mockInnerSourceRepositoryService', [
      'getRepositoryConnections',
    ]);
    $q = _$q_;
    getRepositoryConnectionsDeferred = $q.defer();
  }));
  function initializeVm(isInnerSourceRepositorySupported = true, ownerId = 'organizationId') {
    vm = $componentController(
      'innerSourceRepositoryTile',
      {
        $scope,
        InnerSourceRepositoryService: mockInnerSourceRepositoryService,
      },
      {
        isOrg: true,
        isInnerSourceRepositorySupported,
        ownerId,
        ownerType: 'organization',
      }
    );
    mockInnerSourceRepositoryService.getRepositoryConnections.and.callFake(function () {
      return getRepositoryConnectionsDeferred.promise;
    });
  }

  describe('on initialization', () => {
    it('subscribes to the redux store', () => {
      initializeVm();
      expect(vm.unsubscribe).toBeDefined();
    });

    describe('isInnerSourceRepositorySupported is not set', () => {
      it('does not call loadRepositoryConnections', () => {
        initializeVm(false);
        expect(mockInnerSourceRepositoryService.getRepositoryConnections).not.toHaveBeenCalled();
        expect(vm.isInnerSourceRepositorySupported).toBeFalse();
        expect(vm.innerSourceRepository).toBeUndefined();
      });
    });

    describe('isInnerSourceRepositorySupported is set', () => {
      it('calls loadRepositoryConnections', () => {
        initializeVm(true, 'organizationId2');
        getRepositoryConnectionsDeferred.resolve({
          repositoryConnectionStatus: {
            inheritedFromOrganizationName: null,
            inheritedFromOrgEnabled: null,
            enabled: false,
            allowChange: true,
          },
          repositoryConnections: [
            { ownerId: 'organizationId', baseUrl: 'https://some.base.url.1' },
            { ownerId: 'organizationId', baseUrl: 'https://some.base.url.2' },
          ],
        });
        $scope.$apply();
        vm.ownerId = 'organizationId';
        $scope.$apply();
        expect(mockInnerSourceRepositoryService.getRepositoryConnections).toHaveBeenCalledWith(
          'organization',
          'organizationId',
          true
        );
        expect(vm.error).toBeUndefined();
        expect(vm.loading).toBeFalse();
        expect(vm.isInnerSourceRepositorySupported).toBeTrue();
        expect(vm.innerSourceRepositories).toEqual([
          { ownerId: 'organizationId', baseUrl: 'https://some.base.url.1' },
          { ownerId: 'organizationId', baseUrl: 'https://some.base.url.2' },
        ]);
        expect(vm.innerSourceRepositoriesEnabled).toBeFalse();
      });
    });
  });

  describe('on $destroy()', () => {
    it('unsubscribes from redux store', () => {
      initializeVm();
      expect(vm.unsubscribe).not.toHaveBeenCalled();
      $scope.$destroy();
      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });
});
