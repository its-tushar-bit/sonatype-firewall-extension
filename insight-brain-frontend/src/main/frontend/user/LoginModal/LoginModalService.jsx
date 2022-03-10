/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { actions } from 'MainRoot/user/LoginModal/userLoginSlice';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { getSamlSsoLoginUrl } from 'MainRoot/util/CLMLocation';

export default function LoginModalService(rootScope, ngRedux) {
  let modalPromise = null;
  let resolveModalPromise;
  let rejectModalPromise;

  function resetIsShowing() {
    modalPromise = null;
  }

  function redirect(destination) {
    window.location.assign(destination);
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

  const onClickSSO = () => {
    let destination = getSamlSsoLoginUrl(window.location.hash);
    redirect(destination);
  };

  function open(showSamlSso) {
    if (modalPromise) {
      return modalPromise;
    }

    modalPromise = new Promise((resolve, reject) => {
      resolveModalPromise = resolve;
      rejectModalPromise = reject;
    });

    ngRedux.dispatch(actions.setIsLicensed(rootScope.licensed));
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

  function setUnauthenticatedPagesEnabled(areUnauthenticatedPagesEnabled) {
    ngRedux.dispatch(actions.setUnauthenticatedPagesEnabled(areUnauthenticatedPagesEnabled));
  }

  return { onClickSSO, onSubmit, dismiss, open, setUnauthenticatedPagesEnabled };
}

LoginModalService.$inject = ['$rootScope', '$ngRedux'];
