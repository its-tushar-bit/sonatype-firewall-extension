/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function OwnerDetailTreeViewController(
  $scope,
  $q,
  $http,
  $state,
  CLMLocations,
  CLMContextLocations,
  ApplicationStore,
  OrganizationStore,
  LocalRoleService,
  ProductFeatures
) {
  var vm = this;

  vm.areAnyCategoriesDefined = undefined;
  vm.isMonitoringSupported = undefined;
  vm.isGrandfatheringSupported = undefined;
  vm.isApp = CLMContextLocations.isApplication();
  vm.isOrg = CLMContextLocations.isOrganization();
  vm.isRepositories = CLMContextLocations.isRepositories();
  vm.state = $state;
  vm.ownerName = undefined;
  vm.details = undefined;
  vm.doLoad = doLoad;
  vm.rolesWithoutLocalMembersExist = undefined;
  vm.error = undefined;
  vm.accessState = { isExpanded: vm.state.$current.name.endsWith('access') };
  vm.categoryState = {
    isExpanded: vm.state.$current.name.endsWith('category'),
  };
  vm.labelState = { isExpanded: vm.state.$current.name.endsWith('label') };
  vm.policyState = { isExpanded: vm.state.$current.name.endsWith('policy') };
  vm.ltgState = {
    isExpanded: vm.state.$current.name.endsWith('license-threat-group'),
  };

  vm.doLoad();

  function doLoad() {
    var promises = [$http.get(CLMContextLocations.getOwnerDetailsUrl())];

    if (vm.isApp) {
      promises.push(ApplicationStore.getById(CLMContextLocations.getEntityId()));
      promises.push($http.get(CLMLocations.getApplicableOrganizationTags(CLMContextLocations.getEntityId())));
    } else if (!vm.isRepositories) {
      promises.push(OrganizationStore.getById(CLMContextLocations.getEntityId()));
    }

    promises.push(ProductFeatures.load());

    $q.all(promises).then(
      function (results) {
        vm.details = results[0].data;
        var allMembersByRoles = vm.details.roles.membersByRole;
        vm.details.roles = LocalRoleService.getRolesWithLocalMembers(allMembersByRoles);
        vm.rolesWithoutLocalMembersExist = LocalRoleService.getRolesWithoutLocalMembers(allMembersByRoles).length > 0;

        if (!vm.isRepositories) {
          vm.ownerName = results[1].name;

          if (vm.isApp) {
            vm.areAnyCategoriesDefined = results[2].data.length > 0;
          }
        } else {
          vm.ownerName = 'Repositories';
        }
        vm.isMonitoringSupported = ProductFeatures.isAvailable('policy-monitoring');
        vm.isGrandfatheringSupported = ProductFeatures.isAvailable('policy-grandfathering');
      },
      function (error) {
        vm.error = error;
      }
    );

    delete vm.error;
  }

  $scope.$on('resource.data.modified', vm.doLoad);
}

OwnerDetailTreeViewController.$inject = [
  '$scope',
  '$q',
  '$http',
  '$state',
  'CLMLocations',
  'CLMContextLocations',
  'ApplicationStore',
  'OrganizationStore',
  'local.role.service',
  'ProductFeatures',
];
