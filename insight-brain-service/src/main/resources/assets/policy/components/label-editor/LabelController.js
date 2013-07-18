/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp */
(function () {
  'use strict';

  var labelTemplate = {id: null, ownerId: null, label: '', labelLowercase: null, color: null};

  var labelModule = angular.module('Labels', ['AngularCommon', 'CLMAppLocation', 'CommonServices']);

  labelModule.service('LabelStore',['CLMResource', 'CLMAppLocations', function(CLMResource, CLMAppLocations){
    var currentStoreId = null, labelStore = null;

    function refreshLabelStore() {
      var isNew = !labelStore || currentStoreId !== CLMAppLocations.getEntityId();
      if (isNew) {
        currentStoreId = CLMAppLocations.getEntityId();
        labelStore = CLMResource.getStore(angular.extend({ url: CLMAppLocations.getLabelsUrl() }, labelStoreTemplate));
      }
      return isNew;
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

  labelModule.controller('LabelController', ['$scope', '$http', '$dialog', 'CLMAppLocations', 'Messages', 'CLMResource', 'LabelStore',
                                             function ($scope, $http, $dialog, clmAppLocations, messages, clmResource, LabelStore) {
      function deselect() {
        delete $scope.selectedLabel;
        delete $scope.label;
        $scope.editorUrl = '';
      }

      $scope.deselect = deselect;
      $scope.doLoad = function () {
        $scope.error = null;
        LabelStore.get().then(function(labels){
          $scope.labels = labels;
        }, function(error){
          $scope.error = error;
        });
      };

      $scope.editLabel = function (label) {
        deselect();
        $scope.selectedLabel = angular.copy(label); //TODO KR - see if this has the desired effect
        $scope.editorUrl = '../policy-assets/components/label-editor/label-editor.html?' + clmBuildTimestamp;
      };

      $scope.deleteLabel = function (label) {
        label.$delete().then(function(){
          deselect();
        }, function(error){
          $scope.alerts.push({
            type: 'error',
            msg: 'An error occurred while deleting the label. (' +
                messages.getHttpErrorMessage({ status: error.status, data: error.data}) + ')'
          });
          deselect();
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

        $scope.saveLabelClick = function (valid, label) {
            if (!$scope.canSaveEdit(valid, label)) {
                return;
            }

          $scope.submitActive = true;
          $scope.selectedLabel.$save().then(function () {
            $scope.$emit('labels.cancelEditLabel', $scope.selectedLabel);
          }, errorFn);
        };

		$scope.$watch('selectedLabel', function (newValue) {
			if (newValue) {
				$scope.submitActive = false;
			}
		});
		$scope.$on('pageChangeStarted', function (event) {
		    if ($scope.selectedLabel && $scope.selectedLabel.id) {
		        angular.forEach($scope.labels, function (candidate) {
		            if (candidate.id === $scope.selectedLabel.id && !angular.equals(candidate, $scope.selectedLabel)) {
		                event.preventDefault();
		            }
		        });
		    } else if ($scope.selectedLabel && $scope.selectedLabel.label) {
		        event.preventDefault();
		    }
		});

      $scope.click = function () {
        if (!$scope.label) {
          $scope.label = LabelStore.create();
          $scope.selectedLabel = $scope.label;
        }
      };
      $scope.cancel = function () {
        if ($scope.label) {
          $scope.label = null;
        }
      };
      $scope.setInlineColor = function(color){
        $scope.label.color = color;
      };
      $scope.saveLabel = function () {
        $scope.label.$save().then(function (label) {
          $scope.deselect();
        }, function (error) {
          $scope.alerts.push({
            type : 'error',
            msg : 'An error occurred while saving the label. (' + messages.getHttpErrorMessage(error) + ')'
          });
        });
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

  labelModule.directive('inlineLabelCreator', function () {
    return {
      templateUrl : "../policy-assets/components/label-editor/label-inline-editor.html",
      controller: 'LabelEditorController'
    };
  })
}());
