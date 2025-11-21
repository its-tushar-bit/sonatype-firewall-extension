/* eslint-disable */
/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angularDebug */
import { Messages } from './util/CommonServices';
import { httpInterceptors, unauthenticatedResponseHttpInterceptor } from './utilAngular/HttpInterceptors';
import IqHttpInterceptorsModule from './utilAngular/IqHttpInterceptors';
import configurationModule, { GETTING_STARTED_STATE } from './configuration/module';
import './reduxConfig/store';
import store from './reduxConfig/store';
import {
  DEPARTED_ACTION,
  REDIRECTED_ACTION,
  submitData,
} from './configuration/gettingStarted/gettingStartedTelemetryServiceHelper';
import reduxUiRouterModule from './reduxUiRouter/module';
import MainHeader from './mainHeader/MainHeader.jsx';
import userReducer from './user/userReducer';
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import NavigationContainer from './navigationContainer/NavigationContainer';
import ReportModule from './ReportApp';
import dashboardModule from './dashboard/dashboard.module';
import Report from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/module';
import routeProductLicenseValidator from './routeProductLicenseValidator/module';
import pendoService, { setUrlService } from './pendo/mainBundlePendoService';
import {
  initialize as initializeRouteStateUtilService,
  stateRequiresAuthenticationSync,
  stateRequiresAuthentication,
} from './utility/services/routeStateUtilService';
import * as ProductLicense from './utility/services/ProductLicense';
import loginModalModule from './user/LoginModal/module';
import legalModule from './legal/legal.module';
import toastContainerModule from './toastContainer/module';
import displayThemeModule from './configuration/displayTheme/module';
import modalContainerModule from './modalContainer/module';
import footerModule from './react/Footer/module';
import { contains, isEmpty, not, path, tryCatch } from 'ramda';
import { attachAxiosInterceptors } from './utility/axiosConfig';
import { requestNotificationPermission } from './utility/services/notificationService';
import {
  selectHasLifecycleLicense,
  selectIsLicenseInstalled,
  selectProductEdition,
  selectProducts,
} from 'MainRoot/productFeatures/productLicenseSelectors';
import { actions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { selectUsername } from 'MainRoot/user/userSessionSelectors';
import { setError, clearError } from 'MainRoot/session/appErrorSlice';
import { selectError } from 'MainRoot/session/appErrorSelectors';
import {
  selectIsAllowExternalHyperlinksSupported,
  selectIsFirewallSupportedForNavigationContainer,
  selectIsDashboardSupported,
  selectIsFirewallSupported,
  selectIsReportListSupported,
  selectIsSbomManagerEnabled,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { unwrapResult } from '@reduxjs/toolkit';
import { actions as toastSliceActions } from 'MainRoot/toastContainer/toastSlice';
import { selectToastSlice } from 'MainRoot/toastContainer/toastSelectors';
import { load as loadProductLicense, loadFulfilled } from 'MainRoot/configuration/license/productLicenseActions';
import { getDaysFromNow } from 'MainRoot/util/jsUtil';
import { selectUnconfiguredRepoManager } from 'MainRoot/firewallOnboarding/firewallOnboardingSelectors';
import { actions as firewallOnboardingActions } from 'MainRoot/firewallOnboarding/firewallOnboardingSlice';
import { fab } from '@fortawesome/free-brands-svg-icons';
import { library } from '@fortawesome/fontawesome-svg-core';
import { selectIsSbomManagerOnlyLicense } from 'MainRoot/productFeatures/productLicenseSelectors';
import { actions as externalLinkModalActions } from 'MainRoot/modals/externalLinkModal/externalLinkModalSlice';
import { actions as unsavedChangesModalActions } from 'MainRoot/modals/unsavedChangesModal/unsavedChangesModalSlice';
import { checkSessionExpiredLater } from 'MainRoot/session/sessionExpirationManager';
import { fetchUser, waitForLogin } from 'MainRoot/user/userSessionUtils';

export const InitModule = angular
  .module(
    'InitModule',
    [
      'ui.router',
      ReportModule.name,
      Report.name,
      unauthenticatedResponseHttpInterceptor.name,
      httpInterceptors.name,
      IqHttpInterceptorsModule.name,
      dashboardModule.name,
      legalModule.name,
      reduxUiRouterModule.name,
      configurationModule.name,
      loginModalModule.name,
      toastContainerModule.name,
      routeProductLicenseValidator.name,
      displayThemeModule.name,
      modalContainerModule.name,
      footerModule.name,
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
                $q = injector.get('$q');
              return $q
                .all([
                  store.dispatch(actions.fetchProductFeaturesIfNeeded()),
                  store.dispatch(loadProductLicense()),
                  store.dispatch(firewallOnboardingActions.loadUnconfiguredRepoManagers()),
                  waitForLogin(store),
                ])
                .then((results) => {
                  unwrapResult(results[0]);
                  const state = store.getState();
                  const hasLifecycleLicense = selectHasLifecycleLicense(state);
                  const isDashboardAvailable = selectIsDashboardSupported(state);
                  const isFirewallAvailable = selectIsFirewallSupported(state);
                  const isFirewallEnabled = selectIsFirewallSupportedForNavigationContainer(state);
                  const isReportsListAvailable = selectIsReportListSupported(state);
                  const isSbomManagerEnabled = selectIsSbomManagerEnabled(state);
                  const isSbomManagerOnlyLicense = selectIsSbomManagerOnlyLicense(state);
                  const unconfiguredRepoManager = selectUnconfiguredRepoManager(state);

                  if (isSbomManagerEnabled && isSbomManagerOnlyLicense) {
                    return 'sbomManager.dashboard';
                  } else if (isFirewallAvailable && unconfiguredRepoManager && isFirewallEnabled) {
                    return 'firewallOnboarding.firewallOnboardingPage';
                  } else if (isDashboardAvailable) {
                    return 'dashboard.overview.violations';
                  } else if (!isDashboardAvailable && isReportsListAvailable && hasLifecycleLicense) {
                    return 'violations'; //Landing page is reports page if dashboard is not available for LC
                  } else if (isFirewallAvailable) {
                    return 'firewall.firewallPage';
                  } else if (isReportsListAvailable) {
                    return 'violations';
                  }

                  return 'gettingStarted';
                })
                .catch((err) => {
                  store.dispatch(setError(Messages.getHttpErrorMessage(err)));
                });
            },
          })
          .state('home', {
            url: '/',
            redirectTo: 'root',
          })

          // See CLM-34076. Some customers want a way to get to the local login page even if they have
          // SSO exclusively enabled, as a recovery option in case of SSO misconfiguration. LoginModalService
          // has special case handling for this state.
          .state('backupLogin', {
            url: '/backupLogin',
            redirectTo: 'root',
          });

        var unknownErrorFunction = function () {
          store.dispatch(setError('Unknown Address'));
        };

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
    '$q',
    '$urlRouter',
    '$timeout',
    '$urlService',
    'LoginModalService',
    '$transitions',
    function ($rootScope, $state, $window, $q, $urlRouter, $timeout, $urlService, LoginModalService, $transitions) {
      // Initialize the singleton pendoService with urlService
      setUrlService($urlService);

      // Initialize the ES6 routeStateUtilService module with Angular dependencies
      initializeRouteStateUtilService($state, store);

      var savedState = null,
        cancelPreLoginStateHandler,
        cancelUnlicensedStateChangeHandler;
      /**
       * Before login, prevent navigation to pages that require authentication, and trigger the login modal
       * when access to one is attempted
       */
      function preLoginStateHandler(event, state, params) {
        function attemptLoginIfNeeded() {
          // Check if user is logged in by reading from Redux state
          const reduxState = store.getState();
          const username = selectUsername(reduxState);
          if (!username) {
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

        const stateRequiresAuthenticationNow = stateRequiresAuthenticationSync(state);

        switch (stateRequiresAuthenticationNow) {
          // don't know if auth required yet: prevent state load and wait for async result
          case undefined:
            event.preventDefault();
            stateRequiresAuthentication(state).then((stateRequiresAuth) => {
              if (stateRequiresAuth) {
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

      attachAxiosInterceptors($window, LoginModalService, store);

      function setRootError(err) {
        store.dispatch(setError(Messages.getHttpErrorMessage(err)));
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

        function onLicenseSuccess(licenseData) {
          // Dispatch license data to Redux immediately so it's available before login.
          // Use the loadFulfilled action creator from productLicenseActions.
          store.dispatch(
            loadFulfilled({
              ...licenseData,
              ...(licenseData.expiryTimestamp && { daysToExpiration: getDaysFromNow(licenseData.expiryTimestamp) }),
            })
          );

          // License data will be synced to $rootScope automatically via the subscribe mechanism

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

        return ProductLicense.loadIfNotYetLoaded()
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
        // Read productEdition from Redux state
        const state = store.getState();
        const productEdition = selectProductEdition(state);
        checkSessionExpiredLater(store, productEdition);
        loadFontAwesomeBrandIcons();
      }

      function initFailure(err) {
        store.dispatch(setError(Messages.getHttpErrorMessage(err) || 'Unable to initialize the application'));
      }

      $transitions.onStart({ from: 'productlicense', to: 'gettingStarted' }, () => {
        const {
          productLicense: { installed },
        } = store.getState();

        if (!installed) return false;

        // Stop preventing state changes.  Otherwise, the navigation to the Getting Started page cannot be performed
        if (cancelUnlicensedStateChangeHandler) cancelUnlicensedStateChangeHandler();

        submitData(REDIRECTED_ACTION, {
          pageNavigatedFrom: $state.current.name,
        });
      });

      function initExternalLinkClickHandler() {
        $q.all([store.dispatch(actions.fetchProductFeaturesIfNeeded())])
          .then(([result]) => {
            unwrapResult(result);
            // Check if external hyperlinks are allowed by reading from Redux state
            const state = store.getState();
            const isAllowExternalHyperlinks = selectIsAllowExternalHyperlinksSupported(state);

            if (!isAllowExternalHyperlinks) {
              const externalLinkClickHandler = (e) => {
                const isExternalLink = (anchor) => anchor.hostname && anchor.hostname !== location.hostname;
                const anchor = getAnchor(e.target);
                if (isExternalLink(anchor)) {
                  store.dispatch(externalLinkModalActions.open(anchor.href));
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
        fetchUser(store);
      }

      function doStart() {
        // Subscribe to Redux state changes for $rootScope properties
        const unsubscribeRootScope = store.subscribe(() => {
          const state = store.getState();
          $rootScope.isAllowExternalHyperlinks = selectIsAllowExternalHyperlinksSupported(state);
        });
        $rootScope.$on('$destroy', unsubscribeRootScope);

        // Initialize rootScope properties
        const initialState = store.getState();
        $rootScope.isAllowExternalHyperlinks = selectIsAllowExternalHyperlinksSupported(initialState);

        // Subscribe to Redux error state changes and sync to $rootScope for index.html template compatibility
        const unsubscribeError = store.subscribe(() => {
          const state = store.getState();
          const error = selectError(state);
          if (error) {
            $rootScope.error = error;
          } else {
            delete $rootScope.error;
          }
        });
        $rootScope.$on('$destroy', unsubscribeError);

        // Initialize error from Redux state
        const initialError = selectError(initialState);
        if (initialError) {
          $rootScope.error = initialError;
        }

        // Subscribe to Redux state changes for user session and license data
        const unsubscribeLicenseAndUser = store.subscribe(() => {
          const state = store.getState();
          $rootScope.username = selectUsername(state);
          $rootScope.licensed = selectIsLicenseInstalled(state);
          $rootScope.productEdition = selectProductEdition(state);
          $rootScope.products = selectProducts(state);
        });
        $rootScope.$on('$destroy', unsubscribeLicenseAndUser);

        // Initialize from Redux state
        const initialUsername = selectUsername(initialState);
        const initialLicensed = selectIsLicenseInstalled(initialState);
        const initialProductEdition = selectProductEdition(initialState);
        const initialProducts = selectProducts(initialState);

        if (initialUsername) {
          $rootScope.username = initialUsername;
        }
        $rootScope.licensed = initialLicensed;
        if (initialProductEdition) {
          $rootScope.productEdition = initialProductEdition;
        }
        if (initialProducts) {
          $rootScope.products = initialProducts;
        }
        $q.all([waitForLogin(store), checkLicenseInfo()])
          .then(function ([authenticationStatus]) {
            store.dispatch(loadProductLicense());
            // Username will be synced to $rootScope automatically via the subscribe mechanism
            cancelLoginDismissListener();
            // This was already called at the bottom of `doStart`, but call it again here now that the user is
            // logged in.  It is safe to call multiple times
            pendoService.start();
          })
          .then(initSuccess, initFailure);

        function clearRootScopeError() {
          store.dispatch(clearError());
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
            store.dispatch(setError(error));
          } else {
            setRootError(error);
          }
        });

        let isProcessingStateChange = false;

        function isPageDirty() {
          const state = store.getState();
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
          const state = store.getState();
          const toast = selectToastSlice(state);
          if (!isEmpty(toast.toasts)) {
            store.dispatch(toastSliceActions.removeAllToasts());
          }
          if (!isProcessingStateChange) {
            var e = $rootScope.$broadcast('pageChangeStarted');
            if (e.defaultPrevented || isPageDirty()) {
              isProcessingStateChange = true;
              event.preventDefault();
              store
                .dispatch(unsavedChangesModalActions.open())
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
        window.unloadListener = unloadListener;

        // make sure to cleanup event listeners
        $rootScope.$on('$destroy', function () {
          $rootScope.$broadcast('pageChangeAccepted');
          $($window).unbind('beforeunload', unloadListener);
        });

        // this causes the browser to notify the user that the page contains unsaved data
        $($window).bind('beforeunload', unloadListener);

        // Try to fetch the current user in order to see if we are already logged in, but do not attempt
        // to initiate a login here (we might be on a page that doesn't require auth)
        fetchUser(store, false);

        pendoService.start();
      }

      return { start: doStart };
    },
  ])
  .value('userReducer', userReducer)
  .component('mainHeader', iqReact2Angular(MainHeader, ['clmServerVersion'], ['$state']))
  .component(
    'navigationContainer',
    iqReact2Angular(NavigationContainer, ['productEdition', 'clmServerVersion'], ['$rootScope', '$state'])
  );

export const MainModule = angular.module('MainModule', [InitModule.name]).run([
  'initService',
  function (initService) {
    initService.start();
  },
]);
