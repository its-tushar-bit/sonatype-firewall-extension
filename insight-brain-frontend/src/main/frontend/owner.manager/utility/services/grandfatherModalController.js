/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function GrandfatherModalController($scope, $http, Messages, CLMLocations, selectedApplication) {
  const vm = this;

  Object.assign(vm, {
    grandfatherMask: undefined,
    error: undefined,
    applicationPublicId: selectedApplication.publicId,

    grandfather() {
      vm.grandfatherMask.wrap(vm.doSubmit()).then(
        function () {
          $scope.$dismiss();
        },
        function (error) {
          vm.error = Messages.getHttpErrorMessage(error);
        }
      );
    },

    doSubmit() {
      return $http.put(CLMLocations.getGrandfatherUrl(vm.applicationPublicId));
    },
  });
}

GrandfatherModalController.$inject = ['$scope', '$http', 'Messages', 'CLMLocations', 'selectedApplication'];
