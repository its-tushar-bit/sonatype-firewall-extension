/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global clmEndpoint */
export default function StatusController($scope, State, Coordinates, OwnerContext) {
  $scope.openView = function ($event, action) {
    $event.preventDefault();
    clmEndpoint.openView($scope, action);
  };

  $scope.$watch(
    function () {
      return State.get();
    },
    function (state) {
      $scope.state = state;
    }
  );

  $scope.$watch(
    function () {
      return State.getArgs();
    },
    function (stateArgs) {
      $scope.stateArgs = stateArgs;
    }
  );

  $scope.$watch(
    function () {
      return OwnerContext.ownerId;
    },
    function () {
      $scope.selectedApp = OwnerContext.ownerId;
    }
  );
}
StatusController.$inject = ['$scope', 'State', 'Coordinates', 'OwnerContext'];
