/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes
 *          the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp */
/* jslint plusplus: true */
(function () {
    'use strict';

    var module = angular.module('NotificationManagement', []);

	function sort(emailList) {
		emailList.sort(function(emailA, emailB) {
			return emailA > emailB ? 1 : emailA < emailB ? -1 : 0;
		});
	}

    module.controller('NotificationManagementController', [ '$scope', '$timeout', function($scope, $timeout) {
        var EMAIL_REGEXP = /^\S+@\S+\.\S+$/;
        $scope.setEditorError = function (error) {
            $scope.error = error;
        };

        $scope.validateEmail = function (value) {
            return EMAIL_REGEXP.test(value);
        };
        
		function resetInput() {
			delete $scope.currentNotificationEmail;
            $timeout(function () {
				// This seems to be required to trigger the validity check
                $scope.neditor.email.$setViewValue('');
            });
		}
		$scope.$on('editNotification', function (event, addresses) {
            $scope.notificationEmailList = angular.copy(addresses);
            sort($scope.notificationEmailList);
            $('#editNotificationsModal').modal('show');
            $('#editNotificationsModal input').focus();
		});

        $scope.cancelNotificationEmail = function() {
            $('#editNotificationsModal').modal('hide');
        };

        $scope.doneNotificationEmail = function() {
            $scope.$broadcast('editNotificationDone',$scope.notificationEmailList)
			$('#editNotificationsModal').modal('hide');
        };

        
        //ditch edits in this case
        $scope.$on('pageChangeAccepted', function (event) {
            $scope.cancelNotificationEmail();
        });
    }]);

    module.directive('notificationmanagement', function() {
        return {
            restrict : 'A',
            replace : true,
            transclude : true,
            templateUrl : '../assets/components/notification-manager/notification-manager.html?' + clmBuildTimestamp
        };
    });
}());
