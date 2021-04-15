/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pick } from 'ramda';
import modalWrapperTemplate from './userDetailsModalWrapper.html';
import { faUserCircle } from '@fortawesome/pro-solid-svg-icons';
import { faCaretDown } from '@fortawesome/free-solid-svg-icons';
import template from './userMenu.html';
import { showUserTokenModal } from './userToken/userTokenActions';

function UserMenuController($rootScope, $scope, $http, $ngRedux, CLMLocations, Modal, messages, pendoService, actions) {
  var vm = this;
  vm.logoutMask = undefined;

  vm.faUserCircle = faUserCircle;
  vm.faCaretDown = faCaretDown;

  const mapDispatchToProps = {
    ...actions,
    showUserTokenModal,
  };

  Object.assign(vm, {
    $onInit() {
      vm.unsubscribe = $ngRedux.connect(mapStateToThis, mapDispatchToProps)(vm);
      vm.loadUser();
    },

    $onDestroy() {
      vm.unsubscribe();
    },

    logout() {
      const serverLogout = () => $http['delete'](CLMLocations.getSessionLogoutUrl());

      vm.logoutMask.wrap(
        pendoService
          .flush()
          // continue the logout whether the pendo flush succeeds or fails
          .then(serverLogout, serverLogout)
          .then(function (response) {
            $scope.$emit('logout', response.headers('Location'));
          })
      );
    },

    changePassword() {
      Modal.open({
        templateUrl: 'change-password-template',
        animation: false,
        backdrop: 'static',
        keyboard: false,
        controller: [
          '$scope',
          function (scope) {
            scope.result = {};
            scope.save = function () {
              if (this.passwordForm.$valid) {
                const { newPassword, originalPassword } = scope.result,
                  actuallyChanged = newPassword !== originalPassword;

                scope.error = null;
                scope.submitActive = true;

                $http
                  .put(CLMLocations.getChangeMyPasswordUrl(), {
                    oldPassword: originalPassword,
                    newPassword,
                  })
                  .then(
                    function () {
                      if (actuallyChanged) {
                        vm.passwordChanged();
                      }

                      scope.$close();
                    },
                    function (error) {
                      scope.submitActive = false;
                      scope.error = messages.getHttpErrorMessage(error);
                    }
                  );
              }
            };
          },
        ],
      });
    },

    manageUserToken() {
      vm.showUserTokenModal();
    },

    details() {
      function modalController($scope) {
        $scope.currentUser = vm.currentUser;
      }

      modalController.$inject = ['$scope'];

      Modal.open({
        template: modalWrapperTemplate,
        controller: modalController,
      });
    },
  });
}

function mapStateToThis({ user, userToken }) {
  return {
    ...pick(['currentUser', 'shouldDisplayNotice', 'canChangePassword'], user),
    ...pick(['isUserTokenModalVisible'], userToken),
  };
}

UserMenuController.$inject = [
  '$rootScope',
  '$scope',
  '$http',
  '$ngRedux',
  'CLMLocations',
  'Modal',
  'Messages',
  'pendoService',
  'userActions',
];

export default {
  template,
  controller: UserMenuController,
  controllerAs: 'vm',
};
