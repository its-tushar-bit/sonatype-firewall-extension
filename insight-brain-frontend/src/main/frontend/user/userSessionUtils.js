/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { actions } from './userSessionSlice';

export function fetchUser(store, waitForLogin = true) {
  // NOTE: When waiting for login, the http promise might remain unresolved forever if login is cancelled.  A
  // successive attempt to login again should result in a new call to `fetchUser` to get a fresh promise.

  if (!store) {
    throw new Error('Redux store is required. Pass the store as the first parameter.');
  }

  // Dispatch Redux action to fetch user session
  store.dispatch(actions.fetchUserSession(waitForLogin));
}

export function waitForLogin(store) {
  if (!store) {
    throw new Error('Redux store is required. Pass the store as the first parameter.');
  }

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
        unsubscribe(); // Clean up subscription to avoid memory leak
        resolve(userSession.data);
      } else if (userSession.error) {
        // Only reject for actual errors (non-401 errors)
        // 401 errors don't set error in state, so this will only trigger for real errors
        unsubscribe(); // Clean up subscription to avoid memory leak
        reject(userSession.error);
      }
    });
  });
}

export async function _resetForTest(store) {
  if (store) {
    store.dispatch(actions.resetUserSession());
  }
}
