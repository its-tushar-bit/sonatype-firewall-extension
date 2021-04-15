/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import template from './successMetricsReport.html';

export default {
  template,
  controller: successMetricsReportController,
  controllerAs: 'vm',
};

function successMetricsReportController(
  $q,
  $state,
  $stateParams,
  systemConfigurationPropertyService,
  successMetricsDataService
) {
  const vm = this;

  vm.loaded = false;
  vm.error = undefined;
  vm.activeApplicationCount = undefined;
  vm.successMetricsReport = undefined;
  vm.singleApplicationName = undefined;
  vm.isSingleApplicationReport = undefined;
  vm.lastUpdated = undefined;
  vm.monthCount = undefined;
  vm.doLoad = doLoad;
  vm.isMttrDisabled = isMttrDisabled;
  vm.goToList = goToList;
  vm.hasDisabledError = hasDisabledError;

  function doLoad() {
    const { successMetricsReportId } = $stateParams;

    delete vm.error;

    $q.all([
      systemConfigurationPropertyService.checkSuccessMetricsEnabled(),
      successMetricsDataService.getSuccessMetricsReportsForCurrentUser(),
    ])
      .then(function ([, successMetricsReports]) {
        // this would be nicer if Array.prototype.find was available in all browsers
        for (let i = 0; i < successMetricsReports.length && vm.successMetricsReport === undefined; i++) {
          if (successMetricsReports[i].id === successMetricsReportId) {
            vm.successMetricsReport = successMetricsReports[i];
          }
        }

        if (vm.successMetricsReport) {
          return $q.all([
            successMetricsDataService.getChartData(vm.successMetricsReport),
            successMetricsDataService.getComponentCountsData(vm.successMetricsReport),
          ]);
        } else {
          return $q.reject(`Could not find report with id ${successMetricsReportId}`);
        }
      })
      .then(function ([chartData, componentCountsData]) {
        const {
          applicationCountsData,
          mttrData,
          averagesData,
          violationsByCategoryData,
          lastUpdated,
          monthCount,
          violationCounts,
        } = chartData;

        angular.extend(vm, {
          applicationCountsData,
          mttrData,
          averagesData,
          lastUpdated,
          monthCount,
          componentCountsData,
          violationsByCategoryData,
          violationCounts,
        });

        vm.activeApplicationCount = applicationCountsData.activeApplications;
        vm.isSingleApplicationReport =
          !!(
            vm.successMetricsReport &&
            vm.successMetricsReport.scope.applicationIds &&
            vm.successMetricsReport.scope.applicationIds.length === 1
          ) &&
          (!vm.successMetricsReport.scope.organizationIds ||
            vm.successMetricsReport.scope.organizationIds.length === 0);
        if (vm.isSingleApplicationReport && vm.activeApplicationCount > 0) {
          return successMetricsDataService
            .getApplicationByInternalId(vm.successMetricsReport.scope.applicationIds[0])
            .then(function (owner) {
              vm.singleApplicationName = owner.name;
            });
        }
      })
      .catch(function (error) {
        vm.error = error;
      })
      .finally(function () {
        vm.loaded = true;
      });
  }

  function isMttrDisabled() {
    return !vm.mttrData || vm.mttrData.length === 0;
  }

  function goToList() {
    $state.go('labs.successMetrics');
  }

  function hasDisabledError() {
    return vm.error === systemConfigurationPropertyService.SUCCESS_METRICS_DISABLED_MESSAGE;
  }

  doLoad();
}

successMetricsReportController.$inject = [
  '$q',
  '$state',
  '$stateParams',
  'systemConfigurationPropertyService',
  'successMetricsDataService',
];
