/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function MonitoredStageEditorController($q, StageTypeStore, PolicyMonitoringStore, Messages, MonitoredStageService)
  {
    var originalStage,
        vm = this;

    vm.loadError = undefined;
    vm.submitError = undefined;
    vm.stages = undefined;
    vm.monitoredStage = undefined;
    vm.doLoad = doLoad;
    vm.save = save;
    vm.isDirty = isDirty;
    vm.continuousMonitoringEditor = undefined;
    vm.continuousMonitoringEditorMask = undefined;

    vm.doLoad();

    function doLoad() {
      delete vm.loadError;
      $q.all([StageTypeStore.get(), PolicyMonitoringStore.getApplicable()]).then(function(results) {
        vm.stages = angular.copy(results[0]);
        var policyMonitoringByOwner = results[1].data.policyMonitoringByOwner;

        vm.stages.unshift(MonitoredStageService.createInheritOrNoMonitorOption(policyMonitoringByOwner, vm.stages));
        vm.monitoredStage = MonitoredStageService.getMonitoredStage(policyMonitoringByOwner[0].policyMonitoring,
            vm.stages);

        originalStage = angular.copy(vm.monitoredStage);
      }, function(error) {
        vm.loadError = Messages.getHttpErrorMessage(error);
      });
    }

    function save() {
      delete vm.submitError;

      vm.continuousMonitoringEditorMask.wrap(vm.monitoredStage.stageTypeId ?
          PolicyMonitoringStore.save(vm.monitoredStage) : PolicyMonitoringStore.remove()).then(function() {
        originalStage = angular.copy(vm.monitoredStage);
      }, function(error) {
        vm.submitError = Messages.getHttpErrorMessage(error);
      });
    }

    function isDirty() {
      return originalStage.stageTypeId !== vm.monitoredStage.stageTypeId;
    }
  }

  MonitoredStageEditorController.$inject = [
    '$q', 'StageTypeStore', 'PolicyMonitoringStore', 'Messages', 'monitored.stage.service'
  ];

  angular //
      .module('owner.manager.module') //
      .controller('monitored.stage.editor.controller', MonitoredStageEditorController);

}(angular));
