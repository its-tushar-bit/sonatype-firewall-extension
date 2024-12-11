/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import firewallModule from '../../../main/frontend/firewall/module';
import { mapStateToThis } from '../../../main/frontend/firewall/firewall';

describe('firewall', function () {
  let vm, scope;

  beforeEach(
    angular.mock.module(firewallModule.name, function ($provide) {
      SpecUtil.mockNgRedux($provide);
    })
  );

  beforeEach(inject(function ($componentController, $rootScope) {
    scope = $rootScope.$new();
    vm = $componentController('firewall', {
      $scope: scope,
    });
    scope.vm = vm;
    vm.selectedComponent = undefined;
    vm.$onInit();
    scope.$digest();
  }));

  describe('$onDestroy()', function () {
    it('unsubscribes from redux store', function () {
      expect(vm.unsubscribe).not.toHaveBeenCalled();
      vm.$onDestroy();
      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });

  describe('mapStateToThis', () => {
    it('sets selectedComponent', function () {
      const selectedComponent = { foo: 'bar' };
      const state = {
        firewall: {
          cip: {
            selectedComponent: selectedComponent,
          },
        },
      };

      const output = mapStateToThis(state);
      expect(output.selectedComponent).toBe(selectedComponent);
    });
  });
});
