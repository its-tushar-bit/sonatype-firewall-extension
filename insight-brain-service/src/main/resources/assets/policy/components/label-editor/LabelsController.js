/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp */
(function () {
    'use strict';

  var labelTemplate = {id: null, applicationId: null, label: '', labelLowercase: null, color: null};

  var labelModule = angular.module('Labels', ['AngularCommon', 'Hudson', 'CLMAppLocation']);

  labelModule.service('LabelStore', ['CLMLocations', 'CLMAppLocations', 'CLMResource', function (clmLocations, clmAppLocations, clmResource) {
    var labelStore = clmResource.getStore({
      url: clmAppLocations.getLabelsUrl(),
      template: labelTemplate,
      params: {
        timestamp: new Date().getTime()
      }
    });
    return labelStore;
  }]);

    labelModule.controller('LabelController', ['$scope', '$http', '$dialog', 'CLMAppLocations', 'LabelStore', function ($scope, $http, $dialog, clmAppLocations, labelStore) {
		function deselect() {
			delete $scope.selectedLabel;
			$scope.editorUrl = '';
		}
      $scope.labelStore = labelStore;
      $scope.doLoad = function () {
        $scope.error = null;
        labelStore.get().then(function(labels){
          $scope.labels = labels;
        }, function(error){
          $scope.error = error;
        });
      };

      $scope.doLoad();

        $scope.editLabel = function (label) {
			$scope.selectedLabel = angular.copy(label || labelTemplate);
			$scope.editorUrl = '../policy-assets/components/label-editor/label-editor.html?' + clmBuildTimestamp;
        };

        $scope.deleteLabel = function (label) {
            $scope.deletedEnabled = false;
            $http['delete'](clmAppLocations.getDeleteLabelsUrl(label)).success(function () {
                var index = null;
                angular.forEach($scope.labels, function (candidate, key) {
                    if (candidate.id === label.id) {
                        index = key;
                        return false;
                    }
                });
                $scope.labels.splice(index, 1);
                deselect();
                $('#deleteLabelModal').modal('hide');
            }).error(function () {
                $('#deleteLabelModal').modal('hide');
                $scope.$broadcast('showServerError', arguments);
            });
        };

		$scope.$on('labels.cancelEditLabel', function (event, label) {
			event.stopPropagation();
			if (!label || label === $scope.selectedLabel) {
				deselect();
			}
		});

		$scope.doLoad();
    }]);

    labelModule.controller('LabelEditorController', ['$scope', '$http', 'hudson', 'CLMAppLocations', 'Messages', 'LabelStore', function ($scope, $http, hudson, clmAppLocations, messages, labelStore) {
        $scope.alerts = [];

        function errorFn(data, status, headersFn, config) {
            $scope.submitActive = false;
            $scope.alerts.push({
				type : 'error',
				msg : 'An error occurred while saving the label. (' + messages.getHttpErrorMessage({ status: status, data: data}) + ')'
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
            if (label.id == null) {
                hudson.post(clmAppLocations.getLabelsUrl(), label).success(function (data) {
					$scope.labels.push(data);
					$scope.$emit('labels.cancelEditLabel', label);
                }).error(errorFn);
            } else {
                $http.put(clmAppLocations.getLabelsUrl(), label).success(function (data) {
                    angular.forEach($scope.labels, function (labelCandidate, key) {
                        if (data.id === labelCandidate.id) {
                            $scope.labels[key] = data;
                            return false;
                        }
                    });
                    $scope.$emit('labels.cancelEditLabel', label);
                }).error(errorFn);
            }
        };
        
		$scope.$watch('selectedLabel', function (newValue) {
			if (newValue) {
				$scope.submitActive = false;
			}
		});
		$scope.$on('pageChangeStarted', function (event) {
		    if ($scope.selectedLabel.id) {
		        angular.forEach($scope.labels, function (candidate) {
		            if (candidate.id === $scope.selectedLabel.id && !angular.equals(candidate, $scope.selectedLabel)) {
		                event.preventDefault();
		            }
		        });
		    } else if ($scope.selectedLabel.label) {
		        event.preventDefault();
		    }
		});
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

  labelModule.directive('inlineLabelCreator', ['Messages', 'LabelStore', function (messages, labelStore) {
    return {
      restrict : 'A',
      templateUrl : "../policy-assets/components/label-editor/label-inline-editor.html",
      scope : {  },
      link : function (scope, element, attrs, ctrl) {
        scope.click = function () {
          if (!scope.label) {
            scope.label = labelStore.create();
          }
        };
        scope.cancel = function () {
          if (scope.label) {
              scope.label = null;
          }
        };
        scope.setInlineColor = function(color, $event){
          //scope.$apply(function(){
            scope.label.color = color;
            //angular.element($event.target).addClass('active');
          //});
        }
        scope.saveLabel = function () {
          scope.label.$save().then(function (label) {
            scope.label = null;
          }, function (error) {
            scope.alerts.push({
              type : 'error',
              msg : 'An error occurred while saving the label. (' + messages.getHttpErrorMessage(error) + ')'
            });
          });
        };
        scope.$on('labels.cancelEditLabel', function(){
          scope.label = null;
        });
      }
    };
  }])
}());
