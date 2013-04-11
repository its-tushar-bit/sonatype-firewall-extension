/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp */
(function () {
    "use strict";

    var clmManagementApp = angular.module('clmManagementApp', ['AngularCommon', 'ApplicationManagement', 'Report', 'Profile', 'Admin'], ['$routeProvider', function ($routeProvider) {
        $routeProvider.when('/management', {
            templateUrl: 'components/management.html?' + clmBuildTimestamp,
            controller: 'ApplicationManagementController'
        });
        $routeProvider.when('/profiles', {
            templateUrl: 'components/profiles.html?' + clmBuildTimestamp,
            controller: 'ProfilePageController'
        });
        $routeProvider.when('/admin', {
            templateUrl: 'components/admin.html?' + clmBuildTimestamp,
            controller: 'AdminController'
        });
        $routeProvider.when('/report/:encodedApplicationId/:encodedStageId', {
            templateUrl: 'components/report.html?' + clmBuildTimestamp,
            controller: 'ReportController'
        });
        $routeProvider.otherwise({redirectTo: '/management'});
    }]);

    clmManagementApp.controller('TabController', ['$scope', '$location', '$rootScope', function ($scope, $location, $rootScope) {
        function handleTabClick(path, $event) {
            $event.preventDefault();
            function doTabChange() {
                $location.path(path);
            }

            var tabChangeEvent = $rootScope.$emit('tabChange', [$location.path(), doTabChange]);
            if (!tabChangeEvent.defaultPrevented) {
                doTabChange();
            }
        }

        $scope.managementTabClick = function ($event) {
            handleTabClick('/management', $event);
        };

        $scope.$watch(function () {
            return $location.path();
        }, function () {
            $scope.tabUrl = $location.path();
            angular.element('.modal-backdrop').remove(); // Bootstrap modal creates elements at the document root
        });
    }]);

	clmManagementApp.service('ApplicationId', function () {
		return {
			encoded : 'orgf0367c36c57a42f2a494ecb1ba26b7e7'
		};
	});
}());