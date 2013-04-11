/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp, window */
(function () {
	'use strict';

	var managementModule = angular.module('ApplicationManagement', ['AngularCommon', 'Hudson', 'CLMLocation', 'Profile']);

	managementModule.controller('ApplicationManagementController', ['$scope', '$http', 'hudson', 'CLMLocations', 'commonCodeFactory', 'ProfileStore', function ($scope, $http, hudson, clmLocations, commonCodeFactory, profileStore) {
		$scope.orderColumn = 'name';
		$scope.orderDirection = true;
		$scope.canGetRobotIcon = false;

		var error = commonCodeFactory.getEncodedQueryString('errorMessage');
		if (error) {
			$scope.syncErrorResponse = decodeURIComponent(error);
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

		profileStore.get().then(function (data) {
			$scope.profiles = data;
		}, function () {
			$scope.showServerError(arguments[0].data, arguments[0].status, arguments[0].headersFn, arguments[0].config);
		});

		$('#newApplicationModal').on('hide', function () {
			// AngularJS barfs if $apply is made unnecessarily, however hide may or may not be called within scope
			// $scope.$$phase is "$digest" while processing, null otherwise, this is undocumented
			// https://groups.google.com/forum/#!topic/angular/FJwxJ-XbJaE
			if ($scope.$$phase) {
				$scope.newApplicationUrl = ''; // unloads form, resets state
			} else {
				$scope.$apply(function () {
					$scope.newApplicationUrl = ''; // unloads form, resets state
				});
			}
		});

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

			// Must access DOM element for the icon input to reset the file input
			var applicationIconInput = angular.element('#file');
			if (applicationIconInput.length > 0) {
				applicationIconInput.replaceWith(applicationIconInput.val('').clone(true));
			}

			$scope.selectedApplication = { id: null, publicId: null, name: null };
			if (application) {
				$scope.isEditMode = true;
				$scope.selectedApplication = angular.extend($scope.selectedApplication, application);
				// After the image source is set to blob (see below), angular will not respond to changing ng-src
				// Image source adjustments need to be done through attr
				angular.element('#applicationIcon').attr('src', '../rest/application/icon/' + encodeURIComponent($scope.selectedApplication.publicId));
			} else {
				$scope.isEditMode = false;
				angular.element('#applicationIcon').attr('src', '../assets/img/defaulticon_application.png');
			}
			$scope.hasRobotSource = false;
			$scope.iconChanged = false;
			$scope.robotHash = null;

			$('#newApplicationModal').modal('show');
			angular.element('#applicationName').focus();
		};

		$scope.confirmDeleteApplication = function (application) {
			$scope.selectedApplication = application;
			$scope.deletedEnabled = true;
			$('#deleteApplicationModal').modal('show');
		};

		$scope.deleteApplication = function () {
			$scope.deletedEnabled = false;
			$http['delete'](clmLocations.getApplicationUrl($scope.selectedApplication.publicId)).success(function () {
				angular.forEach($scope.applications, function (applicationCandidate, key) {
					if (applicationCandidate.id === $scope.selectedApplication.id) {
						$scope.applications.splice(key, 1);
						return false;
					}
				});
				$('#deleteApplicationModal').modal('hide');
			}).error($scope.showServerError);
		};

		$scope.clearAddApplicationError = function () {
			$scope.addApplicationError = null;
		};

		$scope.clearSyncEditError = function () {
			$scope.syncErrorResponse = null;
			location.search = '';
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
			} else {
				for (var i = 0; i < application.policyEvaluations.length; i++) {
					if ($scope.orderColumn === application.policyEvaluations[i].stage.stageTypeId) {
						return application.policyEvaluations[i].time;
					}
				}
			}
			// return max value to prevent empty values showing up as low
			return Number.MAX_VALUE;
		};

		$scope.encodeURIComponent = window.encodeURIComponent;
	}]);

	managementModule.filter('filterReportColumns', function () {
		return function (items) {
			var arrayToReturn = [];
			if (items) {
				var validReportColumns = ['Build', 'Stage Release', 'Release'];
				for (var i = 0; i < items.length; i++) {
					if (jQuery.inArray(items[i].name, validReportColumns) > -1) {
						arrayToReturn.push(items[i]);
					}
				}
			}
			return arrayToReturn;
		};
	});

	managementModule.controller('EditApplicationController', ['$scope', '$http', 'hudson', 'CLMLocations', 'regexFactory', function ($scope, $http, hudson, clmLocations, regexFactory) {
		$scope.submitActive = false;
		$scope.addApplicationSync = clmLocations.addIconSync();

		// On the first instantiation of the edit modal, setting the source in editApplication in the ManagementController
		// Has no effect because applicationIcon does not exist in the DOM. Set it here instead
		angular.element('#applicationIcon').attr('src', '../rest/application/icon/' + encodeURIComponent($scope.selectedApplication.publicId));

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

		function onAJAXError(data, status, headersFn, config) {
			var header = headersFn();
			if (header['content-type'] && header['content-type'].indexOf('text/html') === 0) {
				$scope.errorResponse = 'Server Error';
			} else {
				$scope.errorResponse = data;
			}
			element.modal('show');
		}

		$scope.clearEditError = function () {
			$scope.errorResponse = null;
		};

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
			$scope.hasRobotSource = true;
			$scope.iconChanged = true;
		}

		$scope.fileChanged = function (element) {
			if (element.files && element.files.length > 0) {
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
						angular.element('#applicationIcon').attr('src', src);
						$scope.hasRobotSource = false;
					});
				} else {
					$scope.$apply(function () {
						angular.element('#applicationIcon').attr('src', '../assets/img/defaulticon_application.png');
						$scope.hasRobotSource = false;
					});
				}
			} else {
				$scope.$apply(function () {
					angular.element('#applicationIcon').attr('src', '../assets/img/defaulticon_application.png');
					$scope.hasRobotSource = false;
				});
			}
			$scope.iconChanged = true;
		};

		$scope.canSaveEdit = function () {
			liveApplicationRules();
			return $scope.applicationEditor.$valid && !$scope.submitActive;
		};

		// This needs to be invoked by onsubmit rather than ng-submit to suppress submit when necessary
		$scope.saveClick = function () {
			if ($scope.submitActive) {
				return true;
			}

			liveApplicationRules();
			if (!$scope.applicationEditor.$valid) {
				return false;
			}
			if (!preSaveRules()) {
				return false;
			}

			if (window.FormData) {
				var icon = angular.element('#file')[0];
				if (icon.files.length > 0) {
					if (icon.files[0].size > 5242880) {
						$scope.errorResponse.push('Icon file size must be smaller than 5 MB.')
					}
				}
			}

			$scope.submitActive = true;

			var application = {
				id: $scope.selectedApplication.id,
				publicId: $scope.selectedApplication.publicId,
				name: $scope.selectedApplication.name,
				applicationProfileId : $scope.selectedApplication.applicationProfileId
			};

			if (!$scope.isEditMode) {
				hudson.post(clmLocations.getApplicationsUrl(), application).success(function (data) {
					$scope.applications.push(data);
					$scope.selectedApplication = data;
					$scope.isEditMode = true;
					saveIcon();
				}).error(onAJAXError);
			} else {
				$http.put(clmLocations.getApplicationsUrl(), application).success(function (data) {
					angular.forEach($scope.applications, function (application, key) {
						if (data.id === application.id) {
							$scope.applications[key] = data;
							$scope.selectedApplication = data;
							return false;
						}
					});
					saveIcon();
				}).error(onAJAXError);
			}

			return false;
		};

		function saveIcon() {
			if (!$scope.iconChanged) {
				$('#newApplicationModal').modal('hide');
				return;
			}

			// Angular modal does not adjust value of form element so when posting these values need to be set
			angular.element('[name=applicationId]').val($scope.selectedApplication.id);
			angular.element('[name=hasRobotSource]').val($scope.hasRobotSource);
			angular.element('[name=robotHash]').val($scope.robotHash);

			var form = angular.element('#applicationEditor');

			if (window.FormData) {
				$scope.isUploadingIcon = true;

				var formData = new FormData(form[0]);
				var icon = angular.element('#file')[0];
				if (icon.files.length > 0) {
					formData.append('file', icon.files[0]);
				}

				hudson.ajaxPost({
					url: clmLocations.addIcon(),
					data: formData,
					success: function (data, status, jqXHR) {
						// We need to regrab the icon here because it doesn't exist when the browser first requests
						var iconSource = "../rest/application/icon/" + encodeURIComponent($scope.selectedApplication.publicId);
						angular.element("img[ng-src='" + iconSource + "']").attr('src', iconSource);
						$scope.submitActive = false;
						$scope.isUploadingIcon = false;
						$('#newApplicationModal').modal('hide');
					},
					error: function (jqXHR) {
						$scope.$apply(function () {
							$scope.isUploadingIcon = false;
							$scope.submitActive = false;
							onError(jqXHR);
						});
					}
				});
			} else {
				form.submit();
			}
		}

		// Angular automatically trims input so when removing leading or trailing spaces, the rules are not automatically fired
		$scope.fireLiveApplicationRules = function () {
			$scope.$apply(function () {
				liveApplicationRules();
			});
		};

		function liveApplicationRules() {
			var applicationId = angular.element('#applicationPublicId');
			$scope.applicationEditor.applicationPublicId.$setValidity('required', applicationId.val());
			var isDuplicateName = jQuery.grep($scope.applications,function (application) {
				return application.id !== $scope.selectedApplication.id && application.publicId === applicationId.val();
			}).length > 0;
			$scope.applicationEditor.applicationPublicId.$setValidity('duplicate', !isDuplicateName);

			var applicationName = angular.element('#applicationName');
			$scope.applicationEditor.applicationName.$setValidity('required', applicationName.val());
			$scope.applicationEditor.applicationName.$setValidity('alphaNumeric', !applicationName.val().match(new RegExp('[^-' + regexFactory.allLetters().source + '0-9 ]', 'i')));
			isDuplicateName = jQuery.grep($scope.applications,function (application) {
				return application.id !== $scope.selectedApplication.id && application.name && application.name.toLowerCase() === applicationName.val().toLowerCase();
			}).length > 0;
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