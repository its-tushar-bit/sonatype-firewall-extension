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

	function sort(emailList) {
		emailList.sort(function(emailA, emailB) {
			return emailA > emailB ? 1 : emailA < emailB ? -1 : 0;
		});
	}

    module.controller('NotificationManagementController', [ '$scope', '$timeout', function($scope, $timeout) {
		function resetInput() {
			$scope.currentNotificationEmail = '';
            $timeout(function () {
				// This seems to be required to trigger the validity check
                $scope.neditor.email.$setViewValue('');
            });
		}

		$scope.$on('editNotification', function (event, actionData) {
            resetInput();
            $scope.notificationEmailList = [];
            $scope.currentActionStep = actionData;
			if ($scope.currentActionStep) {
				angular.forEach($scope.currentActionStep.actions, function (item, index) {
					if (item.action === 'notify') {
						$scope.notificationEmailList.push(item.target);
					}
				});
				sort($scope.notificationEmailList)
			}
            $('#editNotificationsModal').modal('show');
            $('#editNotificationsModal input').focus();
		});

        $scope.addNotificationEmail = function() {
            $scope.notificationEmailList.push($scope.currentNotificationEmail);
            resetInput();
            sort($scope.notificationEmailList);
        };

        $scope.cancelNotificationEmail = function() {
            $('#editNotificationsModal').modal('hide');
        };

        $scope.doneNotificationEmail = function() {
            $('#editNotificationsModal').modal('hide');
			for ( var i = $scope.currentActionStep.actions.length - 1; i >= 0; i--) {
				if ($scope.currentActionStep.actions[i].action === 'notify') {
					$scope.currentActionStep.actions.splice(i, 1);
				}
			}
			$scope.currentActionStep.notifyCount = 0;
			for ( var i = 0; i < $scope.notificationEmailList.length; i++) {
				$scope.currentActionStep.actions.push({
					action : 'notify',
					target : $scope.notificationEmailList[i]
				});
				$scope.currentActionStep.notifyCount++;
			}
        };

        $scope.removeNotificationEmail = function(index) {
            $scope.notificationEmailList.splice(index, 1);
        };
    }]);

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
				ctrl.$setValidity('invalid', false);
				ctrl.$parsers.unshift(function(newValue) {
					var notInvalid = !newValue || EMAIL_REGEXP.test(newValue),
						notDuplicate = true;
					for ( var i = 0; i < directiveScope.notificationEmailList.length; i++) {
						if (directiveScope.notificationEmailList[i] === newValue) {
							notDuplicate = false;
							break;
						}
					}
					ctrl.$setValidity('invalid', notInvalid);
					ctrl.$setValidity('unique', notDuplicate);
                    return (notInvalid && notDuplicate) ? newValue : undefined;
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
