/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import innerSourceRepositoryModule from 'MainRoot/owner.manager/innersource.repository/module';
import utilityModule from 'MainRoot/utility/utility.module';
import ownerManagerModule from 'MainRoot/owner.manager/owner.manager.module';

describe('innerSourceRepositoryTile', function () {
  let $rootScope, $scope, vm, $componentController;

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

  beforeEach(inject(function (_$rootScope_, _$componentController_) {
    $rootScope = _$rootScope_;
    $componentController = _$componentController_;
    $scope = $rootScope.$new();
  }));
  function initializeVm(isInnerSourceRepositorySupported = true, ownerId = 'organizationId') {
    vm = $componentController(
      'innerSourceRepositoryTile',
      {
        $scope,
      },
      {
        isOrg: true,
        isInnerSourceRepositorySupported,
        ownerId,
        ownerType: 'organization',
      }
    );
  }

  describe('on initialization', () => {
    it('subscribes to the redux store', () => {
      initializeVm();
      expect(vm.unsubscribe).toBeDefined();
    });

    describe('isInnerSourceRepositorySupported is not set', () => {
      it('does not call loadRepositoryConnections', () => {
        initializeVm(false);
        expect(vm.loadRepositoryConnections).not.toHaveBeenCalled();
        expect(vm.isInnerSourceRepositorySupported).toBeFalse();
        expect(vm.innerSourceRepository).toBeUndefined();
      });
    });

    describe('isInnerSourceRepositorySupported is set', () => {
      it('calls loadRepositoryConnections', () => {
        initializeVm(true, 'organizationId2');
        $scope.$apply();
        vm.ownerId = 'organizationId';
        $scope.$apply();
        expect(vm.loadRepositoryConnections).toHaveBeenCalledWith({ ownerId: 'organizationId', inherit: true });
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
