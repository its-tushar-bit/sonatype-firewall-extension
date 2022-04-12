/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { actions } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesLabelsSlice';
import { actions as rootActions } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesRootSlice';
import { selectOwnerName } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import {
  selectApplicableLabels,
  selectLabelsLoading,
  selectLabelsLoadError,
} from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesLabelsSelectors';

export default function LabelTileController($scope, EventNameConstant, $ngRedux) {
  const vm = this;

  vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
    updateOwnerName: rootActions.updatedOwnerHandler,
    loadApplicableLabels: actions.loadApplicableLabels,
    goToEditLabel: actions.goToEditLabel,
  })(vm);

  Object.assign(vm, {
    doLoad() {
      vm.loadApplicableLabels();
    },

    editLabel(labelId, inherited) {
      if (!inherited) {
        vm.goToEditLabel(labelId);
      }
    },

    updatedOwnerHandler(_, newOwner) {
      vm.updateOwnerName(newOwner.name);
    },
  });

  // TODO: next three lines should be migrated when appropriate piece of state is created
  $scope.$on(EventNameConstant.POLICY_IMPORTED, vm.doLoad);
  $scope.$on(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA, vm.doLoad);
  $scope.$on(EventNameConstant.OWNER_UPDATED, vm.updatedOwnerHandler);

  vm.doLoad();

  $scope.$on('$destroy', function () {
    vm.unsubscribe();
  });
}

export const mapStateToThis = (state) => ({
  applicableLabels: angular.copy(selectApplicableLabels(state)),
  error: selectLabelsLoadError(state),
  loading: selectLabelsLoading(state),
  ownerName: selectOwnerName(state),
});

LabelTileController.$inject = ['$scope', 'event.name.constant', '$ngRedux'];
