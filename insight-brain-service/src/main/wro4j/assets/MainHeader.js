/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
 /* global angular, clmServerVersion, clmBuildTimestamp, AngularUtils */
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
        backdrop : 'static',
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
                scope.error = [AngularUtils.toAlert(messages.getHttpErrorMessage(arguments))];
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

    $scope.markAsRead = function(notification) {
      if (!notification.viewed) {
        $http.post(CLMLocations.getNotificationViewedUrl(), {
          id: notification.id
        }).success(function() {
          notification.viewed = true;
          $scope.unreadNotificationCount--;
        });
      }
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

    $scope.majorMinorVersion = clmServerVersion.split('.').splice(0, 2).join('.');

    $scope.isDashboardLicensed = ProductFeatures.isDashboardLicensed;

    PermissionService.isAuthorized(['ADMIN']).then(function(isAuthorized) {
      $scope.isAdmin = isAuthorized;
    });
  }]);

  module.directive('mainHeader', function () {
    return {
      restrict: 'A',
      controller: 'mainHeaderController',
      templateUrl : '../assets/components/main-header/main-header.html?' + clmBuildTimestamp
    };
  });

  module.directive('dropdownDetailPanel', [function() {
    return {
      templateUrl : 'dropdown-detail-panel-template',
      transclude : true,
      scope : {
        items : '=',
        item : '='
      },
      link: function($scope, element) {
        element.parent().on('click', function() {
          // Do not hide or toggle detail panel when clicking on the detail panel
          if (angular.element('.dropdown-sub-menu:hover').length > 0) {
            var hoverLink = angular.element('.dropdown-sub-menu:hover a:hover');
            if (hoverLink.length > 0) {
              window.open(hoverLink.attr('href'), '_blank');
            }

            return false;
          }

          AngularUtils.safeApply($scope, function() {
            angular.forEach($scope.items, function(item){
              if ($scope.item.id === item.id) {
                item.selected = !item.selected;
              }
              else {
                item.selected = false;
              }
            });
          });
          return false;
        });

        // Dropdown can be closed by both the dropdown toggle and by clicking on the page. Both these elements emit
        // a click.dropdown.data-api event which can be captured to deselect all dropdown items.
        element.parents('.dropdown').children('a[data-toggle="dropdown"]').first().add(angular.element('html')).on('click.dropdown.data-api', function() {
          AngularUtils.safeApply($scope, function() {
            $scope.item.selected = false;
          });
        });
      }
    };
  }]);

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