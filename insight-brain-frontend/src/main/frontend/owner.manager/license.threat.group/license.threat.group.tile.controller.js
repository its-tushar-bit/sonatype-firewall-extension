/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function LicenseThreatGroupTileController(
  $scope,
  $http,
  CLMContextLocations,
  SameOwnerStateNavigationService,
  EventNameConstant
) {
  var vm = this;
  vm.ownerName = undefined;
  vm.applicableLicenseGroups = undefined;
  vm.editLTG = editLTG;
  vm.error = undefined;
  vm.doLoad = doLoad;

  vm.doLoad();

  $scope.$on('policy.imported', doLoad);
  $scope.$on(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA, doLoad);
  $scope.$on(EventNameConstant.OWNER_UPDATED, updatedOwnerHandler);

  function doLoad() {
    $http.get(CLMContextLocations.getApplicableLicenseGroupsUrl()).then(
      function (results) {
        vm.applicableLicenseGroups = results.data.licenseThreatGroupsByOwner;
        vm.applicableLicenseGroups.forEach(function (applicableLicenseGroup, index) {
          applicableLicenseGroup.inherited = index > 0;
        });

        vm.ownerName = vm.applicableLicenseGroups[0].ownerName;
        vm.isOrg = vm.applicableLicenseGroups[0].ownerType === 'organization';
      },
      function (error) {
        vm.error = error;
      }
    );

    delete vm.error;
  }

  function editLTG(licenseThreatGroupId, isInherited) {
    if (!isInherited) {
      SameOwnerStateNavigationService.goEdit('edit-license-threat-group', {
        licenseThreatGroupId: licenseThreatGroupId,
      });
    }
  }

  function updatedOwnerHandler(event, newOwner) {
    vm.ownerName = newOwner.name;
  }
}

LicenseThreatGroupTileController.$inject = [
  '$scope',
  '$http',
  'CLMContextLocations',
  'SameOwnerStateNavigationService',
  'event.name.constant',
];
