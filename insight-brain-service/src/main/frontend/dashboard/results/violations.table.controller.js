/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function ViolationsTableController(StageTypeStore, $window, $state) {
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
  }

  ViolationsTableController.$inject = ['StageTypeStore', '$window', '$state'];

  angular //
      .module('dashboard.utils') //
      .controller('violations.table.controller', ViolationsTableController);

}(angular));
