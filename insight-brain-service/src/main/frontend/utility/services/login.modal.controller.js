/**
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global Base64 */
(function(angular) {
  'use strict';

  function LoginModalController($scope, $http, CLMLocations, $q, Messages, UnauthenticatedRequestQueueService) {
    var vm = this;

    vm.username = undefined;
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
        // blow through each failed request and resolve them
        $q.all(UnauthenticatedRequestQueueService.getPromises()).finally(function() {
          $scope.$close();
          UnauthenticatedRequestQueueService.clearRequests();
        });
      }, function(error) {
        vm.error = Messages.getHttpErrorMessage(error);
      });
    };
  }

  LoginModalController.$inject = ['$scope', '$http', 'CLMLocations', '$q', 'Messages',
                                  'UnauthenticatedRequestQueueService'];

  angular //
      .module('utility.services') //
      .controller('login.modal.controller', LoginModalController);

}(angular));
