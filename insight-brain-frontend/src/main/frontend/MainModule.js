/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angularDebug */
import commonServicesModule from './util/CommonServices';
import formsModule from './FormsModule';
import CLMLocationModule from './util/CLMLocation';
import { httpInterceptors, unauthenticatedResponseHttpInterceptor } from './util/HttpInterceptors';
import IqHttpInterceptorsModule from './util/IqHttpInterceptors';
import productFeaturesModule from './util/ProductFeatures';
import { GETTING_STARTED_STATE } from './configuration/module';
import {
  REDIRECTED_ACTION,
  DEPARTED_ACTION,
  submitData,
} from './configuration/gettingStarted/gettingStartedTelemetryServiceHelper';
import reduxConfigModule from './reduxConfig/module';
import SessionSecurityModule from './SessionSecurityModule';
import mainHeaderModule from './mainHeader/module';
import navigationContainer from './navigationContainer/module';
import ReportModule from './ReportApp';
import dashboardModule from './dashboard/dashboard.module';
import Report from './report/ReportController';
import pendoModule from './pendo/module';
import externalLinkModule from './externalLink/module';
import utilityServicesModule from './utility/services/utility.services.module';
import unsavedChangesModalModule from './unsavedChangesModal/module';
import configurationModule from './configuration/module';
import legalModule from './legal/legal.module';
import { not, contains, path } from 'ramda';
import { attachAxiosInterceptors } from './utility/axiosConfig';

// this is a fix to bootstrap to stop the 'too much recursion' error when multiple modals are fighting for focus
$.fn.modal.Constructor.prototype.enforceFocus = function () {
  var that = this;
  var done = false;
  $(document).on('focusin.modal', function (e) {
    if (!done && that.$element[0] !== e.target && !that.$element.has(e.target).length) {
      done = true;
      that.$element.focus();
    }
  });
};

export const InitModule = angular
  .module(
    'InitModule',
    [
      'ui.router',
      'ui.bootstrap',
      CLMLocationModule.name,
      commonServicesModule.name,
      'ngAria',
      ReportModule.name,
      Report.name,
      mainHeaderModule.name,
      navigationContainer.name,
      'ngRoute',
      unauthenticatedResponseHttpInterceptor.name,
      'xeditable',
      productFeaturesModule.name,
      httpInterceptors.name,
      IqHttpInterceptorsModule.name,
      dashboardModule.name,
      formsModule.name,
      SessionSecurityModule.name,
      pendoModule.name,
      externalLinkModule.name,
      utilityServicesModule.name,
      unsavedChangesModalModule.name,
      legalModule.name,
      reduxConfigModule.name,
      configurationModule.name,
    ],
    [
      '$stateProvider',
      '$urlRouterProvider',
      function ($stateProvider, $urlRouterProvider) {
        $stateProvider
          .state('root', {
            url: '^',
            redirectTo: function (transition) {
              const injector = transition.injector(),
                ProductFeatures = injector.get('ProductFeatures'),
                ProductLicense = injector.get('ProductLicense'),
                CurrentUser = injector.get('CurrentUser'),
                $rootScope = injector.get('$rootScope'),
                $q = injector.get('$q'),
                Messages = injector.get('Messages');

              return $q.all([ProductFeatures.load(), ProductLicense.load(), CurrentUser.waitForLogin()]).then(
                function () {
                  if (ProductFeatures.isDashboardAvailable()) {
                    return 'dashboard.overview.violations';
                  } else if (ProductFeatures.isReportsListAvailable()) {
                    return 'violations';
                  } else {
                    return 'gettingStarted';
                  }
                },
                function (err) {
                  $rootScope.error = Messages.getHttpErrorMessage(err);
                }
              );
            },
          })
          .state('home', {
            url: '/',
            redirectTo: 'root',
          });

        var unknownErrorFunction = function ($rootScope) {
          $rootScope.error = 'Unknown Address';
        };
        unknownErrorFunction.$inject = ['$rootScope'];

        // Show unknown routing error if route is unknown
        $urlRouterProvider.otherwise(function ($injector) {
          $injector.invoke(unknownErrorFunction);
        });
      },
    ]
  )
  .config([
    '$compileProvider',
    function ($compileProvider) {
      /**
       * Allow for images to be sourced from blobs. This was removed from AngularJS with closed issue:
       * https://github.com/angular/angular.js/issues/3889
       */
      $compileProvider.imgSrcSanitizationWhitelist(/^\s*(https?|ftp|file|blob):|data:image\//);
      $compileProvider.debugInfoEnabled(angularDebug);
    },
  ])
  .service('initService', [
    '$rootScope',
    'ProductFeatures',
    '$state',
    '$window',
    '$location',
    'Messages',
    'CurrentUser',
    '$q',
    '$http',
    '$urlRouter',
    '$timeout',
    'state.history.service',
    'SessionSecurityService',
    'pendoService',
    'externalLinkModalService',
    'LoginModalService',
    'routeStateUtilService',
    'CLMLocations',
    'Messages',
    'ProductLicense',
    'unsavedChangesModalService',
    '$ngRedux',
    '$transitions',
    function (
      $rootScope,
      ProductFeatures,
      $state,
      $window,
      $location,
      messages,
      currentUser,
      $q,
      $http,
      $urlRouter,
      $timeout,
      StateHistoryService,
      SessionSecurityService,
      pendoService,
      externalLinkModalService,
      LoginModalService,
      routeStateUtilService,
      CLMLocations,
      Messages,
      ProductLicense,
      unsavedChangesModalService,
      $ngRedux,
      $transitions
    ) {
      var savedState = null,
        cancelPreLoginStateHandler,
        cancelUnlicensedStateChangeHandler;

      /**
       * Before login, prevent navigation to pages that require authentication, and trigger the login modal
       * when access to one is attempted
       */
      function preLoginStateHandler(event, state, params) {
        /*
         * as we init the system, we mix the preventing of $stateChangeStart events.
         * Because of this, the $urlRouter will not be updated with the proper current url if the user changes urls
         * while we are in a blocked state (need to login). This is something our functional tests do.  So here we
         * will force the urlRouter to update to whatever is in the address bar, thus not losing what page we go to
         * when the user logs in
         */
        $urlRouter.update(true);
        savedState = { state, params };

        if (routeStateUtilService.stateRequiresAuthentication(state)) {
          if (!$rootScope.username) {
            attemptLogin();
          }

          if (event) {
            event.preventDefault();
          }
        }
      }

      attachAxiosInterceptors(SessionSecurityService.setServerDate, $rootScope, $window, LoginModalService.show);

      function setRootError(err) {
        $rootScope.error = Messages.getHttpErrorMessage(err);
      }

      /**
       * @return a promise that resolves if the license is found or not installed, and which is rejected if there
       * is some other error getting the license information
       */
      function checkLicenseInfo() {
        /**
         * Before the license is fetched, do not go to any route
         */
        function preLicenseFetchStateHandler(event, state, params) {
          savedStateDuringLicenseFetch = { state, params };
          event.preventDefault();
        }

        function registerPreLoginStateHandler() {
          cancelPreLoginStateHandler = $rootScope.$on('$stateChangeStart', preLoginStateHandler);
        }

        function onLicenseSuccess({ productEdition }) {
          $rootScope.licensed = true;
          $rootScope.productEdition = productEdition;

          // replay state transtion caught while license was loading so that preLoginStateHandler can process it
          if (savedStateDuringLicenseFetch) {
            $state.go(savedStateDuringLicenseFetch.state, savedStateDuringLicenseFetch.params);
          }
        }

        function unlicensedStateChangeHandler(transition) {
          if (not(contains(transition.to().name, ['productlicense', 'proxyConfig']))) {
            return false;
          }
        }

        function onLicenseFailure(err) {
          cancelUnlicensedStateChangeHandler = $transitions.onStart({}, unlicensedStateChangeHandler);

          if (err.status === 402) {
            $state.go('productlicense');
          } else {
            return $q.reject();
          }
        }

        let savedStateDuringLicenseFetch = null,
          cancelPreLicenseFetchStateHandler = $rootScope.$on('$stateChangeStart', preLicenseFetchStateHandler);

        return ProductLicense.load()
          .finally(cancelPreLicenseFetchStateHandler)
          .finally(registerPreLoginStateHandler)
          .then(onLicenseSuccess, onLicenseFailure);
      }

      function initSuccess() {
        $rootScope.$state = $state;

        initExternalLinkClickHandler();

        cancelPreLoginStateHandler(); // Remove block

        if (savedState) {
          $state.go(savedState.state, savedState.params);
        }

        SessionSecurityService.init();
      }

      function initFailure() {
        $rootScope.error = 'Unable to initialize the application';
      }

      $transitions.onStart({ from: 'productlicense', to: 'gettingStarted' }, () => {
        const {
          productLicense: { installed },
        } = $ngRedux.getState();

        if (!installed) return;

        // Stop preventing state changes.  Otherwise, the navigation to the Getting Started page cannot be performed
        if (cancelUnlicensedStateChangeHandler) cancelUnlicensedStateChangeHandler();

        $state.go('gettingStarted');
        submitData(REDIRECTED_ACTION, {
          pageNavigatedFrom: $state.current.name,
        });

        $timeout(function () {
          $window.location.reload();
        });
      });

      function initExternalLinkClickHandler() {
        ProductFeatures.load().then(function () {
          if (!ProductFeatures.isAvailable('allow-external-hyperlinks')) {
            const externalLinkClickHandler = (e) => {
              const isExternalLink = (anchor) => anchor.hostname && anchor.hostname !== location.hostname;
              const anchor = getAnchor(e.target);
              if (isExternalLink(anchor)) {
                externalLinkModalService.open(anchor.href);
                e.stopImmediatePropagation();
                return false;
              }
            };
            $(document).on('click', 'a', externalLinkClickHandler);
            $window.externalLinkClickHandler = externalLinkClickHandler;
          }
        }, setRootError);
      }

      function getAnchor(target) {
        if (target.nodeName === 'A') {
          return target;
        } else {
          return getAnchor(target.parentNode);
        }
      }

      function attemptLogin() {
        currentUser.fetch();
      }

      function doStart() {
        $q.all([currentUser.waitForLogin(), checkLicenseInfo()])
          .then(function ([authenticationStatus]) {
            $rootScope.username = authenticationStatus.username;
            cancelLoginDismissListener();

            // This was already called at the bottom of `doStart`, but call it again here now that the user is
            // logged in.  It is safe to call multiple times
            pendoService.start();
          })
          .then(initSuccess, initFailure);

        //Init the service on app load
        StateHistoryService.register();

        $rootScope.$on('logout', function (event, toLocation) {
          $rootScope.username = null;
          if (toLocation != null) {
            $window.location.href = toLocation;
          } else {
            $window.location.assign('../');
          }
        });

        function clearRootScopeError() {
          if ($rootScope.error) {
            delete $rootScope.error;
          }
        }

        $rootScope.$on('$locationChangeStart', clearRootScopeError);
        $rootScope.$on('$stateChangeSuccess', clearRootScopeError);

        // This listener is active until login succeeds. If a page navigation occurs (successfully) before that
        // time, that means that the page navigated to must be one that allows unauthenticated use, so the login
        // modal should be closed without completing the login
        let cancelLoginDismissListener = $rootScope.$on('$stateChangeSuccess', function () {
          LoginModalService.dismiss('Navigated to a page that does not require authentication');
        });

        $rootScope.$on('$stateChangeError', function (event, toState, toParams, fromState, fromParams, error) {
          if (typeof error === 'string') {
            $rootScope.error = error;
          } else {
            setRootError(error);
          }
        });

        let isProcessingStateChange = false;

        function isPageDirty() {
          const state = $ngRedux.getState();
          const currentState = state.router.currentState;
          const isDirtyPath = currentState.data && currentState.data.isDirty;
          return isDirtyPath ? path(isDirtyPath, state) : false;
        }

        $rootScope.$on('$stateChangeStart', function (event, toState, toParams) {
          if (!isProcessingStateChange) {
            var e = $rootScope.$broadcast('pageChangeStarted');
            if (e.defaultPrevented || isPageDirty()) {
              isProcessingStateChange = true;
              event.preventDefault();
              unsavedChangesModalService
                .open()
                .then(
                  function () {
                    $state.go(toState, toParams);
                    $rootScope.$broadcast('pageChangeAccepted');
                  },
                  function () {
                    $rootScope.$broadcast('pageChangeCanceled');
                  }
                )
                .finally(() => {
                  isProcessingStateChange = false;
                });
            } else {
              $rootScope.$broadcast('pageChangeAccepted');
            }
          }
        });

        function unloadListener() {
          if ($state.current.name === GETTING_STARTED_STATE) {
            submitData(DEPARTED_ACTION, null, true);
          }

          if (!isProcessingStateChange) {
            var e = $rootScope.$broadcast('pageChangeStarted');

            $timeout(function () {
              $rootScope.$broadcast('pageChangeCanceled');
            });

            return e.defaultPrevented || isPageDirty()
              ? e.message || 'The page may contain unsaved changes, continuing will discard them.'
              : undefined;
          }
        }

        // make sure to cleanup event listeners
        $rootScope.$on('$destroy', function () {
          $rootScope.$broadcast('pageChangeAccepted');
          $($window).unbind('beforeunload', unloadListener);
        });

        // this causes the browser to notify the user that the page contains unsaved data
        $($window).bind('beforeunload', unloadListener);

        // Try to fetch the current user in order to see if we are already logged in, but do not attempt
        // to initiate a login here (we might be on a page that doesn't require auth)
        currentUser.fetch(false);

        pendoService.start();
      }

      return { start: doStart };
    },
  ]);

export const MainModule = angular.module('MainModule', [InitModule.name]).run([
  'initService',
  function (initService) {
    initService.start();
  },
]);
