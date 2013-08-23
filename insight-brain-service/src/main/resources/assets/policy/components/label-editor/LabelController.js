/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp */
(function () {
  'use strict';

  var labelTemplate = {id: null, ownerId: null, label: '', labelLowercase: null, color: null, description: null};

  var labelModule = angular.module('Labels', ['AngularCommon', 'CLMAppLocation', 'CommonServices', 'ResourceModule']);

  labelModule.service('LabelStore',['CLMResource', 'CLMAppLocations', function(CLMResource, CLMAppLocations){
    var labelStore = null, labelStores = {};

    function refreshLabelStore() {
      var entityId = CLMAppLocations.getEntityId();
      labelStore = labelStores[entityId];
      if (!labelStore) {
        labelStore = labelStores[entityId] = CLMResource.getStore(angular.extend({ url: CLMAppLocations.getLabelsUrl() }, labelStoreTemplate));
      }
    }

    var labelStoreTemplate = {
      template: labelTemplate,
      params: {
        timestamp: new Date().getTime()
      }
    };

    return {
      get: function () {
        refreshLabelStore();
        return labelStore.get();
      },
      refresh: function () {
        refreshLabelStore();
        return labelStore.refresh();
      },
      create: function () {
        return labelStore.create();
      }
    };
  }]);

  labelModule.controller('LabelController', ['$scope', '$http', '$q', 'CLMAppLocations', 'Messages', 'CLMResource', 'LabelStore',
                                             function ($scope, $http, $q, clmAppLocations, messages, clmResource, LabelStore) {
      function deselect() {
        if ($scope.selectedLabel) {
          $scope.selectedLabel.$revert();
        }
        $scope.selectedLabel = null;
        $scope.submitActive = false;
        $scope.alerts.length = 0;
      }

      function isDirty() {
        return $scope.selectedLabel && $scope.selectedLabel.isDirty();
      }

      $scope.deselect = deselect;

      $scope.doLoad = function () {
        $scope.error = null;
        var promises = [LabelStore.get(), $http.get(clmAppLocations.getApplicableLabelsUrl(), {
          params: { timestamp: new Date().getTime() }
        })];

        $q.all(promises).then(function(results){
          $scope.applicableLabels = results[1].data.labelsByOwner;
          angular.forEach($scope.applicableLabels, function (applicableLabel, index) {
            applicableLabel.editable = index === 0;
            if (index === 0) {
              applicableLabel.labels = results[0];
            }
          });
        }, function(error){
          $scope.error = error;
        });
      };

      $scope.editLabel = function (isEditable, label) {
        if (!isEditable) {
          return;
        }
        if (isDirty()) {
          $scope.alerts.push({
            type: 'error',
            msg: 'Please finish editing before trying to modify another label.'
          });
        } else {
          deselect();
          $scope.selectedLabel = label.$clone();
        }
      };

      $scope.createNew = function () {
        if (isDirty()) {
          $scope.alerts.push({
            type: 'error',
            msg: 'Please finish editing before trying to create a label.'
          });
        } else {
          $scope.label = LabelStore.create();
          $scope.selectedLabel = $scope.label;
        }
      };

      $scope.deleteLabel = function (label) {
        label.$delete().then(function(){
          deselect();
        }, function(error){
          deselect();
          $scope.alerts.push({
            type: 'error',
            msg: 'An error occurred while deleting the label. (' +
                messages.getHttpErrorMessage({ status: error.status, data: error.data}) + ')'
          });
        });
      };

      $scope.$on('labels.cancelEditLabel', function (event, label) {
              event.stopPropagation();
              deselect();
      });

      $scope.doLoad();
    }]);

    labelModule.controller('LabelEditorController', ['$scope', '$http', 'CLMAppLocations', 'Messages', 'LabelStore',
                                                     function ($scope, $http, clmAppLocations, messages, LabelStore) {
      function errorFn(error) {
        $scope.submitActive = false;
        $scope.alerts.push({
          type: 'error',
          msg: 'An error occurred while saving the label. (' +
              messages.getHttpErrorMessage({ status: error.status, data: error.data}) + ')'
        });
      }
        $scope.colors = [null, 'white', 'grey', 'black', 'green', 'yellow', 'orange', 'red', 'blue'];

        $scope.setColor = function (color) {
            $scope.selectedLabel.color = color;
        };

        $scope.cancelEditLabel = function () {
                $scope.$emit('labels.cancelEditLabel');
        };

        $scope.canSaveEdit = function (valid, label) {
            return valid && !$scope.submitActive && label != null && label.label;
        };

        $scope.$on('pageChangeStarted', function (event) {
            if ($scope.applicableLabel && $scope.selectedLabel && $scope.selectedLabel.id) {
                angular.forEach($scope.applicableLabel.labels, function (candidate) {
                    if (candidate.isDirty()) {
                        event.preventDefault();
                      return false;
                    }
                });
            } else if ($scope.selectedLabel && $scope.selectedLabel.label) {
                event.preventDefault();
            }
        });

      $scope.saveLabel = function () {
        $scope.submitActive = true;
        $scope.selectedLabel.$save().then(function (label) {
          $scope.deselect();
        }, errorFn);
      };
    }]);

  labelModule.directive('itemLabel', function () {
    return {
      require: 'ngModel',
      link: function (scope, element, attrs, ctrl) {
        ctrl.$parsers.unshift(function (newValue) {
          var unique = true,
              notEmpty = newValue.length !== 0,
              notInvalid = newValue.indexOf(' ') === -1 && newValue.indexOf('\t') === -1;
          ctrl.$setValidity('empty', notEmpty);

          angular.forEach(scope.labels, function (item, key) {
            if (item.id !== scope.selectedLabel.id) {
              unique = unique && (item.label.toLowerCase() !== newValue.toLowerCase());
            }
          });
          ctrl.$setValidity('duplicate', unique);
          ctrl.$setValidity('invalid', notInvalid);

          return (notEmpty && unique && notInvalid) ? newValue : undefined;
        });
      }
    };
  });
}());
