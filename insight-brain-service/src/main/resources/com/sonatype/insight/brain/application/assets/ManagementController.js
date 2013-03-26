/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp */
(function () {
	'use strict';

	var managementModule = angular.module('Management', ['AngularCommon', 'Hudson', 'CLMLocation']);

	managementModule.controller('ManagementController', ['$scope', '$location', '$http', 'hudson', 'CLMLocations', function ($scope, $location, $http, hudson, clmLocations) {
		$scope.orderColumn = 'publicId';
		$scope.orderDirection = true;

		function onAddApplicationError(data, status, headersFn, config) {
			var header = headersFn();
			if (header['content-type'] && header['content-type'].indexOf('text/html') === 0) {
				$scope.addApplicationError = 'Server Error';
			} else {
				$scope.addApplicationError = data;
			}
		}

		$http.get(clmLocations.getActionStageUrl(), {
			params: { timestamp: new Date().getTime() }
		}).success(function (data) {
			$scope.stages = data;
		}).error($scope.showServerError);

		$http.get(clmLocations.getApplicationsUrl(), {
			params: { timestamp: new Date().getTime() }
		}).success(function (data) {
			$scope.applications = data;
		}).error($scope.showServerError);

		$scope.getApplicationNames = function () {
			var names = [];
			if ($scope.applications) {
				for (var i = 0; i < $scope.applications.length; i++) {
					names.push($scope.applications[i].publicId);
				}
			}
			return names;
		};

		$scope.registerNewApplication = function () {
			$scope.newApplicationUrl = 'components/new-application-editor.html?' + clmBuildTimestamp;

			$scope.selectedApplication = { id: null, publicId: null };

			$('#newApplicationModal').modal('show');
		};

		$scope.addApplication = function () {
			if (!$scope.applicationPublicId) {
				$scope.addApplicationError = 'Please enter a value for the Application Id';
			}
			hudson.post(clmLocations.getApplicationsUrl(), $scope.applicationPublicId).success(function (application) {
				$scope.applications.push(application);
				$scope.clearAddApplicationError();
				$('#addApplicationModal').modal('hide');
			}).error(onAddApplicationError);
		};

		$scope.clearAddApplicationError = function () {
			$scope.addApplicationError = null;
		};

		$scope.order = function (column) {
			if ($scope.orderColumn === column) {
				$scope.orderDirection = !$scope.orderDirection;
			} else {
				$scope.orderColumn = column;
				$scope.orderDirection = true;
			}
		};

		$scope.orderBy = function (application) {
			if ($scope.orderColumn === 'publicId') {
				return application.publicId;
			} else if ($scope.orderColumn === application.policyEvaluation.stage.stageTypeId) {
				return application.policyEvaluation.time;
			}
			// return max value to prevent empty values showing up as low
			return Number.MAX_VALUE;
		};

		$scope.encodeURIComponent = function (value) {
			return encodeURIComponent(value);
		}
	}]);

	managementModule.filter('filterReportColumns', function () {
		return function (items) {
			var arrayToReturn = [];
			if (items) {
				var validReportColumns = ['Build', 'Stage Release', 'Release'];
				for (var i = 0; i < items.length; i++) {
					if (validReportColumns.indexOf(items[i].name) > -1) {
						arrayToReturn.push(items[i]);
					}
				}
			}
			return arrayToReturn;
		};
	});

	managementModule.controller('NewApplicationController', ['$scope', 'hudson', 'CLMLocations', function ($scope, hudson, clmLocations) {
		$scope.submitActive = false;
		$scope.addApplicationSync = clmLocations.getAddApplicationSyncUrl();

		function onError(jqXHR) {
			var contentType = jqXHR.getResponseHeader('Content-Type');
			if ($scope.applicationEditor) {
				if (contentType.indexOf('text/html') === 0) {
					$scope.errorResponse = 'Server Error';
				} else {
					$scope.errorResponse = jqXHR.responseText;
				}
			}
		};

		$scope.fileChanged = function (element) {
			if (element.files.length > 0) {
				var file = element.files[0],
				src;
				if (window.URL) {
					src = window.URL.createObjectURL(file);
				} else if (window.webkitURL) {
					src = window.webkitURL.createObjectURL(file);
				}
				if (src) {
					$scope.$apply(function () {
						$scope.hasIconSource = true;
						$scope.applicationIconSource = src;
					});
				} else {
					$scope.$apply(function () {
						$scope.hasIconSource = false;
					});
				}
			} else {
				$scope.$apply(function () {
					$scope.hasIconSource = false;
				});
			}
		};

		$scope.canSaveEdit = function () {
			applicationRules();
			return $scope.applicationEditor.$valid && !$scope.submitActive;
		};

		// This needs to be invoked by onsubmit rather than ng-submit to suppress submit when necessary
		$scope.saveClick = function () {
			applicationRules();
			if (!$scope.applicationEditor.$valid) {
				return false;
			}

			if (window.FormData) {
				$scope.uploadInProgress = true;
				var formData = new FormData(angular.element('#applicationEditor')[0]);
				var icon = angular.element('#file')[0];
				if (icon.files.length > 0) {
					if (icon.files[0].size > 5242880) {
						$scope.errorResponse.push('Icon file size must be smaller than 5 MB.')
					}
					formData.append('file', icon.files[0]);
				}

				hudson.ajaxPost({
					url: clmLocations.getApplicationsUrl(),
					data: formData,
					success: function (data, status, jqXHR) {
						alert(status);
					},
					error: function (jqXHR) {
						$scope.$apply(function () {
							$scope.uploadInProgress = false;
							$scope.submitActive = false;
							onError(jqXHR);
						});
					}
				});
				return false;
			}
			return true;
		};

		// This needs to be explicitly called by key up on the name rule as Angular ignores change events for leading
		// and trailing spaces. This is fixed in Angular 1.1.1 with ng-trim directive
		$scope.applicationRules = function () {
			$scope.$apply(function () {
				applicationRules();
			});
		};

		function applicationRules() {
			var applicationId = angular.element('#applicationId');
			$scope.applicationEditor.applicationId.$setValidity('required', applicationId.val());
			$scope.applicationEditor.applicationId.$setValidity('alphaNumeric', !applicationId.val().match(/[^a-z0-9 ]/i));
			var isDuplicateName = $scope.applications.some(function (application) {
				return application.publicId === applicationId.val();
			});
			$scope.applicationEditor.applicationId.$setValidity('duplicate', !isDuplicateName);

			var applicationName = angular.element('#applicationName');
			$scope.applicationEditor.applicationName.$setValidity('required', applicationName.val());
			$scope.applicationEditor.applicationName.$setValidity('alphaNumeric', !applicationName.val().match(/[^a-z0-9 ]/i));
			$scope.applicationEditor.applicationName.$setValidity('spaces', !applicationName.val().match(/^ | {2,}| $/));
			isDuplicateName = $scope.applications.some(function (application) {
				return application.name.toLowerCase() === applicationName.val().toLowerCase();
			});
			$scope.applicationEditor.applicationName.$setValidity('duplicate', !isDuplicateName);
		}
	}]);
}());