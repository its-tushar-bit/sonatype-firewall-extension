/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import CLMLocationModule from '../util/CLMLocation';
import utilityServicesModule from '../utility/services/utility.services.module';
import loginModalModule from 'MainRoot/user/LoginModal/module';

import isIqIframe from './isIqFrame';

export var httpInterceptors = angular.module('HttpInterceptors', []);

httpInterceptors.factory('unauthenticatedResponseHttpInterceptor', [
  '$window',
  '$q',
  '$rootScope',
  function ($window, $q, $rootScope) {
    return {
      responseError: function (response) {
        if (response.status === 401) {
          // $rootScope.username will be present if this is the top frame and login had already succeeded previously.
          // If we are in a child frame (for a report), the username won't be available but we can still detect that
          // we are in a child frame.
          if ($rootScope.username || isIqIframe($window)) {
            // session expired - tell SessionSecurityService of the main IQ UI, which resides in the top frame of
            // the page.
            $window.top.sessionExpired();
          } else {
            // new promise for each failure, that will be completed once login suceeds
            var deferred = $q.defer();

            //fresh page load, not logged in yet
            $rootScope.$emit('userNeedsAuthentication', response, deferred);

            return deferred.promise;
          }
        }

        return $q.reject(response);
      },
    };
  },
]);

// This is the cache busting interceptor factory, which handles adding a timestamp query parameter to each request
httpInterceptors.factory('cacheBusterHttpInterceptor', [
  function () {
    return {
      request: function (config) {
        if (
          (config.url.indexOf('/rest/') > -1 || config.url.indexOf('/api/') > -1 || config.url.indexOf('.json') > -1) &&
          config.url.indexOf('timestamp=') < 0
        ) {
          config.params = config.params || {};
          config.params.timestamp = new Date().getTime();
        }
        return config;
      },
    };
  },
]);

// Apply the interceptor to the httpProvider during config
httpInterceptors.config([
  '$httpProvider',
  function ($httpProvider) {
    $httpProvider.interceptors.push('unauthenticatedResponseHttpInterceptor');
    $httpProvider.interceptors.push('cacheBusterHttpInterceptor');
    $httpProvider.defaults.xsrfCookieName = 'CLM-CSRF-TOKEN';
    $httpProvider.defaults.xsrfHeaderName = 'X-CSRF-TOKEN';
  },
]);

// Ideally this would be merged into the above code, no event would be emitted, but sadly,
// ui.bootstrap (for $modal) has a dependency on $http, therefore putting modal code in an http interceptor
// creates a circular dependency
export var unauthenticatedResponseHttpInterceptor = angular
  .module('UnauthenticatedResponseHttpInterceptor', [
    httpInterceptors.name,
    'ui.bootstrap',
    CLMLocationModule.name,
    utilityServicesModule.name,
    loginModalModule.name,
  ])
  .run([
    '$rootScope',
    '$q',
    '$http',
    '$ngRedux',
    'UnauthenticatedRequestQueueService',
    'LoginModalService',
    function ($rootScope, $q, $http, $ngRedux, UnauthenticatedRequestQueueService, LoginModalService) {
      $rootScope.$on('userNeedsAuthentication', function (event, response, deferred) {
        if (response.config && response.config.waitForLogin === false) {
          deferred.reject(response);
        } else {
          // add a new function to the queue that will handle resolving the promise retrieved from event emitter
          UnauthenticatedRequestQueueService.addRequest(function () {
            // simply replay the request
            $http(response.config).then(
              function () {
                deferred.resolve(arguments[0]);
              },
              function () {
                deferred.reject(arguments[0]);
              }
            );
          });
          // we only want to pop up the dialog for the first error, as many requests may be sent asynchronously, for
          // the other messages, the data will be added to the queue, but the dialog portion will be ignored
          if (UnauthenticatedRequestQueueService.getRequests().length === 1) {
            LoginModalService.authenticate(response.headers('WWW-Authenticate') === 'SAML').then(
              function () {
                // retry failed requests and then clear the queue
                $q.all(UnauthenticatedRequestQueueService.getPromises()).finally(function () {
                  UnauthenticatedRequestQueueService.clearRequests();
                });
              },
              function () {
                // login was cancelled
                UnauthenticatedRequestQueueService.clearRequests();
              }
            );
          }
        }
      });
    },
  ]);
