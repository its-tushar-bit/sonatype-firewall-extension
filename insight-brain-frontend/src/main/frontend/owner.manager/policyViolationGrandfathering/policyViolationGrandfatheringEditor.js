/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './policyViolationGrandfatheringEditor.html';
import { actions as productFeaturesActions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { selectIsGrandfatheringSupported } from 'MainRoot/productFeatures/productFeaturesSelectors';

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
  $ngRedux
) {
  const vm = this;
  vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
    loadProductFeatures: productFeaturesActions.fetchProductFeaturesIfNeeded,
  })(vm);

  Object.assign(vm, {
    loadError: undefined,
    submitError: undefined,
    originalConfiguration: undefined,
    currentConfiguration: undefined,
    statusMessage: undefined,
    violationGrandfatheringEditorMask: undefined,

    isApp: CLMContextLocations.isApplication(),
    isRootOrg: CLMContextLocations.isRootOrg(),

    doLoad() {
      delete vm.loadError;
      const promises = [PolicyViolationGrandfatheringService.getGrandfathering(), vm.loadProductFeatures()];

      $q.all(promises).then(
        function (results) {
          vm.originalConfiguration = results[0];
          vm.currentConfiguration = angular.copy(results[0]);
          vm.statusMessage = PolicyViolationGrandfatheringService.getStatusMessage(vm.originalConfiguration);
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
  '$q',
  'Messages',
  'CLMContextLocations',
  'policyViolationGrandfatheringService',
  '$ngRedux',
];
