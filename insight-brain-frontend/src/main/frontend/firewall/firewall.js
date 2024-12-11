/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pick } from 'ramda';
import template from './firewall.html';

export default {
  template,
  controller: FirewallController,
  controllerAs: 'vm',
};

function FirewallController($state, $ngRedux) {
  const vm = this;

  Object.assign(vm, {
    $onInit() {
      vm.unsubscribe = $ngRedux.connect(mapStateToThis)(vm);
    },

    $onDestroy() {
      vm.unsubscribe();
    },
  });
}

export function mapStateToThis({ firewall }) {
  return {
    ...pick(['selectedComponent'], firewall.cip),
  };
}

FirewallController.$inject = ['$state', '$ngRedux'];
