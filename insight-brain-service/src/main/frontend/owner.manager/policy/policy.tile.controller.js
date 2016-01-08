/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function PolicyTileController($scope, $q, $http, CLMAppLocations, StageTypeStore, SameOwnerStateNavigationService,
                                PolicyMonitoringStore, MonitoredStageService)
  {
    var vm = this;
    vm.ownerName = undefined;
    vm.policiesByOwner = undefined;
    vm.error = undefined;
    vm.actionStages = undefined;
    vm.monitoredStage = undefined;
    vm.editPolicy = editPolicy;

    vm.doLoad = doLoad;

    vm.doLoad();

    $scope.$on('policy.imported', doLoad);

    function doLoad() {
      $q.all([
        $http.get(CLMAppLocations.getApplicablePolicies()),
        StageTypeStore.getActionStages(),
        PolicyMonitoringStore.getApplicable()
      ]).then(function(results) {
        vm.actionStages = [];
        vm.policiesByOwner = results[0].data.policiesByOwner;
        vm.policiesByOwner.forEach(function(policyOwner, index) {
          policyOwner.inherited = index > 0;
        });

        vm.ownerName = vm.policiesByOwner[0].ownerName;

        var stages = results[1];
        stages.forEach(function(actionStage) {
          vm.actionStages.push(actionStage.stageTypeId);
        });

        var policyMonitoringByOwner = results[2].data.policyMonitoringByOwner;
        vm.monitoredStage = MonitoredStageService.getMonitoredStage(policyMonitoringByOwner[0].policyMonitoring,
            stages);
        if (!vm.monitoredStage) {
          vm.monitoredStage = MonitoredStageService.createInheritOrNoMonitorOption(policyMonitoringByOwner, stages);
        }
      }, function(error) {
        vm.error = error;
      });

      delete vm.error;
    }

    function editPolicy(policyId, inherited) {
      if (!inherited) {
        SameOwnerStateNavigationService.goEdit('policy', {policyId: policyId});
      }
    }
  }

  PolicyTileController.$inject = [
    '$scope', '$q', '$http', 'CLMAppLocations', 'StageTypeStore',
    'SameOwnerStateNavigationService', 'PolicyMonitoringStore', 'monitored.stage.service'
  ];

  angular //
      .module('owner.manager.module') //
      .controller('policy.tile.controller', PolicyTileController);
}(angular));
