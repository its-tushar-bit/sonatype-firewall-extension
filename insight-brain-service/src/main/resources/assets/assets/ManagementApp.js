/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(){
	"use strict";

	var dashboardApp = angular.module('dashboardApp', ['ui.compat', 'ui.bootstrap', 'OrganizationModule', 'ApplicationModule'], ['$stateProvider', '$routeProvider', '$urlRouterProvider', function ($stateProvider, $routeProvider, $urlRouterProvider) {
		$stateProvider.state('home', {
			url : '/',
			controller : angular.noop
		});
		$routeProvider.when('', { redirectTo : '/management/application' });

		var fn = function ($rootScope, messages) {
			$rootScope.error = 'Unknown Address';
		};
		fn.$inject = [ '$rootScope', 'Messages' ];
		$urlRouterProvider.otherwise( function ($injector, $location) {
			$injector.invoke(fn);
		} );
	}]).run(['$rootScope', '$location', '$dialog', 'Messages', function ($rootScope, $location, $dialog, messages) {
		// The page contains unsaved changes, continuing will discard them.
		var state = null;

		$rootScope.$on('$stateChangeError', function (event, toState, toParams, fromState, fromParams, error) {
		    $rootScope.error = messages.getHttpErrorMessage(error);
		});

		$rootScope.$on('$locationChangeStart', function (event, newUrl, oldUrl) {
			// initial page load triggers state where new URL == old URL
			var destination = $location.$$url,
				e;
			if (newUrl !== oldUrl && newUrl != state) {
			    //give components a chance to negate the page change
				e = $rootScope.$broadcast('pageChangeStarted', destination);
				if (e.defaultPrevented) {
					event.preventDefault();
					$dialog.dialog({
						backdrop : true,
						keyboard : true,
						dialogFade : true,
						backdropClick : true,
						controller : 'UnsavedController',
						template : '<div class="modal-header">Unsaved Changes</div>' +
						    '<div class="modal-body">The page may contain unsaved changes, continuing will discard them.</div>' +
						    '<div class="modal-footer"><button type="button" class="btn" ng-click="close(false)">Cancel</button> <button type="button" class="btn btn-danger" ng-click="close(true)">Continue</button></div>'
					}).open().then(function (continueChange) {
						if (continueChange) {
							$rootScope.$broadcast('pageChangeAccepted', destination);
							state = newUrl;
							$location.url(destination);
						}
					});
					return;
				}
			}
		    state = null;
		});

		var fn = function (event) {
			var e = $rootScope.$broadcast('pageChangeStarted');
			return e.defaultPrevented  ? e.message || 'The page may contain unsaved changes, continuing will discard them.' : undefined;
		};

		//make sure to cleanup event listeners
		$rootScope.$on('$destroy', function () {
			$rootScope.$broadcast('pageChangeAccepted', destination);
			$(window).unbind('beforeunload', fn);
		});
		
		//this causes the browser to notify the user that the page contains unsaved data
		$(window).bind('beforeunload', fn);
	}]);

	dashboardApp.controller('UnsavedController', ['$scope', 'dialog', function ($scope, dialog) {
		$scope.close = function(shouldContinue) {
			dialog.close(shouldContinue);
		};
	}]);

	dashboardApp.controller('dashboardController', function($scope, $state) {
		function switchDashboard() {
			for (var i = 0; i < $scope.availableDashboards.length; i++) {
				if ($state.current.name.indexOf($scope.availableDashboards[i].state) !== -1) {
					$scope.selectedDashboard = $scope.availableDashboards[i];
					break;
				}
			}
		}
		
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
		
		$scope.$watch('$state.current.name', switchDashboard);
		switchDashboard();
	});
}());

(function () {
	'use strict';

	var managementModule = angular.module('ManagementModule', ['ui.compat'], ['$stateProvider', function ($stateProvider) {
		$stateProvider.state('management', {
			url : '/management',
			templateUrl : '../assets/management.html',
			controller : 'ManagementController'
		});
	}]);

	managementModule.controller('ManagementController', function($scope, $state, commonCodeFactory) {
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
			}
		];
		
		for (var i = 0; i < $scope.managementPanes.length; i++) {
			var normalizedState = $scope.managementPanes[i].state.replace('/', '.');
			if ($scope.$state.current.name.indexOf(normalizedState) !== -1) {
				$scope.$state.selectedPane = $scope.managementPanes[i];
				break;
			}
		}
		
		$scope.$watch('$state.current.name', function() {
			if ($state.current.name === 'management') {
				$state.transitionTo('management.application');
			}
		});

        $scope.syncAlerts = [];
        var error = commonCodeFactory.getEncodedQueryString('errorMessage');
        if (error) {
            $scope.syncAlerts.push({ type: 'error', msg: decodeURIComponent(error) });
        }
	});
}());