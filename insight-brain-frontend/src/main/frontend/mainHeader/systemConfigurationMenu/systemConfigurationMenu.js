/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { faCog } from '@fortawesome/pro-regular-svg-icons';
import template from './systemConfigurationMenu.html';

function SystemConfigurationMenuController($state, $ngRedux, scmOnboardingActions) {
  var vm = this;
  vm.state = $state;
  vm.$onInit = doLoad;
  vm.$onDestroy = doDestroy;

  vm.faCog = faCog;

  function doLoad() {
    vm.unsubscribe = $ngRedux.connect(mapStateToThis, scmOnboardingActions)(vm);
    if (vm.state.configState === undefined || vm.state.configState.scmOnboarding === undefined) {
      vm.loadConfig();
    }
  }

  function doDestroy() {
    vm.unsubscribe();
  }
}

function mapStateToThis(state) {
  return {
    isScmOnboardingFeatureEnabled: state.scmOnboarding.configState.isScmOnboardingFeatureEnabled
  };
}

SystemConfigurationMenuController.$inject = ['$state', '$ngRedux', 'scmOnboardingActions'];

export default {
  template,
  controller: SystemConfigurationMenuController,
  controllerAs: 'vm',
  bindings: {
    permissions: '<',
    isWebhooksSupported: '<'
  }
};
