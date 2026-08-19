/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as isIqIframeUtil from 'MainRoot/util/isIqFrame';
import * as sessionExpirationManager from 'MainRoot/session/sessionExpirationManager';
import { addRequest, clearRequests, getRequests } from 'MainRoot/utility/services/unauthenticatedRequestQueue';
import axios from 'axios';
import * as userSessionUtils from 'MainRoot/user/userSessionUtils';
import { attachAxiosInterceptors } from 'MainRoot/utility/axiosConfig';
import store from 'MainRoot/reduxConfig/store';

describe('axiosConfig', () => {
  let mockSessionExpired;

  beforeEach(function () {
    // We'll spy on axios interceptors directly
  });

  describe('attachAxiosInterceptors', () => {
    let $window, attachInterceptors, mockReduxState, authenticateSpy;

    beforeEach(() => {
      $window = {
        location: {
          assign: jest.fn(),
        },
      };

      mockSessionExpired = jest.fn().mockName('mockSessionExpired');
      $window.sessionExpired = mockSessionExpired;
      $window.top = $window;

      // axiosConfig uses window.top.sessionExpired(), not $window
      // Set up the mock on the real window.top object
      window.top.sessionExpired = mockSessionExpired;

      // Create a mock Redux state that can be modified
      mockReduxState = {
        userSession: {
          data: null,
        },
        appError: {
          error: null,
        },
        userLogin: {},
      };

      // Spy on store.getState to return our mock state
      jest.spyOn(store, 'getState').mockReturnValue(mockReduxState);

      // Spy on axios interceptor methods
      jest.spyOn(axios.interceptors.response, 'use');
      jest.spyOn(axios.interceptors.request, 'use');

      // Mock axios default function (used by axios(config) when replaying requests)
      // This intercepts calls to axios() that happen when queued requests are replayed
      const mockAxiosAdapter = (config) => {
        // Return success for test URLs to prevent actual HTTP requests
        if (config && config.url === '/api/test') {
          return Promise.resolve({ data: 'test', status: 200, config });
        }
        // For other URLs, return 404 to match real behavior
        return Promise.reject({ response: { status: 404, config } });
      };

      // Set up axios adapter to use our mock
      axios.defaults.adapter = mockAxiosAdapter;

      // Spy on userSessionUtils.authenticate
      authenticateSpy = jest.spyOn(userSessionUtils, 'authenticate').mockReturnValue(Promise.resolve());

      // attachAxiosInterceptors now takes no parameters
      attachInterceptors = () => attachAxiosInterceptors();
    });

    afterEach(() => {
      // clear any requests/promises in queue
      clearRequests();
    });

    it('attaches interceptors for the request and response of the rest calls', () => {
      attachInterceptors();

      expect(axios.interceptors.response.use).toHaveBeenCalledTimes(2);
      expect(axios.interceptors.request.use).toHaveBeenCalledTimes(1);
    });

    describe('request interceptors', () => {
      const getInterceptorHandlerAt = (index) => {
        const [fulfilled, rejected] = axios.interceptors.request.use.mock.calls[index];
        return { fulfilled, rejected };
      };

      describe('cache busting interceptor', () => {
        const getCacheBustingInterceptor = () => getInterceptorHandlerAt(0);

        beforeEach(() => {
          jest.useFakeTimers();
          jest.setSystemTime(new Date(2022, 3, 20));

          attachInterceptors();
        });
        afterEach(() => jest.useRealTimers());

        it('adds a timestamp for request to rest/api endpoints or endpoints that return json', () => {
          const cacheBustingInterceptor = getCacheBustingInterceptor();
          const currentDate = new Date().getTime();

          const restRequest = {
            url: 'remote.com/rest/resource',
          };
          const enhancedRestRequest = cacheBustingInterceptor.fulfilled(restRequest);
          expect(enhancedRestRequest.url).toEqual('remote.com/rest/resource');
          expect(enhancedRestRequest.params).toEqual(expect.objectContaining({ timestamp: currentDate }));

          const apiRequest = {
            url: 'remote.com/api/resource',
          };
          const enhancedApiRequest = cacheBustingInterceptor.fulfilled(apiRequest);
          expect(enhancedApiRequest.url).toEqual('remote.com/api/resource');
          expect(enhancedApiRequest.params).toEqual(expect.objectContaining({ timestamp: currentDate }));

          const jasonRequest = {
            url: 'remote.com/container/page/content.json',
          };
          const enhancedJsonRequest = cacheBustingInterceptor.fulfilled(jasonRequest);
          expect(enhancedJsonRequest.url).toEqual('remote.com/container/page/content.json');
          expect(enhancedJsonRequest.params).toEqual(expect.objectContaining({ timestamp: currentDate }));
        });

        it('does not add timestamp to rest/api endpoints or json requests if they already have a timestamp', () => {
          const cacheBustingInterceptor = getCacheBustingInterceptor();

          const restRequest = {
            url: 'remote.com/rest/resource?timestamp=0000000',
          };
          const enhancedRestRequest = cacheBustingInterceptor.fulfilled(restRequest);
          expect(enhancedRestRequest.url).toEqual('remote.com/rest/resource?timestamp=0000000');
          expect(enhancedRestRequest.params).toBeUndefined();

          const apiRequest = {
            url: 'remote.com/api/resource?timestamp=0000000',
          };
          const enhancedApiRequest = cacheBustingInterceptor.fulfilled(apiRequest);
          expect(enhancedApiRequest.url).toEqual('remote.com/api/resource?timestamp=0000000');
          expect(enhancedApiRequest.params).toBeUndefined();

          const jasonRequest = {
            url: 'remote.com/container/page/content.json?timestamp=0000000',
            params: { dummyParam: 'dummyParam' },
          };
          const enhancedJsonRequest = cacheBustingInterceptor.fulfilled(jasonRequest);
          expect(enhancedJsonRequest.url).toEqual('remote.com/container/page/content.json?timestamp=0000000');
          expect(enhancedJsonRequest.params.timestamp).toBeUndefined();
          expect(enhancedJsonRequest.params).toEqual({ dummyParam: 'dummyParam' });
        });

        it('does not add timestamp for requests to other resources or that do not return json', () => {
          const cacheBustingInterceptor = getCacheBustingInterceptor();

          const request = {
            url: 'remote.com/container/page/resource.csv',
          };
          const enhancedRequest = cacheBustingInterceptor.fulfilled(request);
          expect(enhancedRequest.url).toEqual('remote.com/container/page/resource.csv');
          expect(enhancedRequest.params).toBeUndefined();

          const requestWithParams = {
            url: 'remote.com/container/page',
            params: { dummyParam: 'dummyParam' },
          };
          const enhancedRequestWithParams = cacheBustingInterceptor.fulfilled(requestWithParams);
          expect(enhancedRequestWithParams.url).toEqual('remote.com/container/page');
          expect(enhancedRequestWithParams.params.timestamp).toBeUndefined();
          expect(enhancedRequestWithParams.params).toEqual({ dummyParam: 'dummyParam' });
        });

        it('adds security cookie and header names to the request', () => {
          const cacheBustingInterceptor = getCacheBustingInterceptor();
          const requestWithParams = {
            url: 'remote.com/container/page',
            params: { dummyParam: 'dummyParam' },
          };
          const enhancedRequestWithParams = cacheBustingInterceptor.fulfilled(requestWithParams);

          const expectedEnhancedRequest = {
            url: 'remote.com/container/page',
            params: { dummyParam: 'dummyParam' },
            xsrfCookieName: 'CLM-CSRF-TOKEN',
            xsrfHeaderName: 'X-CSRF-TOKEN',
          };
          expect(enhancedRequestWithParams).toEqual(expectedEnhancedRequest);
        });
      });
    });

    describe('response interceptors', () => {
      const getInterceptorHandlerAt = (index) => {
        const [fulfilled, rejected] = axios.interceptors.response.use.mock.calls[index];
        return { fulfilled, rejected };
      };

      beforeEach(() => attachInterceptors());

      describe('authentication interceptor', () => {
        const getAuthenticationInterceptor = () => getInterceptorHandlerAt(0);
        const promiseShouldNotBeResolvedFailure = () => {
          throw new Error('promise should have been rejected');
        };

        it('returns the rejected promise immediately if the rejection was not due to requiring authentication', (done) => {
          const authenticationInterceptor = getAuthenticationInterceptor();

          const errorFromRequest = { response: { status: 503 }, message: 'server error' };
          const interceptorResolution = authenticationInterceptor.rejected(errorFromRequest);

          interceptorResolution.then(promiseShouldNotBeResolvedFailure, (error) => {
            expect(error).toEqual(errorFromRequest);
            expect(getRequests().length).toBe(0);
            done();
          });
        });

        describe('expires an active UI session if the session has expired in the server', () => {
          it('checks for an existing username in the scope', (done) => {
            const authenticationInterceptor = getAuthenticationInterceptor();
            const errorFromRequest = { response: { status: 401 } };
            // Set username in Redux state to simulate existing session
            mockReduxState.userSession.data = { username: 'previous_session_username' };

            const interceptorResolution = authenticationInterceptor.rejected(errorFromRequest);
            interceptorResolution.then(promiseShouldNotBeResolvedFailure, () => {
              expect(mockSessionExpired).toHaveBeenCalledTimes(1);
              expect(getRequests().length).toBe(0);
              done();
            });
          });

          it('checks if it is an iframe from the same origin', (done) => {
            const authenticationInterceptor = getAuthenticationInterceptor();
            const errorFromRequest = { response: { status: 401 } };
            // there's only one method exported by default in this util
            jest.spyOn(isIqIframeUtil, 'default').mockReturnValue(true);

            const interceptorResolution = authenticationInterceptor.rejected(errorFromRequest);
            interceptorResolution.then(promiseShouldNotBeResolvedFailure, () => {
              expect(mockSessionExpired).toHaveBeenCalledTimes(1);
              expect(getRequests().length).toBe(0);
              done();
            });
          });
        });

        it(
          'returns the rejected promise if the request was not expecting to wait for login ' +
            'without adding it to the queue',
          (done) => {
            const authenticationInterceptor = getAuthenticationInterceptor();
            const errorFromRequest = { response: { status: 401, config: { waitForLogin: false } } };

            const interceptorResolution = authenticationInterceptor.rejected(errorFromRequest);
            interceptorResolution.then(promiseShouldNotBeResolvedFailure, (error) => {
              expect(getRequests().length).toBe(0);
              expect(error).toEqual(errorFromRequest);
              done();
            });
          }
        );

        describe('intercepts a request that is waiting for login and is rejected due to authentication', () => {
          beforeEach(() => {
            // Mock isIqIframe to return false so we don't trigger sessionExpired
            jest.spyOn(isIqIframeUtil, 'default').mockReturnValue(false);
          });

          it('adds the request to the unauthenticatedRequestsQueue if it was waiting for login', (done) => {
            const authenticationInterceptor = getAuthenticationInterceptor();
            const errorFromRequest = {
              response: {
                status: 401,
                headers: {},
                config: { url: '/api/test' },
              },
            };

            const interceptorResolution = authenticationInterceptor.rejected(errorFromRequest);

            expect(getRequests().length).toBe(1);

            // Promise resolves when authentication succeeds
            interceptorResolution.then(() => {
              setTimeout(() => {
                expect(getRequests().length).toBe(0);
                done();
              }, 0);
            });
          });

          describe('when there is a single request in the queue', () => {
            it('requests the opening of the login modal without SSO if the appropriate header is not present', (done) => {
              const authenticationInterceptor = getAuthenticationInterceptor();
              const errorFromRequest = {
                response: {
                  status: 401,
                  headers: {},
                  config: { url: '/api/test' },
                },
              };

              const interceptorResolution = authenticationInterceptor.rejected(errorFromRequest);

              interceptorResolution.then(() => {
                expect(authenticateSpy).toHaveBeenCalledTimes(1);
                expect(authenticateSpy).toHaveBeenCalledWith(undefined, undefined);
                done();
              });
            });

            it('requests the opening of the login modal with SAML SSO if the SAML header is present', (done) => {
              const authenticationInterceptor = getAuthenticationInterceptor();
              const errorFromRequest = {
                response: {
                  status: 401,
                  headers: {
                    'www-authenticate': 'SAML',
                    'x-sso-login-url': '/saml/login',
                  },
                  config: { url: '/api/test' },
                },
              };

              const interceptorResolution = authenticationInterceptor.rejected(errorFromRequest);

              interceptorResolution.then(() => {
                expect(authenticateSpy).toHaveBeenCalledTimes(1);
                expect(authenticateSpy).toHaveBeenCalledWith('SAML', '/saml/login');
                done();
              });
            });

            it('requests the opening of the login modal with OIDC SSO if the OIDC header is present', (done) => {
              const authenticationInterceptor = getAuthenticationInterceptor();
              const errorFromRequest = {
                response: {
                  status: 401,
                  headers: {
                    'www-authenticate': 'OIDC',
                    'x-sso-login-url': '/oidc/login',
                  },
                  config: { url: '/api/test' },
                },
              };

              const interceptorResolution = authenticationInterceptor.rejected(errorFromRequest);

              interceptorResolution.then(() => {
                expect(authenticateSpy).toHaveBeenCalledTimes(1);
                expect(authenticateSpy).toHaveBeenCalledWith('OIDC', '/oidc/login');
                done();
              });
            });

            it('resolves all promises in the queue and clears any requests after authentication is successful', (done) => {
              let resolveAuthentication, newRequestResolve;
              const authenticationPromise = new Promise((resolve) => {
                resolveAuthentication = resolve;
              });
              const newRequestedPromise = new Promise((resolve) => {
                newRequestResolve = resolve;
              });

              // Reset the spy and mock authenticate to return a controllable promise and add an additional request
              authenticateSpy.mockImplementation(() => {
                // Add a second request to the queue when authenticate is called
                addRequest(
                  () => newRequestedPromise,
                  () => {}
                );
                // Return the authentication promise directly (no longer a thunk)
                return authenticationPromise;
              });

              const authenticationInterceptor = getAuthenticationInterceptor();
              const errorFromRequest = {
                response: {
                  status: 401,
                  headers: {},
                  config: { url: '/api/test' },
                },
              };

              const interceptorResolution = authenticationInterceptor.rejected(errorFromRequest);

              // Wait for authenticate to be called and the second request to be added
              setTimeout(() => {
                // Should have 2 requests: the initial 401 retry + the one added in authenticate
                expect(getRequests().length).toBe(2);

                // Resolve authentication, which should trigger settleAll()
                resolveAuthentication();

                // Also resolve the new request that was added
                newRequestResolve();

                // Wait for all promises to settle
                interceptorResolution.then(() => {
                  setTimeout(() => {
                    // All requests should be cleared after successful replay
                    expect(getRequests().length).toBe(0);
                    done();
                  }, 0);
                });
              }, 10);
            });

            it('clears any remaining requests if authentication is not successful or cancelled', (done) => {
              // Mock authenticate to return a rejected promise (cancelled/failed authentication)
              // Use mockImplementation to defer promise creation until the spy is actually called
              authenticateSpy.mockImplementation(() => Promise.reject(new Error('Authentication cancelled')));

              const authenticationInterceptor = getAuthenticationInterceptor();
              const errorFromRequest = {
                response: {
                  status: 401,
                  headers: {},
                  config: { url: '/api/test' },
                },
              };

              const interceptorResolution = authenticationInterceptor.rejected(errorFromRequest);

              interceptorResolution.then(promiseShouldNotBeResolvedFailure, () => {
                // Wait a tick to allow rejectAll() to complete
                setTimeout(() => {
                  expect(getRequests().length).toBe(0);
                  done();
                }, 0);
              });
            });
          });
        });
      });

      describe('server date interceptor', () => {
        const getSeverDateInterceptor = () => getInterceptorHandlerAt(1);

        it('calls the setServerDate passed function when the request returns the appropriate header', () => {
          jest.spyOn(sessionExpirationManager, 'setServerDate');

          const serverDateInterceptor = getSeverDateInterceptor();
          serverDateInterceptor.fulfilled({ headers: {} });
          expect(sessionExpirationManager.setServerDate).not.toHaveBeenCalled();

          const expectedDate = new Date('Thu, 17 Mar 2022 17:36:03 GMT');
          serverDateInterceptor.fulfilled({ headers: { date: 'Thu, 17 Mar 2022 17:36:03 GMT' } });
          expect(sessionExpirationManager.setServerDate).toHaveBeenCalledWith(expectedDate);
        });
      });
    });
  });
});
