/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(){
	"use strict";
	
	var dashboardStores = angular.module('dashboardStores', ['CLMLocation', 'ResourceModule', 'Hudson']);
	
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
	
	var dashboardApp = angular.module('dashboardApp', ['ui.compat', 'ui.bootstrap', 'dashboardStores'], ['$stateProvider', '$routeProvider', function ($stateProvider, $routeProvider) {	
		$stateProvider.state('home', {
			url : '/',
			controller : angular.noop
		}).state('management', {
			url : '/management',
			templateUrl : '../management.html',
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
			templateUrl : '../../application-assets/components/application-navigator.html',
			onEnter : function($state) {
				if ($state.current.name.indexOf("application.view") !== -1) {
					if (!$state.params.id) {
						$state.selectedApplication = null;
					}
				}
			}
		}).state('management.application.view', {
			parent : 'management.application',
			url : '/{id}',
			controller : 'applicationEditorController',
			templateUrl : '../../application-assets/components/application-editor.html',
			onExit : function($state) {
				
			}
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
    			state: '',
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
	
	dashboardApp.controller('applicationController', function($scope, $state, applicationStore) {
		$scope.$state = $state;
		
		applicationStore.get().then(function(applications) {
			$scope.applications = applications;
			
			if ($scope.$state.current.name.indexOf("application.view") !== -1) {
				for (var i = 0; i < $scope.applications.length; i++) {
					if ($scope.$state.params.id === $scope.applications[i].id) {
						$scope.$state.selectedApplication = $scope.applications[i];
						break;
					}
				}
			}
		}, function (error) {
			alert(error.data);
		});
	});
	
	dashboardApp.controller('applicationEditorController', function($scope, $state, applicationStore) {
		$scope.$state = $state;

		$scope.encodeURIComponent = window.encodeURIComponent;
	});
}());