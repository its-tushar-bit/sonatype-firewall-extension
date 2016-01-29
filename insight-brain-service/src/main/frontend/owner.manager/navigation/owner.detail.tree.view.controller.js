/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function OwnerDetailTreeViewController($scope, $q, $http, $state, CLMLocations, CLMAppLocations, ApplicationStore,
                                         OrganizationStore, LocalRoleService)
  {
    var vm = this;

    vm.areAnyCategoriesDefined = undefined;
    vm.isApp = CLMAppLocations.isApplication();
    vm.state = $state;
    vm.ownerName = undefined;
    vm.details = undefined;
    vm.doLoad = doLoad;
    vm.rolesWithoutLocalMembersExist = undefined;
    vm.error = undefined;
    vm.accessState = {isExpanded: vm.state.$current.name.endsWith('access')};
    vm.categoryState = {isExpanded: vm.state.$current.name.endsWith('category')};
    vm.labelState = {isExpanded: vm.state.$current.name.endsWith('label')};
    vm.policyState = {isExpanded: vm.state.$current.name.endsWith('policy')};
    vm.ltgState = {isExpanded: vm.state.$current.name.endsWith('license-threat-group')};

    vm.doLoad();

    function doLoad() {
      var promises = [
        (vm.isApp ? ApplicationStore : OrganizationStore)[vm.error ? 'refresh' : 'get'](),
        $http.get(CLMAppLocations.getOwnerDetailsUrl())
      ];

      if (vm.isApp) {
        promises.push($http.get(CLMLocations.getApplicableOrganizationTags(CLMAppLocations.getEntityId())));
      }

      $q.all(promises).then(function(results) {
        results[0].some(function(candidate) {
          if (candidate[vm.isApp ? 'publicId' : 'id'] === CLMAppLocations.getEntityId()) {
            vm.ownerName = candidate.name;
            return true;
          }
        });

        vm.details = results[1].data;
        var allMembersByRoles = vm.details.roles.membersByRole;
        vm.details.roles = LocalRoleService.getRolesWithLocalMembers(allMembersByRoles);
        vm.rolesWithoutLocalMembersExist = LocalRoleService.getRolesWithoutLocalMembers(allMembersByRoles).length > 0;

        if (vm.isApp) {
          vm.areAnyCategoriesDefined = results[2].data.length > 0;
        }

        if (!vm.ownerName) {
          vm.error = 'Could not find an ' + (vm.isApp ? 'application' : 'organization') + ' with ID ' +
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
    '$scope', '$q', '$http', '$state', 'CLMLocations', 'CLMAppLocations', 'ApplicationStore', 'OrganizationStore',
    'local.role.service'
  ];

  angular //
      .module('owner.manager.module') //
      .controller('OwnerDetailTreeViewController', OwnerDetailTreeViewController);
}(angular));
