/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import sourceControlModule from 'MainRoot/owner.manager/source.control/module';
import utilityModule from 'MainRoot/utility/utility.module';
import ownerManagerModule from 'MainRoot/owner.manager/owner.manager.module';

describe('source.control.tile', function () {
  let $rootScope, $scope, vm;

  beforeEach(
    angular.mock.module(ownerManagerModule.name, function ($provide) {
      $provide.value('$cookies', {
        get: angular.noop,
      });
      SpecUtil.mockNgRedux($provide);
    })
  );

  beforeEach(angular.mock.module(sourceControlModule.name, utilityModule.name));

  beforeEach(inject(function (_$rootScope_, $injector, _$componentController_) {
    $rootScope = _$rootScope_;
    $scope = $rootScope.$new();

    vm = _$componentController_('sourceControlTile', {
      $scope: $scope,
    });
  }));
  describe('on component init', () => {
    it('subscribes to the redux store', () => {
      expect(vm.unsubscribe).toBeDefined();
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
