/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import cipModalWrapper from './firewallCipModal/firewallCipModalWrapper.html';
import { pick } from 'ramda';
import template from './firewall.html';

export default {
  template,
  controller: FirewallController,
  controllerAs: 'vm',
};

function FirewallController($state, $ngRedux, $scope, $timeout, Modal, OwnerContext) {
  const vm = this;

  Object.assign(vm, {
    $onInit() {
      vm.unsubscribe = $ngRedux.connect(mapStateToThis)(vm);
      $scope.$watch('vm.showCipModal', function () {
        if (vm.showCipModal) {
          Modal.open({
            template: cipModalWrapper,
            windowClass: 'iq-modal iq-modal__cip',
            backdropClass: 'iq-modal-backdrop',
          });
        }
      });

      $scope.$watch('vm.selectedComponent', function () {
        if (vm.showCipModal) {
          OwnerContext.setOwnerId(vm.selectedComponent.repositoryId);
          OwnerContext.setOwnerType('repository');
        }
      });
    },

    $onDestroy() {
      vm.unsubscribe();
    },
  });
}

export function mapStateToThis({ firewall }) {
  return {
    ...pick(['selectedComponent', 'showCipModal'], firewall.cip),
  };
}

FirewallController.$inject = ['$state', '$ngRedux', '$scope', '$timeout', 'Modal', 'OwnerContext'];
