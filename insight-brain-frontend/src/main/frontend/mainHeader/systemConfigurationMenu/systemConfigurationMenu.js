/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { faCog } from '@fortawesome/pro-regular-svg-icons';
import template from './systemConfigurationMenu.html';

function SystemConfigurationMenuController($state) {
  var vm = this;
  vm.state = $state;

  vm.faCog = faCog;
}

SystemConfigurationMenuController.$inject = ['$state'];

export default {
  template,
  controller: SystemConfigurationMenuController,
  controllerAs: 'vm',
  bindings: {
    permissions: '<',
    isWebhooksSupported: '<'
  }
};
