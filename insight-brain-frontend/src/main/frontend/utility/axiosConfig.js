/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import isIqIframe from '../utilAngular/isIqFrame';

/**
 * @param setServerDate   Angular service SessionSecurityService setServerDate method.
 * @param rootScope       Angular's $rootScope variable.
 * @param window     Angular's $window variable.
 * @param loginModalService    LoginModalService (open login modal)
 * @param UnauthenticatedRequestQueueService the queue service to provide control of the outstanding requests
 **/

export const attachAxiosInterceptors = (
  setServerDate,
  rootScope,
  window,
  loginModalService,
  UnauthenticatedRequestQueueService
) => {
  // http interceptor
  axios.interceptors.response.use(
    (response) => {
      return response;
    },
    (error) => {
      const isUnauthorized = error.response?.status === 401;
      if (isUnauthorized) {
        // rootScope.username will be present if this is the top frame and login had already succeeded previously.
        // If we are in a child frame (for a report), the username won't be available but we can still detect that
        // we are in a child frame.
        if (rootScope.username || isIqIframe(window)) {
          // session expired - tell SessionSecurityService of the main IQ UI, which resides in the top frame of
          // the page.
          window.top.sessionExpired();
        } else {
          if (error.response.config && error.response.config.waitForLogin === false) {
            return Promise.reject(error.response);
          } else {
            UnauthenticatedRequestQueueService.addRequest(() => {
              // simply replay the request
              axios(error.response.config);
            });
            // we only want to pop up the dialog for the first error, as many requests may be sent asynchronously, for
            // the other messages, the data will be added to the queue, but the dialog portion will be ignored
            if (UnauthenticatedRequestQueueService.getRequests().length === 1) {
              loginModalService.authenticate(error.response.headers['www-authenticate'] === 'SAML').then(
                () => {
                  // retry failed requests and then clear the queue
                  Promise.all(UnauthenticatedRequestQueueService.getPromises()).finally(() =>
                    UnauthenticatedRequestQueueService.clearRequests()
                  );
                },
                () => {
                  // login was cancelled
                  UnauthenticatedRequestQueueService.clearRequests();
                }
              );
            }
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
