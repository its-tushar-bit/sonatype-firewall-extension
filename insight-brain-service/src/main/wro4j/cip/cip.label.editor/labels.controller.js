/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, CLM */
(function() {
  'use strict';

  //main label controller handling the main view, and launching the other modals when necessary
  function LabelsController($q, $http, $scope, LabelModification, SelectedComponent, OwnerContext, messages) {
    function errorFn() {
      $scope.alerts.length = 0;
      $scope.alerts.push({
        type: 'error',
        msg: messages.getHttpErrorMessage(arguments)
      });
    }

    function flattenLabelList(data) {
      var list = [];
      angular.forEach(data.labelsByOwner, function(labelOwner) {
        angular.forEach(labelOwner.labels, function(label) {
          label.ownerId = labelOwner.ownerId;
          label.ownerType = labelOwner.ownerType;
          label.ownerName = labelOwner.ownerName;
          list.push(label);
        });
      });
      return list;
    }

    $scope.doLoad = function () {
      $scope.itemLabels = $scope.availableLabels = undefined;
      $scope.error = undefined;

      var promises = [];
      promises.push($http.get(CLM.path + 'rest/label/component/' + OwnerContext.ownerType + '/' +
              OwnerContext.ownerId + '/' + SelectedComponent.get().hash));
      promises.push($http.get(CLM.path + 'rest/label/' + OwnerContext.ownerType + '/' + OwnerContext.ownerId +
              '/applicable'));

      $q.all(promises).then(function (results) {
        $scope.itemLabels = flattenLabelList(results[0].data);
        if (results.length > 1) {
          $scope.availableLabels = flattenLabelList(results[1].data);
        }
      }, function (error) {
        $scope.error = error;
      });
    };

    $scope.removeLabel = function(label) {
      LabelModification.remove(label).then(function () {
        $scope.doLoad();
      });
    };

    //for labels owned by the app, we simply do the add here, as there is no need to view the dialog to select the owner, app is the only option
    $scope.addLabel = function(label) {
      if (label.ownerType === 'application') {
        $http.post(CLM.path + 'rest/label/component/' + OwnerContext.ownerType + '/' + OwnerContext.ownerId + '/' +
                SelectedComponent.get().hash, label).success(function() {
          $scope.doLoad();
        }).error(errorFn);
      }
      else {
        LabelModification.add(label).then(function () {
          $scope.doLoad();
        });
      }
    };
    $scope.isWhite = function(label) {
      return label.color === 'green' || label.color === 'black' || label.color === 'orange' ||
          label.color === 'red' || label.color === 'blue';
    };
    $scope.isApplied = function(label) {
      var duplicate = false;
      angular.forEach($scope.itemLabels, function(candidate) {
        duplicate = duplicate || (candidate.label === label.label);
        return !duplicate;
      });
      return !duplicate;
    };
    $scope.alerts = [];
    $scope.ownerType = OwnerContext.ownerType;

    $scope.$watch(function () {
      return SelectedComponent.get();
    }, function (component) {
      if (component) {
        $scope.doLoad();
      }
    });
  }
  LabelsController.$inject = ['$q', '$http', '$scope', 'LabelModification', 'SelectedComponent', 'OwnerContext', 'Messages'];

  angular.module('cip.label.editor').controller('LabelsController', LabelsController);
}());
