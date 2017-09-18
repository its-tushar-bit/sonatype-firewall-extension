/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import template from './successMetricsChartPage.html';

export default {
  template,
  controller: successMetricsChartPageController,
  controllerAs: 'vm'
};

function successMetricsChartPageController($q, $stateParams, systemConfigurationPropertyService, successMetricsDataService) {
  const vm = this;

  vm.loaded = false;
  vm.error = undefined;
  vm.activeApplicationCount = undefined;
  vm.successMetricsName = undefined;
  vm.doLoad = doLoad;
  vm.isMttrDisabled = isMttrDisabled;

  function doLoad() {
    const { successMetricsId } = $stateParams;

    delete vm.error;

    $q.all([
      systemConfigurationPropertyService.checkSuccessMetricsEnabled(),
      successMetricsDataService.getSuccessMetricsForCurrentUser()
    ]).then(function([, successMetricsList]) {
      let serviceParameters;

      // this would be nicer if Array.prototype.find was available in all browsers
      for (let i = 0; i < successMetricsList.length && serviceParameters === undefined; i++) {
        if (successMetricsList[i].id === successMetricsId) {
          serviceParameters = successMetricsList[i].scope;
          vm.successMetricsName = successMetricsList[i].name;
        }
      }

      if (serviceParameters) {
        return $q.all([
          successMetricsDataService.getApplicationCountsData(serviceParameters),
          successMetricsDataService.getMttrData(serviceParameters),
          successMetricsDataService.getAveragesData(serviceParameters),
          successMetricsDataService.getComponentCountsData(serviceParameters)
        ]);
      }
      else {
        return $q.reject(`Could not find Success Metrics with id ${successMetricsId}`);
      }
    }).then(function([applicationCountsData, mttrData, averagesData, componentCountsData]) {
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

successMetricsChartPageController.$inject = [
  '$q', '$stateParams', 'systemConfigurationPropertyService', 'successMetricsDataService'
];
