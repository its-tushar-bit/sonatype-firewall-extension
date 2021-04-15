/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, AngularUtils*/
import { pick } from 'ramda';

export default function UserListController(
  $http,
  clmLocations,
  UserStore,
  messages,
  CurrentUser,
  $scope,
  DeleteModalService,
  Modal,
  $q,
  isAuthorized,
  $state,
  $ngRedux,
  userActions
) {
  var username = null;

  $scope.context = {
    userEditMap: {},
    users: [],
  };

  $scope.isAuthorized = isAuthorized;
  const actions = pick(['passwordChangedForUser'], userActions);
  const unsubscribe = $ngRedux.connect(undefined, actions)($scope);
  $scope.$on('$destroy', unsubscribe);

  $scope.doLoad = function () {
    if (isAuthorized) {
      $scope.error = null;

      $q.all([UserStore.refresh(), CurrentUser.waitForLogin()]).then(
        function (results) {
          $scope.context.users = results[0];
          username = results[1].username;
        },
        function (error) {
          $scope.error = error;
        }
      );
    }
  };

  $scope.editClick = function (user) {
    $scope.context.userEditMap[user.id] = user;
    $scope.$broadcast('userEditClick', {
      userId: user.id,
    });
  };

  $scope.isCurrentUser = function (user) {
    return username === user.username;
  };

  $scope.resetPasswordClick = function (user) {
    Modal.open({
      templateUrl: 'reset-password-modal',
      scope: $scope,
      backdrop: 'static',
      keyboard: false,
      controller: [
        '$scope',
        function (scope) {
          scope.state = 'ready';
          scope.user = user;

          scope.cancelClick = function () {
            scope.$close();
          };

          scope.resetClick = function () {
            scope.state = 'pending';
            $http
              .put(clmLocations.getUserUrl() + '/' + user.id + '/reset')
              .then(
                function (response) {
                  scope.newPassword = response.data.newPassword;
                  scope.state = 'complete';
                  $scope.passwordChangedForUser(scope.user);
                },
                function (error) {
                  scope.state = 'failed';
                  scope.error = messages.getHttpErrorMessage(error);
                }
              );
          };

          scope.okClick = function () {
            scope.$close();
          };

          scope.flashInstalled = function () {
            return AngularUtils.hasFlash();
          };
        },
      ],
    });
  };

  $scope.removeClick = function (user) {
    DeleteModalService.deleteResource('User', user.username, user);
  };

  $scope.newUserClick = function () {
    $state.go('users.create');
  };

  $scope.closeUserCreateForm = function () {
    $scope.context.users.sort(function (a, b) {
      if (a.usernameLowercase < b.usernameLowercase) {
        return -1;
      } else if (a.usernameLowercase > b.usernameLowercase) {
        return 1;
      } else {
        return 0;
      }
    });

    // when a user is added by the user-create page, change the state back to the user list page
    $state.go('users');
  };

  $scope.closeUserEditForm = function (user) {
    $scope.context.userEditMap[user.id] = null;
  };

  $scope.doLoad();
}

UserListController.$inject = [
  '$http',
  'CLMLocations',
  'UserStore',
  'Messages',
  'CurrentUser',
  '$scope',
  'DeleteModalService',
  'Modal',
  '$q',
  'isAuthorized',
  '$state',
  '$ngRedux',
  'userActions',
];
