/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp */
(function () {
    'use strict';

    var labelModule = angular.module('Labels', ['AngularCommon', 'Hudson', 'CLMLocation']);

    labelModule.controller('LabelController', ['$scope', '$http', '$dialog', 'CLMAppLocations', function ($scope, $http, $dialog, clmAppLocations) {
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
		var labelTemplate = {id: null, applicationId: null, label: '', labelLowercase: null, color: null};

		$scope.doLoad = function () {
			$scope.error = null;
            $http.get(clmAppLocations.getLabelsUrl(), {
                params: { timestamp: new Date().getTime() }
            }).success(function (data) {
                $scope.labels = data;
            }).error(function (data, status, headers, config) {
                $scope.error = {
					data: data,
					status : status,
					headers : headers,
					config : config
                };
            });
        };

        $scope.editLabel = function (label) {
			$scope.selectedLabel = angular.copy(label || labelTemplate);
			$scope.editorUrl = '../policy-assets/components/label-editor/label-editor.html?' + clmBuildTimestamp;
        };

        $scope.confirmDeleteLabel = function () {
            $scope.deletedEnabled = true;
            $('#deleteLabelModal').modal('show');
        };

        $scope.deleteLabel = function () {
            $scope.deletedEnabled = false;
            $http['delete'](clmAppLocations.getDeleteLabelsUrl($scope.selectedLabel)).success(function () {
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

		$scope.doLoad();
    }]);

    labelModule.controller('LabelEditorController', ['$scope', '$http', 'hudson', 'CLMAppLocations', function ($scope, $http, hudson, clmAppLocations) {

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
        $scope.colors = [null, 'white', 'grey', 'black', 'green', 'yellow', 'orange', 'red', 'blue'];

        $scope.setColor = function (color) {
            $scope.selectedLabel.color = color;
        };

		$scope.cancelEditLabel = function () {
			$scope.$emit('labels.cancelEditLabel');
		};

        $scope.canSaveEdit = function (valid) {
            return valid && !$scope.submitActive && $scope.selectedLabel != null && $scope.selectedLabel.label;
        };

        $scope.saveLabelClick = function () {
            if (!$scope.canSaveEdit($scope.labelEditor.$valid)) {
                return;
            }

            var label = $scope.selectedLabel;
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

        $scope.clearEditError = function () {
            if ($scope.labelEditor) {
                $scope.labelEditor.editErrorResponse = null;
            }
        };

		$scope.$watch('selectedLabel', function (newValue) {
			if (newValue) {
				$scope.submitActive = false;
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
}());
