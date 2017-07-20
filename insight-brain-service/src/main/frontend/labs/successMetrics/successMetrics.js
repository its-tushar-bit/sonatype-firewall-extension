/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export default {
  templateUrl: 'labs/successMetrics/successMetrics.html?' + clmBuildTimestamp,
  controller: successMetricsController,
  controllerAs: 'vm'
};

function successMetricsController($state, systemConfigurationPropertyService, successMetricsDataService) {
  var vm = this;

  vm.rootOrgAvailable = undefined;
  vm.loaded = false;
  vm.error = undefined;
  vm.goToRootOrgSuccessMetrics = goToRootOrgSuccessMetrics;

  vm.$onInit = function() {
    delete vm.error;
    vm.rootOrgAvailable = successMetricsDataService.isRootOrgAvailable();

    systemConfigurationPropertyService.checkSuccessMetricsEnabled().catch(function(error) {
      vm.error = error;
    }).finally(function() {
      vm.loaded = true;
    });

  };

  function goToRootOrgSuccessMetrics() {
    if (vm.rootOrgAvailable) {
      $state.go('labs.rootOrg');
    }
  }
}

successMetricsController.$inject = ['$state', 'systemConfigurationPropertyService', 'successMetricsDataService'];
