/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(){
	"use strict";
	
	var dashboardStores = angular.module('dashboardStores', ['CLMLocation', 'ResourceModule', 'Hudson', 'Policy']);
	
	dashboardStores.service('applicationStore', ['CLMLocations', 'CLMResource', function (clmLocations, clmResource) {
		var applicationStore = clmResource.getStore({
			id : 'id',
			url : clmLocations.getApplicationsUrl(),
			template : { id: null, publicId: null, name: null },
			params : {
				timestamp : new Date().getTime()
			}
		});
		return applicationStore;
	}]);
	
	var dashboardApp = angular.module('dashboardApp', ['ui.compat', 'ui.bootstrap', 'dashboardStores', 'Organization', 'LicenseThreatGroup'], ['$stateProvider', '$routeProvider', function ($stateProvider, $routeProvider) {	
		$stateProvider.state('home', {
			url : '/',
			controller : angular.noop
		}).state('management', {
			url : '/management',
			templateUrl : '../assets/management.html',
			controller : 'managementController',
			onEnter : function($state) {
				$state.selectedDashboard = {
					name: 'Management',
					state: 'management'
				};
			}
		}).state('management.application', {
			parent : 'management',
			url : '/application',
			controller : 'applicationController',
			templateUrl : '../application-assets/components/application-navigator.html',
			onEnter : function($state) {
				if ($state.current.name.indexOf("application.view") !== -1) {
					if (!$state.params.id) {
						$state.selectedApplication = null;
					}
				}
			}
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
		}).state('management.application.view.labels', {
			parent : 'management.application.view',
			url : '/labels',
			controller : angular.noop,
			template : ''
		}).state('management.application.view.licenses', {
			parent : 'management.application.view',
			url : '/licenses',
			controller : 'LicenseThreatGroupController',
			templateUrl : '../policy-assets/components/license-threat-group/license-threat-group.html'
		}).state('management.organization', {
			parent : 'management',
			url : '/organization',
			controller : 'OrganizationController',
			templateUrl : '../organization-assets/components/organization-navigator.html'			
		}).state('management.organization.view', {
			parent : 'management.organization',
			url : '/{organizationId}',
			controller : 'OrganizationController',
			templateUrl : '../organization-assets/components/organization-editor.html'
		});
		$routeProvider.when('', { redirectTo : '/management' });
	}]);
	
	dashboardApp.controller('dashboardController', function($scope, $state) {
		$scope.$state = $state;
		$scope.availableDashboards = [ 
			{
				name: 'Dashboard',
				state: 'dashboard'
			}, {
				name: 'Management',
				state: 'management'
			}, {
				name: 'Reports',
				state: 'reports'
			}];
	});

	dashboardApp.controller('managementController', function($scope, $state) {
		$scope.$state = $state;
		
		$scope.managementPanes = [
    		{
    			name: 'Applications',
    			state: 'management/application',
    			isEnabled: true
    		},
    		{
    			name: 'Organizations',
    			state: 'management/organization',
    			isEnabled: true
    		},
    		{
    			name: 'Security',
    			state: '',
    			isEnabled: true
    		},
    		{
    			name: 'Metadata',
    			state: '',
    			isEnabled: false
    		}
		];
		
		for (var i = 0; i < $scope.managementPanes.length; i++) {
			var normalizedState = $scope.managementPanes[i].state.replace('/', '.');
			if ($scope.$state.current.name.indexOf(normalizedState) !== -1) {
				$scope.$state.selectedPane = $scope.managementPanes[i];
				break;
			}
		}
	});
	
	dashboardApp.controller('applicationController', function($scope, $state, $timeout, applicationStore) {
		function switchApplication() {
			$scope.selectedApplication = null;
			if ($scope.$state.params.applicationPublicId !== null && $scope.applications) {
				for (var i = 0; i < $scope.applications.length; i++) {
					if ($scope.$state.params.applicationPublicId === $scope.applications[i].publicId) {
						$timeout(function () {
							$scope.selectedApplication = $scope.applications[i];
						}, 200);
						return;
					}
				}
				// TODO We might want to consider reloading the store at this point?
			}
		}

		$scope.$state = $state;
		$scope.isCurrentTab = function (tabName) {
			return $state.current.name.lastIndexOf(tabName) === $state.current.name.length - tabName.length;
		};

		applicationStore.get().then(function(applications) {
			$scope.applications = applications;
			switchApplication();
			$scope.$watch('$state.params.applicationPublicId', switchApplication);
		}, function (error) {
			alert(error.data);
		});
	});

	dashboardApp.controller('applicationEditorController', function($scope, $state, applicationStore) {
		$scope.$state = $state;

		$scope.encodeURIComponent = window.encodeURIComponent;
	});

	dashboardApp.service('ApplicationId', ['commonCodeFactory', '$state', function (commonCodeFactory, $state) {
		// TODO Are ui-router parameters encoded or decoded?
		return {
			encoded : function () {
				var applicationPublicId = $state.params.applicationPublicId;
				return applicationPublicId ? encodeURI(applicationPublicId) : null;
			}
		};
	}]);
}());