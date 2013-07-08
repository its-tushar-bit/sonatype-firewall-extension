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
	}]).run(['$rootScope', '$location', 'Messages', function ($rootScope, $location, messages) {
		// The page contains unsaved changes, continuing will discard them.
		var state = null;

		$rootScope.$on('$stateChangeError', function (event, toState, toParams, fromState, fromParams, error) {
		    $rootScope.error = messages.getHttpErrorMessage(error);
		});

		$rootScope.$on('$locationChangeStart', function (event, newUrl, oldUrl) {
			// initial page load triggers state where new URL == old URL
			var e;
			$rootScope.tempNewUrl = null;
			$rootScope.tempState = null;
			$rootScope.tempDestination = $location.$$url;
			
            if (newUrl !== oldUrl && newUrl != $rootScope.tempState) {
			    //give components a chance to negate the page change
				e = $rootScope.$broadcast('pageChangeStarted', $rootScope.tempDestination);
				if (e.defaultPrevented) {
					event.preventDefault();
					$rootScope.tempNewUrl = newUrl;
					$('#unsavedModal').modal('show');
					$('.modal-backdrop').addClass('master-modal-backdrop');
					return;
				}
			}
            $rootScope.tempState = null;
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

	dashboardApp.controller('UnsavedController', ['$rootScope', '$scope', '$location', function ($rootScope, $scope, $location) {
		$scope.close = function(shouldContinue) {
		    if (shouldContinue) {
                $rootScope.$broadcast('pageChangeAccepted', $rootScope.tempDestination);
                $rootScope.tempState = $rootScope.tempNewUrl;
                $location.url($rootScope.tempDestination);
            }
		    $('#unsavedModal').modal('hide');
		    $('.modal-backdrop').removeClass('master-modal-backdrop');
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
				name: 'Management',
				state: 'management'
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