/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, $, angularDebug */
import commonServicesModule from './util/CommonServices';
import formsModule from './FormsModule';
import CLMLocationModule from './util/CLMLocation';
import {httpInterceptors, unauthenticatedResponseHttpInterceptor} from './util/HttpInterceptors';
import IqHttpInterceptorsModule from './util/IqHttpInterceptors';
import productFeaturesModule from './util/ProductFeatures';

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
  'ui.router', 'ui.bootstrap', CLMLocationModule.name, commonServicesModule.name, 'ngAria',
  'ReportModule', 'Report', 'mainHeader', 'ngRoute', unauthenticatedResponseHttpInterceptor.name, 'xeditable',
  productFeaturesModule.name, httpInterceptors.name, IqHttpInterceptorsModule.name, 'dashboard.module', formsModule.name,
  'SessionSecurityModule'
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
        $http.get(CLMLocations.getValidateLicenseUrl()).then(function(response) {
          deferred.resolve(response.data);
        }, function(errorResponse) {
          deferred.reject(errorResponse.status);
        });
        return deferred.promise;
      }
    };
  }
]).service('initService', [
  'licenseChecker', '$rootScope', 'ProductFeatures', '$state', '$window', '$location', 'Messages', 'CurrentUser',
  '$q', '$urlRouter', 'Modal', '$timeout', 'state.history.service', 'SessionSecurityService',
  function(licenseChecker, $rootScope, ProductFeatures, $state, $window, $location, messages, currentUser, $q,
           $urlRouter, Modal, $timeout, StateHistoryService, SessionSecurityService) {
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
        }),
        deregisterUnlicensedStateChangePreventer;

    function initSuccess(data) {
      $rootScope.licensed = true;
      $rootScope.initialized = true;
      $rootScope.productEdition = data[0].productEdition;
      $rootScope.$state = $state;

      stateChangePrevention(); // Remove block
      if (savedState) {
        $state.transitionTo(savedState.toState, savedState.toParams);
      }
      else if (ProductFeatures.isDashboardLicensed()) {
        $state.go('dashboard.overview.violations');
      }
      else {
        $state.go('violations');
      }

      SessionSecurityService.init();
    }

    function initFailure(data) {
      if (data === 402) {
        // Server is unlicensed, redirect to product licensing page
        if ($window.location.href.indexOf('/index.html') === -1) {
          $window.location.replace('index.html#/productlicense');
        }
        else {
          stateChangePrevention(); // Remove existing block
          deregisterUnlicensedStateChangePreventer = $rootScope.$on('$stateChangeStart', function(event, toState) {
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

    $rootScope.$on('licenseInstalled', function() {
      // Stop preventing state changes.  Otherwise, the navigation to the Getting Started page cannot be performed
      if (deregisterUnlicensedStateChangePreventer) {
        deregisterUnlicensedStateChangePreventer();
      }

      $state.go('gettingStarted');

      $timeout(function() {
        $window.location.reload();
      });
    });

    function doStart() {
      currentUser.then(function(authenticationStatus) {
        $rootScope.username = authenticationStatus.username;
      }).then(function() {
        return $q.all([licenseChecker.check(), ProductFeatures.load()]);
      }).then(function(data) {
        initSuccess(data);
      }, function(data) {
        initFailure(data);
      });

      //Init the service on app load
      StateHistoryService.register();

      $rootScope.$on('logout', function(event, toLocation) {
        $rootScope.username = null;
        if (toLocation != null) {
          $window.location.href = toLocation;
        }
        else {
          $window.location.assign('../');
        }
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

      $rootScope.$on('$stateChangeStart', function(event, toState, toParams) {
        if (!isShowingModal) {
          var e = $rootScope.$broadcast('pageChangeStarted');
          if (e.defaultPrevented) {
            event.preventDefault();
            isShowingModal = true;
            Modal.open({
              backdrop: 'static',
              keyboard: false,
              templateUrl: 'unsaved-modal'
            }).result.then(function() {
              resetIsShowing();
              $state.go(toState, toParams);
              $rootScope.$broadcast('pageChangeAccepted');
            }, function() {
              resetIsShowing();
              $rootScope.$broadcast('pageChangeCanceled');
            });
          }
          else {
            $rootScope.$broadcast('pageChangeAccepted');
          }
        }
      });

      var fn = function() {
        if (!isShowingModal) {
          var e = $rootScope.$broadcast('pageChangeStarted');

          $timeout(function() {
            $rootScope.$broadcast('pageChangeCanceled');
          });

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
