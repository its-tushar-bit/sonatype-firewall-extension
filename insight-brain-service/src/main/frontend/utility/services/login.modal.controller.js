/**
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global Base64 */
(function(angular) {
  'use strict';

  function LoginModalController($scope, $http, CLMLocations, Messages, username) {
    var vm = this;

    username = username || undefined;

    // username may optionally be specified programmatically using `resolve` or some other method of injection.
    // If the username is specified in this way then it will not be editable in the UI.  This facilitates
    // safe re-login after the session expires
    vm.username = username;
    vm.isUsernameDisabled = function() {
      return angular.isDefined(username);
    };
    vm.password = undefined;
    vm.error = undefined;
    vm.loginMask = undefined;

    $scope.$watchGroup([function() {
      return vm.username;
    }, function() {
      return vm.password;
    }], function() {
      vm.error = undefined;
    });

    vm.signIn = function() {
      vm.error = undefined;

      vm.loginMask.wrap($http.post(CLMLocations.getSessionUrl(), {}, {
        clmLogin: true,
        headers: {
          'Authorization': 'Basic ' + Base64.encode(vm.username + ':' + vm.password)
        }
      })).then(function() {
        $scope.$close();
      }, function(error) {
        vm.error = Messages.getHttpErrorMessage(error);
      });
    };
  }

  LoginModalController.$inject = ['$scope', '$http', 'CLMLocations', 'Messages', 'username'];

  angular //
      .module('utility.services') //
      .controller('login.modal.controller', LoginModalController);

}(angular));
