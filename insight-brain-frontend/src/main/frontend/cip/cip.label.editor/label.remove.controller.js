/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global CLM */
export default function LabelRemoveController($scope, $http, label, SelectedComponent, messages) {
  //accept, send delete request to server
  $scope.accept = function() {
    $scope.labelDeleting = true;
    $scope.labelRemoveError = null;
    $http['delete'](CLM.path + 'rest/label/component/' + label.ownerType + '/' + label.ownerId + '/' +
        SelectedComponent.get().hash + '/' + label.id).then(function() {
      $scope.$emit('reevaluate.component', {hash: SelectedComponent.get().hash});
      $scope.$close();
    }, function(error) {
      $scope.labelDeleting = false;
      $scope.labelRemoveError = messages.getHttpErrorMessage(error);
    });
  };
  $scope.labelRemoveError = null;
}

LabelRemoveController.$inject = ['$scope', '$http', 'label', 'SelectedComponent', 'Messages'];
