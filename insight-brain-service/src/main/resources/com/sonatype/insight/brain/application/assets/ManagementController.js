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
		$scope.orderColumn = 'name';
		$scope.orderDirection = true;
		$scope.canGetRobotIcon = false;

		function onAddApplicationError(data, status, headersFn, config) {
			var header = headersFn();
			if (header['content-type'] && header['content-type'].indexOf('text/html') === 0) {
				$scope.addApplicationError = 'Server Error';
			} else {
				$scope.addApplicationError = data;
			}
		}

		$http.get(clmLocations.getCanGetHashIcon(), {
			params: { timestamp: new Date().getTime() }
		}).success(function (data) {
			if (data && data === "true") {
				$scope.canGetRobotIcon = true;
			}
		});

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
					names.push($scope.applications[i].name);
				}
			}
			return names;
		};

		$scope.editApplication = function (application) {
			$scope.newApplicationUrl = 'components/new-application-editor.html?' + clmBuildTimestamp;

			// Must access DOM element for the form to reset the file input
			var editApplicationForm = angular.element('#applicationEditor');
			if (editApplicationForm.length > 0) {
				editApplicationForm[0].reset();
			}

			$scope.selectedApplication = { id: null, publicId: null, name: null };
			if (application) {
				$scope.isEditMode = true;
				$scope.selectedApplication = angular.extend($scope.selectedApplication, application);
				$scope.hasIconSource = true;
				// After the image source is set to blob (see below), angular will not respond to changing ng-src
				// Image source adjustments need to be done through attr
				angular.element('#applicationIcon').attr('src', '../rest/application/icon/' + $scope.selectedApplication.publicId);
			} else {
				$scope.isEditMode = false;
				$scope.hasIconSource = false;
				angular.element('#applicationIcon').attr('src', null);
			}
			$scope.hasRobotSource = false;
			$scope.robotHash = null;

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
			if ($scope.orderColumn === 'name') {
				return application.name;
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

	managementModule.controller('EditApplicationController', ['$scope', 'hudson', 'CLMLocations', 'regexFactory', function ($scope, hudson, clmLocations, regexFactory) {
		$scope.submitActive = false;
		$scope.addApplicationSync = clmLocations.getAddApplicationSyncUrl();

		// On the first instantiation of the edit modal, setting the source in editApplication in the ManagementController
		// Has no effect because applicationIcon does not exist in the DOM. Set it here instead
		angular.element('#applicationIcon').attr('src', '../rest/application/icon/' + $scope.selectedApplication.publicId);

		function onError(jqXHR) {
			var contentType = jqXHR.getResponseHeader('Content-Type');
			if ($scope.applicationEditor) {
				if (contentType.indexOf('text/html') === 0) {
					$scope.errorResponse = 'Server Error';
				} else {
					$scope.errorResponse = jqXHR.responseText;
				}
			}
		}

		$scope.generateIcon = function () {
			var name = $scope.selectedApplication.name;
			var hash = 0;
			if (!name) {
				hash = Math.floor(Math.random() * 100);
			} else {
				for (var i = 0; i < name.length; i++) {
					var charAtI = name.charCodeAt(i);
					hash = ((hash << 5) - hash) + charAtI;
					hash = hash & hash;
				}
			}
			$scope.robotHash = hash;
			$scope.hasIconSource = false;
			$scope.hasRobotSource = true;
		}

		$scope.fileChanged = function (element) {
			if (element.files.length > 0) {
				$scope.hasRobotSource = false;
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
						angular.element('#applicationIcon').attr('src', src);
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
			liveApplicationRules();
			return $scope.applicationEditor.$valid && !$scope.submitActive;
		};

		// This needs to be invoked by onsubmit rather than ng-submit to suppress submit when necessary
		$scope.saveClick = function () {
			liveApplicationRules();
			if (!$scope.applicationEditor.$valid) {
				return false;
			}
			if (!preSaveRules()) {
				return false;
			}

			$scope.submitActive = true;

			// Angular modal does not adjust value of form element so when posting these values need to be set
			angular.element('[name=isEditMode]').val($scope.isEditMode);
			angular.element('[name=hasRobotSource]').val($scope.hasRobotSource);
			angular.element('[name=robotHash]').val($scope.robotHash);

			if (window.FormData) {
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
						if (!$scope.isEditMode) {
							$scope.$apply(function () {
								$scope.applications.push(data);
								$scope.submitActive = false;
							});
						} else {
							angular.forEach($scope.applications, function (application, key) {
								if (data.id === application.id) {
									$scope.$apply(function () {
										$scope.applications[key] = data;
										$scope.submitActive = false;
									});
									return false;
								}
							});
						}
						$('#newApplicationModal').modal('hide');
					},
					error: function (jqXHR) {
						$scope.$apply(function () {
							$scope.submitActive = false;
							onError(jqXHR);
						});
					}
				});
				return false;
			}
			return true;
		};

		// Angular automatically trims input so when removing leading or trailing spaces, the rules are not automatically fired
		$scope.fireLiveApplicationRules = function () {
			$scope.$apply(function () {
				liveApplicationRules();
			});
		};

		function liveApplicationRules() {
			var applicationId = angular.element('#applicationId');
			$scope.applicationEditor.applicationId.$setValidity('required', applicationId.val());
			var isDuplicateName = $scope.applications.some(function (application) {
				return application.id !== $scope.selectedApplication.id && application.publicId === applicationId.val();
			});
			$scope.applicationEditor.applicationId.$setValidity('duplicate', !isDuplicateName);

			var applicationName = angular.element('#applicationName');
			$scope.applicationEditor.applicationName.$setValidity('required', applicationName.val());
			$scope.applicationEditor.applicationName.$setValidity('alphaNumeric', !applicationName.val().match(new RegExp('[^-' + regexFactory.allLetters().source + '0-9 ]', 'i')));
			isDuplicateName = $scope.applications.some(function (application) {
				return application.id !== $scope.selectedApplication.id && application.name && application.name.toLowerCase() === applicationName.val().toLowerCase();
			});
			$scope.applicationEditor.applicationName.$setValidity('duplicate', !isDuplicateName);

			// Hide the spaces error when needed. We only show this error on saving to reduce gui clutter
			if ($scope.applicationEditor.applicationName.$error.spaces) {
				var whitespacePass = applicationName.val().match(/^ | {2,}| $/);
				$scope.suggestedApplicationName = ($scope.selectedApplication.name || '').replace(/^ | $/g, '').replace(/ {2,}/, ' ');
				$scope.applicationEditor.applicationName.$setValidity('spaces', !whitespacePass);
			}
		}

		function preSaveRules() {
			var applicationName = angular.element('#applicationName');
			var whitespacePass = applicationName.val().match(/^ | {2,}| $/);
			$scope.$apply(function () {
				$scope.suggestedApplicationName = ($scope.selectedApplication.name || '').replace(/^ | $/g, '').replace(/ {2,}/, ' ');
				$scope.applicationEditor.applicationName.$setValidity('spaces', !whitespacePass);
			});
			return !whitespacePass;
		}
	}]);
}());