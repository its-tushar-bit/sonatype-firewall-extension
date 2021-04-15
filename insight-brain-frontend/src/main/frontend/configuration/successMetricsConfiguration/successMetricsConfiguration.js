/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './successMetricsConfiguration.html';

var successMetricsConfiguration = {
  controller: SuccessMetricsConfigurationController,
  bindings: {
    isAuthorized: '<',
  },
  controllerAs: 'vm',
  template: template,
};

function SuccessMetricsConfigurationController(systemConfigurationPropertyService) {
  var vm = this;
  vm.successMetricsEnabled = undefined;
  vm.savedSuccessMetricsEnabled = undefined;
  vm.error = undefined;
  vm.load = load;
  vm.save = save;
  vm.cancel = cancel;
  vm.isChanged = isChanged;

  vm.load();

  function load() {
    vm.error = undefined;
    systemConfigurationPropertyService
      .isSuccessMetricsEnabled()
      .then(function (response) {
        vm.savedSuccessMetricsEnabled = response;
        vm.successMetricsEnabled = response;
      })
      .catch(function (error) {
        vm.error = error;
      });
  }

  function save() {
    vm.error = undefined;
    systemConfigurationPropertyService
      .saveSuccessMetricsEnabled(vm.successMetricsEnabled)
      .then(function () {
        vm.savedSuccessMetricsEnabled = vm.successMetricsEnabled;
      })
      .catch(function (error) {
        vm.error = error;
      });
  }

  function cancel() {
    vm.successMetricsEnabled = vm.savedSuccessMetricsEnabled;
  }

  function isChanged() {
    return !angular.equals(vm.savedSuccessMetricsEnabled, vm.successMetricsEnabled);
  }
}

SuccessMetricsConfigurationController.$inject = ['systemConfigurationPropertyService'];

export default successMetricsConfiguration;
