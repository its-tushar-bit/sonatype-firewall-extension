/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, CLM */
(function() {
  'use strict';

  //the remove controller, controlling the remove modal
  angular.module('cip.label.editor').controller('LabelRemoveController', [
    '$scope', '$http', 'label', 'SelectedComponent', 'Messages',
    function($scope, $http, label, SelectedComponent, messages) {
      //accept, send delete request to server
      $scope.accept = function() {
        $scope.labelDeleting = true;
        $scope.labelRemoveError = null;
        $http['delete'](CLM.path + 'rest/label/component/' + label.ownerType + '/' + label.ownerId + '/' +
                SelectedComponent.get().hash + '/' + label.id).success(function() {
          $scope.$close();
        }).error(function() {
          $scope.labelDeleting = false;
          $scope.labelRemoveError = messages.getHttpErrorMessage(arguments);
        });
      };
      $scope.labelRemoveError = null;
    }
  ]);
}());
