/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import template from './firewallCipModal.html';
import { cipModalClosed, selectComponent } from '../firewallActions';

export default {
  template,
  controller: FirewallCipModalController,
  controllerAs: 'vm',
  bindings: {
    dismiss: '&',
  },
};

function FirewallCipModalController($rootScope, $ngRedux, $scope, ComponentUpdateService, OwnerContext) {
  const vm = this;

  Object.assign(vm, {
    $onInit() {
      vm.unsubscribeFromReduxStore = $ngRedux.connect(mapStateToThis, { selectComponent, cipModalClosed })(vm);
      $scope.$on('modal.closing', function () {
        vm.cipModalClosed();
      });

      $rootScope.$on('reevaluate.component', function (event, componentKey) {
        if (OwnerContext.ownerType === 'repository') {
          ComponentUpdateService.reevaluate(componentKey, true);
        }
      });
      $rootScope.$on('reload.component', function (event, componentKey) {
        if (OwnerContext.ownerType === 'repository') {
          ComponentUpdateService.reevaluate(componentKey, false);
        }
      });
    },

    $onDestroy() {
      vm.unsubscribeFromReduxStore();
    },
  });
}

export function mapStateToThis({ firewall }) {
  let { selectedComponentIndex, selectedComponent, displayedEntries } = firewall.cip;

  return {
    selectedComponent,
    selectedComponentIndex,
    displayedEntries,
  };
}

FirewallCipModalController.$inject = ['$rootScope', '$ngRedux', '$scope', 'ComponentUpdateService', 'OwnerContext'];
