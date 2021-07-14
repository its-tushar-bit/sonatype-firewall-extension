/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function UserListController(UserStore, CurrentUser, $scope, $q, isAuthorized, $state) {
  var username = null;

  $scope.context = {
    users: [],
  };

  $scope.isAuthorized = isAuthorized;

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
    $state.go('editUser', { userId: user.id });
  };

  $scope.isCurrentUser = function (user) {
    return username === user.username;
  };

  $scope.newUserClick = function () {
    $state.go('create');
  };

  $scope.doLoad();
}

UserListController.$inject = ['UserStore', 'CurrentUser', '$scope', '$q', 'isAuthorized', '$state'];
