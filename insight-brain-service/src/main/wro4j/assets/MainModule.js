/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, $, angularDebug */
(function() {
  'use strict';

  // this is a fix to bootstrap to stop the 'too much recursion' error when multiple modals are fighting for focus
  $.fn.modal.Constructor.prototype.enforceFocus = function() {
    var that = this;
    var done = false;
    $(document).on('focusin.modal', function(e) {
      if (!done && that.$element[0] !== e.target && !that.$element.has(e.target).length) {
        done = true;
        that.$element.focus();
      }
    });
  };

  angular.module('InitModule', [
    'ui.router', 'ui.bootstrap', 'CLMLocation', 'CommonServices', 'ngAria',
    'ReportModule', 'Report', 'MainHeader', 'ngRoute', 'UnauthenticatedResponseHttpInterceptor', 'xeditable',
    'ProductFeaturesModule', 'HttpInterceptors', 'DashboardModule', 'FormsModule'
  ], [
    '$stateProvider', '$routeProvider', '$urlRouterProvider',
    function($stateProvider, $routeProvider, $urlRouterProvider) {
      $stateProvider.state('home', {
        url: '/',
        controller: angular.noop
      });

      var unknownErrorFunction = function($rootScope) {
        if ($rootScope.initialized) {
          $rootScope.error = 'Unknown Address';
        }
      };
      var removeErrorFunction = function($rootScope) {
        if ($rootScope.error) {
          delete $rootScope.error;
        }
      };
      unknownErrorFunction.$inject = removeErrorFunction.$inject = ['$rootScope'];

      // First remove any existing routing errors
      $urlRouterProvider.rule(function ($injector) {
        $injector.invoke(removeErrorFunction);
      });
      // Show unknown routing error if route is unknown
      $urlRouterProvider.otherwise(function($injector) {
        $injector.invoke(unknownErrorFunction);
      });
    }
  ]).config([
    '$compileProvider', function($compileProvider) {
      /**
       * Allow for images to be sourced from blobs. This was removed from AngularJS with closed issue:
       * https://github.com/angular/angular.js/issues/3889
       */
      $compileProvider.imgSrcSanitizationWhitelist(/^\s*(https?|ftp|file|blob):|data:image\//);
      $compileProvider.debugInfoEnabled(angularDebug);
    }
  ]).service('licenseChecker', [
    '$http', '$q', 'CLMLocations', function($http, $q, CLMLocations) {
      return {
        check: function() {
          var deferred = $q.defer();
          $http.get(CLMLocations.getValidateLicenseUrl()).success(function(data) {
            deferred.resolve(data);
          }).error(function(data, status) {
            deferred.reject(status);
          });
          return deferred.promise;
        }
      };
    }
  ]).service('initService', [
    'licenseChecker', '$rootScope', 'ProductFeatures', '$state', '$window', '$location', 'Messages', 'CurrentUser', '$q', '$urlRouter', '$modal', '$timeout',
    function(licenseChecker, $rootScope, ProductFeatures, $state, $window, $location, messages, currentUser, $q, $urlRouter, $modal, $timeout) {
      var savedState = null,
          stateChangePrevention = $rootScope.$on('$stateChangeStart', function(event, toState, toParams) {
            //as we init the system, we mix the preventing of $stateChangeStart events and $locationChangeStart events
            //because of this, the $urlRouter will not be updated with the proper current url if the user changes urls
            //while we are in a blocked state (need to login), this is something our ITs do a fair amount.  So here we
            //will force the urlRouter to update to whatever is in the address bar, thus not losing what page we go to
            //when the user logs in
            $urlRouter.update(true);
            event.preventDefault();
            savedState = {
              toState: toState,
              toParams: toParams
            };
          });

      function initSuccess(data) {
        $rootScope.licensed = true;
        $rootScope.username = data[1].username;
        $rootScope.initialized = true;
        $rootScope.productEdition = data[0].productEdition;

        stateChangePrevention(); // Remove block
        if (savedState) {
          $state.transitionTo(savedState.toState, savedState.toParams);
        }
        else if (ProductFeatures.isDashboardLicensed()) {
          $state.go('dashboard.overview.newest-risk');
        }
        else {
          $state.go('violations');
        }
      }

      function initFailure(data) {
        if (data === 402) {
          // Server is unlicensed, redirect to product licensing page
          if ($window.location.href.indexOf('/index.html') === -1) {
            $window.location.replace('index.html#/productlicense');
          }
          else {
            stateChangePrevention(); // Remove existing block
            $rootScope.$on('$stateChangeStart', function(event, toState) {
              if (toState.name !== 'productlicense') {
                event.preventDefault();
              }
            });
            $rootScope.initialized = true;
            $state.transitionTo('productlicense');
          }
        }
        else {
          $rootScope.error = 'Unable to initialize the application';
        }
      }

      function doStart() {
        $q.all([licenseChecker.check(), currentUser, ProductFeatures.load()]).then(function(data){
          initSuccess(data);
        }, function(data){
          initFailure(data);
        });

        $rootScope.$on('logout', function() {
          $rootScope.username = null;
          $window.location.assign('..');
        });

        $rootScope.$on('$stateChangeSuccess', function() {
          if ($rootScope.error) {
            delete $rootScope.error;
          }
        });

        $rootScope.$on('$stateChangeError', function(event, toState, toParams, fromState, fromParams, error) {
          if (typeof error === 'string') {
            $rootScope.error = error;
          }
          else {
            $rootScope.error = messages.getHttpErrorMessage(error);
          }
        });

        var isShowingModal = false;
        function resetIsShowing() {
          // Allow $stateChangeStart to process before resetting modal
          $timeout(function() {
            isShowingModal = false;
          });
        }

        $rootScope.$on('$stateChangeStart', function(event, toState, toParams, fromState, fromParams) {
          if ((toState.name !== fromState.name || !angular.equals(toParams, fromParams)) && !isShowingModal) {
            var e = $rootScope.$broadcast('pageChangeStarted', toState, toParams);
            if (e.defaultPrevented) {
              event.preventDefault();
              isShowingModal = true;
              $modal.open({
                backdrop: 'static',
                keyboard: false,
                templateUrl: 'unsaved-modal',
                windowClass: 'master-modal'
              }).result.then(function() {
                resetIsShowing();
                $rootScope.$broadcast('pageChangeAccepted', toState, toParams);
                $state.go(toState, toParams);
              }, resetIsShowing);
            } else {
              $rootScope.$broadcast('pageChangeAccepted', toState, toParams);
            }
          }
        });

        var fn = function() {
          if (!isShowingModal) {
            var e = $rootScope.$broadcast('pageChangeStarted');
            return e.defaultPrevented ? e.message ||
                'The page may contain unsaved changes, continuing will discard them.' : undefined;
          }
        };

        // make sure to cleanup event listeners
        $rootScope.$on('$destroy', function() {
          $rootScope.$broadcast('pageChangeAccepted');
          $(window).unbind('beforeunload', fn);
        });

        // this causes the browser to notify the user that the page contains unsaved data
        $(window).bind('beforeunload', fn);
      }

      return {
        start: function() {
          doStart();
        }
      };
    }
  ]);

  angular.module('MainModule', ['InitModule']).run([
    'initService',
    function(initService) {
      initService.start();
    }
  ]);
}());
