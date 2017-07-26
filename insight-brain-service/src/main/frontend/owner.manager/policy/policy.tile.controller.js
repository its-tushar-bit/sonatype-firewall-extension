/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
function PolicyTileController($scope, $q, StageTypeStore, SameOwnerStateNavigationService,
                              PolicyMonitoringStore, MonitoredStageService, EventNameConstant, PolicyHierarchyStore,
                              ProprietaryConfigHierarchyStore, CLMAppLocations, ProductFeatures)
{
  var vm = this;
  vm.ownerName = undefined;
  vm.policiesByOwner = undefined;
  vm.error = undefined;
  vm.actionStages = undefined;
  vm.monitoredStage = undefined;
  vm.localProprietaryCount = 0;
  vm.inheritedProprietaryCount = 0;
  vm.isRootOrg = CLMAppLocations.isRootOrg();
  vm.isMonitoringSupported = undefined;
  vm.editPolicy = editPolicy;
  vm.doLoad = doLoad;

  vm.doLoad();

  $scope.$on('policy.imported', doLoad);
  $scope.$on(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA, doLoad);
  $scope.$on(EventNameConstant.OWNER_UPDATED, updatedOwnerHandler);

  function doLoad() {
    $q.all([
      PolicyHierarchyStore.get(),
      StageTypeStore.getActionStages(),
      PolicyMonitoringStore.getApplicable(),
      ProprietaryConfigHierarchyStore.get(),
      ProductFeatures.load()
    ]).then(function(results) {
      vm.policiesByOwner = results[0];
      vm.actionStages = results[1];

      vm.policiesByOwner.forEach(function(policyOwner, index) {
        policyOwner.inherited = index > 0;
        policyOwner.policies.forEach(function(policy) {
          policy.enforcementAction = {};
          vm.actionStages.forEach(function(actionStage) {
            if (policy.actions[actionStage.stageTypeId]) {
              policy.enforcementAction[actionStage.stageTypeId] = policy.actions[actionStage.stageTypeId];
            }
          });
        });
      });

      vm.ownerName = vm.policiesByOwner[0].ownerName;

      var policyMonitoringByOwner = results[2].data.policyMonitoringByOwner;
      vm.monitoredStage = MonitoredStageService.getMonitoredStage(policyMonitoringByOwner[0].policyMonitoring,
          vm.actionStages);
      if (!vm.monitoredStage) {
        vm.monitoredStage = MonitoredStageService.createInheritOrNoMonitorOption(policyMonitoringByOwner,
            vm.actionStages);
      }

      var proprietaryMatchers = results[3];
      proprietaryMatchers.forEach(function(configOwner, index) {
        var config = configOwner.proprietaryConfig[0],
            matcherTotal = config.packages.length + config.regexes.length;
        if (index === 0) {
          vm.localProprietaryCount += matcherTotal;
        }
        else {
          vm.inheritedProprietaryCount += matcherTotal;
        }
      });

      vm.isMonitoringSupported = ProductFeatures.isAvailable('policy-monitoring');
    }, function(error) {
      vm.error = error;
    });

    delete vm.error;
  }

  function editPolicy(policyId) {
    SameOwnerStateNavigationService.goEdit('policy', {policyId: policyId});
  }

  function updatedOwnerHandler(event, newOwner) {
    vm.ownerName = newOwner.name;
  }
}

PolicyTileController.$inject = [
  '$scope', '$q', 'StageTypeStore', 'SameOwnerStateNavigationService',
  'PolicyMonitoringStore', 'monitored.stage.service', 'event.name.constant', 'PolicyHierarchyStore',
  'ProprietaryConfigHierarchyStore', 'CLMAppLocations', 'ProductFeatures'
];

angular //
    .module('owner.manager.module') //
    .controller('policy.tile.controller', PolicyTileController);
