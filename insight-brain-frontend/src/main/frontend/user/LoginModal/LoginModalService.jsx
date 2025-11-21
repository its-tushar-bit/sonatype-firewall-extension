/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { actions } from 'MainRoot/user/LoginModal/userLoginSlice';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { assign } from 'MainRoot/util/CLMLocation';
import { actions as productFeaturesActions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { unwrapResult } from '@reduxjs/toolkit';
import { clearRequests } from 'MainRoot/utility/services/unauthenticatedRequestQueue';
import store from 'MainRoot/reduxConfig/store';
import { selectIsLicenseInstalled, selectProducts } from 'MainRoot/productFeatures/productLicenseSelectors';
import {
  selectIsUnauthenticatedPagesEnabled,
  selectIsQuarantinedComponentViewAnonymousAccessEnabled,
  selectSsoLoginUrl,
} from 'MainRoot/user/LoginModal/userLoginSelectors';

export default function LoginModalService($window) {
  let modalPromise = null;
  let resolveModalPromise;
  let rejectModalPromise;

  function resetIsShowing() {
    modalPromise = null;
  }

  function redirect(destination) {
    assign(destination);
  }

  /**
   * See CLM-34076. Some customers want a way to get to the local login page even if they have
   * SSO exclusively enabled, as a recovery option in case of SSO misconfiguration. So if they
   * go to the backupLogin state, we always show the login modal rather than redirecting to SSO.
   */
  function isBackupLogin() {
    return $window.location.hash === '#/backupLogin';
  }

  const onSubmit = (loginUsername, loginPassword) => {
    return store.dispatch(actions.submitUserLogin(loginUsername, loginPassword)).then(() => {
      setTimeout(() => {
        // Clean up modal promise and DOM presence without returning login promise rejection.
        resetIsShowing();
        return resolveModalPromise();
      }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
    });
  };

  const onClickSSO = async () => {
    // Get SSO login URL from Redux state
    const state = store.getState();
    const ssoLoginUrl = selectSsoLoginUrl(state);
    await redirectToIdP(ssoLoginUrl);
  };

  async function authenticate(wwwAuthenticateHeader, ssoLoginUrl) {
    const isSsoOnlyEnabled = await loadIsSsoOnlyEnabled();

    // Parse available SSO methods from WWW-Authenticate header
    const hasSso =
      wwwAuthenticateHeader && (wwwAuthenticateHeader.includes('SAML') || wwwAuthenticateHeader.includes('OIDC'));

    // Auto-redirect if SSO-only mode is enabled and at least one SSO method is available
    if (isSsoOnlyEnabled && hasSso && !isBackupLogin()) {
      clearRequests();
      return await redirectToIdP(ssoLoginUrl);
    }

    // Show SSO button if any SSO method is available
    return await open(hasSso, ssoLoginUrl);
  }

  const loadIsSsoOnlyEnabled = () => {
    return store.dispatch(productFeaturesActions.loadIsSsoOnlyEnabled()).then(unwrapResult);
  };

  async function redirectToIdP(ssoLoginUrl) {
    // Backend provides the SSO login URL (e.g., "/saml/login" or "/oidc/login")
    // Append hash parameter if present to preserve user navigation state
    // Note: hash must be URL-encoded to handle special characters like '/', '?', '#', etc.
    const destination = $window.location.hash
      ? `${ssoLoginUrl}?hash=${encodeURIComponent($window.location.hash)}`
      : ssoLoginUrl;

    redirect(destination);
  }

  async function open(showSsoButton, ssoLoginUrl) {
    if (modalPromise) {
      return modalPromise;
    }

    // Read license and config values from Redux state. License data is already loaded by
    // MainModule.checkLicenseInfo() before the login modal is shown. Config values
    // (isUnauthenticatedPagesEnabled, etc.) are loaded by various early initialization calls.
    // We just read whatever is currently in state - no need to fetch anything.
    const state = store.getState();
    const isLicensed = selectIsLicenseInstalled(state);
    const products = selectProducts(state);
    const isUnauthenticatedPagesEnabled = selectIsUnauthenticatedPagesEnabled(state);
    const isQuarantinedComponentViewEnabled = selectIsQuarantinedComponentViewAnonymousAccessEnabled(state);

    // Dispatch all values to loginModalState for use by the LoginModal component
    store.dispatch(actions.setIsLicensed(isLicensed));
    store.dispatch(actions.setProducts(products));
    store.dispatch(actions.setUnauthenticatedPagesEnabled(isUnauthenticatedPagesEnabled));
    store.dispatch(actions.setQuarantinedComponentViewAnonymousAccessEnabled(isQuarantinedComponentViewEnabled));

    store.dispatch(actions.setShowLoginModal(true));
    store.dispatch(actions.setShowSso(showSsoButton));
    store.dispatch(actions.setSsoLoginUrl(ssoLoginUrl));

    modalPromise = new Promise((resolve, reject) => {
      resolveModalPromise = resolve;
      rejectModalPromise = reject;
    });

    return modalPromise;
  }

  // Triggered on login modal/form cancellation or by MainModule's cancelLoginDismissListener,
  // which is active until successful login. If the user has previously visited a page that
  // requires authentication, there will be a pending modal promise and container, so we have
  // to handle rejecting the promise and removing the modal container from the DOM. Otherwise
  // we can safely do nothing.
  function dismiss() {
    store.dispatch(actions.resetLoginSubmitState());
    resetIsShowing();

    if (rejectModalPromise) {
      return rejectModalPromise();
    }
  }

  return { onClickSSO, onSubmit, dismiss, open, redirectToIdP, authenticate };
}

LoginModalService.$inject = ['$window'];
