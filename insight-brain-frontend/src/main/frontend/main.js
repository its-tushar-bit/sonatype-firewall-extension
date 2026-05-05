/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { Messages } from './util/CommonServices';
import { GETTING_STARTED_STATE } from './configuration/route';
import './reduxConfig/store';
import store from './reduxConfig/store';
import {
  DEPARTED_ACTION,
  REDIRECTED_ACTION,
  submitData,
} from './configuration/gettingStarted/gettingStartedTelemetryServiceHelper';
import pendoService from './pendo/mainBundlePendoService';
import {
  initialize as initializeRouteStateUtilService,
  stateRequiresAuthenticationSync,
  stateRequiresAuthentication,
} from './utility/services/routeStateUtilService';
import * as ProductLicense from './utility/services/ProductLicense';
import { contains, isEmpty, not, tryCatch } from 'ramda';
import { attachAxiosInterceptors } from './utility/axiosConfig';
import { requestNotificationPermission } from './utility/services/notificationService';
import { actions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { selectUsername } from 'MainRoot/user/userSessionSelectors';
import { selectProductEdition } from 'MainRoot/productFeatures/productLicenseSelectors';
import { setError, clearError } from 'MainRoot/session/appErrorSlice';
import { selectIsAllowExternalHyperlinksSupported } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { unwrapResult } from '@reduxjs/toolkit';
import { RejectType } from '@uirouter/core';
import { actions as toastSliceActions } from 'MainRoot/toastContainer/toastSlice';
import { selectToastSlice } from 'MainRoot/toastContainer/toastSelectors';
import { load as loadProductLicense, loadFulfilled } from 'MainRoot/configuration/license/productLicenseActions';
import { getDaysFromNow } from 'MainRoot/util/jsUtil';
import { fab } from '@fortawesome/free-brands-svg-icons';
import { library } from '@fortawesome/fontawesome-svg-core';
import { actions as externalLinkModalActions } from 'MainRoot/modals/externalLinkModal/externalLinkModalSlice';
import { actions as unsavedChangesModalActions } from 'MainRoot/modals/unsavedChangesModal/unsavedChangesModalSlice';
import { checkSessionExpiredLater } from 'MainRoot/session/sessionExpirationManager';
import { fetchUser, waitForLogin } from 'MainRoot/user/userSessionUtils';
import { actions as loginModalActions } from 'MainRoot/user/LoginModal/userLoginSlice';
import { selectIsCurrentRouteDirty } from 'MainRoot/reduxUiRouter/routerSelectors';
import initDisplayTheme from './configuration/displayTheme/initDisplayTheme';

// Module-scoped mutable state, reinitialized on each call to main()
let savedState = null;
let cancelPreLoginStateHandler = null;
let cancelUnlicensedStateChangeHandler = null;
let isProcessingStateChange = false;

/**
 * Dispatches an HTTP error to the Redux store for display to the user
 */
function setRootError(err) {
  store.dispatch(setError(Messages.getHttpErrorMessage(err)));
}

/**
 * Handles initialization failure by dispatching an error to the Redux store
 */
function initFailure(err) {
  store.dispatch(setError(Messages.getHttpErrorMessage(err) || 'Unable to initialize the application'));
}

/**
 * Recursively finds the nearest anchor element in the DOM tree
 */
function getAnchor(target) {
  if (target.nodeName === 'A') {
    return target;
  } else {
    return getAnchor(target.parentNode);
  }
}

/**
 * Sets up click handler to intercept external links when external hyperlinks are disabled
 */
async function initExternalLinkClickHandler() {
  try {
    const result = await store.dispatch(actions.fetchProductFeaturesIfNeeded());

    unwrapResult(result);
    // Check if external hyperlinks are allowed by reading from Redux state
    const state = store.getState();
    const isAllowExternalHyperlinks = selectIsAllowExternalHyperlinksSupported(state);

    if (!isAllowExternalHyperlinks) {
      const externalLinkClickHandler = (e) => {
        const isExternalLink = (anchor) => anchor.hostname && anchor.hostname !== location.hostname;
        const anchor = getAnchor(e.target);
        if (anchor && isExternalLink(anchor)) {
          store.dispatch(externalLinkModalActions.open(anchor.href));
          e.stopImmediatePropagation();
          e.preventDefault();
        }
      };

      document.addEventListener('click', externalLinkClickHandler);
      window.externalLinkClickHandler = externalLinkClickHandler;
    }
  } catch (err) {
    setRootError(err);
  }
}

/**
 * Safely loads FontAwesome brand icons, catching any errors
 */
const loadFontAwesomeBrandIcons = tryCatch(() => library.add(fab), console.error);

/**
 * Prevents navigation to any page except product license and proxy config when unlicensed
 */
function unlicensedStateChangeHandler(transition) {
  if (not(contains(transition.to().name, ['productlicense', 'proxyConfig']))) {
    return false;
  }
}

/**
 * Fetches license information and sets up navigation restrictions based on license status
 */
function checkLicenseInfo(stateService, transitionService) {
  let savedStateDuringLicenseFetch = null;

  function preLicenseFetchStateHandler(transition) {
    savedStateDuringLicenseFetch = { state: transition.to().name, params: transition.params() };
    return false; // Prevent transition
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

    // replay state transition caught while license was loading so that preLoginStateHandler can process it
    if (savedStateDuringLicenseFetch) {
      stateService.go(savedStateDuringLicenseFetch.state, savedStateDuringLicenseFetch.params);
    }
  }

  function onLicenseFailure(err) {
    if (err?.response?.status === 402) {
      // Differentiate entitlement failures (tier-gated features) from license failures
      const code = err?.response?.data?.code;
      if (code === 'ENTITLEMENT_REQUIRED') {
        return Promise.reject(err); // Let calling code handle inline upsell
      }
      cancelUnlicensedStateChangeHandler = transitionService.onStart({}, unlicensedStateChangeHandler);
      stateService.go('productlicense');
    } else {
      return Promise.reject(err);
    }
  }

  // Register preLicenseFetchStateHandler to block during license fetch
  const cancelPreLicenseFetchStateHandler = transitionService.onStart({}, preLicenseFetchStateHandler);

  function registerPreLoginStateHandler() {
    cancelPreLoginStateHandler = transitionService.onStart({}, (transition) =>
      preLoginStateHandler(transition, stateService)
    );
  }

  return ProductLicense.loadIfNotYetLoaded()
    .finally(cancelPreLicenseFetchStateHandler)
    .finally(registerPreLoginStateHandler)
    .then(onLicenseSuccess, onLicenseFailure);
}

/**
 * Prevents navigation to authenticated pages before login, triggering login modal when needed
 */
function preLoginStateHandler(transition, stateService) {
  const state = transition.to().name;
  const params = transition.params();

  function attemptLoginIfNeeded() {
    // Check if user is logged in by reading from Redux state
    const reduxState = store.getState();
    const username = selectUsername(reduxState);
    if (!username) {
      fetchUser(true);
    }
  }

  savedState = { state, params };

  const stateRequiresAuthenticationNow = stateRequiresAuthenticationSync(transition.to());

  switch (stateRequiresAuthenticationNow) {
    // don't know if auth required yet: prevent state load and wait for async result
    case undefined:
      stateRequiresAuthentication(transition.to()).then((stateRequiresAuth) => {
        if (stateRequiresAuth) {
          attemptLoginIfNeeded();
        } else {
          stateService.go(state, params);
        }
      });
      return false; // Prevent transition

    // page does require auth: prevent state load and show login
    case true:
      attemptLoginIfNeeded();
      return false; // Prevent transition
    // case false, page does not require auth: no need to do anything, just let the page show
  }
}

/**
 * Completes successful initialization by setting up UI handlers and checking session
 */
function initSuccess(stateService) {
  initExternalLinkClickHandler();

  cancelPreLoginStateHandler(); // Remove block

  if (savedState) {
    stateService.go(savedState.state, savedState.params);
  }
  requestNotificationPermission();
  // Read productEdition from Redux state
  const state = store.getState();
  const productEdition = selectProductEdition(state);
  checkSessionExpiredLater(store, productEdition);
  loadFontAwesomeBrandIcons();
}

/**
 * Clears error state from Redux store
 */
function clearRootScopeError() {
  store.dispatch(clearError());
}

/**
 * Checks if the current page has unsaved changes by consulting the router state
 */
function isPageDirty() {
  const state = store.getState();
  return selectIsCurrentRouteDirty(state);
}

/**
 * Handles browser unload event, submitting telemetry and warning about unsaved changes
 */
function unloadListener(stateService) {
  if (stateService.current.name === GETTING_STARTED_STATE) {
    submitData(DEPARTED_ACTION, null, true);
  }

  if (!isProcessingStateChange && isPageDirty()) {
    return 'The page may contain unsaved changes, continuing will discard them.';
  }
}

/**
 * Handles transition from product license page to getting started page
 */
function handleGettingStartedTransition(stateService) {
  const {
    productLicense: { installed },
  } = store.getState();

  if (!installed) return false;

  // Stop preventing state changes.  Otherwise, the navigation to the Getting Started page cannot be performed
  if (cancelUnlicensedStateChangeHandler) cancelUnlicensedStateChangeHandler();

  submitData(REDIRECTED_ACTION, {
    pageNavigatedFrom: stateService.current.name,
  });
}

/**
 * Dismisses the login modal
 */
function dismissLoginModal() {
  store.dispatch(loginModalActions.dismiss());
}

/**
 * Handles transition errors by dispatching them to the Redux store
 */
function handleTransitionError(transition) {
  const error = transition.error();

  // Ignore intentional transition rejections that shouldn't be shown as errors
  // SUPERSEDED: new transition started while previous was running
  // ABORTED: hooks redirecting to different states
  // IGNORED: attempting to transition to the same state with same params
  const ignoredRejectTypes = [RejectType.SUPERSEDED, RejectType.ABORTED, RejectType.IGNORED];
  if (error?.type && ignoredRejectTypes.includes(error.type)) {
    return;
  }

  if (typeof error === 'string') {
    store.dispatch(setError(error));
  } else {
    setRootError(error);
  }
}

/**
 * Handles transition start event, checking for unsaved changes and clearing toasts
 */
function handleTransitionStart() {
  const state = store.getState();

  const toast = selectToastSlice(state);
  if (!isEmpty(toast.toasts)) {
    store.dispatch(toastSliceActions.removeAllToasts());
  }
  if (!isProcessingStateChange && isPageDirty()) {
    isProcessingStateChange = true;
    return store
      .dispatch(unsavedChangesModalActions.open())
      .then(
        function () {
          return true; // Allow transition to proceed
        },
        function () {
          return false; // Cancel transition
        }
      )
      .finally(() => {
        isProcessingStateChange = false;
      });
  }
}

/**
 * Main intialization function for the application.
 * @param {object} stateService - The ui-router state service
 * @param {object} transitionService - The ui-router transition service
 */
export default async function main(stateService, transitionService) {
  // Reinitialize module-scoped state
  savedState = null;
  cancelPreLoginStateHandler = null;
  cancelUnlicensedStateChangeHandler = null;
  isProcessingStateChange = false;

  // Initialize display theme (connects Redux to localStorage and DOM)
  initDisplayTheme();

  // Initialize routeStateUtilService with the Redux store.
  // This also eagerly loads unauthenticated pages config so the login modal can show the vulnerability link.
  initializeRouteStateUtilService(store);

  transitionService.onStart({ from: 'productlicense', to: 'gettingStarted' }, () =>
    handleGettingStartedTransition(stateService)
  );

  // Clear errors on navigation
  transitionService.onBefore({}, clearRootScopeError);
  transitionService.onSuccess({}, clearRootScopeError);

  // This listener is active until login succeeds. If a page navigation occurs (successfully) before that
  // time, that means that the page navigated to must be one that allows unauthenticated use, so the login
  // modal should be closed without completing the login
  let cancelLoginDismissListener = transitionService.onSuccess({}, dismissLoginModal);

  transitionService.onError({}, handleTransitionError);

  transitionService.onStart({}, handleTransitionStart);

  const unloadListenerHandler = () => unloadListener(stateService);
  window.unloadListener = unloadListenerHandler;
  window.addEventListener('beforeunload', unloadListenerHandler);

  // Clear errors on URL hash changes when ui-router hooks don't fire
  // This handles the case where navigating back to a route after a failed transition
  // doesn't trigger ui-router hooks (because it thinks it's already on that route)
  window.addEventListener('hashchange', () => {
    // Delay to let ui-router process the URL first
    setTimeout(() => {
      // Only clear error if ui-router successfully resolved to a valid state
      // and the current URL hash matches ui-router's current state (navigation succeeded)
      if (stateService.current && stateService.current.name) {
        const currentStateHref = stateService.href(stateService.current.name, stateService.params);
        const currentHash = window.location.hash;

        // If the URL matches the current state, navigation was successful - clear any error
        if (currentHash === currentStateHref) {
          clearRootScopeError();
        }
      }
    }, 50);
  });

  attachAxiosInterceptors();

  pendoService.start();

  // Try to fetch the current user in order to see if we are already logged in, but do not attempt
  // to initiate a login here (we might be on a page that doesn't require auth)
  fetchUser(false);

  try {
    await Promise.all([waitForLogin(), checkLicenseInfo(stateService, transitionService)]);

    store.dispatch(loadProductLicense());
    // Username will be synced to $rootScope automatically via the subscribe mechanism
    cancelLoginDismissListener();
    // This was already called at the bottom of `main`, but call it again here now that the user is
    // logged in.  It is safe to call multiple times
    pendoService.start();

    initSuccess(stateService);
  } catch (err) {
    initFailure(err);
  }
}
