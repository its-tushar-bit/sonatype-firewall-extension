/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, window, $ */

// global function
(function() {
  "use strict";

  // interceptor module
  var httpInterceptors = angular.module('HttpInterceptors', ['AngularCommon', 'CLMLocation']);

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
          $rootScope.$broadcast('userNeedsAuthentication', response, deferred);
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
        if (!config.params) {
          config.params = {};
        }
        config.params.timestamp = new Date().getTime();
        return config;
      }
    };
  }]);

  // Apply the interceptor to the httpProvider during config
  httpInterceptors.config(function($httpProvider) {
    $httpProvider.interceptors.push('unauthenticatedResponseHttpInterceptor');
  });

  // run the module and spin up the listener note that I would have ideally just put this in the httpInterceptor factory
  // above rather than using an event, but then a circular dep error comes into play (seems $modal has a dep on $http)
  httpInterceptors.run([
      '$rootScope',
      '$modal',
      '$q',
      '$http',
      function($rootScope, $modal, $q, $http) {
        $rootScope.$on('userNeedsAuthentication', function(event, response, deferred) {
          // we only care about unauthenticated errors for this interceptor
          if (response.status == 401) {
            // if user is already processing login, this will be a login failure response so reject and let them try
            // again
            if ($rootScope.processingLogin) {
              $rootScope.processingLogin = false;
              deferred.reject(response);
            } else {
              if (!$rootScope.requestQueue) {
                $rootScope.requestQueue = [];
              }
              // add a new function to the queue that will handle resolving the promise retrieved from event emitter
              $rootScope.requestQueue.push(function() {
                // simply replay the request
                $http(response.config).then(function() {
                  deferred.resolve(arguments[0]);
                }, function() {
                  deferred.reject(arguments[0]);
                });
              });
              // we only want to pop up the dialog for the first error, as many requests may be sent asynchronously, for
              // the other messages, the data will be added to the queue, but the dialog portion will be ignored
              if ($rootScope.requestQueue.length === 1) {
                $modal.open({
                  backdrop: 'static',
                  keyboard: false,    
                  template: '<div class="modal-header" id="loginModalHeader"><h3>Authentication Required</h3></div>'
                    + '<form name="loginForm" class="form-horizontal">'      
                    + '<div class="modal-body"><p style="text-align:center;">Seems that your '
                          + 'session timed out, please sign in again</p><br>'
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
                        $scope.$watch('data', function() {
                          $scope.loginError = null;
                        });

                        // give template access to the processing state
                        $scope.isProcessing = function() {
                          return $rootScope.processingLogin;
                        };

                        // sign in the user
                        $scope.signIn = function() {
                          var authz = Base64.encode($scope.data.username + ':' + $scope.data.password);

                          $scope.loginError = null;
                          $rootScope.processingLogin = true;

                          $http.post(CLMLocations.getSessionUrl(), {}, {
                            headers: {
                              'Authorization': 'Basic ' + authz
                            }
                          }).success(function() {
                            var promises = [];
                            // blow through each failed request and resolve them
                            angular.forEach($rootScope.requestQueue, function(request) {
                              promises.push(request());
                            });
                            $q.all(promises).then(function() {
                              $rootScope.processingLogin = false;
                              $scope.$close();
                              $rootScope.requestQueue = [];
                            });
                          }).error(function(data, status, headers, config) {
                            $rootScope.processingLogin = false;
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
          } else {
            deferred.reject(response);
          }
        });
      }]);
}());
