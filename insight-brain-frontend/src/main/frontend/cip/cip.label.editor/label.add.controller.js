/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, CLM */
import { pick } from 'ramda';

//the add controller, controlling the add modal
export default function LabelAddController(
  $scope,
  label,
  SelectedComponent,
  OwnerContext,
  messages,
  $http
) {
  var component = SelectedComponent.get();
  $scope.displayName = component.displayName;

  //they accept, update the server
  $scope.accept = function () {
    const parts = $scope.label.selectedOwner.split('$$'),
      payload = pick(['color', 'label', 'id', 'description', 'ownerId'], label);

    $scope.labelSaving = true;
    $scope.labelAddError = null;

    const url = `${CLM.path}rest/label/component/${parts[1]}/${parts[0]}/${component.hash}`;

    $http.post(url, payload).then(
      function () {
        $scope.$emit('reevaluate.component', { hash: component.hash });
        $scope.$close(label);
      },
      function (error) {
        $scope.labelSaving = false;
        $scope.labelAddError = messages.getHttpErrorMessage(error);
      }
    );
  };

  $scope.doLoad = function () {
    $scope.labelLoading = true;
    $scope.labelAddError = null;
    $scope.label = {
      selectedOwner: OwnerContext.ownerId + '$$' + OwnerContext.ownerType,
    };
    $scope.labelOwners = [];

    const url =
      `${CLM.path}api/v2/labels/${OwnerContext.ownerType}/` +
      `${OwnerContext.ownerId}/applicable/context/${label.id}`;

    $http.get(url).then(
      function (response) {
        $scope.labelLoading = false;
        function processItem(item) {
          $scope.labelOwners.push(item);
          angular.forEach(item.children, function (child) {
            processItem(child);
          });
        }

        processItem(response.data);
        $scope.labelOwners.reverse();
      },
      function (error) {
        $scope.labelLoading = false;
        $scope.labelAddError = messages.getHttpErrorMessage(error);
      }
    );
  };

  $scope.doLoad();
}
LabelAddController.$inject = [
  '$scope',
  'label',
  'SelectedComponent',
  'OwnerContext',
  'Messages',
  '$http',
];
