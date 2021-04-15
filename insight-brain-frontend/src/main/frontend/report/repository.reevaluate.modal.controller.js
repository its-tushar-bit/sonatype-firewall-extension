/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular */
export default function RepositoryReEvaluateModalController(
  $scope,
  $http,
  $stateParams,
  CLMLocations,
  Messages
) {
  var vm = this;
  vm.error = undefined;
  vm.reEvaluatePolicy = reEvaluatePolicy;
  vm.reEvaluatePolicyMask = undefined;

  function reEvaluatePolicy() {
    delete vm.error;
    vm.reEvaluatePolicyMask
      .wrap(
        $http.post(
          CLMLocations.getRepositoryEvaluateUrl($stateParams.repositoryId)
        )
      )
      .then(
        function () {
          $scope.$close();
        },
        function (error) {
          vm.error = Messages.getHttpErrorMessage(error);
        }
      );
  }
}

RepositoryReEvaluateModalController.$inject = [
  '$scope',
  '$http',
  '$stateParams',
  'CLMLocations',
  'Messages',
];
