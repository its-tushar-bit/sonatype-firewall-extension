/*global insightApp, angular, $ */
(function () {
	'use strict';

	var labelModule = angular.module('Labels', []);

	labelModule.controller('LabelController', ['$scope', '$http', function ($scope, $http) {
		// TODO Failure?
		$http.get(insightApp.getLabelsUrl()).success(function (data) {
			$scope.labels = data;
		});

		$scope.colors = [null, 'white', 'grey', 'black', 'green', 'yellow', 'orange', 'red', 'blue'];

		$('#labelEditModal').on('hide', function () {
			// AngularJS barfs if $apply is made unnecessarily, however hide may or may not be called within scope
			// $scope.$$phase is "$digest" while processing, null otherwise, this is undocumented
			// https://groups.google.com/forum/#!topic/angular/FJwxJ-XbJaE
			if ($scope.$$phase) {
			    $scope.editorUrl = ''; // unloads form, resets state
			} else {
				$scope.$apply(function () {
				    $scope.editorUrl = ''; // unloads form, resets state
				});
			}
		});

		$scope.editLabel = function (label) {
			$scope.editorUrl = 'components/labels-editor.html'; // loads form

	        $scope.selectedLabel = {id : null, applicationId : null, label : '', labelLowercase : null, color : null};
		    if (label) {
		        $scope.selectedLabel = angular.extend($scope.selectedLabel, label);
		    }

		    $('#labelEditModal').modal('show');
		};

		$scope.confirmDeleteLabel = function (label) {
			$scope.selectedLabel = angular.extend({id : null, applicationId : null, label : null, labelLowercase : null, color : null}, label);
			$scope.deletedEnabled = true;
			$('#deleteLabelModal').modal('show');
		};

		$scope.deleteLabel = function () {
		    $scope.deletedEnabled = false;
		    $http.delete(insightApp.getDeleteLabelsUrl($scope.selectedLabel)).success(function () {
		        var index = null;
		        angular.forEach($scope.labels, function (candidate, key) {
		            if (candidate.id === $scope.selectedLabel.id) {
		                index = key;
		                return false;
		            }
		        });
		        $scope.labels.splice(index, 1);
		        $('#deleteLabelModal').modal('hide');
		    });
		};
	}]);

	labelModule.controller('LabelEditorController', ['$scope', '$http', function ($scope, $http) {

		function errorFn (data, status, headers, config) {
            $scope.submitActive = false;
			var header = headersFn();
			if ($scope.labelEditor) {
			    if (header['content-type'] && header['content-type'].indexOf('text/html') === 0) {
			        $scope.labelEditor.editErrorResponse = 'Server Error';
			    } else {
					$scope.labelEditor.editErrorResponse = data;
			    }
			}
		}

		$scope.submitActive = false;

		$scope.setColor = function (color) {
			$scope.selectedLabel.color = color;
		};

		$scope.canSaveEdit = function (valid) {
			return valid && !$scope.submitActive && $scope.selectedLabel != null && $scope.selectedLabel.label.length > 0;
		};

		$scope.saveLabelClick = function () {
			if (!$scope.canSaveEdit($scope.labelEditor.$valid))
				return;

			var label = $scope.selectedLabel;
		    $scope.submitActive = true;
		    if (label.id == null) {
		        $http.post(insightApp.getLabelsUrl(), label).success(function (data) {
		            $scope.labels.push(data);
		            $('#labelEditModal').modal('hide');
		        }).error(errorFn);
		    } else {
		        $http.put(insightApp.getLabelsUrl(), label).success(function (data) {
		            angular.forEach($scope.labels, function (labelCandidate, key) {
		                if (data.id === labelCandidate.id) {
		                    $scope.labels[key] = data;
		                    return false;
		                }
		            });
		            $('#labelEditModal').modal('hide');
		        }).error(errorFn);
		    }
		};

		$scope.clearEditError = function() {
			if ($scope.labelEditor) {
				$scope.labelEditor.editErrorResponse = null;
			}
		};
	}]);

	labelModule.directive('itemLabel', function () {
		return {
			require : 'ngModel',
			link : function (scope, element, attrs, ctrl) {
				ctrl.$parsers.unshift(function (newValue) {
					var nonDuplicate = true,
						notEmpty = newValue.length !== 0;
					ctrl.$setValidity('empty', notEmpty)

					angular.forEach(scope.labels, function (item, key) {
						if (item.id !== scope.selectedLabel.id) {
							nonDuplicate = nonDuplicate && (item.label.toLowerCase() != newValue.toLowerCase());
						}
					});
					ctrl.$setValidity('duplicate', nonDuplicate);

					return (notEmpty && nonDuplicate) ? newValue : undefined;
				});
			}
		};
	});
}());
