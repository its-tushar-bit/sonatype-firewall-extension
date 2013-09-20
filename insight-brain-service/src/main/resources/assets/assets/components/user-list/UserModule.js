/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  "use strict";
  // Temporary home
  var module = angular.module('SecurityModule', ['ui.compat', 'ManagementModule'], ['$stateProvider', function($stateProvider) {
    $stateProvider.state('management.security', {
      parent: 'management',
      url: '/security',
      templateUrl: '../assets/components/user-list/security-navigation.html?' + clmBuildTimestamp,
      controller : 'SecurityMenuController'
    });
  }]);

  module.controller('SecurityMenuController', ['$state', '$scope', function ($state, $scope) {
    $scope.$state = $state;
  }]);
}());

(function() {
  "use strict";

  var module = angular.module('UserModule', ['ui.compat', 'SecurityModule', 'CLMLocation', 'ResourceModule'], ['$stateProvider', function($stateProvider) {
      $stateProvider.state('management.security.users', {
        parent: 'management.security',
        url: '/users',
        controller: 'UserListController',
        templateUrl: '../assets/components/user-list/user-list.html?' + clmBuildTimestamp
      });
  }]);
  
  module.service('UserStore', ['CLMLocations', 'CLMResource', function (clmLocations, clmResource) {
    var config = {
            id : 'id',
            template : {
              id : null,
              username : '',
              password : '',
              firstName : '',
              lastName : '',
              email : ''
            },
            url : clmLocations.getUserListUrl(),
            params: {
              timestamp: new Date().getTime()
            }
      },
      store = clmResource.getStore(config);

    return store;
  }]);

  module.controller('UserListController', ['$http', 'CLMLocations', 'UserStore', 'Messages', '$scope', function ($http, clmLocations, UserStore, messages, $scope) {
    $scope.doLoad = function () {
      $scope.error = null;

      UserStore.refresh().then(function (data) {
        $scope.users = data;
      }, function (error) {
        $scope.error = error;
      });
    };
    $scope.doLoad();
  }]);
}());