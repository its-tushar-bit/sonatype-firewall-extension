/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pick } from 'ramda';

function UserMenuController($rootScope, $scope, $http, $ngRedux, CLMLocations, Modal, messages, pendoService, actions) {
  var vm = this;

  Object.assign(vm, {
    $onInit() {
      vm.unsubscribe = $ngRedux.connect(mapStateToThis, actions)(vm);
      vm.loadUser();
    },

    $onDestroy() {
      vm.unsubscribe();
    },

    logout() {
      function serverLogout() {
        // TODO This ought to perform a dirty check before it simply logs the user out
        // https://issues.sonatype.org/browse/CLM-1251
        return $http['delete'](CLMLocations.getSessionLogoutUrl());
      }

      pendoService.flush()
          // continue the logout whether the pendo flush succeeds or fails
          .then(serverLogout, serverLogout)
          .then(function(response) {
            $scope.$emit('logout', response.headers('Location'));
          });
    },

    changePassword() {
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
                    vm.passwordChanged();
                    $rootScope.$broadcast('recalculateContainerHeights');
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
  });
}

function mapStateToThis({user}) {
  return pick(['currentUser', 'shouldDisplayNotice', 'canChangePassword'], user);
}

UserMenuController.$inject = [
  '$rootScope', '$scope', '$http', '$ngRedux', 'CLMLocations', 'Modal', 'Messages',
  'pendoService', 'userActions'
];

export default {
  templateUrl: 'mainHeader/userMenu/userMenu.html?' + clmBuildTimestamp,
  controller: UserMenuController,
  controllerAs: 'vm'
};
