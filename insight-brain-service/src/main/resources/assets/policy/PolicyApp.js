/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, window, console, clmBuildTimestamp */
var policyApp;
(function () {
    "use strict";

    policyApp = angular.module('policyApp', ['AngularCommon', 'Labels', 'Policy', 'PolicyEditor', 'LicenseGroup', 'NotificationManagement', 'ngSanitize'], ['$routeProvider', function ($routeProvider) {
        $routeProvider.when('/policy', {
            templateUrl: 'components/policy.html?' + clmBuildTimestamp,
            controller: 'InsightPolicyController'
        });
        $routeProvider.when('/labels', {
            templateUrl: 'components/labels.html?' + clmBuildTimestamp,
            controller: 'LabelController'
        });
        $routeProvider.when('/policy/:policyId', {
            templateUrl: 'templates/policy-edit-page.html?' + clmBuildTimestamp,
            controller: 'PolicyEditorController'
        });
        $routeProvider.when('/license-threat-group', {
            templateUrl: 'components/license-threat-group.html?' + clmBuildTimestamp,
            controller: 'InsightLicenseGroupController'
        });
        $routeProvider.otherwise({redirectTo: '/policy'});
    }]);

    policyApp.controller('TabController', ['$scope', '$location', '$rootScope', function ($scope, $location, $rootScope) {
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

        $scope.policyTabClick = function ($event) {
            handleTabClick('/policy', $event);
        };

        $scope.labelTabClick = function ($event) {
            handleTabClick('/labels', $event);
        };

        $scope.licenseGroupTabClick = function ($event) {
            handleTabClick('/license-threat-group', $event);
        };

        $scope.$watch(function () {
            return $location.path();
        }, function () {
            $scope.tabUrl = $location.path();
            angular.element('.modal-backdrop').remove(); // Bootstrap modal creates elements at the document root
        });

        $rootScope.$on('editPolicyComplete', function () {
            window.location.hash = '#/policy';
        });
    }]);

    policyApp.run(['$http', '$rootScope', function ($http, $rootScope) {
        $rootScope.features = {};
        $http.get('../rest/features').success(function (data) {
            angular.forEach(data, function (value, key) {
                $rootScope.features[value] = true;
            });
        }).error(function () {
                if (console) {
                    console.log('Failed to load features, some features may not be available');
                }
            });
    }]);

    policyApp.filter('escape', function () {
        return function (input) {
            if (!input) {
                return input;
            }

            if (input.indexOf('<html>') >= 0) {
                return input;
            } else {
                return input.replace(/&/g, '&amp;')
                    .replace(/</g, '&lt;')
                    .replace(/>/g, '&gt;')
                    .replace(/\n/g, '<br/>');
            }
        };
    });

    policyApp.factory('global', function ($rootScope) {
        return {};
    });

    policyApp.service('ApplicationId', ['commonCodeFactory', function (commonCodeFactory) {
        return {
            encoded : commonCodeFactory.getEncodedQueryString('appId')
        }
    }]);
}());