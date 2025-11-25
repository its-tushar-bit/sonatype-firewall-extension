/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import isIqIframe from '../util/isIqFrame';
import { setServerDate } from 'MainRoot/session/sessionExpirationManager';
import { addRequest, getRequests, rejectAll, settleAll } from 'MainRoot/utility/services/unauthenticatedRequestQueue';

/**
 * @param rootScope    Angular's $rootScope variable.
 * @param window     Angular's $window variable.
 * @param loginModalActions    loginModalActions (Redux action creators for login modal)
 * @param store    Redux store
 **/

export const attachAxiosInterceptors = (rootScope, window, loginModalActions, store) => {
  // http interceptor
  axios.interceptors.response.use(
    (response) => {
      return response;
    },
    (error) => {
      const isUnauthorized = error.response?.status === 401;
      if (isUnauthorized) {
        // Check if user is logged in by reading from Redux state
        const state = store.getState();
        const username = state.userSession?.data?.username;

        // username will be present if this is the top frame and login had already succeeded previously.
        // If we are in a child frame (for a report), the username won't be available but we can still detect that
        // we are in a child frame.
        if (username || isIqIframe(window)) {
          // session expired - tell sessionExpirationManager of the main IQ UI, which resides in the top frame of
          // the page.
          window.top.sessionExpired();
        } else {
          if (error.response.config && error.response.config.waitForLogin === false) {
            return Promise.reject(error);
          } else {
            return new Promise((resolve, reject) => {
              addRequest(() => {
                // simply replay the request
                axios(error.response.config).then(resolve, reject);
              }, reject);
              // we only want to pop up the dialog for the first error, as many requests may be sent asynchronously, for
              // the other messages, the data will be added to the queue, but the dialog portion will be ignored
              if (getRequests().length === 1) {
                // Dispatch authenticate action and wait for user to log in
                store
                  .dispatch(
                    loginModalActions.authenticate(
                      error.response.headers['www-authenticate'],
                      error.response.headers['x-sso-login-url']
                    )
                  )
                  .then(() => {
                    // User logged in successfully, replay all queued requests
                    settleAll();
                  })
                  .catch(() => {
                    // Login was cancelled or failed, reject all queued requests
                    rejectAll();
                  });
              }
            });
          }
        }
      }
      return Promise.reject(error);
    }
  );

  // iq interceptor
  axios.interceptors.response.use(function (response) {
    const dateString = response.headers?.date;
    const serverDate = dateString ? new Date(dateString) : undefined;

    if (serverDate) {
      setServerDate(serverDate);
    }

    return response;
  });

  // cache busting interceptor factory, which handles adding a timestamp query parameter to each request
  axios.interceptors.request.use(
    function (config) {
      // Do something before request is sent
      if (
        (config.url.indexOf('/rest/') > -1 || config.url.indexOf('/api/') > -1 || config.url.indexOf('.json') > -1) &&
        config.url.indexOf('timestamp=') < 0
      ) {
        config.params = config.params || {};
        config.params.timestamp = new Date().getTime();
      }
      config.xsrfCookieName = 'CLM-CSRF-TOKEN';
      config.xsrfHeaderName = 'X-CSRF-TOKEN';
      return config;
    },
    function (error) {
      // Do something with request error
      return Promise.reject(error);
    }
  );
};
