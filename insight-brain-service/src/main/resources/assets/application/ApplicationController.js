(function () {
	'use strict';

	var applicationModule = angular.module('ApplicationModule', ['ui.compat', 'ManagementModule', 'Policy', 'LicenseThreatGroup', 'Labels'], ['$stateProvider', function ($stateProvider) {
		$stateProvider.state('management.application', {
			parent : 'management',
			url : '/application',
			controller : 'applicationController',
			templateUrl : '../application-assets/components/application-navigator.html'
		}).state('management.application.view', {
			parent : 'management.application',
			url : '/{applicationPublicId}',
			controller : 'applicationEditorController',
			templateUrl : '../application-assets/components/application-editor.html'
		}).state('management.application.view.policies', {
			parent : 'management.application.view',
			url : '/policies',
			controller : 'PolicyController',
			templateUrl : '../policy-assets/components/policy/policy.html'
		}).state('management.application.view.policies.edit', {
			parent : 'management.application.view',
			url : '/policies/{policyId}',
			controller : 'PolicyEditorController',
			templateUrl : '../assets/components/policy-editor/policy-editor.html'
		}).state('management.application.view.labels', {
			parent : 'management.application.view',
			url : '/labels',
			controller : 'LabelController',
			templateUrl : '../policy-assets/components/label-editor/labels.html'
		}).state('management.application.view.licenses', {
			parent : 'management.application.view',
			url : '/licenses',
			controller : 'LicenseThreatGroupController',
			templateUrl : '../policy-assets/components/license-threat-group/license-threat-group.html'
		});
	}]);

	applicationModule.controller('applicationController', function($scope, $state, $timeout, $location, applicationStore) {
		function switchApplication() {
			$scope.selectedApplication = null;
			$scope.userIconSource = null;
			if ('_new_' === $scope.$state.params.applicationPublicId) {
                $timeout(function () {
                    $scope.selectedApplication = applicationStore.create();
					$scope.userIconSource = '../assets/img/defaulticon_application.png';
                }, 100);
            } else if ($scope.$state.params.applicationPublicId !== null && $scope.applications) {
				for (var i = 0; i < $scope.applications.length; i++) {
					if ($scope.$state.params.applicationPublicId === $scope.applications[i].publicId) {
						$timeout(function () {
							$scope.selectedApplication = $scope.applications[i];
							$scope.userIconSource = '../rest/application/icon/' + encodeURIComponent($scope.selectedApplication.publicId);
						}, 100);
						return;
					}
				}
				// TODO We might want to consider reloading the store at this point?
			}
		}
		$scope.location =  $location;

		$scope.$state = $state;
		$scope.isCurrentTab = function (tabName) {
			return $state.current.name.lastIndexOf(tabName) === $state.current.name.length - tabName.length;
		};

		applicationStore.get().then(function(applications) {
			$scope.applications = applications;
			switchApplication();
			$scope.$watch('$state.params.applicationPublicId', switchApplication);
			$scope.$on('resetApplication', switchApplication);
		}, function (error) {
            // TODO Error handling
			alert(error.data);
		});
	});

	applicationModule.controller('applicationEditorController', function($scope, $state, applicationStore, OrganizationStore, CLMLocations, $http, hudson) {
		function formReset() {
			var applicationPublicId = $scope.selectedApplication.publicId;
			$state.transitionTo('management.application').then(function() {
				$state.transitionTo('management.application.view.policies', { applicationPublicId: applicationPublicId });
			});
		}
		
		var isPostingIcon = false;
		
		$scope.$state = $state;

		$scope.submitActive = false;
		$scope.addApplicationSync = CLMLocations.addIconSync();
		$scope.hasRobotSource = false;
		
		OrganizationStore.get().then(function(results) {
            $scope.organizations = results;
        });
		
		$scope.getOrganizationName = function(organizationId) {
			if ($scope.organizations) {
				for (var i = 0; i < $scope.organizations.length; i++) {
					var organizationIter = $scope.organizations[i];
					if (organizationIter.id === organizationId) {
						return organizationIter.name;
					}
				}
			}
		};
		
		$scope.changeOrganization = function(organization) {
			$scope.selectedApplication.organizationId = organization.id;
		};
		
		$scope.alerts = [];
		$scope.closeAlert = function(index) {
			$scope.alerts.splice(index, 1);
		};
		
		$scope.generateIcon = function () {
			var name = $scope.selectedApplication.name,
				hash = 0;
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
		};

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
						$scope.userIconSource = src;
						$scope.hasRobotSource = false;
					});
				} else {
					$scope.$apply(function () {
						$scope.userIconSource = '../assets/img/defaulticon_application.png';
						$scope.hasRobotSource = false;
					});
				}
			} else {
				$scope.$apply(function () {
					$scope.userIconSource = '../assets/img/defaulticon_application.png';
					$scope.hasRobotSource = false;
				});
			}
			$scope.$apply(function() {
				$scope.iconChanged = true;
			});
		};
		
		$scope.encodeURIComponent = window.encodeURIComponent;
		
		$scope.isFormDirty = function() {
			if (!$scope.selectedApplication) {
				return false;
			}
			var originalApplication = $scope.selectedApplication.$getOriginal();
			var currentApplication = $scope.selectedApplication;
			return currentApplication.publicId !== originalApplication.publicId || currentApplication.name !== originalApplication.name 
				|| currentApplication.organizationId !== originalApplication.organizationId || $scope.iconChanged;
		};
		
		$scope.$on('pageChangeStarted', function(event, destination) {
			var application = $scope.selectedApplication;
			if (!destination || (application && destination.indexOf('application/' + application.publicId) === -1)) {
				if ($scope.isFormDirty() && !isPostingIcon) {
					event.preventDefault();
				}
			}
	    });
		
		$scope.$on('pageChangeAccepted', function() {
			var originalApplication = $scope.selectedApplication.$getOriginal();
			angular.extend($scope.selectedApplication, originalApplication);
		});

		$scope.canSaveEdit = function () {
			return $scope.applicationEditor.$valid && !$scope.submitActive;
		};
		
		$scope.cancel = function() {
			if (!$scope.selectedApplication.id) {
				$state.transitionTo('management.application');
			} else {
				var originalApplication = $scope.selectedApplication.$getOriginal();
				angular.extend($scope.selectedApplication, originalApplication);
				formReset();
			}
		};

		// This needs to be invoked by onsubmit rather than ng-submit to suppress submit when necessary
		$scope.save = function () {
			if ($scope.submitActive) {
				return true;
			}

			if (!$scope.applicationEditor.$valid) {
				return false;
			}
			
			if (window.FormData) {
				var icon = angular.element('#file')[0];
				if (icon.files.length > 0) {
					if (icon.files[0].size > 5242880) {
						$scope.$apply(function() {
							$scope.alerts.push({ type: 'error', msg: 'Icon file size must be smaller than 5 MB.' });
						});
						return false;
					}
				}
			}

			$scope.submitActive = true;

			var application = {
				id: $scope.selectedApplication.id,
				publicId: $scope.selectedApplication.publicId,
				name: $scope.selectedApplication.name,
				organizationId: $scope.selectedApplication.organizationId
			};

			if (!application.id) {
				hudson.post(CLMLocations.getApplicationsUrl(), application).success(function (data) {
					applicationStore.refresh().then(function() {
						saveIcon();
					});
				}).error(function (data) { 
					$scope.submitActive = false;
					$scope.alerts.push({ type: 'error', msg: data }); 
				});
			} else {
				$http.put(CLMLocations.getApplicationsUrl(), application).success(function (data) {
					applicationStore.refresh().then(function() {
						saveIcon();
					});
				}).error(function (data) {
					$scope.submitActive = false;
					$scope.alerts.push({ type: 'error', msg: data }); 
				});
			}

			return false;
		};

		function saveIcon() {
			if (!$scope.iconChanged) {
				$scope.submitActive = false;
				formReset();
				$scope.$emit('resetApplication');
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
					url: CLMLocations.addIcon(),
					data: formData,
					success: function (data, status, jqXHR) {
						$scope.$apply(function () {
							$scope.submitActive = false;
							$scope.isUploadingIcon = false;
							formReset();
							$scope.$emit('resetApplication');
						});
					},
					error: function (jqXHR) {
						$scope.$apply(function () {
							$scope.isUploadingIcon = false;
							$scope.submitActive = false;
							$scope.$broadcast('postAlert', jqXHR);
						});
					}
				});
			} else {
				isPostingIcon = true;
				form.submit();
			}
		}
	});

	applicationModule.service('applicationStore', ['CLMLocations', 'CLMResource', function (clmLocations, clmResource) {
		var applicationStore = clmResource.getStore({
			id : 'id',
			url : clmLocations.getApplicationsUrl(),
			template : { id: null, publicId: null, name: null, organizationId: null },
			params : {
				timestamp : new Date().getTime()
			}
		});
		return applicationStore;
	}]);

	applicationModule.service('ApplicationId', ['commonCodeFactory', '$state', function (commonCodeFactory, $state) {
		// TODO Are ui-router parameters encoded or decoded?
		return {
			encoded : function () {
				var applicationPublicId = $state.params.applicationPublicId;
				return applicationPublicId ? encodeURI(applicationPublicId) : null;
			}
		};
	}]);
}());