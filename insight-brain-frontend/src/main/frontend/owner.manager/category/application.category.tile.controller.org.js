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
import { selectIsOrganization } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectSelectedOwnerName } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';

export default function ApplicationCategoryTileControllerOrg($scope, EventNameConstant, $ngRedux) {
  var vm = this;

  vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
    loadApplicableCategories: actions.loadApplicableCategories,
    goToEditCategory: actions.goToEditCategory,
  })(vm);

  Object.assign(vm, {
    doLoad() {
      if (vm.isOrg) {
        vm.loadApplicableCategories();
      }
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

  vm.doLoad();

  $scope.$on('$destroy', function () {
    vm.unsubscribe();
  });
}

export const mapStateToThis = (state) => ({
  appCategoryOwners: angular.copy(selectAppCategoryOwners(state)),
  error: selectLoadError(state),
  loading: selectIsLoading(state),
  ownerName: selectSelectedOwnerName(state),
  isOrg: selectIsOrganization(state),
});

ApplicationCategoryTileControllerOrg.$inject = ['$scope', 'event.name.constant', '$ngRedux'];
