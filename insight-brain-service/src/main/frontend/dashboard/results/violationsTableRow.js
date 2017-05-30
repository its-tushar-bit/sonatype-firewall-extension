/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

function ViolationsTableRowController(StageTypeStore, $window, $state) {
  var vm = this;

  vm.doLoad = doLoad;
  vm.openReport = openReport;
  vm.stageTypes = undefined;

  vm.doLoad();

  function doLoad() {
    StageTypeStore.getDashboardStages().then(function(data) {
      vm.stageTypes = data.reduce(function(map, stageType) {
        map[stageType.stageTypeId] = stageType;
        return map;
      }, {});
    });
  }

  function openReport(appPublicId, scanId) {
    if (scanId) {
      $window.open($state.href('report', {
        publicId: appPublicId,
        scanId: scanId
      }), '_blank');
    }
  }

  vm.latestReport = vm.risk.stageDetails.reduce(function(latestReportSoFar, report) {
    return latestReportSoFar && latestReportSoFar.time > report.time ? latestReportSoFar : report;
  }, undefined);
}

ViolationsTableRowController.$inject = ['StageTypeStore', '$window', '$state'];

var violationsTableRowComponent = {
  bindings: {
    risk: '<'
  },
  controllerAs: 'vm',
  controller: ViolationsTableRowController,
  templateUrl: 'violations-table-row'
};

export default violationsTableRowComponent;
