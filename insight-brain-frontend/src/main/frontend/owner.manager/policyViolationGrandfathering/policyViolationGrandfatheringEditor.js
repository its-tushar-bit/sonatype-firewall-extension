/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './policyViolationGrandfatheringEditor.html';
import { selectIsGrandfatheringSupported } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { actions as policyViolationGrandfatheringActions } from 'MainRoot/OrgsAndPolicies/policyViolationGrandfatheringSlice';
import {
  selectGrandfatheringStatusMessage,
  selectLoading,
  selectLoadError,
} from 'MainRoot/OrgsAndPolicies/policyViolationGrandfatheringSelectors';

import { selectIsApplication, selectIsRootOrganization } from '../../reduxUiRouter/routerSelectors';
import { selectPolicyViolationGrandfatheringConfig } from '../../OrgsAndPolicies/policyViolationGrandfatheringSelectors';

export default {
  template,
  controller: PolicyViolationGrandfatheringEditorController,
  controllerAs: 'vm',
};

function PolicyViolationGrandfatheringEditorController(
  $scope,
  Messages,
  PolicyViolationGrandfatheringService,
  $ngRedux
) {
  const vm = this;
  vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
    getGrandfathering: policyViolationGrandfatheringActions.loadPolicyViolationGrandfathering,
  })(vm);

  Object.assign(vm, {
    submitError: undefined,
    violationGrandfatheringEditorMask: undefined,

    doLoad() {
      vm.getGrandfathering();
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
  loading: selectLoading(state),
  loadError: selectLoadError(state),
  originalConfiguration: selectPolicyViolationGrandfatheringConfig(state),
  currentConfiguration: angular.copy(selectPolicyViolationGrandfatheringConfig(state)),
  statusMessage: selectGrandfatheringStatusMessage(state),
  isApp: selectIsApplication(state),
  isRootOrg: selectIsRootOrganization(state),
});

PolicyViolationGrandfatheringEditorController.$inject = [
  '$scope',
  'Messages',
  'policyViolationGrandfatheringService',
  '$ngRedux',
];
