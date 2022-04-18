/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectLoadError,
  selectIsLoading,
  selectAppCategoryOwners,
} from 'MainRoot/OrgsAndPolicies/createEditApplicationCategoriesSelectors';

import { actions } from 'MainRoot/OrgsAndPolicies/createEditApplicationCategoriesSlice';
import { actions as orgsAndPoliciesRootActions } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesRootSlice';
import { selectIsOrganization } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectOwnerName } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
export default function ApplicationCategoryTileControllerOrg($scope, EventNameConstant, $ngRedux) {
  var vm = this;

  vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
    loadApplicableCategories: actions.loadApplicableCategories,
    updateOwnerHandler: orgsAndPoliciesRootActions.updatedOwnerHandler,
    goToEditCategory: actions.goToEditCategory,
  })(vm);

  Object.assign(vm, {
    doLoad() {
      if (vm.isOrg) {
        vm.loadApplicableCategories();
      }
    },

    updatedOwnerHandler(_, newOwner) {
      vm.updateOwnerHandler(newOwner.name);
    },

    editCategory(categoryId, inherited) {
      if (!inherited) {
        vm.goToEditCategory(categoryId);
      }
    },
  });

  // TODO: next three lines should be migrated when appropriate peace of state is created
  $scope.$on(EventNameConstant.POLICY_IMPORTED, vm.doLoad);
  $scope.$on(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA, vm.doLoad);
  $scope.$on(EventNameConstant.OWNER_UPDATED, vm.updatedOwnerHandler);

  vm.doLoad();

  $scope.$on('$destroy', function () {
    vm.unsubscribe();
  });
}

export const mapStateToThis = (state) => ({
  appCategoryOwners: angular.copy(selectAppCategoryOwners(state)),
  error: selectLoadError(state),
  loading: selectIsLoading(state),
  ownerName: selectOwnerName(state),
  isOrg: selectIsOrganization(state),
});

ApplicationCategoryTileControllerOrg.$inject = ['$scope', 'event.name.constant', '$ngRedux'];
