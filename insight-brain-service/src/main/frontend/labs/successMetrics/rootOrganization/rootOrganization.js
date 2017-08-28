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
  vm.isMttrDisabled = isMttrDisabled;

  function doLoad() {
    delete vm.error;
    $q.all([
      systemConfigurationPropertyService.checkSuccessMetricsEnabled(),
      successMetricsDataService.getApplicationCountsData(),
      successMetricsDataService.getMttrData(),
      successMetricsDataService.getAveragesData(),
      successMetricsDataService.getComponentCountsData()
    ]).then(function([, applicationCountsData, mttrData, averagesData, componentCountsData]) {
      angular.extend(vm, { applicationCountsData, mttrData, averagesData, componentCountsData });

      vm.activeApplicationCount = applicationCountsData.activeApplications;
    }).catch(function(error) {
      vm.error = error;
    }).finally(function() {
      vm.loaded = true;
    });
  }

  function isMttrDisabled() {
    return !vm.mttrData || vm.mttrData.length === 0;
  }

  doLoad();
}

rootOrganizationController.$inject = [
  '$q', 'systemConfigurationPropertyService', 'successMetricsDataService'
];
