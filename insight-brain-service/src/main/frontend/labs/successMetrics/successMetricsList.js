/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import modalWrapperTemplate from './addSuccessMetricsModalWrapper.html';

export default {
  templateUrl: 'labs/successMetrics/successMetricsList.html?' + clmBuildTimestamp,
  controller: successMetricsController,
  controllerAs: 'vm'
};

function successMetricsController($state, $q, systemConfigurationPropertyService, successMetricsDataService, Modal) {
  const vm = this;

  vm.loaded = false;
  vm.error = undefined;
  vm.successMetricsList = undefined;

  vm.$onInit = $onInit;
  vm.goToCharts = goToCharts;
  vm.openAddSuccessMetricsModal = openAddSuccessMetricsModal;

  function $onInit() {
    delete vm.error;

    $q.all([
      successMetricsDataService.getSuccessMetricsForCurrentUser(),
      systemConfigurationPropertyService.checkSuccessMetricsEnabled()
    ]).then(function([successMetricsList]) {
      vm.successMetricsList = successMetricsList;
    }).catch(function(error) {
      vm.error = error;
    }).finally(function() {
      vm.loaded = true;
    });
  }

  function goToCharts(successMetricsId) {
    $state.go('labs.successMetricsChart', { successMetricsId });
  }

  function openAddSuccessMetricsModal() {
    // simple controller to help get the successMetricsList property into the Modal template's scope
    function modalController($scope) {
      $scope.successMetricsList = vm.successMetricsList;
    }

    modalController.$inject = ['$scope'];

    const modalPromise = Modal.open({
      template: modalWrapperTemplate,
      controller: modalController
    }).result;

    modalPromise.then(function(successMetrics) {
      vm.successMetricsList.push(successMetrics);
    });
  }
}

successMetricsController.$inject =
  ['$state', '$q', 'systemConfigurationPropertyService', 'successMetricsDataService', 'Modal'];
