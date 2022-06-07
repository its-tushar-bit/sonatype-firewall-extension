/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import artifactoryRepositoryModule from 'MainRoot/owner.manager/artifactory.repository/module';
import utilityModule from 'MainRoot/utility/utility.module';
import ownerManagerModule from 'MainRoot/owner.manager/owner.manager.module';

describe('artifactoryRepositoryTile', function () {
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
    angular.mock.module(artifactoryRepositoryModule.name, function ($provide) {
      SpecUtil.mockNgRedux($provide);
    })
  );

  beforeEach(angular.mock.module(utilityModule.name));

  beforeEach(inject(function (_$rootScope_, _$componentController_) {
    $rootScope = _$rootScope_;
    $componentController = _$componentController_;
    $scope = $rootScope.$new();
  }));
  function initializeVm(isArtifactoryRepositorySupported = true, ownerId = 'organizationId') {
    vm = $componentController(
      'artifactoryRepositoryTile',
      {
        $scope,
      },
      {
        isOrg: true,
        isArtifactoryRepositorySupported,
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

    describe('isArtifactoryRepositorySupported is not set', () => {
      it('does not call loadArtifactoryConnection', () => {
        initializeVm(false);

        vm.doLoad();

        expect(vm.loadArtifactoryConnection).not.toHaveBeenCalled();
        expect(vm.isArtifactoryRepositorySupported).toBeFalse();
        expect(vm.artifactoryRepositories).toBeUndefined();
      });
    });

    describe('isArtifactoryRepositorySupported is set', () => {
      it('calls loadArtifactoryConnection', () => {
        initializeVm(true, 'organizationId2');
        vm.ownerId = 'organizationId';

        vm.doLoad();

        expect(vm.loadArtifactoryConnection).toHaveBeenCalledWith({ ownerId: 'organizationId', inherit: true });
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
