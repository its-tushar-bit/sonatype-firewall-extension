/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import logoutWarningModalModule from './utility/services/logoutWarningModal/module';

const COOKIE_NAME = 'IQ-SESSION-EXPIRATION-TIMESTAMP';

const TWO_MINUTES = 2 * 60 * 1000;
/**
 * A service that keeps track of how long it has been since the session was refreshed, and if it has been too long,
 * assumes that the session has expired and refreshes the page for security
 */
function SessionSecurityService($cookies, $window, $rootScope, logoutWarningModalService, CLMLocations) {
  /*
   * the approximate difference between the server's clock time and the time on the client.  This is necessary to
   * more reliably determine whether the server session has timed out.  Note that this value cannot be exact because
   * it also includes an unknown and not necessarily consistent amount of network latency.  If the client's clock
   * is ahead of the server's, this value will be positive
   */
  let serverDateDifference = 0;

  /**
   * Get the timestamp from the cookie and adjust it to compensate for clock differences between
   * the client and server.
   * @return the current value of the session expiration, adjusted
   */
  function getSessionExpirationTimestamp() {
    const sessionExpirationTimestampStr = $cookies.get(COOKIE_NAME),
      sessionExpirationTimestamp = parseInt(sessionExpirationTimestampStr, 10);

    return sessionExpirationTimestamp + serverDateDifference;
  }

  /**
   * check to see if the current value of the session expiration cookie is in the past, and run sessionExpired if so
   */
  function checkSessionExpired() {
    if (Date.now() > getSessionExpirationTimestamp()) {
      sessionExpired();
    } else {
      checkSessionExpiredLater();
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

  /**
   * Refresh the page now that the session is expired
   */
  function sessionExpired() {
    // unbind the beforeunload handler so that the page refresh cannot be cancelled
    $($window).unbind('beforeunload');
    const xhr = new XMLHttpRequest();
    xhr.onload = () => {
      // for MTIQ, the session logout call returns the URL to logout from Auth0 in the Location header, so
      // we obtain that URL and we ensure we perform the logout from Auth0 too
      const location = xhr.getResponseHeader('Location');
      if (location) {
        $window.location.href = location;
      } else {
        $window.location.reload();
      }
    };
    xhr.open('DELETE', CLMLocations.getSessionLogoutUrl());
    xhr.send();
  }

  /**
   * Set up a timeout to call checkSessionExpired at the time of the sessionExpirationTimestamp
   */
  function checkSessionExpiredLater() {
    const sessionTimeoutMillis = getSessionTimeoutMillis();
    const alertTimeoutMillis = sessionTimeoutMillis - TWO_MINUTES;

    if (!isNaN(alertTimeoutMillis)) {
      // NOTE don't use $timeout here. Angular appears to have an issue where having more than one
      // forever-repeating timeout/interval causes it to never consider the page to be "stable", which
      // breaks everything that relies on our StableBodyService. By using setTimeout instead of $timeout, we
      // avoid letting angular know about this timeout so that problem is avoided.
      // Cleanup of the StableBodyService is in https://issues.sonatype.org/browse/CLM-7840
      if (alertTimeoutMillis > 0) {
        setTimeout(checkSessionExpiredLater, alertTimeoutMillis);
      } else {
        logoutWarningModalService.open(Math.floor(sessionTimeoutMillis / 1000), $rootScope.productEdition);
        setTimeout(checkSessionExpired, sessionTimeoutMillis);
      }
    } else {
      console.warn(COOKIE_NAME + ' cookie is missing. Session timeout detection will be disabled');
    }
  }

  /**
   * Notify this service of the current timestamp according to the server
   */
  function setServerDate(serverDate) {
    serverDateDifference = new Date() - serverDate;
  }

  return {
    init: checkSessionExpiredLater,
    sessionExpired: sessionExpired,
    setServerDate: setServerDate,
  };
}

SessionSecurityService.$inject = ['$cookies', '$window', `$rootScope`, 'logoutWarningModalService', 'CLMLocations'];

export default angular
  .module('SessionSecurityModule', ['ngCookies', logoutWarningModalModule.name])
  .service('SessionSecurityService', SessionSecurityService)
  .run([
    '$window',
    'SessionSecurityService',
    function ($window, SessionSecurityService) {
      // expose sessionExpired globally so it can be called by code from child iframes
      $window.sessionExpired = SessionSecurityService.sessionExpired;
    },
  ]);
