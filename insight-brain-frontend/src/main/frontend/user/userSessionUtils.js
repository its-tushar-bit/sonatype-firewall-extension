/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import store from 'MainRoot/reduxConfig/store';
import { actions } from './userSessionSlice';
import { actions as productFeaturesActions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { actions as loginModalActions } from 'MainRoot/user/LoginModal/userLoginSlice';
import { unwrapResult } from '@reduxjs/toolkit';
import { clearRequests } from 'MainRoot/utility/services/unauthenticatedRequestQueue';
import { redirectToIdP } from 'MainRoot/user/LoginModal/userLoginSlice';

/**
 * This file consists of user-session related functions that are intended to be called from _outside_ of redux
 */

function isBackupLogin() {
  return window.location.hash === '#/backupLogin';
}

/**
 * Fetch user session from the server
 * @param {boolean} [waitForLogin=true] - Whether to wait for login if request fails with 401
 */
export function fetchUser(waitForLogin = true) {
  // NOTE: When waiting for login, the http promise might remain unresolved forever if login is cancelled.  A
  // successive attempt to login again should result in a new call to `fetchUser` to get a fresh promise.

  store.dispatch(actions.fetchUserSession(waitForLogin));
}

/**
 * Wait for user to be logged in
 * @returns {Promise} Resolves with user data when logged in
 */
export function waitForLogin() {
  return new Promise((resolve, reject) => {
    // Check if already logged in (synchronous check)
    const currentState = store.getState();

    if (currentState.userSession.data) {
      return resolve(currentState.userSession.data);
    }

    // Subscribe to store changes
    const unsubscribe = store.subscribe(() => {
      const state = store.getState();
      const { userSession } = state;

      if (userSession.data) {
        unsubscribe();
        resolve(userSession.data);
      } else if (userSession.error) {
        // Only reject for actual errors (non-401 errors)
        // 401 errors don't set error in state, so this will only trigger for real errors
        unsubscribe();
        reject(userSession.error);
      }
    });
  });
}

/**
 * Authenticate user by showing login modal and waiting for login
 * @param {string} wwwAuthenticateHeader - WWW-Authenticate header from 401 response
 * @param {string} ssoLoginUrl - SSO login URL if SSO is configured
 * @returns {Promise} Resolves when user successfully logs in
 */
export async function authenticate(wwwAuthenticateHeader, ssoLoginUrl) {
  let isSsoOnlyEnabled;
  try {
    isSsoOnlyEnabled = await store.dispatch(productFeaturesActions.loadIsSsoOnlyEnabled()).then(unwrapResult);
  } catch (error) {
    isSsoOnlyEnabled = false;
  }

  const hasSso =
    wwwAuthenticateHeader && (wwwAuthenticateHeader.includes('SAML') || wwwAuthenticateHeader.includes('OIDC'));

  if (isSsoOnlyEnabled && hasSso && !isBackupLogin()) {
    clearRequests();
    redirectToIdP(ssoLoginUrl);
    return;
  }

  store.dispatch(loginModalActions.open(hasSso, ssoLoginUrl));
  return waitForLogin();
}
