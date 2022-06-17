/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import firewallModule from '../../../main/frontend/firewall/module';
import { mapStateToThis } from '../../../main/frontend/firewall/firewall';

describe('firewall', function () {
  let vm, scope, Modal, OwnerContext;

  beforeEach(
    angular.mock.module(firewallModule.name, function ($provide) {
      SpecUtil.mockNgRedux($provide);
    })
  );

  beforeEach(inject(function ($componentController, $rootScope, _Modal_, _OwnerContext_) {
    scope = $rootScope.$new();
    Modal = _Modal_;
    OwnerContext = _OwnerContext_;
    vm = $componentController('firewall', {
      $scope: scope,
    });
    scope.vm = vm;
    vm.selectedComponent = undefined;
    vm.showCipModal = false;
    vm.$onInit();
    scope.$digest();
  }));

  describe('vm.showCipModal watcher', function () {
    it('shows the modal when vm.showCipModal is changed to true', function () {
      spyOn(Modal, 'open');
      expect(Modal.open).not.toHaveBeenCalled();
      //vm.selectedComponent = {repositoryId: 'id'};
      vm.showCipModal = true;
      scope.$digest();
      expect(Modal.open).toHaveBeenCalledTimes(1);
      vm.showCipModal = false;
      scope.$digest();
      expect(Modal.open).toHaveBeenCalledTimes(1);
    });
  });

  describe('vm.selectedComponent watcher', function () {
    it('updates the OwnerContext when modal is shown and selected component changes', function () {
      spyOn(OwnerContext, 'setOwnerId');
      spyOn(OwnerContext, 'setOwnerType');
      spyOn(Modal, 'open');
      expect(OwnerContext.setOwnerId).not.toHaveBeenCalled();
      expect(OwnerContext.setOwnerType).not.toHaveBeenCalled();
      vm.selectedComponent = { repositoryId: 'id' };
      vm.showCipModal = true;
      scope.$digest();
      expect(OwnerContext.setOwnerId).toHaveBeenCalledWith('id');
      expect(OwnerContext.setOwnerType).toHaveBeenCalledWith('repository');
      vm.showCipModal = false;
      vm.selectedComponent = { repositoryId: 'id_2' };
      scope.$digest();
      expect(OwnerContext.setOwnerId).not.toHaveBeenCalledWith('id_2');
    });
  });

  describe('$onDestroy()', function () {
    it('unsubscribes from redux store', function () {
      expect(vm.unsubscribe).not.toHaveBeenCalled();
      vm.$onDestroy();
      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });

  describe('mapStateToThis', () => {
    it('sets selectedComponent and showCipModal', function () {
      const selectedComponent = { foo: 'bar' };
      const state = {
        firewall: {
          cip: {
            selectedComponent: selectedComponent,
            showCipModal: true,
          },
        },
      };

      const output = mapStateToThis(state);
      expect(output.selectedComponent).toBe(selectedComponent);
      expect(output.showCipModal).toBe(true);
    });
  });
});
