/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';

  function UserMenuController($scope, $http, CLMLocations, modal, messages, CurrentUser) {
    var vm = this;

    vm.$onInit = getCurrentUser;
    vm.logout = logout;
    vm.canChangePassword = canChangePassword;
    vm.changePassword = changePassword;

    function getCurrentUser() {
      CurrentUser.then(function(authenticationStatus) {
        vm.currentUser = authenticationStatus;
      }, angular.noop);
    }

    function logout() {
      // TODO This ought to perform a dirty check before it simply logs the user out
      // https://issues.sonatype.org/browse/CLM-1251
      $http['delete'](CLMLocations.getSessionLogoutUrl()).then(function() {
        $scope.$emit('logout');
      });
    }

    function canChangePassword() {
      return vm.currentUser && vm.currentUser.clmUser;
    }

    function changePassword() {
      modal.open({
        templateUrl: 'change-password-template',
        animation: false,
        backdrop: 'static',
        keyboard: false,
        windowClass: 'clm-modal',
        controller: [
          '$scope', function(scope) {
            scope.result = {};
            scope.save = function() {
              if (this.passwordForm.$valid) {
                scope.error = null;
                scope.submitActive = true;

                $http.put(CLMLocations.getChangeMyPasswordUrl(), {
                  oldPassword: scope.result.originalPassword,
                  newPassword: scope.result.newPassword
                }).then(function() {
                  scope.$close();
                }, function(error) {
                  scope.submitActive = false;
                  scope.error = messages.getHttpErrorMessage(error);
                });
              }
            };
          }
        ]
      });
    }
  }

  UserMenuController.$inject = ['$scope', '$http', 'CLMLocations', '$modal', 'Messages', 'CurrentUser'];

  angular.module('mainHeader').component('userMenu', {
    templateUrl: 'mainHeader/userMenu/userMenu.html?' + clmBuildTimestamp,
    controller: UserMenuController,
    controllerAs: 'vm'
  });

}());
