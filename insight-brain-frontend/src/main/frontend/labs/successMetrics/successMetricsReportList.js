/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import modalWrapperTemplate from './addSuccessMetricsReportModalWrapper.html';
import template from './successMetricsReportList.html';

export default {
  template,
  controller: successMetricsReportController,
  controllerAs: 'vm',
};

function successMetricsReportController(
  $state,
  $q,
  systemConfigurationPropertyService,
  successMetricsDataService,
  Modal
) {
  const vm = this;

  vm.loaded = false;
  vm.error = undefined;
  vm.successMetricsReports = undefined;

  vm.$onInit = $onInit;
  vm.goToCharts = goToCharts;
  vm.openAddSuccessMetricsReportModal = openAddSuccessMetricsReportModal;
  vm.hasDisabledError = hasDisabledError;

  function $onInit() {
    delete vm.error;

    $q.all([
      successMetricsDataService.getSuccessMetricsReportsForCurrentUser(),
      systemConfigurationPropertyService.checkSuccessMetricsEnabled(),
    ])
      .then(function ([successMetricsReports]) {
        vm.successMetricsReports = successMetricsReports;
      })
      .catch(function (error) {
        vm.error = error;
      })
      .finally(function () {
        vm.loaded = true;
      });
  }

  function goToCharts(successMetricsReportId) {
    $state.go('labs.successMetricsReport', { successMetricsReportId });
  }

  function openAddSuccessMetricsReportModal() {
    // simple controller to help get the successMetricsReports property into the Modal template's scope
    function modalController($scope) {
      $scope.successMetricsReports = vm.successMetricsReports;
    }

    modalController.$inject = ['$scope'];

    const modalPromise = Modal.open({
      template: modalWrapperTemplate,
      controller: modalController,
    }).result;

    modalPromise.then(function (successMetricsReport) {
      vm.successMetricsReports.push(successMetricsReport);
    });
  }

  function hasDisabledError() {
    return (
      vm.error ===
      systemConfigurationPropertyService.SUCCESS_METRICS_DISABLED_MESSAGE
    );
  }
}

successMetricsReportController.$inject = [
  '$state',
  '$q',
  'systemConfigurationPropertyService',
  'successMetricsDataService',
  'Modal',
];
