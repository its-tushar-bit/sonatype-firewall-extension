/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
var angularCommon;
(function () {
    "use strict";

    angularCommon = angular.module('AngularCommon', []);

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

    angularCommon.directive('typeAhead', ['$parse', function ($parse) {
        'use strict';

        return {
            restrict: 'A',
            require: '?ngModel',
            link: function postLink($scope, element, attrs, controller) {
                var source = $parse(attrs.typeAhead)($scope);
                $scope.$watch(attrs.typeAhead, function (newSource, oldSource) {
                    if (oldSource !== newSource) {
                        source = newSource;
                    }
                });

                element.attr('data-provide', 'typeahead');
                element.typeahead({
                    source: function (query) {
                        return angular.isFunction(source) ? source.apply(this, arguments) : source;
                    },
                    updater: function (item) {
                        if (controller) {
                            $scope.$apply(function () {
                                controller.$setViewValue(item);
                            });
                        }
                        return item;
                    }
                });
            }
        };
    }]);

    angularCommon.directive('tip', function () {
        return function (scope, element, attrs) {
            $(element).tooltip();
        };
    });
}());