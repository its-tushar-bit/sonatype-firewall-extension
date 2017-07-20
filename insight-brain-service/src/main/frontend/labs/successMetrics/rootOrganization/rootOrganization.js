/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export default {
  templateUrl: 'labs/successMetrics/rootOrganization/rootOrganization.html?' + clmBuildTimestamp,
  controller: rootOrganizationController,
  controllerAs: 'vm'
};

function rootOrganizationController($q, systemConfigurationPropertyService, successMetricsDataService) {
  var vm = this;
  vm.rootOrgAvailable = successMetricsDataService.isRootOrgAvailable();

  vm.loaded = false;
  vm.error = undefined;
  vm.activeApplicationCount = undefined;
  vm.doLoad = doLoad;

  function doLoad() {
    delete vm.error;
    $q.all([
      systemConfigurationPropertyService.checkSuccessMetricsEnabled(),
      successMetricsDataService.getApplicationCountsData()
    ]).then(function(results) {
      vm.activeApplicationCount = results[1].activeApplications;
    }).catch(function(error) {
      vm.error = error;
    }).finally(function() {
      vm.loaded = true;
    });
  }

  doLoad();
}

rootOrganizationController.$inject = [
  '$q', 'systemConfigurationPropertyService', 'successMetricsDataService'
];
