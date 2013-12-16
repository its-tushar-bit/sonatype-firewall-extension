/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp */
(function() {
  'use strict';

  var labelTemplate = {id: null, ownerId: null, label: '', labelLowercase: null, color: null, description: null};

  var labelModule = angular.module('Labels', ['AngularCommon', 'CLMAppLocation', 'CommonServices', 'ResourceModule']);

  labelModule.service('LabelStore', [
    'CLMResource', 'CLMAppLocations', function(CLMResource, CLMAppLocations) {
      var labelStore = null, labelStores = {};

      function refreshLabelStore() {
        var entityId = CLMAppLocations.getEntityId();
        labelStore = labelStores[entityId];
        if (!labelStore) {
          labelStore = labelStores[entityId] = CLMResource.getStore(angular.extend({ url: CLMAppLocations.getLabelsUrl() },
              labelStoreTemplate));
        }
      }

      var labelStoreTemplate = {
        template: labelTemplate,
        params: {
          timestamp: new Date().getTime()
        }
      };

      return {
        get: function() {
          refreshLabelStore();
          return labelStore.get();
        },
        refresh: function() {
          refreshLabelStore();
          return labelStore.refresh();
        },
        create: function() {
          return labelStore.create();
        }
      };
    }
  ]);

  function showAlert(alerts, alert) {
    alerts.length = 0;
    alerts.push(alert);
  }

  labelModule.controller('LabelController', [
    '$scope', '$http', '$q', '$modal', 'CLMAppLocations', 'Messages', 'CLMResource', 'LabelStore', 'ownerChange',
    function($scope, $http, $q, $modal, clmAppLocations, messages, clmResource, LabelStore, ownerChange) {
      $scope.alerts = [];

      function deselect() {
        if ($scope.selectedLabel) {
          $scope.selectedLabel.$revert();
        }
        $scope.selectedLabel = null;
        $scope.submitActive = false;
        $scope.alerts.length = 0;
      }

      function executeIfClean(fn) {
        if ($scope.selectedLabel && $scope.selectedLabel.isDirty()) {
          showAlert($scope.alerts, {
            type: 'error',
            msg: 'Please finish editing before trying to modify another label.'
          });
        }
        else {
          fn();
        }
      }

      $scope.deselect = deselect;

      $scope.doLoad = function() {
        $scope.error = null;
        $scope.applicableLabels = null;
        var promises = [
          LabelStore.refresh(), $http.get(clmAppLocations.getApplicableLabelsUrl(), {
            params: { timestamp: new Date().getTime() }
          })
        ];

        $q.all(promises).then(function(results) {
          $scope.applicableLabels = results[1].data.labelsByOwner;
          angular.forEach($scope.applicableLabels, function(applicableLabel, index) {
            applicableLabel.editable = index === 0;
            if (index === 0) {
              applicableLabel.labels = results[0];
            }
          });
        }, function(error) {
          $scope.error = error;
        });
      };

      $scope.$on('ownerChanged', ownerChange.getEventHandler($scope, 'applicableLabels'));
      $scope.$on('refresh', $scope.doLoad);

      $scope.editLabel = function(isEditable, label) {
        if (!isEditable) {
          return;
        }
        executeIfClean(function() {
          deselect();
          $scope.selectedLabel = label.$clone();
        });
      };

      $scope.createNew = function() {
        executeIfClean(function() {
          $scope.label = LabelStore.create();
          $scope.selectedLabel = $scope.label;
        });
      };

      $scope.deleteLabel = function(label, $event) {
        $event.stopPropagation();
        $modal.open({
          scope: $scope,
          backdrop: 'static',
          templateUrl: 'delete-label-modal',
          controller: [
            '$scope', function(modalScope) {
              modalScope.label = label;
              modalScope.cancel = function() {
                modalScope.$close();
              };
              modalScope.doDeleteLabel = function() {
                modalScope.$close(true);
              };
            }
          ]
        }).result.then(function () {
          label.$delete().then(function () {
            if ($scope.selectedLabel && label.id === $scope.selectedLabel.id) {
              $scope.selectedLabel = null;
            }
          }, function(error) {
            showAlert($scope.alerts, {
              type: 'error',
              msg: 'An error occurred while deleting the label ' + label.label + '. (' +
                  messages.getHttpErrorMessage(error) + ')'
            });
          });
        }, angular.noop);
      };

      $scope.$on('labels.cancelEditLabel', function(event, label) {
        event.stopPropagation();
        deselect();
      });

      $scope.doLoad();
    }
  ]);

  labelModule.controller('LabelEditorController', [
    '$scope', '$http', 'CLMAppLocations', 'Messages', 'LabelStore',
    function($scope, $http, clmAppLocations, messages, LabelStore) {
      function errorFn(error) {
        $scope.submitActive = false;
        showAlert($scope.editorAlerts, {
          type: 'error',
          msg: 'An error occurred while saving the label. (' +
              messages.getHttpErrorMessage(error) + ')'
        });
      }

      $scope.editorAlerts = []; 

      $scope.colors = [null, 'white', 'grey', 'black', 'green', 'yellow', 'orange', 'red', 'blue'];

      $scope.setColor = function(color) {
        $scope.selectedLabel.color = color;
      };

      $scope.cancelEditLabel = function() {
        $scope.$emit('labels.cancelEditLabel');
      };

      $scope.canSaveEdit = function(valid, label) {
        return valid && !$scope.submitActive && label != null && label.label;
      };

      $scope.$on('pageChangeStarted', function(event) {
        if ($scope.selectedLabel) {
          if ($scope.selectedLabel.isDirty()) {
            event.preventDefault();
          }
        }
      });

      $scope.saveLabel = function() {
        $scope.submitActive = true;
        $scope.selectedLabel.$save().then(function(label) {
          $scope.deselect();
        }, errorFn);
      };
    }
  ]);

  labelModule.directive('itemLabel', function() {
    return {
      require: 'ngModel',
      link: function(scope, element, attrs, ctrl) {
        ctrl.$parsers.unshift(function(newValue) {
          var unique = true,
              notInvalid = newValue.indexOf(' ') === -1 && newValue.indexOf('\t') === -1;

          angular.forEach(scope.labels, function(item, key) {
            if (item.id !== scope.selectedLabel.id) {
              unique = unique && (item.label.toLowerCase() !== newValue.toLowerCase());
            }
          });
          ctrl.$setValidity('duplicate', unique);
          ctrl.$setValidity('invalid', notInvalid);

          return (unique && notInvalid) ? newValue : undefined;
        });
      }
    };
  });
}());
