/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, CLM */
(function() {
  'use strict';

  //the add controller, controlling the add modal
  function LabelAddController($scope, label, SelectedComponent, OwnerContext, messages, $http) {
    var component = SelectedComponent.get();
    $scope.displayName = component.displayName;

    //they accept, update the server
    $scope.accept = function() {
      $scope.labelSaving = true;
      $scope.labelAddError = null;
      var parts = $scope.label.selectedOwner.split('$$');
      $http.post(CLM.path + 'rest/label/component/' + parts[1] + '/' + parts[0] + '/' +
              component.hash, label).success(function() {
        $scope.$emit('reevaluate.component', {hash: component.hash});
        $scope.$close(label);
      }).error(function() {
        $scope.labelSaving = false;
        $scope.error = messages.getHttpErrorMessage(arguments);
      });
    };

    $scope.doLoad = function () {
      $scope.labelLoading = true;
      $scope.labelAddError = null;
      $scope.label = {
        selectedOwner: OwnerContext.ownerId + '$$application'
      };
      $scope.labelOwners = [];

      $http.get(CLM.path + 'rest/label/' + OwnerContext.ownerType + '/' + OwnerContext.ownerId + '/applicable/context/' +
              label.id).success(function(data) {
        $scope.labelLoading = false;
        function processItem(item) {
          $scope.labelOwners.push(item);
          angular.forEach(item.children, function(child) {
            processItem(child);
          });
        }

        processItem(data);
        $scope.labelOwners.reverse();
      }).error(function() {
        $scope.labelLoading = false;
        $scope.error = messages.getHttpErrorMessage(arguments);
      });
    };

    $scope.doLoad();
  }
  LabelAddController.$inject = ['$scope', 'label', 'SelectedComponent', 'OwnerContext', 'Messages', '$http'];

  angular.module('cip.label.editor').controller('LabelAddController', LabelAddController);
}());
