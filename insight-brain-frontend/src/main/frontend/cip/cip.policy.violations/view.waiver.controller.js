/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, CLM */

export default function ViewWaiverController(
  $scope,
  $http,
  OwnerContext,
  SelectedComponent,
  messages
) {
  function handleHttpError(error) {
    $scope.appError = messages.getHttpErrorMessage(error);
  }

  function doLoad() {
    $scope.waiversLoading = true;
    // get the waivers from the server
    $http
      .get(
        CLM.path +
          'rest/policyWaiver/' +
          OwnerContext.ownerType +
          '/' +
          OwnerContext.ownerId +
          '/component/' +
          SelectedComponent.get().hash
      )
      .then(function (response) {
        $scope.waiversLoading = false;
        $scope.waivers = [];
        angular.forEach(
          response.data.waiversByOwner,
          function (waiversByOwner) {
            angular.forEach(waiversByOwner.waivers, function (waiver) {
              waiver.type = waiversByOwner.ownerType;
              waiver.ownerName = waiversByOwner.ownerName;
              $scope.waivers.push(waiver);
            });
          }
        );
      }, handleHttpError);
  }

  doLoad();

  $scope.remove = function (waiver) {
    $scope.confirmDelete = waiver;
    $scope.appError = null;
  };

  $scope.removeWaiver = function () {
    var waiver = $scope.confirmDelete;
    $scope.confirmDelete = null;
    $scope.appError = null;
    $http['delete'](
      CLM.path +
        'api/v2/policyWaivers/' +
        waiver.type +
        '/' +
        waiver.ownerId +
        '/' +
        waiver.id
    ).then(function () {
      $scope.$emit(
        'reevaluate.component',
        waiver.hash ? { hash: waiver.hash } : null
      );
      $scope.waivers.splice($scope.waivers.indexOf(waiver), 1);
    }, handleHttpError);
  };
}
ViewWaiverController.$inject = [
  '$scope',
  '$http',
  'OwnerContext',
  'SelectedComponent',
  'Messages',
];
