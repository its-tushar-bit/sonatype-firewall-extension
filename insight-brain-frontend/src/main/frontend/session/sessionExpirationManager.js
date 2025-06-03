/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import Cookies from 'js-cookie';
import { actions as logoutWarningModalActions } from 'MainRoot/modals/logoutWarningModal/logoutWarningModalSlice';
import { getSessionLogoutUrl } from 'MainRoot/util/CLMLocation';

/**
 * Session Expiration Manager
 *
 * This module manages user session expiration in the application. It tracks the session expiration timestamp
 * from a cookie, compensates for clock differences between client and server, and handles the session
 * expiration workflow including warnings and logout.
 */

const COOKIE_NAME = 'IQ-SESSION-EXPIRATION-TIMESTAMP';

const TWO_MINUTES = 2 * 60 * 1000;

/*
 * The approximate difference between the server's clock time and the time on the client. This is necessary to
 * more reliably determine whether the server session has timed out. Note that this value cannot be exact because
 * it also includes an unknown and not necessarily consistent amount of network latency. If the client's clock
 * is ahead of the server's, this value will be positive.
 */
let serverDateDifference = 0;

/**
 * Get the timestamp from the cookie and adjust it to compensate for clock differences between
 * the client and server. The cookie value is based on the server's clock but this function returns a value based on
 * the client's clock.
 * @return the current value of the session expiration, adjusted
 */
function getSessionExpirationTimestamp() {
  const sessionExpirationTimestampStr = Cookies.get(COOKIE_NAME),
    sessionExpirationTimestamp = parseInt(sessionExpirationTimestampStr, 10);

  return sessionExpirationTimestamp + serverDateDifference;
}

/**
 * Check to see if the current value of the session expiration cookie is in the past, and run sessionExpired if so
 */
function checkSessionExpired(store, productEdition) {
  if (Date.now() > getSessionExpirationTimestamp()) {
    sessionExpired();
  } else {
    checkSessionExpiredLater(store, productEdition);
  }
}

/**
 * Returns the difference between the sessionExpirationTimestamp and the current date.
 * If this difference is negative, returns 0
 * @returns milliseconds left for the session
 */
function getSessionTimeoutMillis() {
  const difference = getSessionExpirationTimestamp() - Date.now();
  return Math.max(difference, 0);
}

export function sessionExpired() {
  // unbind the beforeunload handler so that the page refresh cannot be cancelled
  window.removeEventListener('beforeunload', window.unloadListener);
  const xhr = new XMLHttpRequest();
  xhr.onload = () => {
    // for MTIQ, the session logout call returns the URL to logout from Auth0 in the Location header, so
    // we obtain that URL and we ensure we perform the logout from Auth0 too
    const location = xhr.getResponseHeader('Location');
    if (location) {
      window.location.href = location;
    } else {
      window.location.reload();
    }
  };
  xhr.open('DELETE', getSessionLogoutUrl());
  xhr.send();
}

/**
 * Set up a timeout to call checkSessionExpired at the time of the sessionExpirationTimestamp
 */
export function checkSessionExpiredLater(store, productEdition) {
  const sessionTimeoutMillis = getSessionTimeoutMillis();
  const alertTimeoutMillis = sessionTimeoutMillis - TWO_MINUTES;

  if (!isNaN(alertTimeoutMillis)) {
    if (alertTimeoutMillis > 0) {
      setTimeout(() => checkSessionExpiredLater(store, productEdition), alertTimeoutMillis);
    } else {
      store.dispatch(
        logoutWarningModalActions.open({
          startingCount: Math.floor(sessionTimeoutMillis / 1000),
          productEdition: productEdition,
        })
      );
      setTimeout(() => checkSessionExpired(store, productEdition), sessionTimeoutMillis);
    }
  } else {
    console.warn(COOKIE_NAME + ' cookie is missing. Session timeout detection will be disabled');
  }
}

/**
 * Notify this service of the current timestamp according to the server
 */
export function setServerDate(serverDate) {
  serverDateDifference = new Date() - serverDate;
}

window.sessionExpired = sessionExpired;
