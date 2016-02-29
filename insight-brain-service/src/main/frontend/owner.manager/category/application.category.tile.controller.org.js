/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function ApplicationCategoryTileControllerOrg($scope, $http, CLMAppLocations, SameOwnerStateNavigationService) {
    var vm = this;

    vm.appCategoryOwners = [];
    vm.doLoad = doLoad;
    vm.editCategory = editCategory;
    vm.error = undefined;
    vm.isOrg = CLMAppLocations.isOrganization();
    vm.ownerName = undefined;

    vm.doLoad();

    $scope.$on('policy.imported', doLoad);

    function doLoad() {
      if (vm.isOrg) {
        $http.get(CLMAppLocations.getTagsUrl()).then(function(result) {
          vm.appCategoryOwners = [];
          result.data.tagsByOwner.forEach(function(owner, index) {
            vm.appCategoryOwners.push(owner);

            if (index === 0) {
              vm.ownerName = owner.ownerName;
            }
            else {
              vm.appCategoryOwners[index].parent = true;
            }
          });
        }, function(error) {
          vm.error = error;
        });

        delete vm.error;
      }
    }

    function editCategory(categoryId, inherited) {
      if (!inherited) {
        SameOwnerStateNavigationService.goEdit('category', { categoryId: categoryId });
      }
    }
  }

  ApplicationCategoryTileControllerOrg.$inject = [
    '$scope', '$http', 'CLMAppLocations', 'SameOwnerStateNavigationService'
  ];

  angular //
      .module('owner.manager.module') //
      .controller('ApplicationCategoryTileControllerOrg', ApplicationCategoryTileControllerOrg);

}(angular));
