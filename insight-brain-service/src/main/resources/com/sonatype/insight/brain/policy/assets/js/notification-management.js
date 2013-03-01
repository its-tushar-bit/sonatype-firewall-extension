/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes
 *          the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
/* global angular, $ */
/* jslint plusplus: true */
(function() {
    'use strict';

    var module = angular.module('NotificationManagement', []);

    module.controller('NotificationManagementController', [ '$scope', 'global', function($scope, global) {
        $scope.state = global;

        $scope.editNotifications = function(actionData) {
            delete $scope.state.currentNotificationEmail;
            $scope.state.notificationEmailList = [];
            $scope.state.currentActionStep = actionData;
            if ($scope.state.currentActionStep.target) {
                $scope.state.notificationEmailList = $scope.state.currentActionStep.target.split('\n');
            }
            $('#editNotificationsModal').modal('show');
        }

        $scope.addNotificationEmail = function() {
            for (var i = $scope.state.notificationEmailList.length - 1; i >= 0; i--) {
                if ($scope.state.notificationEmailList[i] == $scope.state.currentNotificationEmail) {
                    return;
                }
            } 
            $scope.state.notificationEmailList.push($scope.state.currentNotificationEmail);
            item.targetCount = item.target.split('\n').length;
        }

        $scope.cancelNotificationEmail = function() {
            $('#editNotificationsModal').modal('hide');
        }

        $scope.doneNotificationEmail = function() {
            $('#editNotificationsModal').modal('hide');
            $scope.state.currentActionStep.target = $scope.state.notificationEmailList.join('\n');
            $scope.state.currentActionStep.targetCount = $scope.state.notificationEmailList.length;
        }

        $scope.removeNotificationEmail = function(index) {
            $scope.state.notificationEmailList.splice(index, 1);
        }
    } ]);

    var EMAIL_REGEXP = /^\S+@\S+\.\S+$/;
    module.directive('emailInput', function() {
        return {
            require : 'ngModel',
            link : function(scope, elm, attrs, ctrl) {
                ctrl.$parsers.unshift(function(viewValue) {
                    if (!viewValue) {
                        scope.state.notificationValidationMsg = "Enter an email address";
                        return undefined;
                    } else if (!EMAIL_REGEXP.test(viewValue)) {
                        scope.state.notificationValidationMsg = "Enter a valid email address";
                        return undefined;
                    } else {
                        delete scope.state.notificationValidationMsg;
                        return viewValue;
                    }
                });
            }
        };
    });

    module.directive('notificationManagement', function() {
        return {
            restrict : 'A',
            replace : true,
            transclude : true,
            templateUrl : 'components/notification-management.html'
        }
    });
}());
