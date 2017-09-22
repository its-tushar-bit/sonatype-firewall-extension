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
    successMetrics: '<',
    onDelete: '&',
    isSingleApplicationReport: '<'
  }
};

function summaryStatementTileController(DeleteModalService, successMetricsDataService) {

  const vm = this;

  vm.showNoDataMessage = undefined;
  vm.delete = deleteSuccessMetrics;

  vm.$onInit = function() {
    vm.showNoDataMessage = vm.averagesData.activeApplicationCount === 0;
  };

  function deleteSuccessMetrics() {
    DeleteModalService.deleteCustom('Delete Success Metrics',
        `You are about to delete ${vm.successMetrics.name}. This action cannot be undone.`, 'Deleting', function() {
          return successMetricsDataService.deleteSuccessMetrics(vm.successMetrics.id);
        }).then(function() {
      vm.onDelete();
    });
  }
}

summaryStatementTileController.$inject = ['DeleteModalService', 'successMetricsDataService'];
