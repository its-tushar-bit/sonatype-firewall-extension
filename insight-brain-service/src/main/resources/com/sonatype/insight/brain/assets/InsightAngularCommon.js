/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
var angularCommon;
(function () {
    "use strict";

    angularCommon = angular.module('InsightAngularCommon', []);

    angularCommon.directive('errorModal', function () {
        return {
            replace: true,
            templateUrl: '/assets/components/errorModal.html',
            link: function ($scope, element) {
                $scope.showError = function (errorResponse) {
                    $scope.errorResponse = errorResponse;
                    element.modal('show');
                };
                $scope.showServerError = function (data, status, headersFn, config) {
                    var header = headersFn();
                    if (header['content-type'] && header['content-type'].indexOf('text/html') === 0) {
                        $scope.errorResponse = 'Server Error';
                    } else {
                        $scope.errorResponse = data;
                    }
                    element.modal('show');
                };
                $scope.hideError = function () {
                    element.modal('hide');
                };
            }
        };
    });
}());