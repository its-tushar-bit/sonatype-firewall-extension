/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';
  function LicenseThreatGroupTileController($scope, $http, CLMAppLocations, SameOwnerStateNavigationService) {
    var vm = this;
    vm.ownerName = undefined;
    vm.applicableLicenseGroups = undefined;
    vm.editLTG = editLTG;
    vm.error = undefined;
    vm.doLoad = doLoad;

    vm.doLoad();

    $scope.$on('policy.imported', doLoad);

    function doLoad() {
      $http.get(CLMAppLocations.getApplicableLicenseGroupsUrl()).then(function(results) {
        vm.applicableLicenseGroups = results.data.licenseThreatGroupsByOwner;
        vm.applicableLicenseGroups.forEach(function(applicableLicenseGroup, index) {
          applicableLicenseGroup.inherited = index > 0;
        });

        vm.ownerName = vm.applicableLicenseGroups[0].ownerName;
      }, function(error) {
        vm.error = error;
      });

      delete vm.error;
    }

    function editLTG(licenseThreatGroupId, isInherited) {
      if (!isInherited) {
        SameOwnerStateNavigationService.goEdit('edit-license-threat-group', {licenseThreatGroupId: licenseThreatGroupId});
      }
    }
  }

  LicenseThreatGroupTileController.$inject = ['$scope', '$http', 'CLMAppLocations', 'SameOwnerStateNavigationService'];

  angular.module('owner.manager.module').controller('LicenseThreatGroupTileController', LicenseThreatGroupTileController);

}(angular));
