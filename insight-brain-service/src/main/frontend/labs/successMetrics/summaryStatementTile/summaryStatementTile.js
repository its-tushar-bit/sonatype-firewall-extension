/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * 'Sonatype' is a trademark of Sonatype, Inc.
 */

export default {
  templateUrl: 'labs/successMetrics/summaryStatementTile/summaryStatementTile.html?' + clmBuildTimestamp,
  controller: summaryStatementTileController,
  controllerAs: 'vm'
};

function summaryStatementTileController(successMetricsDataService) {
  var vm = this;

  vm.doLoad = doLoad;
  vm.error = undefined;
  vm.averagesData = undefined;

  doLoad();

  function doLoad() {
    vm.error = undefined;

    successMetricsDataService.getAveragesData().then(function(data) {
      vm.averagesData = data;
    }, function(error) {
      vm.error = error;
    });
  }
}

summaryStatementTileController.$inject = ['successMetricsDataService'];
