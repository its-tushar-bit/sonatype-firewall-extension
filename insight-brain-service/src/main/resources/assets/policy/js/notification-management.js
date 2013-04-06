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
		    delete $scope.currentNotificationEmail;
            $timeout(function () {
				// This seems to be required to trigger the validity check
                $scope.neditor.email.$setViewValue('');
            });
		}
		var addressArray = null;
		$scope.$on('editNotification', function (event, addresses) {
            resetInput();
            addressArray = addresses;
            $scope.notificationEmailList = angular.copy(addresses);
			sort($scope.notificationEmailList)
            $('#editNotificationsModal').modal('show');
            $('#editNotificationsModal input').focus();
		});

        $scope.addNotificationEmail = function() {
			if ($scope.neditor.$valid) {
				$scope.notificationEmailList.push($scope.currentNotificationEmail);
				resetInput();
				sort($scope.notificationEmailList);
			}
        };

        $scope.cancelNotificationEmail = function() {
            $('#editNotificationsModal').modal('hide');
        };

        $scope.doneNotificationEmail = function() {
        	addressArray.splice(addressArray.length);
        	angular.copy($scope.notificationEmailList, addressArray);
            $('#editNotificationsModal').modal('hide');
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
