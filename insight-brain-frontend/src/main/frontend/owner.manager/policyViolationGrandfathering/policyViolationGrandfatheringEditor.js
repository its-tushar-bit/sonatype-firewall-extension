/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './policyViolationGrandfatheringEditor.html';

export default {
  template,
  controller: PolicyViolationGrandfatheringEditorController,
  controllerAs: 'vm'
};

function PolicyViolationGrandfatheringEditorController($scope, Messages, CLMContextLocations,
                                                       PolicyViolationGrandfatheringService) {
  const vm = this;

  Object.assign(vm, {
    loadError: undefined,
    submitError: undefined,
    originalConfiguration: undefined,
    currentConfiguration: undefined,
    statusMessage: undefined,
    violationGrandfatheringEditorMask: undefined,

    isApp: CLMContextLocations.isApplication(),
    isRootOrg: CLMContextLocations.isRootOrg(),

    $onInit() {
      vm.doLoad();
    },

    doLoad() {
      delete vm.loadError;
      PolicyViolationGrandfatheringService.getGrandfathering().then(function(data) {
        vm.originalConfiguration = data;
        vm.currentConfiguration = angular.copy(data);
        vm.statusMessage = PolicyViolationGrandfatheringService.getStatusMessage(vm.originalConfiguration);
      }).catch(function(error) {
        vm.loadError = Messages.getHttpErrorMessage(error);
      });
    },

    save() {
      delete vm.submitError;
      vm.violationGrandfatheringEditorMask.wrap(
          PolicyViolationGrandfatheringService.setGrandfathering(vm.currentConfiguration).then(vm.doLoad)
              .catch(function(error) {
                vm.submitError = Messages.getHttpErrorMessage(error);
              }));
    },

    isDirty() {
      return !angular.equals(vm.originalConfiguration, vm.currentConfiguration);
    }
  });

  $scope.$on('pageChangeStarted', function(event) {
    if (vm.isDirty()) {
      event.preventDefault();
    }
  });
}

PolicyViolationGrandfatheringEditorController.$inject = [
  '$scope', 'Messages', 'CLMContextLocations', 'policyViolationGrandfatheringService'
];
