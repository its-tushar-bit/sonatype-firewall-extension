/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function ApplicationCategoryTileControllerOrg(
  $scope,
  CLMContextLocations,
  SameOwnerStateNavigationService,
  TagStore,
  EventNameConstant
) {
  var vm = this;

  vm.appCategoryOwners = [];
  vm.doLoad = doLoad;
  vm.editCategory = editCategory;
  vm.error = undefined;
  vm.isOrg = CLMContextLocations.isOrganization();
  vm.ownerName = undefined;

  vm.doLoad();

  $scope.$on('policy.imported', function () {
    doLoad(true);
  });
  $scope.$on(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA, function () {
    doLoad(true);
  });
  $scope.$on(EventNameConstant.OWNER_UPDATED, updatedOwnerHandler);

  function doLoad(reload) {
    if (vm.isOrg) {
      (reload ? TagStore.refresh() : TagStore.get()).then(
        function (tagsByOwner) {
          vm.appCategoryOwners = [];
          tagsByOwner.forEach(function (owner, index) {
            vm.appCategoryOwners.push(owner);

            if (index === 0) {
              vm.ownerName = owner.ownerName;
            } else {
              vm.appCategoryOwners[index].parent = true;
            }
          });
        },
        function (error) {
          vm.error = error;
        }
      );

      delete vm.error;
    }
  }

  function editCategory(categoryId, inherited) {
    if (!inherited) {
      SameOwnerStateNavigationService.goEdit('category', {
        categoryId: categoryId,
      });
    }
  }

  function updatedOwnerHandler(event, newOwner) {
    vm.ownerName = newOwner.name;
  }
}

ApplicationCategoryTileControllerOrg.$inject = [
  '$scope',
  'CLMContextLocations',
  'SameOwnerStateNavigationService',
  'TagStore',
  'event.name.constant',
];
