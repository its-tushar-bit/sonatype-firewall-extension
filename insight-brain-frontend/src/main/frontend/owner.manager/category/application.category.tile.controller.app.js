/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectLoadApplicationsError, selectLoadingApplications } from 'MainRoot/OrgsAndPolicies/applicationsSelectors';
import {
  selectAppliedCategories,
  selectAreAnyCategoriesDefined,
  selectLoadApplicableCategoriesError,
  selectLoadAppliedCategoriesError,
  selectLoadingApplicableCategories,
  selectLoadingAppliedCategories,
} from 'MainRoot/OrgsAndPolicies/assignApplicationCategoriesSelectors';
import { actions as applicationActions } from 'MainRoot/OrgsAndPolicies/applicationsSlice';
import { actions as assignApplicationCategoriesSlice } from 'MainRoot/OrgsAndPolicies/assignApplicationCategoriesSlice';
import { actions as orgsAndPoliciesRootActions } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesRootSlice';
import { selectOwnerName } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectIsApplication } from 'MainRoot/reduxUiRouter/routerSelectors';
export default function ApplicationCategoryTileControllerApp($scope, EventNameConstant, $ngRedux) {
  $scope.unsubscribe = $ngRedux.connect(mapStateToThis, {
    loadApplications: applicationActions.loadApplications,
    loadApplicableCategories: assignApplicationCategoriesSlice.loadApplicableCategories,
    loadAppliedCategories: assignApplicationCategoriesSlice.loadAppliedCategories,
    goToEditCategories: assignApplicationCategoriesSlice.goToEditCategories,
    updatedOwnerHandlerAction: orgsAndPoliciesRootActions.updatedOwnerHandler,
  })($scope);

  $scope.$on('$destroy', () => {
    $scope.unsubscribe();
  });

  $scope.doLoad = () => {
    if ($scope.isApp) {
      $scope.loadApplications();
      $scope.loadApplicableCategories();
      $scope.loadAppliedCategories();
    }
  };

  $scope.updatedOwnerHandler = (_, owner) => {
    $scope.updatedOwnerHandlerAction(owner.name);
  };

  $scope.$on('policy.imported', $scope.doLoad);
  $scope.$on(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA, $scope.doLoad);
  $scope.$on(EventNameConstant.OWNER_UPDATED, $scope.updatedOwnerHandler);

  $scope.assignCategories = () => {
    if ($scope.areAnyCategoriesDefined) {
      $scope.goToEditCategories();
    }
  };

  $scope.doLoad();
}

export const mapStateToThis = (state) => ({
  ownerName: selectOwnerName(state),
  loading:
    selectLoadingApplications(state) ||
    selectLoadingApplicableCategories(state) ||
    selectLoadingAppliedCategories(state),
  error:
    selectLoadApplicationsError(state) ||
    selectLoadApplicableCategoriesError(state) ||
    selectLoadAppliedCategoriesError(state),
  appliedCategories: angular.copy(selectAppliedCategories(state)),
  areAnyCategoriesDefined: selectAreAnyCategoriesDefined(state),
  isApp: selectIsApplication(state),
});

ApplicationCategoryTileControllerApp.$inject = ['$scope', 'event.name.constant', '$ngRedux'];
