/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * 'Sonatype' is a trademark of Sonatype, Inc.
 */

export default {
  templateUrl: 'labs/successMetrics/summaryStatementTile/summaryStatementTile.html?' + clmBuildTimestamp,
  controllerAs: 'vm',
  controller: summaryStatementTileController,
  bindings: {
    averagesData: '<',
    successMetricsReport: '<',
    onDelete: '&',
    isSingleApplicationReport: '<',
    lastUpdated: '<'
  }
};

function summaryStatementTileController(DeleteModalService, successMetricsDataService) {

  const vm = this;

  vm.showNoDataMessage = undefined;
  vm.dateFormat = undefined;
  vm.delete = deleteSuccessMetrics;

  vm.$onInit = function() {
    vm.showNoDataMessage = vm.averagesData.activeApplicationCount === 0;
    vm.dateFormat = vm.successMetricsReport.includeLatestData ? 'medium' : 'mediumDate';
  };

  function deleteSuccessMetrics() {
    DeleteModalService.deleteCustom('Delete Report',
        `You are about to delete ${vm.successMetricsReport.name}. This action cannot be undone.`, 'Deleting',
        function() {
          return successMetricsDataService.deleteSuccessMetricsReport(vm.successMetricsReport.id);
        }
    ).then(function() {
      vm.onDelete();
    });
  }
}

summaryStatementTileController.$inject = ['DeleteModalService', 'successMetricsDataService'];
