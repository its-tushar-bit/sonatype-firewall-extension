/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function ApplicationCategoryTileController($q, $http, ApplicationStore, CLMAppLocations, CLMLocations) {
    var vm = this;

    vm.appliedCategories = undefined;
    vm.doLoad = doLoad;
    vm.error = undefined;
    vm.isApp = CLMAppLocations.isApplication();
    vm.ownerName = undefined;

    vm.doLoad();

    function doLoad() {
      if (vm.isApp) {
        $q.all([
          ApplicationStore[vm.error ? 'refresh' : 'get'](),
          $http.get(CLMLocations.getApplicationTagUrl(CLMAppLocations.getEntityId()))
        ]).then(function(results) {
          results[0].forEach(function(candidate) {
            if (candidate.publicId === CLMAppLocations.getEntityId()) {
              vm.ownerName = candidate.name;
            }
          });

          vm.appliedCategories = results[1].data;

          if (!vm.ownerName) {
            vm.error = 'Could not find an application with ID ' + CLMAppLocations.getEntityId() + '.';
          }
        }, function() {
          vm.error = arguments;
        });
      }

      delete vm.error;
    }
  }

  ApplicationCategoryTileController.$inject = ['$q', '$http', 'ApplicationStore', 'CLMAppLocations', 'CLMLocations'];

  angular//
      .module('owner.manager.module')//
      .controller('ApplicationCategoryTileController', ApplicationCategoryTileController);

}(angular));
