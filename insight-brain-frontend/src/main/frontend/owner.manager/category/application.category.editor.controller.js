/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { omit, prop } from 'ramda';

export default function ApplicationCategoryEditorController(
  $scope,
  $q,
  $http,
  ApplicationStore,
  CLMContextLocations,
  CLMLocations,
  PolicyHierarchyStore
) {
  var originalCategoryArray,
    vm = this;

  vm.doLoad = doLoad;
  vm.save = save;
  vm.loadError = undefined;
  vm.submitError = undefined;
  vm.submitErrorMessage = undefined;
  vm.categories = undefined;
  vm.isApp = CLMContextLocations.isApplication();
  vm.ownerName = undefined;
  vm.categoryEditor = undefined;
  vm.categoryEditorMask = undefined;
  vm.areCategoriesDirty = areCategoriesDirty;

  vm.doLoad();

  $scope.$on('pageChangeStarted', function (event) {
    if (vm.areCategoriesDirty()) {
      event.preventDefault();
    }
  });

  function doLoad() {
    if (vm.isApp) {
      $q.all([
        ApplicationStore[vm.loadError ? 'refresh' : 'get'](),
        $http.get(
          CLMLocations.getApplicableOrganizationTags(
            CLMContextLocations.getEntityId()
          )
        ),
        $http.get(
          CLMLocations.getApplicationTagUrl(CLMContextLocations.getEntityId())
        ),
      ]).then(
        function (results) {
          var organizationCategories = results[1].data,
            applicationCategories = results[2].data;
          vm.categories = [];

          results[0].some(function (candidate) {
            if (candidate.publicId === CLMContextLocations.getEntityId()) {
              vm.ownerName = candidate.name;
              return true;
            }
          });

          organizationCategories.forEach(function (organizationCategory) {
            organizationCategory.isApplied = false;
            if (
              applicationCategories.some(function (appliedCategory) {
                return appliedCategory.id === organizationCategory.id;
              })
            ) {
              organizationCategory.isApplied = true;
            }
            vm.categories.push(organizationCategory);
          });

          originalCategoryArray = angular.copy(vm.categories);

          if (!vm.ownerName) {
            vm.loadError =
              'Could not find an application with ID ' +
              CLMContextLocations.getEntityId() +
              '.';
          }
        },
        function (error) {
          vm.loadError = error;
        }
      );
    }

    delete vm.loadError;
  }

  function save() {
    delete vm.submitError;

    var appliedCategories = vm.categories
      .filter(prop('isApplied'))
      .map(omit(['isApplied']));

    vm.categoryEditorMask
      .wrap(
        $http.put(
          CLMLocations.getApplicationTagUrl(CLMContextLocations.getEntityId()),
          appliedCategories
        )
      )
      .then(
        function () {
          originalCategoryArray = angular.copy(vm.categories);
          vm.categoryEditor.$setPristine();
          // policies may now be (un)inherited due to the new associations
          PolicyHierarchyStore.refresh();
        },
        function (error) {
          vm.submitError = error;
        }
      );
  }

  function areCategoriesDirty() {
    return !angular.equals(originalCategoryArray, vm.categories);
  }
}

ApplicationCategoryEditorController.$inject = [
  '$scope',
  '$q',
  '$http',
  'ApplicationStore',
  'CLMContextLocations',
  'CLMLocations',
  'PolicyHierarchyStore',
];
