/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { actions } from 'MainRoot/OrgsAndPolicies/licenseThreatGroupSlice';
import { actions as rootActions } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesRootSlice';
import { selectOwnerName } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import {
  selectLicenseThreatGroupLoadError,
  selectApplicableLicenseThreatGroup,
  selectIsLoading,
} from 'MainRoot/OrgsAndPolicies/licenseThreatGroupSelectors';
import { selectIsOrganization } from 'MainRoot/reduxUiRouter/routerSelectors';

export default function LicenseThreatGroupTileController($scope, $state, EventNameConstant, $ngRedux) {
  const vm = this;
  vm.doLoad = doLoadFunction;
  vm.editLTG = editLTG;
  vm.updatedOwnerHandler = updatedOwnerHandler;

  vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
    updateOwnerName: rootActions.updatedOwnerHandler,
    loadApplicableLicenseGroups: actions.loadApplicableLicenseThreatGroups,
    goToEditLTG: actions.goToEditLTG,
  })(vm);

  //TODO: next three lines should be migrated when appropriate piece of state is created
  $scope.$on('policy.imported', vm.doLoad);
  $scope.$on(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA, vm.doLoad);
  $scope.$on(EventNameConstant.OWNER_UPDATED, vm.updatedOwnerHandler);
  $scope.$on('$destroy', () => {
    vm.unsubscribe();
  });

  vm.doLoad();

  function doLoadFunction() {
    vm.loadApplicableLicenseGroups($state);
  }

  function editLTG(licenseThreatGroupId, isInherited) {
    if (!isInherited) {
      vm.goToEditLTG(licenseThreatGroupId);
    }
  }

  function updatedOwnerHandler(_, newOwner) {
    vm.updateOwnerName(newOwner.name);
  }
}

export const mapStateToThis = (state) => ({
  applicableLicenseGroups: angular.copy(selectApplicableLicenseThreatGroup(state)),
  error: selectLicenseThreatGroupLoadError(state),
  loading: selectIsLoading(state),
  ownerName: selectOwnerName(state),
  isOrg: selectIsOrganization(state),
});

LicenseThreatGroupTileController.$inject = ['$scope', '$state', 'event.name.constant', '$ngRedux'];
