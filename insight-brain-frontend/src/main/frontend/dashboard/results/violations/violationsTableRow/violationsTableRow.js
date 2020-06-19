/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import template from './violationsTableRow.html';

export default
function ViolationsTableRow() {
  return {
    restrict: 'A',
    scope: {
      risk: '<'
    },
    bindToController: true,
    controllerAs: 'vm',
    controller: ViolationsTableRowController,
    template
  };
}

function ViolationsTableRowController(StageTypeStore, $window, $state) {
  var vm = this;

  vm.doLoad = doLoad;
  vm.openReport = openReport;
  vm.stageTypes = undefined;

  vm.violationHref = computeViolationHref(vm.risk);

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
      $window.open($state.href('applicationReport.policy', {
        publicId: appPublicId,
        scanId: scanId
      }), '_blank');
    }
  }

  function computeViolationHref(violation) {
    return $state.href('sidebarView.violation', {
      id: violation.policyViolationId,
      type: 'violation',
      sidebarReference: 'filter'
    });
  }
}

ViolationsTableRowController.$inject = ['StageTypeStore', '$window', '$state'];
