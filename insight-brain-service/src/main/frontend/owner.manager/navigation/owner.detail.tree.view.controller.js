/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function OwnerDetailTreeViewController($scope, $q, $http, $state, CLMAppLocations, ApplicationStore,
                                         OrganizationStore)
  {
    var vm = this,
        isApp = CLMAppLocations.isApplication();

    vm.state = $state;
    vm.ownerName = undefined;
    vm.details = undefined;
    vm.doLoad = doLoad;
    vm.error = undefined;
    vm.categoryState = {isExpanded: vm.state.$current.name.endsWith('category')};
    vm.labelState = {isExpanded: vm.state.$current.name.endsWith('label')};

    vm.doLoad();

    function doLoad() {
      $q.all([
        (isApp ? ApplicationStore : OrganizationStore)[vm.error ? 'refresh' : 'get'](),
        $http.get(CLMAppLocations.getOwnerDetailsUrl())
      ]).then(function(results) {
        results[0].some(function(candidate) {
          if (candidate[isApp ? 'publicId' : 'id'] === CLMAppLocations.getEntityId()) {
            vm.ownerName = candidate.name;
            return true;
          }
        });

        vm.details = results[1].data;

        if (!vm.ownerName) {
          vm.error = 'Could not find an ' + (isApp ? 'application' : 'organization') + ' with ID ' +
              CLMAppLocations.getEntityId() + '.';
        }
      }, function(error) {
        vm.error = error;
      });

      delete vm.error;
    }

    $scope.$on('resource.data.modified', vm.doLoad);
  }

  OwnerDetailTreeViewController.$inject = [
    '$scope', '$q', '$http', '$state', 'CLMAppLocations', 'ApplicationStore', 'OrganizationStore'
  ];

  angular //
      .module('owner.manager.module') //
      .controller('OwnerDetailTreeViewController', OwnerDetailTreeViewController);
}(angular));
