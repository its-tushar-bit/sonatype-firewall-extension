/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(){
	"use strict";
	
	var managementApp = angular.module('managementApp', ['ui.compat'], ['$stateProvider', '$routeProvider', function ($stateProvider, $routeProvider) {
		$stateProvider.state('home', {
			url : '/',
			controller : angular.noop,
			views: {
				'navbar' : {
					templateUrl : '../navbar.html',
					controller : angular.noop
				},
				'subnavbar' : {
					templateUrl : '../subnavbar.html',
					controller : function($scope) {
						$scope.availableDashboards = [ 'Dashboard', 'Management', 'Reports' ];
						$scope.selectedDashboard = 'Management';
						
						$scope.changeDashboard = function(dashboard) {
							alert(dashboard + ' is not availabe.');
						};
					}
				},
				'navigation' : {
					templateUrl : '../navigation.html',
					controller : function($scope) {
						$scope.managementPanes = [
                      		{
                      			name: 'Applications',
                      			isEnabled: true,
                      			isSelected: true
                      		},
                      		{
                      			name: 'Organizations',
                      			isEnabled: true
                      		},
                      		{
                      			name: 'Security',
                      			isEnabled: true
                      		},
                      		{
                      			name: 'Metadata',
                      			isEnabled: false
                      		}];
					}
				},
				'subnavigation' : {
					templateUrl : '../subnavigation.html',
					controller : angular.noop
				}
			}
		});
		$routeProvider.when('', { redirectTo : '/' });
	}]);
}());