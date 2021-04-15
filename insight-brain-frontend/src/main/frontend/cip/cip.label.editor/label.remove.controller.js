/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global CLM */
export default function LabelRemoveController(
  $scope,
  $http,
  label,
  SelectedComponent,
  messages
) {
  //accept, send delete request to server
  $scope.accept = function () {
    $scope.labelDeleting = true;
    $scope.labelRemoveError = null;

    const url =
      `${CLM.path}rest/label/component/${label.ownerType}/${label.ownerId}/` +
      `${SelectedComponent.get().hash}/${label.id}`;

    $http['delete'](url).then(
      function () {
        $scope.$emit('reevaluate.component', {
          hash: SelectedComponent.get().hash,
        });
        $scope.$close();
      },
      function (error) {
        $scope.labelDeleting = false;
        $scope.labelRemoveError = messages.getHttpErrorMessage(error);
      }
    );
  };

  $scope.doLoad = function () {
    $scope.labelLoading = true;
    $scope.labelRemoveError = null;

    const url = `${CLM.path}api/v2/labels/${label.ownerType}/${label.ownerId}/applicable/context/${label.id}`;

    $http.get(url).then(
      function () {
        $scope.labelLoading = false;
      },
      function (error) {
        $scope.labelLoading = false;
        $scope.labelRemoveError = messages.getHttpErrorMessage(error);
      }
    );
  };

  $scope.doLoad();
}

LabelRemoveController.$inject = [
  '$scope',
  '$http',
  'label',
  'SelectedComponent',
  'Messages',
];
