/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
var COOKIE_NAME = 'IQ-SESSION-EXPIRATION-TIMESTAMP';
/**
 * A service that keeps track of how long it has been since the session was refreshed, and if it has been too long,
 * assumes that the session has expired and refreshes the page for security
 */
function SessionSecurityService($cookies, $window) {
  /*
   * the approximate difference between the server's clock time and the time on the client.  This is necessary to
   * more reliably determine whether the server session has timed out.  Note that this value cannot be exact because
   * it also includes an unknown and not necessarily consistent amount of network latency.  If the client's clock
   * is ahead of the server's, this value will be positive
   */
  var serverDateDifference = 0;

  /**
   * @return the current value of the session expiration cookie
   */
  function getSessionExpirationTimestamp() {
    var sessionExpirationTimestampStr = $cookies.get(COOKIE_NAME),
      sessionExpirationTimestamp = parseInt(sessionExpirationTimestampStr, 10);

    return sessionExpirationTimestamp;
  }

  /**
   * check to see if the current value of the session expiration cookie is in the past, and run sessionExpired if so
   */
  function checkSessionExpired() {
    // get the timestamp from the cookie and adjust it to compensate for clock differences between
    // the client and server.
    var sessionExpirationTimestamp =
      getSessionExpirationTimestamp() + serverDateDifference;

    if (new Date() > sessionExpirationTimestamp) {
      sessionExpired();
    } else {
      checkSessionExpiredLater(sessionExpirationTimestamp);
    }
  }

  /**
   * Refresh the page now that the session is expired
   */
  function sessionExpired() {
    // unbind the beforeunload handler so that the page refresh cannot be cancelled
    $($window).unbind('beforeunload');
    $window.location.reload();
  }

  /**
   * Set up a timeout to call checkSessionExpired at the time of the sessionExpirationTimestamp
   */
  function checkSessionExpiredLater(sessionExpirationTimestamp) {
    var sessionTimeoutMillis = sessionExpirationTimestamp - new Date();

    if (!isNaN(sessionTimeoutMillis)) {
      // NOTE don't use $timeout here. Angular appears to have an issue where having more than one
      // forever-repeating timeout/interval causes it to never consider the page to be "stable", which
      // breaks everything that relies on our StableBodyService. By using setTimeout instead of $timeout, we
      // avoid letting angular know about this timeout so that problem is avoided.
      // Cleanup of the StableBodyService is in https://issues.sonatype.org/browse/CLM-7840
      setTimeout(checkSessionExpired, sessionTimeoutMillis);
    } else {
      console.warn(
        COOKIE_NAME +
          ' cookie is missing. Session timeout detection will be disabled'
      );
    }
  }

  /**
   * Notify this service of the current timestamp according to the server
   */
  function setServerDate(serverDate) {
    serverDateDifference = new Date() - serverDate;
  }

  function init() {
    checkSessionExpiredLater(getSessionExpirationTimestamp());
  }

  return {
    init: init,
    sessionExpired: sessionExpired,
    setServerDate: setServerDate,
  };
}

SessionSecurityService.$inject = ['$cookies', '$window'];

export default angular
  .module('SessionSecurityModule', ['ngCookies']) //
  .service('SessionSecurityService', SessionSecurityService) //
  .run([
    '$window',
    'SessionSecurityService',
    function ($window, SessionSecurityService) {
      // expose sessionExpired globally so it can be called by code from child iframes
      $window.sessionExpired = SessionSecurityService.sessionExpired;
    },
  ]);
