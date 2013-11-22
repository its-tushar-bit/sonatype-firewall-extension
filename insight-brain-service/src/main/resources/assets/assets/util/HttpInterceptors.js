/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, window, $ */

// global function
(function() {
  "use strict";

  var processingLogin = false, requestQueue = [], httpInterceptors = angular.module('HttpInterceptors', []);

  // This is our unauthenticated interceptor factory, will handle creating the interceptor when necessary
  httpInterceptors.factory('unauthenticatedResponseHttpInterceptor', ['$q', '$rootScope', function($q, $rootScope) {
    return {
      responseError: function(response) {
        // user is unauthenticated, so send out event to handle this state and create a new promise, that will be
        // fulfilled once user properly logs in
        if (response.status == 401) {
          // new promise for each failure, that will be completed once login suceeds
          var deferred = $q.defer();
          // broadcast the authentication event.
          $rootScope.$emit('userNeedsAuthentication', response, deferred);
          return deferred.promise;
        } else {
          // some other general error, just reject it and move on
          return $q.reject(response);
        }
      }
    };
  }]);

  // This is the cache busting interceptor factory, which handles adding a timestamp query parameter to each request
  // note it's not currently in use, would need to be pushed into the interceptor list in the httpInterceptors.config
  // call below
  httpInterceptors.factory('cacheBusterHttpInterceptor', [function() {
    return {
      request: function(config) {
        return angular.extend(config, {
          params: {
            timestamp: new Date().getTime()
          }
        });
      }
    };
  }]);

  // Apply the interceptor to the httpProvider during config
  httpInterceptors.config(function($httpProvider) {
    $httpProvider.interceptors.push('unauthenticatedResponseHttpInterceptor');
  });
  
  //Ideally this would be merged into the above code, no event would be emitted, but sadly, ui.bootstrap (for $modal) has a dependency
  //on $http, therefore putting modal code in an http interceptor creates a circular dependency
  angular.module('UnauthenticatedResponseHttpInterceptor', ['HttpInterceptors', 'AngularCommon', 'ui.bootstrap', 'CLMLocation']).run([
    '$rootScope',
    '$modal',
    '$q',
    '$http',
    function($rootScope, $modal, $q, $http) {
      $rootScope.$on('userNeedsAuthentication', function(event, response, deferred) {
        // if user is already processing login, this will be a login failure response so reject and let them try
        // again
        if (processingLogin) {
          processingLogin = false;
          deferred.reject(response);
        } else {
          // add a new function to the queue that will handle resolving the promise retrieved from event emitter
          requestQueue.push(function() {
            // simply replay the request
            $http(response.config).then(function() {
              deferred.resolve(arguments[0]);
            }, function() {
              deferred.reject(arguments[0]);
            });
          });
          // we only want to pop up the dialog for the first error, as many requests may be sent asynchronously, for
          // the other messages, the data will be added to the queue, but the dialog portion will be ignored
          if (requestQueue.length === 1) {
            $modal.open({
              backdrop: 'static',
              keyboard: false,    
              template: '<div class="modal-header" id="loginModalHeader"><h3>User Login</h3></div>'
                + '<form name="loginForm" class="form-horizontal">'      
                + '<div class="modal-body">'
                       + '<div class="control-group">'
                      + '<label class="control-label" for="login-username">Username</label>'
                      + '<div class="controls">'
                      + '<input type="text" id="login-username" placeholder="Enter Username"'
                      + ' ng-model="data.username" ng-required="true" autofill focus-input="true">' + '</div>' + '</div>'
                      + '<div class="control-group">'
                      + '<label class="control-label" for="login-password">Password</label>'
                      + '<div class="controls">'
                      + '<input type="password" id="login-password" placeholder="Enter Password"'
                      + ' ng-model="data.password" ng-required="true" autofill>' + '</div>' + '</div>' 
                      + '</div>' + '<div class="modal-footer">'
                      + '<span id="login-error" ng-show="loginError" class="alert alert-error"'
                      + 'style="margin-right:10px;">{{loginError}}</span>'
                      + '<button id="login-action" class="btn btn-primary" ng-click="signIn()" '
                      + 'ng-disabled="loginForm.$invalid || isProcessing()">Sign in</button>' + '</div>'+ '</form>',
              controller: ['$scope', '$http', 'CLMLocations', 'Messages', '$q',
                  function($scope, $http, CLMLocations, Messages, $q) {
                    // setup our data for binding
                    $scope.data = {};
                    
                    // Remove error when user changes login information
                    $scope.$watchCollection('data', function() {
                      $scope.loginError = null;
                    });

                    // give template access to the processing state
                    $scope.isProcessing = function() {
                      return processingLogin;
                    };
                    
                    $scope.getRequestQueue = function() {
                      return requestQueue;
                    }
                    
                    // sign in the user
                    $scope.signIn = function() {
                      var authz = Base64.encode($scope.data.username + ':' + $scope.data.password);

                      $scope.loginError = null;
                      processingLogin = true;

                      $http.post(CLMLocations.getSessionUrl(), {}, {
                        headers: {
                          'Authorization': 'Basic ' + authz
                        }
                      }).success(function() {
                        var promises = [];
                        // blow through each failed request and resolve them
                        angular.forEach(requestQueue, function(request) {
                          promises.push(request());
                        });
                        $q.all(promises).then(function() {
                          processingLogin = false;
                          $scope.$close();
                          requestQueue = [];
                        }, function(){
                          processingLogin = false;
                          $scope.$close();
                          requestQueue = [];
                        });
                      }).error(function(data, status, headers, config) {
                        processingLogin = false;
                        if (status === 401) {
                          $scope.loginError = 'Invalid credentials. Please try again.';
                        } else {
                          // Non-login related error occurred
                          $scope.loginError = Messages.getHttpErrorMessage(arguments);
                        }
                      });
                    };
                  }]
            });
          }
        }
      });
    }])
}());
