/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { actions as applicationsActions } from 'MainRoot/OrgsAndPolicies/applicationsSlice';
import { actions as assignApplicationCategoriesActions } from 'MainRoot/OrgsAndPolicies/assignApplicationCategoriesSlice';

import {
  selectCategories,
  selectIsDirty,
  selectLoadApplicableCategoriesError,
  selectLoadAppliedCategoriesError,
  selectLoadingApplicableCategories,
  selectLoadingAppliedCategories,
  selectSubmitApplyCategoriesError,
} from 'MainRoot/OrgsAndPolicies/assignApplicationCategoriesSelectors';
import { selectLoadApplicationsError, selectLoadingApplications } from 'MainRoot/OrgsAndPolicies/applicationsSelectors';

import { selectIsApplication } from 'MainRoot/reduxUiRouter/routerSelectors';
import { omit } from 'ramda';
import { selectOwnerName } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';

export default function ApplicationCategoryEditorController($scope, $ngRedux) {
  const vm = this;

  $scope.unsubscribe = $ngRedux.connect(mapStateToThis, {
    loadApplications: applicationsActions.loadApplicationsIfNeeded,
    loadApplicableCategories: assignApplicationCategoriesActions.loadApplicableCategories,
    loadAppliedCategories: assignApplicationCategoriesActions.loadAppliedCategories,
    updateAppliedCategories: assignApplicationCategoriesActions.updateAppliedCategories,
    saveAppliedCategories: assignApplicationCategoriesActions.saveAppliedCategories,
  })($scope);

  $scope.$on('pageChangeStarted', function (event) {
    if ($scope.areCategoriesDirty) {
      event.preventDefault();
    }
  });

  $scope.$on('$destroy', function () {
    $scope.unsubscribe();
  });

  $scope.doLoad = function () {
    if ($scope.isApp) {
      $scope.loadApplications();
      $scope.loadApplicableCategories();
      $scope.loadAppliedCategories();
    }
  };

  $scope.doLoad();

  $scope.save = function () {
    vm.categoryEditorMask.wrap(
      $scope.saveAppliedCategories({
        onSaveAppliedCategories: () => {
          vm.categoryEditor.$setPristine();
        },
      })
    );
  };

  $scope.onCategoriesChanged = function (category) {
    // The isApplied key is added by the selector and so it has to be removed here to mantain consistency with the data in the reducer
    // The $$hashKey key is added by the association-editor component
    $scope.updateAppliedCategories(omit(['$$hashKey', 'isApplied'])(category));
  };

  vm.categoryEditor = undefined;
  vm.categoryEditorMask = undefined;
}

export const mapStateToThis = (state) => ({
  ownerName: selectOwnerName(state),
  loading:
    selectLoadingApplications(state) ||
    selectLoadingApplicableCategories(state) ||
    selectLoadingAppliedCategories(state),
  loadError:
    selectLoadApplicationsError(state) ||
    selectLoadApplicableCategoriesError(state) ||
    selectLoadAppliedCategoriesError(state),
  categories: angular.copy(selectCategories(state)),
  areCategoriesDirty: selectIsDirty(state),
  isApp: selectIsApplication(state),
  submitError: selectSubmitApplyCategoriesError(state),
});

ApplicationCategoryEditorController.$inject = ['$scope', '$ngRedux'];
