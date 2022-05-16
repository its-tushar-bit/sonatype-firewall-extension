/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { actions } from 'MainRoot/OrgsAndPolicies/labelsSlice';
import { selectSelectedOwnerName } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import {
  selectApplicableLabels,
  selectLabelsLoading,
  selectLabelsLoadError,
} from 'MainRoot/OrgsAndPolicies/labelsSelectors';

export default function LabelTileController($scope, EventNameConstant, $ngRedux) {
  const vm = this;

  vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
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
  });

  // TODO: next three lines should be migrated when appropriate piece of state is created
  $scope.$on(EventNameConstant.POLICY_IMPORTED, vm.doLoad);
  $scope.$on(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA, vm.doLoad);

  vm.doLoad();

  $scope.$on('$destroy', function () {
    vm.unsubscribe();
  });
}

export const mapStateToThis = (state) => ({
  applicableLabels: angular.copy(selectApplicableLabels(state)),
  error: selectLabelsLoadError(state),
  loading: selectLabelsLoading(state),
  ownerName: selectSelectedOwnerName(state),
});

LabelTileController.$inject = ['$scope', 'event.name.constant', '$ngRedux'];
