/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './policyViolationGrandfatheringEditor.html';
import { selectIsGrandfatheringSupported } from 'MainRoot/productFeatures/productFeaturesSelectors';

export default {
  template,
  controller: PolicyViolationGrandfatheringEditorController,
  controllerAs: 'vm',
};

function PolicyViolationGrandfatheringEditorController(
  $scope,
  Messages,
  CLMContextLocations,
  PolicyViolationGrandfatheringService,
  $ngRedux
) {
  const vm = this;
  vm.unsubscribe = $ngRedux.connect(mapStateToThis)(vm);

  Object.assign(vm, {
    loadError: undefined,
    loading: false,
    submitError: undefined,
    originalConfiguration: undefined,
    currentConfiguration: undefined,
    statusMessage: undefined,
    violationGrandfatheringEditorMask: undefined,

    isApp: CLMContextLocations.isApplication(),
    isRootOrg: CLMContextLocations.isRootOrg(),

    doLoad() {
      delete vm.loadError;
      vm.loading = true;
      PolicyViolationGrandfatheringService.getGrandfathering().then(
        function (result) {
          vm.loading = false;
          vm.originalConfiguration = result;
          vm.currentConfiguration = angular.copy(result);
          vm.statusMessage = PolicyViolationGrandfatheringService.getStatusMessage(vm.originalConfiguration);
        },
        function (error) {
          vm.loading = false;
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

  vm.doLoad();

  $scope.$on('$destroy', () => {
    vm.unsubscribe();
  });

  $scope.$on('pageChangeStarted', function (event) {
    if (vm.isDirty()) {
      event.preventDefault();
    }
  });
}

export const mapStateToThis = (state) => ({
  isGrandfatheringSupported: selectIsGrandfatheringSupported(state),
});

PolicyViolationGrandfatheringEditorController.$inject = [
  '$scope',
  'Messages',
  'CLMContextLocations',
  'policyViolationGrandfatheringService',
  '$ngRedux',
];
