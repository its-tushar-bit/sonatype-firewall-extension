/**
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global Base64 */
export default function LoginModalController($scope, $http, CLMLocations, Messages, $window, showSamlSso) {
  var vm = this;

  vm.username = '';
  vm.password = '';
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

  vm.initiateSamlSso = function() {
    $window.location.assign('../saml/login');
  };

  vm.showSamlSso = showSamlSso;

  vm.isSignInButtonDisabled = function() {
    return vm.showSamlSso && (!vm.username || !vm.password);
  };

  vm.isSingleSignOnPreferred = function() {
    return vm.showSamlSso && !vm.username && !vm.password;
  };
}

LoginModalController.$inject = ['$scope', '$http', 'CLMLocations', 'Messages', '$window', 'showSamlSso'];
