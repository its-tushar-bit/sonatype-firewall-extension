/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import firewallModule from '../../../../main/frontend/firewall/module';
import { mapStateToThis } from '../../../../main/frontend/firewall/firewallCipModal/firewallCipModal';

describe('firewallCipModal', function () {
  let vm, scope;

  beforeEach(
    angular.mock.module(firewallModule.name, function ($provide) {
      SpecUtil.mockNgRedux($provide);
    })
  );

  beforeEach(inject(function ($componentController, $rootScope) {
    scope = $rootScope.$new();
    vm = $componentController('firewallCipModal', {
      $scope: scope,
    });
    scope.vm = vm;
    vm.cipModalClosed = jasmine.createSpy();
    vm.$onInit();
  }));

  describe('on modal.closing', function () {
    it('calls cipModalClosed', function () {
      expect(vm.cipModalClosed).not.toHaveBeenCalled();
      scope.$emit('modal.closing');
      expect(vm.cipModalClosed).toHaveBeenCalledTimes(1);
    });
  });

  describe('$onDestroy()', function () {
    it('unsubscribes from redux store', function () {
      expect(vm.unsubscribeFromReduxStore).not.toHaveBeenCalled();
      vm.$onDestroy();
      expect(vm.unsubscribeFromReduxStore).toHaveBeenCalledTimes(1);
    });
  });

  describe('mapStateToThis', () => {
    it('sets selectedComponent, selectedComponentIndex and displayedEntries', function () {
      const selectedComponent = { foo: 'bar' };
      const state = {
        firewall: {
          cip: {
            selectedComponent: selectedComponent,
            selectedComponentIndex: 0,
            displayedEntries: [selectedComponent],
          },
        },
      };

      const output = mapStateToThis(state);
      expect(output.selectedComponent).toBe(selectedComponent);
      expect(output.selectedComponentIndex).toEqual(0);
      expect(output.displayedEntries).toEqual([selectedComponent]);
    });
  });
});
