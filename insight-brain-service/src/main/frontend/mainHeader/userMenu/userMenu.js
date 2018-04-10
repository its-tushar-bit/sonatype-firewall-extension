/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
function UserMenuController($scope, $http, CLMLocations, Modal, messages, CurrentUser, telemetryService,
                            defaultAdminPasswordChangedService) {
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
    $http['delete'](CLMLocations.getSessionLogoutUrl()).then(function(response) {
      $scope.$emit('logout', response.headers('Location'));
    });
  }

  function canChangePassword() {
    return vm.currentUser && vm.currentUser.clmUser;
  }

  function changePassword() {
    Modal.open({
      templateUrl: 'change-password-template',
      animation: false,
      backdrop: 'static',
      keyboard: false,
      controller: [
        '$scope', function(scope) {
          scope.result = {};
          scope.save = function() {
            if (this.passwordForm.$valid) {
              const { newPassword, originalPassword } = scope.result,
                  actuallyChanged = newPassword !== originalPassword;

              scope.error = null;
              scope.submitActive = true;

              $http.put(CLMLocations.getChangeMyPasswordUrl(), {
                oldPassword: originalPassword,
                newPassword
              }).then(function() {
                if (actuallyChanged) {
                  fireDefaultPasswordChangedTelemetry();
                }

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

  // Fire the telemetry event indicating that the default password was changed. Only do so if the password warning
  // is actually displayed - ie, if the current password (before the change) was actually the default
  function fireDefaultPasswordChangedTelemetry() {
    defaultAdminPasswordChangedService.shouldDisplayDefaultPasswordWarning().then(function(passwordIsDefault) {
      if (passwordIsDefault) {
        telemetryService.submitData('ADMIN_PASSWORD_CHANGE', {
          action: 'PASSWORD_CHANGED_FROM_DEFAULT'
        });
      }
    });
  }
}

UserMenuController.$inject = [
  '$scope', '$http', 'CLMLocations', 'Modal', 'Messages', 'CurrentUser', 'telemetryService',
  'defaultAdminPasswordChangedService'
];

angular.module('mainHeader').component('userMenu', {
  templateUrl: 'mainHeader/userMenu/userMenu.html?' + clmBuildTimestamp,
  controller: UserMenuController,
  controllerAs: 'vm'
});
