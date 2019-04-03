/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function RevokeGrandfatheringModalController($scope, $http, Messages, CLMLocations,
                                                            selectedApplication) {
  var vm = this;

  vm.revokeGrandfathering = revokeGrandfathering;
  vm.revokeGrandfatheringMask = undefined;
  vm.error = undefined;
  vm.applicationPublicId = selectedApplication.publicId;

  function revokeGrandfathering() {
    vm.revokeGrandfatheringMask.wrap(doSubmit()).then(function() {
      $scope.$dismiss();
    }, function(error) {
      vm.error = Messages.getHttpErrorMessage(error);
    });
  }

  function doSubmit() {
    return $http.put(CLMLocations.getRevokeGrandfatheringUrl(vm.applicationPublicId));
  }
}

RevokeGrandfatheringModalController.$inject = [
  '$scope', '$http', 'Messages', 'CLMLocations', 'selectedApplication'
];
