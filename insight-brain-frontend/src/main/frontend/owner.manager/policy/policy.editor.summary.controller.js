/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectLoading,
  selectLoadError,
  selectCurrentPolicy,
  selectIsOrgOwner,
} from 'MainRoot/OrgsAndPolicies/policySelectors';
import { selectLoading as selectOwnerDetailTreeLoading } from 'MainRoot/OrgsAndPolicies/ownerDetailTreeSelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/policySlice';
export default function PolicyEditorSummaryController($scope, $ngRedux) {
  var vm = this;

  vm.unsubscribe = $ngRedux.connect(mapStateToThis, { loadPolicyEditor: actions.loadPolicyEditor })(vm);

  $scope.$on('$destroy', function () {
    vm.unsubscribe();
  });
}

export const mapStateToThis = (state) => ({
  loading: selectLoading(state),
  loadError: selectLoadError(state),
  dirtyPolicy: selectCurrentPolicy(state),
  ownerDetailTreeLoading: selectOwnerDetailTreeLoading(state),
  isOrgOwner: selectIsOrgOwner(state),
});

PolicyEditorSummaryController.$inject = ['$scope', '$ngRedux'];
