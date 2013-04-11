/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp */
(function () {
    'use strict';

    var labelModule = angular.module('Labels', ['AngularCommon', 'Hudson', 'CLMLocation']);

    labelModule.controller('LabelController', ['$scope', '$http', 'CLMLocations', function ($scope, $http, clmLocations) {
        function errorFn(data, status, headersFn, config) {
            var header = headersFn();
            if (header['content-type'] && header['content-type'].indexOf('text/html') === 0) {
                $scope.errorResponse = 'Server Error';
            } else {
                $scope.errorResponse = data;
            }
            $('#labelErrorModal').modal('show');
        }

		function deselect() {
			delete $scope.selectedLabel;
			$scope.editorUrl = '';
		}

        $http.get(clmLocations.getLabelsUrl(), {
            params: { timestamp: new Date().getTime() }
        }).success(function (data) {
                $scope.labels = data;
            }).error($scope.showServerError);

        $scope.colors = [null, 'white', 'grey', 'black', 'green', 'yellow', 'orange', 'red', 'blue'];

        $scope.editLabel = function (label) {
            $scope.editorUrl = 'components/labels-editor.html?' + clmBuildTimestamp; // loads form

            $scope.selectedLabel = {id: null, applicationId: null, label: '', labelLowercase: null, color: null};
            if (label) {
                $scope.selectedLabel = angular.extend($scope.selectedLabel, label);
            }
        };

        $scope.confirmDeleteLabel = function () {
            $scope.deletedEnabled = true;
            $('#deleteLabelModal').modal('show');
        };

        $scope.deleteLabel = function () {
            $scope.deletedEnabled = false;
            $http['delete'](clmLocations.getDeleteLabelsUrl($scope.selectedLabel)).success(function () {
                var index = null;
                angular.forEach($scope.labels, function (candidate, key) {
                    if (candidate.id === $scope.selectedLabel.id) {
                        index = key;
                        return false;
                    }
                });
                $scope.labels.splice(index, 1);
                deselect();
                $('#deleteLabelModal').modal('hide');
            }).error(function () {
                $('#deleteLabelModal').modal('hide');
                $scope.showServerError.apply(this, arguments);
            });
        };

		$scope.$on('labels.cancelEditLabel', function (event, label) {
			event.stopPropagation();
			if (!label || label === $scope.selectedLabel) {
				deselect();
			}
		});
    }]);

    labelModule.controller('LabelEditorController', ['$scope', '$http', 'hudson', 'CLMLocations', function ($scope, $http, hudson, clmLocations) {

        function errorFn(data, status, headersFn, config) {
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

		$scope.cancelEditLabel = function () {
			$scope.$emit('labels.cancelEditLabel');
		};

        $scope.canSaveEdit = function (valid) {
            return valid && !$scope.submitActive && $scope.selectedLabel != null && $scope.selectedLabel.label.length > 0;
        };

        $scope.saveLabelClick = function () {
            if (!$scope.canSaveEdit($scope.labelEditor.$valid)) {
                return;
            }

            var label = $scope.selectedLabel;
            $scope.submitActive = true;
            if (label.id == null) {
                hudson.post(clmLocations.getLabelsUrl(), label).success(function (data) {
                    $scope.labels.push(data);
					$scope.$emit('labels.cancelEditLabel', label);
                }).error(errorFn);
            } else {
                $http.put(clmLocations.getLabelsUrl(), label).success(function (data) {
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

        $scope.clearEditError = function () {
            if ($scope.labelEditor) {
                $scope.labelEditor.editErrorResponse = null;
            }
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
