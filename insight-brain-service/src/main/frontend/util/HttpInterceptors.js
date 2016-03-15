/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */

// global function
(function() {
  'use strict';

  var httpInterceptors = angular.module('HttpInterceptors', []);

  // This is our unauthenticated interceptor factory, will handle creating the interceptor when necessary
  httpInterceptors.factory('unauthenticatedResponseHttpInterceptor', ['$q', '$rootScope', function($q, $rootScope) {
    return {
      responseError: function(response) {
        // user is unauthenticated, so send out event to handle this state and create a new promise, that will be
        // fulfilled once user properly logs in
        if (response.status === 401) {
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
        if ((config.url.indexOf('/rest/') > -1 || config.url.indexOf('.json') > -1) && config.url.indexOf('timestamp=') < 0) {
          config.params = config.params || {};
          config.params.timestamp = new Date().getTime();
        }
        return config;
      }
    };
  }]);

  // Apply the interceptor to the httpProvider during config
  httpInterceptors.config(['$httpProvider', function($httpProvider) {
    $httpProvider.interceptors.push('unauthenticatedResponseHttpInterceptor');
    $httpProvider.interceptors.push('cacheBusterHttpInterceptor');
    $httpProvider.defaults.xsrfCookieName = 'CLM-CSRF-TOKEN';
    $httpProvider.defaults.xsrfHeaderName = 'X-CSRF-TOKEN';
  }]);
  
  //Ideally this would be merged into the above code, no event would be emitted, but sadly, ui.bootstrap (for $modal) has a dependency
  //on $http, therefore putting modal code in an http interceptor creates a circular dependency
  angular.module('UnauthenticatedResponseHttpInterceptor', ['HttpInterceptors', 'AngularCommon', 'ui.bootstrap', 'CLMLocation', 'utility.services']).run([
    '$rootScope',
    '$q',
    '$http',
    'LoginModalService',
    'UnauthenticatedRequestQueueService',
    function($rootScope, $q, $http, LoginModalService, UnauthenticatedRequestQueueService) {
      $rootScope.$on('userNeedsAuthentication', function(event, response, deferred) {
        // if user is already processing login, this will be a login failure response so reject and let them try
        // again
        if (response.config && response.config.clmLogin) {
          deferred.reject(response);
        } else {
          // add a new function to the queue that will handle resolving the promise retrieved from event emitter
          UnauthenticatedRequestQueueService.addRequest(function() {
            // simply replay the request
            $http(response.config).then(function() {
              deferred.resolve(arguments[0]);
            }, function() {
              deferred.reject(arguments[0]);
            });
          });
          // we only want to pop up the dialog for the first error, as many requests may be sent asynchronously, for
          // the other messages, the data will be added to the queue, but the dialog portion will be ignored
          if (UnauthenticatedRequestQueueService.getRequests().length === 1) {
            LoginModalService.show();
          }
        }
      });
    }
  ]);
}());
