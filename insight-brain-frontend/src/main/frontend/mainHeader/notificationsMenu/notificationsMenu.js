/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { faInbox } from '@fortawesome/pro-regular-svg-icons';

function NotificationsController($scope, $http, $sce, CLMLocations, timeAgoService, Messages) {

  var vm = this;

  vm.faInbox = faInbox;

  vm.$onInit = getNotifications;
  vm.openDetail = openDetail;
  vm.clearSelected = clearSelected;

  function processNotifications(notifications) {
    $scope.unreadNotificationCount = 0;
    angular.forEach(notifications, function(notification) {
      if (!notification.viewed) {
        $scope.unreadNotificationCount++;
      }
      notification.detailHtml = $sce.trustAsHtml(notification.detailHtml);

      var timeParts = timeAgoService.renderDate(notification.dateCreated);

      notification.age = timeParts.age;
      notification.ageQualifier = timeParts.qualifier;
    });
  }

  function openDetail(notification) {
    if ($scope.selectedNotification &&
        $scope.selectedNotification === notification) {
      $scope.selectedNotification = null;
    }
    else {
      $scope.selectedNotification = notification;
      if (!notification.viewed) {
        $http.post(CLMLocations.getNotificationViewedUrl(), {
          id: notification.id
        }).then(function() {
          notification.viewed = true;
          $scope.unreadNotificationCount--;
        });
      }
    }

    return false;
  }

  function clearSelected() {
    $scope.selectedNotification = null;
  }

  function getNotifications() {
    $scope.loading = true;

    $http.get(CLMLocations.getNotificationUrl()).then(function(response) {
      $scope.loading = false;
      $scope.notifications = response.data.notifications;
      processNotifications($scope.notifications);
    }, function(error) {
      $scope.loading = false;
      $scope.errorText = 'An error occurred while loading notifications. (' +
          Messages.getHttpErrorMessage(error) + ')';
      $scope.unreadNotificationCount = '!';
    });
  }
}

NotificationsController.$inject = ['$scope', '$http', '$sce', 'CLMLocations', 'timeAgoService', 'Messages'];

export default {
  controller: NotificationsController,
  controllerAs: 'vm',
  templateUrl: 'mainHeader/notificationsMenu/notificationsMenu.html?' + clmBuildTimestamp
};
