/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function AccessTileController($http, CLMAppLocations) {
    var vm = this;
    vm.ownerName = undefined;
    vm.membersByRole = undefined;
    vm.error = undefined;
    vm.doLoad = doLoad;
    vm.filterRolesWithMembers = filterRolesWithMembers;

    vm.doLoad();

    function doLoad() {
      $http.get(CLMAppLocations.getRoleMappingUrl()).then(function(results) {
        vm.membersByRole = results.data.membersByRole;
        vm.membersByRole.forEach(function(role) {
          role.membersByOwner.forEach(function(memberOwner, index) {
            memberOwner.inherited = index > 0;
            if (memberOwner.members.length > 0) {
              vm.membersByRole[0].membersByOwner[index].hasMembers = true;
            }
          });
        });

        vm.ownerName = vm.membersByRole[0].membersByOwner[0].ownerName;
      }, function() {
        vm.error = arguments[0];
      });

      delete vm.error;
    }

    function filterRolesWithMembers(index) {
      return function(role) {
        return role.membersByOwner[index].members.length > 0;
      };
    }
  }

  AccessTileController.$inject = ['$http', 'CLMAppLocations'];

  angular //
      .module('owner.manager.module') //
      .controller('AccessTileController', AccessTileController);
}(angular));
