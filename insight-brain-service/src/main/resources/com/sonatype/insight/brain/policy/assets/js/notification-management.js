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
    
    var scope = {};

    module.controller('NotificationManagementController', [ '$scope', function($scope) {
        $scope.notificationManagement = scope;

        scope.editNotifications = function(actionData) {
            delete scope.currentNotificationEmail;
            scope.notificationEmailList = [];
            scope.currentActionStep = actionData;
            if (scope.currentActionStep.target) {
                scope.notificationEmailList = scope.currentActionStep.target.split('\n');
            }
            $('#editNotificationsModal').modal('show');
        };
        scope.addNotificationEmail = function() {
            if (scope.notificationValid) {
                scope.notificationEmailList.push(scope.currentNotificationEmail);
                delete scope.currentNotificationEmail;
                scope.notificationEmailList.sort(function(emailA, emailB){
                    return emailA > emailB ? 1 : emailA < emailB ? -1 : 0;
                });
            }
        };
        scope.cancelNotificationEmail = function() {
            $('#editNotificationsModal').modal('hide');
            delete scope.currentNotificationEmail;
        };
        scope.doneNotificationEmail = function() {
            $('#editNotificationsModal').modal('hide');
            delete scope.currentNotificationEmail;
            scope.currentActionStep.target = scope.notificationEmailList.join('\n');
            scope.currentActionStep.targetCount = scope.notificationEmailList.length;
        };
        scope.removeNotificationEmail = function(index) {
            scope.notificationEmailList.splice(index, 1);
        };
    } ]);
    
    module.directive('entersubmit', function () {
        return function(scope, element, attrs) {
            element.bind('keydown', function(e) {
                if (e.keyCode === 13) { // Enter
                    e.preventDefault();
                    element.trigger('submit');
                }
            });
        };
    });

    var EMAIL_REGEXP = /^\S+@\S+\.\S+$/;
    module.directive('emailInput', function() {
        return {
            require : 'ngModel',
            restrict : 'A',
            scope : false,
            link : function(directiveScope, elm, attrs, ctrl) {
                ctrl.$parsers.unshift(function(viewValue) {
                    delete scope.notificationValid;
                    delete scope.notificationValidationMsg;
                    if (!EMAIL_REGEXP.test(viewValue)) {
                        scope.notificationValidationMsg = "Enter a valid email address";
                    } else if (viewValue) {
                        scope.notificationValid = true;
                        for ( var i = 0; i < scope.notificationEmailList.length; i++) {
                            if (scope.notificationEmailList[i] === viewValue) {
                                scope.notificationValidationMsg = "Enter a unique email address";
                                scope.notificationValid = false;
                                break;
                            }
                        }
                    }

                    return viewValue;
                });
            }
        };
    });

    module.directive('notificationManagement', function() {
        return {
            restrict : 'A',
            replace : true,
            transclude : true,
            templateUrl : 'components/notification-management.html?' + clmBuildTimestamp
        }
    });
}());
