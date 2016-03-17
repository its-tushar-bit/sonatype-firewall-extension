/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
 /* global angular, clmServerVersion, clmBuildTimestamp */
(function() {
  'use strict';

  var module = angular.module('MainHeader',
      ['ui.router', 'AngularCommon', 'CLMLocation', 'ProductFeaturesModule', 'PermissionServiceModule', 'ngSanitize']);

  module.controller('LogoutController', ['$scope', '$http', 'CLMLocations', function ($scope, $http, CLMLocations) {
      $scope.logout = function () {
        // TODO This ought to perform a dirty check before it simply logs the user out
        // https://issues.sonatype.org/browse/CLM-1251
        $http['delete'](CLMLocations.getSessionLogoutUrl()).success(function(){
          $scope.$emit('logout');
        });
      };
    }]);

  module.controller('ChangePassword', ['$scope', '$http', '$modal', 'CLMLocations', 'CurrentUser', 'Messages', function ($scope, $http, modal, clmLocations, currentUser, messages) {
    var clmUser = null;

    // Errors should be handled @ application level
    currentUser.then(function (authenticationStatus) {
      clmUser = authenticationStatus.clmUser;
    }, angular.noop);

    $scope.canChangePassword = function () {
      return clmUser;
    };

    $scope.change = function () {
      modal.open({
        templateUrl : 'change-password-template',
        animation: false,
        backdrop: 'static',
        keyboard: false,
        windowClass: 'clm-modal',
        controller : ['$scope', function (scope) {
          scope.result = {};
          scope.save = function () {
            if (this.passwordForm.$valid) {
              scope.error = null;
              scope.submitActive = true;

              $http.put(clmLocations.getChangeMyPasswordUrl(), {
                oldPassword : scope.result.originalPassword,
                newPassword : scope.result.newPassword
              }).success(function () {
                scope.$close();
              }).error(function () {
                scope.submitActive = false;
                scope.error = messages.getHttpErrorMessage(arguments);
              });
            }
          };
        }]
      });
    };
  }]);

  module.controller('Notifications', ['$scope', '$http', '$sce', 'CLMLocations', 'timeAgoService', 'Messages',
      function($scope, $http, $sce, CLMLocations, timeAgoService, Messages) {
    function processNotifications(notifications) {
      $scope.unreadNotificationCount = 0;
      angular.forEach(notifications, function(notification){
        if (!notification.viewed) {
          $scope.unreadNotificationCount++;
        }
        notification.detailHtml = $sce.trustAsHtml(notification.detailHtml);

        var timeParts = timeAgoService.renderDate(notification.dateCreated);

        notification.age = timeParts.age;
        notification.ageQualifier = timeParts.qualifier;
      });
    }

    $scope.openDetail = function(notification) {
      if ($scope.selectedNotification && $scope.selectedNotification === notification) {
        $scope.selectedNotification = null;
      }
      else {
        $scope.selectedNotification = notification;
        if (!notification.viewed) {
          $http.post(CLMLocations.getNotificationViewedUrl(), {
            id: notification.id
          }).success(function() {
            notification.viewed = true;
            $scope.unreadNotificationCount--;
          });
        }
      }

      return false;
    };

    $scope.clearSelected = function() {
      $scope.selectedNotification = null;
    };

    $scope.getNotifications = function() {
      $scope.loading = true;

      $http.get(CLMLocations.getNotificationUrl()).success(function (data) {
        $scope.loading = false;
        $scope.notifications = data.notifications;
        processNotifications($scope.notifications);
      }).error(function () {
        $scope.loading = false;
        $scope.errorText = 'An error occurred while loading notifications. (' + Messages.getHttpErrorMessage(arguments) + ')';
        $scope.unreadNotificationCount = '!';
      });
    };

    //call on init so that we have a notification count
    $scope.getNotifications();
  }]);

  module.controller('mainHeaderController', ['$scope', '$state', 'CurrentUser', 'ProductFeatures', 'PermissionService', function($scope, $state, currentUser, ProductFeatures, PermissionService) {
    $scope.$state = $state;
    
    currentUser.then(function(status) {
      $scope.displayName = status.displayName;
    });
    
    $scope.getServerVersion = function() {
      return clmServerVersion;
    };

    $scope.hasAnyPermission = function() {
      return !angular.equals({}, $scope.permissions);
    };

    $scope.majorMinorVersion = clmServerVersion.split('.').splice(0, 2).join('.');

    $scope.isDashboardLicensed = ProductFeatures.isDashboardLicensed;

    $scope.permissions = {};

    PermissionService.getValidPermissions([
      'CONFIGURE_SYSTEM', 'MANAGE_PROPRIETARY', 'VIEW_ROLES'
    ]).then(function(permissions) {
      angular.forEach(permissions, function(permission){
        $scope.permissions[permission] = true;
      });
    });
  }]);

  module.directive('mainHeader', function () {
    return {
      restrict: 'A',
      controller: 'mainHeaderController',
      templateUrl : 'components/main-header/main-header.html?' + clmBuildTimestamp
    };
  });

  module.factory('CurrentUser', ['$http', '$q', 'CLMLocations', function ($http, $q, clmLocations) {
    var deferred = $q.defer();
    $http.get(clmLocations.getSessionUrl()).success(function (authenticationStatus) {
      deferred.resolve(authenticationStatus);
    }).error(function () {
      deferred.reject(arguments);
    });
    return deferred.promise;
  }]);
}());
