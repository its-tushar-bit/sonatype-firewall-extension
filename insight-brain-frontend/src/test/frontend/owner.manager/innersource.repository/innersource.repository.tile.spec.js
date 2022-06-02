/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import innerSourceRepositoryModule from 'MainRoot/owner.manager/innersource.repository/module';

describe('innerSourceRepositoryTile', function () {
  let $rootScope, $scope, vm, $componentController;

  beforeEach(
    angular.mock.module(innerSourceRepositoryModule.name, function ($provide) {
      SpecUtil.mockNgRedux($provide);
    })
  );

  beforeEach(inject(function (_$rootScope_, _$componentController_) {
    $rootScope = _$rootScope_;
    $componentController = _$componentController_;
    $scope = $rootScope.$new();

    vm = $componentController('innerSourceRepositoryTile', { $scope });
  }));

  describe('on initialization', () => {
    it('subscribes to the redux store', () => {
      expect(vm.unsubscribe).toBeDefined();
    });

    describe('isInnerSourceRepositorySupported is not set', () => {
      it('does not call loadRepositoryConnections', () => {
        vm.isInnerSourceRepositorySupported = false;

        vm.doLoad();

        expect(vm.loadRepositoryConnections).not.toHaveBeenCalled();
      });
    });

    describe('isInnerSourceRepositorySupported is set', () => {
      it('calls loadRepositoryConnections', () => {
        vm.isInnerSourceRepositorySupported = true;
        vm.ownerId = 'organizationId';

        vm.doLoad();

        expect(vm.loadRepositoryConnections).toHaveBeenCalledOnceWith({ ownerId: 'organizationId', inherit: true });
      });
    });
  });

  describe('on $destroy()', () => {
    it('unsubscribes from redux store', () => {
      expect(vm.unsubscribe).not.toHaveBeenCalled();

      $scope.$destroy();

      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });
});
