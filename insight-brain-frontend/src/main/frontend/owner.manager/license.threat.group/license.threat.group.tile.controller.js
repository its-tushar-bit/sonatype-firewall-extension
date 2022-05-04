/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectSelectedOwnerName } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';

export default function LicenseThreatGroupTileController(
  $scope,
  $http,
  CLMContextLocations,
  SameOwnerStateNavigationService,
  EventNameConstant,
  $ngRedux
) {
  var vm = this;

  vm.unsubscribe = $ngRedux.connect(mapStateToThis)(vm);

  vm.applicableLicenseGroups = undefined;
  vm.editLTG = editLTG;
  vm.error = undefined;
  vm.doLoad = doLoad;

  vm.doLoad();

  $scope.$on('policy.imported', doLoad);
  $scope.$on(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA, doLoad);

  $scope.$on('$destroy', () => {
    vm.unsubscribe();
  });

  function doLoad() {
    $http.get(CLMContextLocations.getApplicableLicenseGroupsUrl()).then(
      function (results) {
        vm.applicableLicenseGroups = results.data.licenseThreatGroupsByOwner;
        vm.applicableLicenseGroups.forEach(function (applicableLicenseGroup, index) {
          applicableLicenseGroup.inherited = index > 0;
        });

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
}

export const mapStateToThis = (state) => ({
  ownerName: selectSelectedOwnerName(state),
});

LicenseThreatGroupTileController.$inject = [
  '$scope',
  '$http',
  'CLMContextLocations',
  'SameOwnerStateNavigationService',
  'event.name.constant',
  '$ngRedux',
];
