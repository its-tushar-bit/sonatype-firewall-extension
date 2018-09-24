/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export default function CIPController($scope, $window, OwnerContext, Coordinates) {
  $scope.canLoad = function() {
    return !$scope.selectApplication || OwnerContext.ownerId;
  };
  $scope.linkTarget = ($window.clmEndpoint && $window.clmEndpoint.linkTarget) || '_blank';

  // Reset the selected coordinates when switching CIP tabs
  $scope.$on('$destroy', function() {
    Coordinates.setSelected(Coordinates.get());
  });
}

CIPController.$inject = ['$scope', '$window', 'OwnerContext', 'Coordinates'];
