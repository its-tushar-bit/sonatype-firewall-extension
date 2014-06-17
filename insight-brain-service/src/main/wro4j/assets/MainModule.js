/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, $ */
(function() {
  'use strict';
  var masterModalShown = false;

  var showMasterModal = function() {
    if (!masterModalShown) {
      masterModalShown = true;
      $('#unsavedModal').modal('show');
      $('.modal-backdrop').addClass('master-modal-backdrop');
    }
  };

  var hideMasterModal = function() {
    $('#unsavedModal').modal('hide');
    $('.modal-backdrop').removeClass('master-modal-backdrop');
    masterModalShown = false;
  };

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
    'ui.router', 'ui.bootstrap', 'CLMLocation', 'CommonServices',
    'ReportModule', 'Report', 'MainHeader', 'ngRoute', 'UnauthenticatedResponseHttpInterceptor', 'xeditable',
    'ProductFeaturesModule', 'HttpInterceptors', 'DashboardModule'
  ], [
    '$stateProvider', '$routeProvider', '$urlRouterProvider',
    function($stateProvider, $routeProvider, $urlRouterProvider) {
      $stateProvider.state('home', {
        url: '/',
        controller: angular.noop
      });

      var fn = function($rootScope) {
        if ($rootScope.initialized) {
          $rootScope.error = 'Unknown Address';
        }
      };
      fn.$inject = ['$rootScope'];
      $urlRouterProvider.otherwise(function($injector) {
        $injector.invoke(fn);
      });
    }
  ]).config([
    '$compileProvider', function($compileProvider) {
      /**
       * Allow for images to be sourced from blobs. This was removed from AngularJS with closed issue:
       * https://github.com/angular/angular.js/issues/3889
       */
      $compileProvider.imgSrcSanitizationWhitelist(/^\s*(https?|ftp|file|blob):|data:image\//);
    }
  ]).service('licenseChecker', [
    '$http', '$q', 'CLMLocations', function($http, $q, CLMLocations) {
      return {
        check: function() {
          var deferred = $q.defer();
          $http.get(CLMLocations.getLicenseSummaryUrl()).success(function(data) {
            deferred.resolve(data);
          }).error(function(data, status) {
            deferred.reject(status);
          });
          return deferred.promise;
        }
      };
    }
  ]).service('initService', [
    'licenseChecker', '$rootScope', 'ProductFeatures', '$state', '$window', '$location', 'Messages', 'CurrentUser', '$q',
    function(licenseChecker, $rootScope, ProductFeatures, $state, $window, $location, messages, currentUser, $q) {
      var savedState = null,
          stateChangePrevention = $rootScope.$on('$stateChangeStart', function(event, toState, toParams) {
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

        stateChangePrevention(); // Remove block
        if (savedState) {
          $state.transitionTo(savedState.toState, savedState.toParams);
        }
        else if (ProductFeatures.isDashboardLicensed()) {
          $state.go('dashboard.overview');
        }
        else {
          $state.go('violations');
        }
      }

      function initFailure(data) {
        if (data === 402) {
          // Server is unlicensed, redirect to product licensing page
          if ($window.location.href.indexOf('/index.html') === -1) {
            $window.location.replace('index.html#/management/configuration/productlicense');
          }
          else {
            stateChangePrevention(); // Remove existing block
            $rootScope.$on('$stateChangeStart', function(event, toState) {
              if (toState.name !== 'management.configuration.productlicense') {
                event.preventDefault();
              }
            });
            $rootScope.initialized = true;
            $state.transitionTo('management.configuration.productlicense');
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

        $rootScope.$on('$stateChangeError', function(event, toState, toParams, fromState, fromParams, error) {
          if (typeof error === 'string') {
            $rootScope.error = error;
          }
          else {
            $rootScope.error = messages.getHttpErrorMessage(error);
          }
        });

        // The page contains unsaved changes, continuing will discard them.
        $rootScope.tempState = null;

        $rootScope.$on('$locationChangeStart', function(event, newUrl, oldUrl) {
          var e;
          $rootScope.tempNewUrl = null;
          $rootScope.tempDestination = $location.url();

          if (newUrl !== oldUrl && newUrl !== $rootScope.tempState) {
            // special case where back button is hit, locationUrl will be the same as the oldUrl!!
            if (oldUrl.indexOf($rootScope.tempDestination) > -1) {
              $rootScope.tempDestination = newUrl.substring(newUrl.indexOf('#') + 1);
            }
            // give components a chance to negate the page change
            e = $rootScope.$broadcast('pageChangeStarted', $rootScope.tempDestination);
            if (e.defaultPrevented) {
              event.preventDefault();
              $rootScope.tempNewUrl = newUrl;
              showMasterModal();
              return;
            }

            $rootScope.$broadcast('pageChangeAccepted', $rootScope.tempDestination);
          }
          $rootScope.tempState = null;
        });

        var fn = function() {
          if (!masterModalShown) {
            var e = $rootScope.$broadcast('pageChangeStarted');
            return e.defaultPrevented ? e.message ||
                'The page may contain unsaved changes, continuing will discard them.' : undefined;
          }
        };

        // make sure to cleanup event listeners
        $rootScope.$on('$destroy', function() {
          $rootScope.$broadcast('pageChangeAccepted', $rootScope.tempDestination);
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
  ]).controller('UnsavedController', [
    '$rootScope', '$scope', '$location',
    function($rootScope, $scope, $location) {
      $scope.close = function(shouldContinue) {
        hideMasterModal();
        if (shouldContinue) {
          $rootScope.$broadcast('pageChangeAccepted', $scope.tempDestination);
          $rootScope.tempState = $rootScope.tempNewUrl;
          $location.url($scope.tempDestination);
        }
      };
    }
  ]);
}());