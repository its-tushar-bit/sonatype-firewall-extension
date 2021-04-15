/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global Base64 */
function LoginModalController(
  $scope,
  $http,
  CLMLocations,
  Messages,
  routeStateUtilService,
  $window,
  showSamlSso,
  identityProviderName
) {
  var vm = this;

  vm.username = '';
  vm.password = '';
  vm.error = undefined;
  vm.loginMask = undefined;
  vm.showSamlSso = showSamlSso;
  vm.identityProviderName = identityProviderName ? identityProviderName : 'identity provider';

  $scope.$watchGroup(
    [
      function () {
        return vm.username;
      },
      function () {
        return vm.password;
      },
    ],
    function () {
      vm.error = undefined;
    }
  );

  vm.inAuthRequiredState = function () {
    return routeStateUtilService.stateRequiresAuthentication();
  };

  vm.signIn = function () {
    vm.error = undefined;

    vm.loginMask
      .wrap(
        $http.post(
          CLMLocations.getSessionUrl(),
          {},
          {
            // don't let the HttpInterceptor retry a failed request, as it might have failed due to bad credentials which this
            // modal should handle
            waitForLogin: false,
            headers: {
              Authorization: 'Basic ' + Base64.encode(vm.username + ':' + vm.password),
            },
          }
        )
      )
      .then(
        function () {
          $scope.$close();
        },
        function (error) {
          vm.error = Messages.getHttpErrorMessage(error);
        }
      );
  };

  vm.initiateSamlSso = function () {
    let destination = '../saml/login';
    if ($window.location.hash) {
      destination += '?hash=' + encodeURIComponent($window.location.hash);
    }
    $window.location.assign(destination);
  };

  vm.showSamlSso = showSamlSso;

  vm.isSignInButtonDisabled = function () {
    return vm.showSamlSso && (!vm.username || !vm.password);
  };

  vm.isSingleSignOnPreferred = function () {
    return vm.showSamlSso && !vm.username && !vm.password;
  };
}

LoginModalController.$inject = [
  '$scope',
  '$http',
  'CLMLocations',
  'Messages',
  'routeStateUtilService',
  '$window',
  'showSamlSso',
  'identityProviderName',
];

export default LoginModalController;
