/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes
 *          the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
(function () {
	'use strict';

	var applicationModule = angular.module('ApplicationModule', ['ui.compat', 'ManagementModule', 'Policy', 'LicenseThreatGroup', 'Labels', 'AngularCommon'], ['$stateProvider', function ($stateProvider) {
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

	applicationModule.controller('applicationController', ['$scope', '$state', '$timeout', '$location', 'applicationStore', function($scope, $state, $timeout, $location, applicationStore) {
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

		$scope.doLoad = function () {
			$scope.error = null;
			applicationStore.get().then(function(applications) {
				$scope.applications = applications;
				switchApplication();
				$scope.$watch('$state.params.applicationPublicId', switchApplication);
				$scope.$on('resetApplication', switchApplication);
			}, function (error) {
				$scope.error = error;
			});
		};
		$scope.doLoad();
	}]);

	applicationModule.controller('applicationEditorController', function($scope, $state, applicationStore, OrganizationStore, CLMAppLocations, $http, hudson, editorTools) {
		var me = this;
		angular.extend(me, editorTools.getEditorController($scope, $state, 'management.application', 'selectedApplication.id',
			'resetApplication', angular.element('[name=applicationId]'), angular.element('#applicationEditor')));
		
		$scope.$state = $state;

		$scope.submitActive = false;
		$scope.addApplicationSync = CLMAppLocations.addIconSync();
		
		OrganizationStore.get().then(function(results) {
            $scope.organizations = results;
        });
		
		$scope.getOrganizationName = function(organizationId) {
			if (!organizationId) {
				return "Select Organization";
			}
			
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
		$scope.messages = editorTools.messages;

		$scope.generateIcon = function() {
			me.generateIcon($scope.selectedApplication.name);
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
		
		//make sure user is aware they are about to lose changes
		$scope.$on('pageChangeStarted', function(event, destination) {
			var application = $scope.selectedApplication;
			if (!destination || (application && destination.indexOf('application/' + application.publicId) === -1)) {
				if ($scope.isFormDirty() && !me.isPostingIcon) {
					event.preventDefault();
				}
			}
	    });
		
		$scope.$on('pageChangeAccepted', function() {
			var originalApplication = $scope.selectedApplication.$getOriginal();
			angular.extend($scope.selectedApplication, originalApplication);
		});

		$scope.canSaveEdit = function () {
			return $scope.isFormDirty() && !$scope.applicationEditor.$invalid && !$scope.submitActive;
		};
		
		$scope.cancel = function() {
			if (!$scope.selectedApplication.id) {
				$state.transitionTo('management.application');
			} else {
				var originalApplication = $scope.selectedApplication.$getOriginal();
				angular.extend($scope.selectedApplication, originalApplication);
				me.formReset();
			}
		};
		
		$scope.confirmDeleteApplication = function (application) {
			$scope.selectedApplication = application;
			$scope.deletedEnabled = true;
			$('#deleteApplicationModal').modal('show');
		};
		
		$scope.deleteApplication = function () {
			$scope.deletedEnabled = false;
			$http['delete'](CLMAppLocations.getEntityUrl()).success(function () {
				angular.forEach($scope.applications, function (applicationCandidate, key) {
					if (applicationCandidate.id === $scope.selectedApplication.id) {
						$scope.applications.splice(key, 1);
						return false;
					}
				});
				$('#deleteApplicationModal').modal('hide');
				$state.transitionTo('management.application');
			}).error(function () { 
				$('#deleteApplicationModal').modal('hide');
				$scope.pushAlert({ type: 'error', msg: rejection.data });
			});
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
							$scope.pushAlert({ type: 'error', msg: 'Icon file size must be smaller than 5 MB.' });
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
				hudson.post(CLMAppLocations.getEntitiesUrl(), application).success(function (data) {
					applicationStore.refresh().then(function() {
                        $scope.selectedApplication.id = data.id;
						me.saveIcon();
					});
				}).error(function (data) { 
					$scope.submitActive = false;
                                        $scope.pushAlert({ type: 'error', msg: data });
				});
			} else {
				$http.put(CLMAppLocations.getEntitiesUrl(), application).success(function (data) {
					applicationStore.refresh().then(function() {
						me.saveIcon();
					});
				}).error(function (data) {
					$scope.submitActive = false;
                                        $scope.pushAlert({ type: 'error', msg: data });
				});
			}

			return false;
		};

          //defer to common name validations(unique, whitespace enforcement, etc)
          $scope.validateApplicationName = function (value) {
            $scope.applicationEditor.$invalid = false;

            var result = editorTools.validateName(value, $scope.selectedApplication, $scope.applications);

            if (result !== true) {
              $scope.applicationEditor.$invalid = true;
              return result;
            }
          };

          //unique IDs are required
          $scope.validateApplicationId = function (publicId) {
            $scope.applicationEditor.$invalid = false;

            if($.trim(publicId) === '_new_') {
              $scope.applicationEditor.$invalid = true;
              return 'This is a reserved value';
            }

            var result = true;
            for (var i = 0; i < $scope.applications.length; i++) {
              if (publicId === $scope.applications[i].publicId) {
                result = false;
              }
            }

            if (result !== true) {
              $scope.applicationEditor.$invalid = true;
              return 'Id is already in use';
            }
          };
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