/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function ApplicationCategoryTileControllerOrg($http, CLMAppLocations) {
    var vm = this;

    vm.appCategoryOwners = [];
    vm.doLoad = doLoad;
    vm.error = undefined;
    vm.isOrg = CLMAppLocations.isOrganization();
    vm.ownerName = undefined;

    vm.doLoad();

    function doLoad() {
      if (vm.isOrg) {
        $http.get(CLMAppLocations.getTagsUrl()).then(function(result) {
          result.data.tagsByOwner.forEach(function(owner, index) {
            vm.appCategoryOwners.push(owner);

            if (index === 0) {
              vm.ownerName = owner.ownerName;
            }
            else {
              vm.appCategoryOwners[index].parent = true;
            }
          });
        }, function() {
          vm.error = arguments;
        });

        delete vm.error;
      }
    }
  }

  ApplicationCategoryTileControllerOrg.$inject = ['$http', 'CLMAppLocations'];

  angular //
      .module('owner.manager.module') //
      .controller('ApplicationCategoryTileControllerOrg', ApplicationCategoryTileControllerOrg);

}(angular));
