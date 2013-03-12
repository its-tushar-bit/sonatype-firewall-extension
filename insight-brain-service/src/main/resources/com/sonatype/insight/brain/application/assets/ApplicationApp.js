/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp */
var applicationApp;
(function () {
    "use strict";

    applicationApp = angular.module('applicationApp', ['InsightAngularCommon', 'Management'], ['$routeProvider', function ($routeProvider) {
        $routeProvider.when('/management', {
            templateUrl: 'components/management.html?' + clmBuildTimestamp,
            controller: 'ManagementController'
        });
        $routeProvider.otherwise({redirectTo: '/management'});
    } ]);
}());