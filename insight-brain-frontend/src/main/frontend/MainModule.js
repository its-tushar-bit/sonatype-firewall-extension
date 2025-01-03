/* eslint-disable */
/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angularDebug */
import commonServicesModule from './utilAngular/CommonServices';
import CLMLocationModule from './util/CLMLocation';
import { httpInterceptors, unauthenticatedResponseHttpInterceptor } from './utilAngular/HttpInterceptors';
import IqHttpInterceptorsModule from './utilAngular/IqHttpInterceptors';
import configurationModule, { GETTING_STARTED_STATE } from './configuration/module';
import {
  DEPARTED_ACTION,
  REDIRECTED_ACTION,
  submitData,
} from './configuration/gettingStarted/gettingStartedTelemetryServiceHelper';
import reduxConfigModule from './reduxConfig/module';
import SessionSecurityModule from './SessionSecurityModule';
import mainHeaderModule from './mainHeader/module';
import navigationContainer from './navigationContainer/module';
import ReportModule from './ReportApp';
import dashboardModule from './dashboard/dashboard.module';
import Report from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/module';
import routeProductLicenseValidator from './routeProductLicenseValidator/module';
import pendoModule from './pendo/module';
import externalLinkModule from './externalLink/module';
import utilityServicesModule from './utility/services/utility.services.module';
import unsavedChangesModalModule from './unsavedChangesModal/module';
import loginModalModule from './user/LoginModal/module';
import legalModule from './legal/legal.module';
import toastContainerModule from './toastContainer/module';
import { contains, isEmpty, not, path, tryCatch } from 'ramda';
import { attachAxiosInterceptors } from './utility/axiosConfig';
import { requestNotificationPermission } from './utility/services/notificationService';
import { actions } from 'MainRoot/productFeatures/productFeaturesSlice';
import {
  selectIsAllowExternalHyperlinksSupported,
  selectIsFirewallSupportedForNavigationContainer,
  selectIsDashboardSupported,
  selectIsDashboardWaiversSupported,
  selectIsFirewallSupported,
  selectIsReportListSupported,
  selectIsSbomManagerEnabled,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { unwrapResult } from '@reduxjs/toolkit';
import { actions as toastSliceActions } from 'MainRoot/toastContainer/toastSlice';
import { selectToastSlice } from 'MainRoot/toastContainer/toastSelectors';
import { load as loadProductLicense } from 'MainRoot/configuration/license/productLicenseActions';
import { selectUnconfiguredRepoManager } from 'MainRoot/firewallOnboarding/firewallOnboardingSelectors';
import { actions as firewallOnboardingActions } from 'MainRoot/firewallOnboarding/firewallOnboardingSlice';
import { fab } from '@fortawesome/free-brands-svg-icons';
import { library } from '@fortawesome/fontawesome-svg-core';
import { selectIsSbomManagerOnlyLicense } from 'MainRoot/productFeatures/productLicenseSelectors';

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
      httpInterceptors.name,
      IqHttpInterceptorsModule.name,
      dashboardModule.name,
      SessionSecurityModule.name,
      pendoModule.name,
      externalLinkModule.name,
      utilityServicesModule.name,
      unsavedChangesModalModule.name,
      legalModule.name,
      reduxConfigModule.name,
      configurationModule.name,
      loginModalModule.name,
      toastContainerModule.name,
      routeProductLicenseValidator.name,
    ],
    [
      '$stateProvider',
      '$urlRouterProvider',
      '$locationProvider',
      function ($stateProvider, $urlRouterProvider, $locationProvider) {
        $stateProvider
          .state('root', {
            url: '^',
            redirectTo: function (transition) {
              const injector = transition.injector(),
                CurrentUser = injector.get('CurrentUser'),
                $rootScope = injector.get('$rootScope'),
                $q = injector.get('$q'),
                $ngRedux = injector.get('$ngRedux'),
                Messages = injector.get('Messages');
              return $q
                .all([
                  $ngRedux.dispatch(actions.fetchProductFeaturesIfNeeded()),
                  $ngRedux.dispatch(loadProductLicense()),
                  $ngRedux.dispatch(firewallOnboardingActions.loadUnconfiguredRepoManagers()),
                  CurrentUser.waitForLogin(),
                ])
                .then((results) => {
                  unwrapResult(results[0]);
                  const state = $ngRedux.getState();
                  const isDashboardAvailable = selectIsDashboardSupported(state);
                  const isFirewallAvailable = selectIsFirewallSupported(state);
                  const isFirewallEnabled = selectIsFirewallSupportedForNavigationContainer(state);
                  const isReportsListAvailable = selectIsReportListSupported(state);
                  const isSbomManagerEnabled = selectIsSbomManagerEnabled(state);
                  const isSbomManagerOnlyLicense = selectIsSbomManagerOnlyLicense(state);
                  const isWaiversDashboardAvailable = selectIsDashboardWaiversSupported(state);
                  const unconfiguredRepoManager = selectUnconfiguredRepoManager(state);

                  if (isSbomManagerEnabled && isSbomManagerOnlyLicense) {
                    return 'sbomManager.dashboard';
                  } else if (isFirewallAvailable && unconfiguredRepoManager && isFirewallEnabled) {
                    return 'firewallOnboarding.firewallOnboardingPage';
                  } else if (isDashboardAvailable) {
                    return 'dashboard.overview.violations';
                  } else if (isWaiversDashboardAvailable && !isFirewallEnabled) {
                    return 'dashboard.overview.waivers';
                  } else if (isFirewallAvailable) {
                    return 'firewall.firewallPage';
                  } else if (isReportsListAvailable) {
                    return 'violations';
                  }

                  return 'gettingStarted';
                })
                .catch((err) => {
                  $rootScope.error = Messages.getHttpErrorMessage(err);
                });
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

        /*
         * Angular catches click events at the <html> element and interferes with link clicks in such a way
         * that react <a> onClick handlers (which get bound on `document`) don't fire. The configuration below
         * disables this angular behavior. It is believed that this has no ill effect for us since we don't use
         * angular's built-in router
         */
        $locationProvider.html5Mode({ rewriteLinks: false });
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
    '$state',
    '$window',
    'CurrentUser',
    '$q',
    '$urlRouter',
    '$timeout',
    'state.history.service',
    'SessionSecurityService',
    'pendoService',
    'externalLinkModalService',
    'LoginModalService',
    'UnauthenticatedRequestQueueService',
    'routeStateUtilService',
    'Messages',
    'ProductLicense',
    'unsavedChangesModalService',
    '$ngRedux',
    '$transitions',
    function (
      $rootScope,
      $state,
      $window,
      currentUser,
      $q,
      $urlRouter,
      $timeout,
      StateHistoryService,
      SessionSecurityService,
      pendoService,
      externalLinkModalService,
      LoginModalService,
      UnauthenticatedRequestQueueService,
      routeStateUtilService,
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
        function attemptLoginIfNeeded() {
          if (!$rootScope.username) {
            attemptLogin();
          }
        }

        /*
         * as we init the system, we mix the preventing of $stateChangeStart events.
         * Because of this, the $urlRouter will not be updated with the proper current url if the user changes urls
         * while we are in a blocked state (need to login). This is something our functional tests do.  So here we
         * will force the urlRouter to update to whatever is in the address bar, thus not losing what page we go to
         * when the user logs in
         */
        $urlRouter.update(true);
        savedState = { state, params };

        const stateRequiresAuthenticationNow = routeStateUtilService.stateRequiresAuthenticationSync(state);

        switch (stateRequiresAuthenticationNow) {
          // don't know if auth required yet: prevent state load and wait for async result
          case undefined:
            event.preventDefault();
            routeStateUtilService.stateRequiresAuthentication(state).then((stateRequiresAuthentication) => {
              if (stateRequiresAuthentication) {
                attemptLoginIfNeeded();
              } else {
                $state.go(savedState.state, savedState.params);
              }
            });
            break;

          // page does require auth: prevent state load and show login
          case true:
            event.preventDefault();
            attemptLoginIfNeeded();
            break;
          // case false, page does not require auth: no need to do anything, just let the page show
        }
      }

      attachAxiosInterceptors(
        SessionSecurityService.setServerDate,
        $rootScope,
        $window,
        LoginModalService,
        UnauthenticatedRequestQueueService
      );

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

        function onLicenseSuccess({ productEdition, products }) {
          $rootScope.licensed = true;
          $rootScope.productEdition = productEdition;
          $rootScope.products = products;

          // replay state transition caught while license was loading so that preLoginStateHandler can process it
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
          if (err?.response?.status === 402) {
            $state.go('productlicense');
          } else {
            return $q.reject(err);
          }
        }

        let savedStateDuringLicenseFetch = null,
          cancelPreLicenseFetchStateHandler = $rootScope.$on('$stateChangeStart', preLicenseFetchStateHandler);

        return ProductLicense.load()
          .finally(cancelPreLicenseFetchStateHandler)
          .finally(registerPreLoginStateHandler)
          .then(onLicenseSuccess, onLicenseFailure);
      }

      const loadFontAwesomeBrandIcons = tryCatch(() => library.add(fab), console.error);

      function initSuccess() {
        $rootScope.$state = $state;

        initExternalLinkClickHandler();

        cancelPreLoginStateHandler(); // Remove block

        if (savedState) {
          $state.go(savedState.state, savedState.params);
        }
        requestNotificationPermission();
        SessionSecurityService.init();
        loadFontAwesomeBrandIcons();
      }

      function initFailure(err) {
        $rootScope.error = Messages.getHttpErrorMessage(err) || 'Unable to initialize the application';
      }

      $transitions.onStart({ from: 'productlicense', to: 'gettingStarted' }, () => {
        const {
          productLicense: { installed },
        } = $ngRedux.getState();

        if (!installed) return false;

        // Stop preventing state changes.  Otherwise, the navigation to the Getting Started page cannot be performed
        if (cancelUnlicensedStateChangeHandler) cancelUnlicensedStateChangeHandler();

        submitData(REDIRECTED_ACTION, {
          pageNavigatedFrom: $state.current.name,
        });
      });

      function initExternalLinkClickHandler() {
        $q.all([$ngRedux.dispatch(actions.fetchProductFeaturesIfNeeded())])
          .then(([result]) => {
            unwrapResult(result);
            if (!$rootScope.isAllowExternalHyperlinks) {
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
          })
          .catch(setRootError);
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
        const unsubscribe = $ngRedux.connect(mapStateToThis, {
          removeAllToasts: toastSliceActions.removeAllToasts,
        })($rootScope);
        $rootScope.$on('$destroy', unsubscribe);

        $q.all([currentUser.waitForLogin(), checkLicenseInfo()])
          .then(function ([authenticationStatus]) {
            $ngRedux.dispatch(loadProductLicense());
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
          LoginModalService.dismiss();
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
          const isDirtyLookup = currentState.data && currentState.data.isDirty;

          // isDirtyLookup can either be an array (state property path) or a selector function
          return Array.isArray(isDirtyLookup)
            ? path(isDirtyLookup, state)
            : typeof isDirtyLookup === 'function'
            ? isDirtyLookup(state)
            : false;
        }

        $rootScope.$on('$stateChangeStart', function (event, toState, toParams) {
          if (!isEmpty($rootScope.toast.toasts)) {
            $rootScope.removeAllToasts();
          }
          if (!isProcessingStateChange) {
            var e = $rootScope.$broadcast('pageChangeStarted');
            if (e.defaultPrevented || isPageDirty()) {
              isProcessingStateChange = true;
              event.preventDefault();
              unsavedChangesModalService
                .open($state.current.data.unsavedChangesModal)
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

export const mapStateToThis = (state) => ({
  isAllowExternalHyperlinks: selectIsAllowExternalHyperlinksSupported(state),
  toast: selectToastSlice(state),
});

export const MainModule = angular.module('MainModule', [InitModule.name]).run([
  'initService',
  function (initService) {
    initService.start();
  },
]);
