/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './policyViolationGrandfatheringEditor.html';

export default {
  template,
  controller: PolicyViolationGrandfatheringEditorController,
  controllerAs: 'vm',
};

function PolicyViolationGrandfatheringEditorController(
  $scope,
  $q,
  Messages,
  CLMContextLocations,
  PolicyViolationGrandfatheringService,
  ProductFeatures
) {
  const vm = this;

  Object.assign(vm, {
    loadError: undefined,
    submitError: undefined,
    originalConfiguration: undefined,
    currentConfiguration: undefined,
    statusMessage: undefined,
    violationGrandfatheringEditorMask: undefined,
    isGrandfatheringSupported: undefined,

    isApp: CLMContextLocations.isApplication(),
    isRootOrg: CLMContextLocations.isRootOrg(),

    $onInit() {
      vm.doLoad();
    },

    doLoad() {
      delete vm.loadError;
      const promises = [PolicyViolationGrandfatheringService.getGrandfathering(), ProductFeatures.load()];

      $q.all(promises).then(
        function (results) {
          vm.originalConfiguration = results[0];
          vm.currentConfiguration = angular.copy(results[0]);
          vm.statusMessage = PolicyViolationGrandfatheringService.getStatusMessage(vm.originalConfiguration);
          vm.isGrandfatheringSupported = ProductFeatures.isAvailable('policy-grandfathering');
        },
        function (error) {
          vm.loadError = Messages.getHttpErrorMessage(error);
        }
      );
    },

    save() {
      delete vm.submitError;
      vm.violationGrandfatheringEditorMask.wrap(
        PolicyViolationGrandfatheringService.setGrandfathering(vm.currentConfiguration)
          .then(vm.doLoad)
          .catch(function (error) {
            vm.submitError = Messages.getHttpErrorMessage(error);
          })
      );
    },

    isDirty() {
      return !angular.equals(vm.originalConfiguration, vm.currentConfiguration);
    },
  });

  $scope.$on('pageChangeStarted', function (event) {
    if (vm.isDirty()) {
      event.preventDefault();
    }
  });
}

PolicyViolationGrandfatheringEditorController.$inject = [
  '$scope',
  '$q',
  'Messages',
  'CLMContextLocations',
  'policyViolationGrandfatheringService',
  'ProductFeatures',
];
