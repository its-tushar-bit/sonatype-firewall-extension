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

export default function LoginModalService(rootScope, ngRedux, $window) {
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
    return ngRedux.dispatch(actions.submitUserLogin(loginUsername, loginPassword)).then(() => {
      setTimeout(() => {
        // Clean up modal promise and DOM presence without returning login promise rejection.
        resetIsShowing();
        return resolveModalPromise();
      }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
    });
  };

  const onClickSSO = async () => {
    // Get SSO login URL provided by backend
    const state = ngRedux.getState();
    const ssoLoginUrl = state.userLogin?.loginModalState?.ssoLoginUrl;
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
    return ngRedux.dispatch(productFeaturesActions.loadIsSsoOnlyEnabled()).then(unwrapResult);
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

    modalPromise = new Promise((resolve, reject) => {
      resolveModalPromise = resolve;
      rejectModalPromise = reject;
    });

    ngRedux.dispatch(actions.setIsLicensed(rootScope.licensed));
    ngRedux.dispatch(actions.setProducts(rootScope.products));
    ngRedux.dispatch(actions.setShowLoginModal(true));
    ngRedux.dispatch(actions.setShowSso(showSsoButton));
    ngRedux.dispatch(actions.setSsoLoginUrl(ssoLoginUrl));

    return modalPromise;
  }

  // Triggered on login modal/form cancellation or by MainModule's cancelLoginDismissListener,
  // which is active until successful login. If the user has previously visited a page that
  // requires authentication, there will be a pending modal promise and container, so we have
  // to handle rejecting the promise and removing the modal container from the DOM. Otherwise
  // we can safely do nothing.
  function dismiss() {
    ngRedux.dispatch(actions.resetLoginSubmitState());
    resetIsShowing();

    if (rejectModalPromise) {
      return rejectModalPromise();
    }
  }

  return { onClickSSO, onSubmit, dismiss, open, redirectToIdP, authenticate };
}

LoginModalService.$inject = ['$rootScope', '$ngRedux', '$window'];
