/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { actions } from 'MainRoot/user/LoginModal/userLoginSlice';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { getOidcLoginUrl, getSamlSsoLoginUrl } from 'MainRoot/util/CLMLocation';
import { assign } from 'MainRoot/util/CLMLocation';
import { actions as productFeaturesActions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { unwrapResult } from '@reduxjs/toolkit';

export default function LoginModalService(rootScope, ngRedux, UnauthenticatedRequestQueueService) {
  let modalPromise = null;
  let resolveModalPromise;
  let rejectModalPromise;

  function resetIsShowing() {
    modalPromise = null;
  }

  function redirect(destination) {
    assign(destination);
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
    const isOAuth2Enabled = await loadOAuth2Enabled();
    await redirectToIdP(isOAuth2Enabled);
  };

  async function authenticate(showSamlSso) {
    const isSsoOnlyEnabled = await loadIsSsoOnlyEnabled();
    const isOAuth2Enabled = await loadOAuth2Enabled();

    if (isSsoOnlyEnabled && (showSamlSso || isOAuth2Enabled)) {
      UnauthenticatedRequestQueueService.clearRequests();
      return await redirectToIdP(isOAuth2Enabled);
    }

    return await open(showSamlSso, isOAuth2Enabled);
  }

  const loadOAuth2Enabled = () => {
    return ngRedux.dispatch(productFeaturesActions.loadIsOauth2Enabled()).then(unwrapResult);
  };

  const loadIsSsoOnlyEnabled = () => {
    return ngRedux.dispatch(productFeaturesActions.loadIsSsoOnlyEnabled()).then(unwrapResult);
  };

  async function redirectToIdP(isOAuth2Enabled) {
    let destination = isOAuth2Enabled
      ? getOidcLoginUrl(window.location.hash)
      : getSamlSsoLoginUrl(window.location.hash);

    redirect(destination);
  }

  async function open(showSamlSso) {
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
    ngRedux.dispatch(actions.setShowSamlSso(showSamlSso));

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

LoginModalService.$inject = ['$rootScope', '$ngRedux', 'UnauthenticatedRequestQueueService'];
