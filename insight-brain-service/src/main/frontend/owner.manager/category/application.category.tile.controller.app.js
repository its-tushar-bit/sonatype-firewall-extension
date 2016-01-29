/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function ApplicationCategoryTileControllerApp($scope, $q, $http, ApplicationStore, CLMAppLocations, CLMLocations,
                                                SameOwnerStateNavigationService)
  {
    var vm = this;

    vm.areAnyCategoriesDefined = undefined;
    vm.appliedCategories = undefined;
    vm.associateCategories = associateCategories;
    vm.doLoad = doLoad;
    vm.error = undefined;
    vm.isApp = CLMAppLocations.isApplication();
    vm.ownerName = undefined;

    vm.doLoad();

    $scope.$on('policy.imported', doLoad);

    function doLoad() {
      if (vm.isApp) {
        $q.all([
          ApplicationStore[vm.error ? 'refresh' : 'get'](),
          $http.get(CLMLocations.getApplicationTagUrl(CLMAppLocations.getEntityId())),
          $http.get(CLMLocations.getApplicableOrganizationTags(CLMAppLocations.getEntityId()))
        ]).then(function(results) {
          results[0].forEach(function(candidate) {
            if (candidate.publicId === CLMAppLocations.getEntityId()) {
              vm.ownerName = candidate.name;
            }
          });

          vm.appliedCategories = results[1].data;
          vm.areAnyCategoriesDefined = results[2].data.length > 0;

          if (!vm.ownerName) {
            vm.error = 'Could not find an application with ID ' + CLMAppLocations.getEntityId() + '.';
          }
        }, function() {
          vm.error = arguments;
        });
      }

      delete vm.error;
    }

    function associateCategories() {
      if (vm.areAnyCategoriesDefined) {
        SameOwnerStateNavigationService.goEdit('category');
      }
    }
  }

  ApplicationCategoryTileControllerApp.$inject = [
    '$scope', '$q', '$http', 'ApplicationStore', 'CLMAppLocations', 'CLMLocations', 'SameOwnerStateNavigationService'
  ];

  angular //
      .module('owner.manager.module') //
      .controller('ApplicationCategoryTileControllerApp', ApplicationCategoryTileControllerApp);

}(angular));
