/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function LabelTileController(
  $scope,
  $http,
  CLMContextLocations,
  SameOwnerStateNavigationService,
  EventNameConstant
) {
  var vm = this;
  vm.ownerName = undefined;
  vm.applicableLabels = undefined;
  vm.error = undefined;
  vm.doLoad = doLoad;
  vm.editLabel = editLabel;

  vm.doLoad();

  $scope.$on('policy.imported', doLoad);
  $scope.$on(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA, doLoad);
  $scope.$on(EventNameConstant.OWNER_UPDATED, updatedOwnerHandler);

  function doLoad() {
    $http.get(CLMContextLocations.getApplicableLabelsUrl()).then(
      function (result) {
        vm.applicableLabels = result.data.labelsByOwner;
        vm.applicableLabels.forEach(function (labels, index) {
          labels.inherited = index > 0;
        });

        vm.ownerName = vm.applicableLabels[0].ownerName;
      },
      function (error) {
        vm.error = error;
      }
    );

    delete vm.error;
  }

  function editLabel(labelId, inherited) {
    if (!inherited) {
      SameOwnerStateNavigationService.goEdit('label', { labelId: labelId });
    }
  }

  function updatedOwnerHandler(event, newOwner) {
    vm.ownerName = newOwner.name;
  }
}

LabelTileController.$inject = [
  '$scope',
  '$http',
  'CLMContextLocations',
  'SameOwnerStateNavigationService',
  'event.name.constant',
];
