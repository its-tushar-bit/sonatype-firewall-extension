/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default
function ApplicationCategoryTileControllerApp($scope, $q, $http, ApplicationStore, CLMContextLocations, CLMLocations,
                                              SameOwnerStateNavigationService, EventNameConstant) {
  var vm = this;

  vm.areAnyCategoriesDefined = undefined;
  vm.appliedCategories = undefined;
  vm.assignCategories = assignCategories;
  vm.doLoad = doLoad;
  vm.error = undefined;
  vm.isApp = CLMContextLocations.isApplication();
  vm.ownerName = undefined;

  vm.doLoad();

  $scope.$on('policy.imported', doLoad);
  $scope.$on(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA, doLoad);
  $scope.$on(EventNameConstant.OWNER_UPDATED, updatedOwnerHandler);

  function doLoad() {
    if (vm.isApp) {
      $q.all([
        ApplicationStore[vm.error ? 'refresh' : 'get'](),
        $http.get(CLMLocations.getApplicationTagUrl(CLMContextLocations.getEntityId())),
        $http.get(CLMLocations.getApplicableOrganizationTags(CLMContextLocations.getEntityId()))
      ]).then(function(results) {
        results[0].forEach(function(candidate) {
          if (candidate.publicId === CLMContextLocations.getEntityId()) {
            vm.ownerName = candidate.name;
          }
        });

        vm.appliedCategories = results[1].data;
        vm.areAnyCategoriesDefined = results[2].data.length > 0;

        if (!vm.ownerName) {
          vm.error = 'Could not find an application with ID ' + CLMContextLocations.getEntityId() + '.';
        }
      }, function(error) {
        vm.error = error;
      });
    }

    delete vm.error;
  }

  function assignCategories() {
    if (vm.areAnyCategoriesDefined) {
      SameOwnerStateNavigationService.goEdit('category');
    }
  }

  function updatedOwnerHandler(event, newOwner) {
    vm.ownerName = newOwner.name;
  }
}

ApplicationCategoryTileControllerApp.$inject = [
  '$scope', '$q', '$http', 'ApplicationStore', 'CLMContextLocations', 'CLMLocations',
  'SameOwnerStateNavigationService', 'event.name.constant'
];
