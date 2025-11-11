/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import loginModalModule from 'MainRoot/user/LoginModal/module';
import * as isIqIframeUtil from 'MainRoot/util/isIqFrame';
import * as sessionExpirationManager from 'MainRoot/session/sessionExpirationManager';
import { addRequest, clearRequests, getRequests } from 'MainRoot/utility/services/unauthenticatedRequestQueue';

describe('axiosConfig', () => {
  let mockAxios, attachAxiosInterceptors, mockSessionExpired;

  beforeEach(function () {
    mockAxios = Object.assign(
      jasmine.createSpy('axios').and.returnValue(Promise.resolve({ data: 'mocked response' })),
      {
        interceptors: {
          response: { use: jasmine.createSpy('axios.interceptors.response.use') },
          request: { use: jasmine.createSpy('axios.interceptors.response.use') },
        },
      }
    );

    const axiosConfig = require('inject-loader!MainRoot/utility/axiosConfig')({ axios: mockAxios });

    attachAxiosInterceptors = axiosConfig.attachAxiosInterceptors;
  });

  describe('attachAxiosInterceptors', () => {
    let $rootScope, $window, loginModalService, attachInterceptors;
    beforeEach(
      angular.mock.module(loginModalModule.name, function ($provide) {
        mockSessionExpired = jasmine.createSpy('mockSessionExpired');
        const $window = {
          location: {
            assign: jasmine.createSpy(),
          },
          sessionExpired: mockSessionExpired,
        };

        $window.top = $window;
        $provide.value('$window', $window);
      })
    );

    beforeEach(inject(function (_$rootScope_, _$window_, _LoginModalService_) {
      $rootScope = _$rootScope_.$new();
      $window = _$window_;
      loginModalService = _LoginModalService_;

      attachInterceptors = () => attachAxiosInterceptors($rootScope, $window, loginModalService);
    }));

    afterEach(() => {
      // clear any requests/promises in queue
      clearRequests();
    });

    it('attaches interceptors for the request and response of the rest calls', () => {
      attachInterceptors();

      expect(mockAxios.interceptors.response.use).toHaveBeenCalledTimes(2);
      expect(mockAxios.interceptors.request.use).toHaveBeenCalledTimes(1);
    });

    describe('request interceptors', () => {
      const getInterceptorHandlerAt = (index) => {
        const [fulfilled, rejected] = mockAxios.interceptors.request.use.calls.argsFor(index);
        return { fulfilled, rejected };
      };

      describe('cache busting interceptor', () => {
        const getCacheBustingInterceptor = () => getInterceptorHandlerAt(0);

        beforeEach(() => {
          jasmine.clock().install();
          jasmine.clock().mockDate(new Date(2022, 3, 20));

          attachInterceptors();
        });
        afterEach(() => jasmine.clock().uninstall());

        it('adds a timestamp for request to rest/api endpoints or endpoints that return json', () => {
          const cacheBustingInterceptor = getCacheBustingInterceptor();
          const currentDate = new Date().getTime();

          const restRequest = {
            url: 'remote.com/rest/resource',
          };
          const enhancedRestRequest = cacheBustingInterceptor.fulfilled(restRequest);
          expect(enhancedRestRequest.url).toEqual('remote.com/rest/resource');
          expect(enhancedRestRequest.params).toEqual(jasmine.objectContaining({ timestamp: currentDate }));

          const apiRequest = {
            url: 'remote.com/api/resource',
          };
          const enhancedApiRequest = cacheBustingInterceptor.fulfilled(apiRequest);
          expect(enhancedApiRequest.url).toEqual('remote.com/api/resource');
          expect(enhancedApiRequest.params).toEqual(jasmine.objectContaining({ timestamp: currentDate }));

          const jasonRequest = {
            url: 'remote.com/container/page/content.json',
          };
          const enhancedJsonRequest = cacheBustingInterceptor.fulfilled(jasonRequest);
          expect(enhancedJsonRequest.url).toEqual('remote.com/container/page/content.json');
          expect(enhancedJsonRequest.params).toEqual(jasmine.objectContaining({ timestamp: currentDate }));
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
        const [fulfilled, rejected] = mockAxios.interceptors.response.use.calls.argsFor(index);
        return { fulfilled, rejected };
      };

      beforeEach(() => attachInterceptors());

      describe('authentication interceptor', () => {
        const getAuthenticationInterceptor = () => getInterceptorHandlerAt(0);
        const promiseShouldNotBeResolvedFailure = () => {
          fail('promise should have been rejected');
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
            $rootScope.username = 'previous_session_username';

            const interceptorResolution = authenticationInterceptor.rejected(errorFromRequest);
            interceptorResolution.then(promiseShouldNotBeResolvedFailure, () => {
              expect(mockSessionExpired).toHaveBeenCalledTimes(1);
              expect(getRequests().length).toBe(0);
              delete $rootScope.username;
              done();
            });
          });

          it('checks if it is an iframe from the same origin', (done) => {
            const authenticationInterceptor = getAuthenticationInterceptor();
            const errorFromRequest = { response: { status: 401 } };
            // there's only one method exported by default in this util
            spyOn(isIqIframeUtil, 'default').and.returnValue(true);

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
          it('adds the request to the unauthenticatedRequestsQueue if it was waiting for login', (done) => {
            let deferred;
            spyOn(loginModalService, 'authenticate').and.callFake(() => {
              return new Promise((resolve, reject) => {
                deferred = { resolve, reject };
              });
            });
            const authenticationInterceptor = getAuthenticationInterceptor();
            const errorFromRequest = {
              response: {
                status: 401,
                headers: {},
              },
            };

            const interceptorResolution = authenticationInterceptor.rejected(errorFromRequest);

            expect(getRequests().length).toBe(1);
            interceptorResolution.then(() => {
              setTimeout(() => {
                expect(getRequests().length).toBe(0);
                done();
              }, 0);
            });
            deferred.resolve();
          });

          describe('when there is a single request in the queue', () => {
            it('requests the opening of the login modal without SSO if the appropriate header is not present', (done) => {
              let deferred;
              const loginModalAuthenticateSpy = spyOn(loginModalService, 'authenticate').and.callFake(() => {
                return new Promise((resolve, reject) => {
                  deferred = { resolve, reject };
                });
              });
              const authenticationInterceptor = getAuthenticationInterceptor();
              const errorFromRequest = {
                response: {
                  status: 401,
                  headers: {},
                },
              };

              const interceptorResolution = authenticationInterceptor.rejected(errorFromRequest);

              interceptorResolution.then(() => {
                expect(loginModalAuthenticateSpy).toHaveBeenCalledOnceWith(undefined, undefined);
                done();
              });
              deferred.resolve();
            });

            it('requests the opening of the login modal with SAML SSO if the SAML header is present', (done) => {
              let deferred;
              const loginModalAuthenticateSpy = spyOn(loginModalService, 'authenticate').and.callFake(() => {
                return new Promise((resolve, reject) => {
                  deferred = { resolve, reject };
                });
              });
              const authenticationInterceptor = getAuthenticationInterceptor();
              const errorFromRequest = {
                response: {
                  status: 401,
                  headers: {
                    'www-authenticate': 'SAML',
                    'x-sso-login-url': '/saml/login',
                  },
                },
              };

              const interceptorResolution = authenticationInterceptor.rejected(errorFromRequest);

              interceptorResolution.then(() => {
                expect(loginModalAuthenticateSpy).toHaveBeenCalledOnceWith('SAML', '/saml/login');
                done();
              });
              deferred.resolve();
            });

            it('requests the opening of the login modal with OIDC SSO if the OIDC header is present', (done) => {
              let deferred;
              const loginModalAuthenticateSpy = spyOn(loginModalService, 'authenticate').and.callFake(() => {
                return new Promise((resolve, reject) => {
                  deferred = { resolve, reject };
                });
              });
              const authenticationInterceptor = getAuthenticationInterceptor();
              const errorFromRequest = {
                response: {
                  status: 401,
                  headers: {
                    'www-authenticate': 'OIDC',
                    'x-sso-login-url': '/oidc/login',
                  },
                },
              };

              const interceptorResolution = authenticationInterceptor.rejected(errorFromRequest);

              interceptorResolution.then(() => {
                expect(loginModalAuthenticateSpy).toHaveBeenCalledOnceWith('OIDC', '/oidc/login');
                done();
              });
              deferred.resolve();
            });

            it('Resolves all promises in the queue and clears any requests after authentication is successful', (done) => {
              let deferred1;
              let deferred2;
              const newRequestedPromise = new Promise((resolve, reject) => {
                deferred2 = { resolve, reject };
              });
              spyOn(loginModalService, 'authenticate').and.callFake(() => {
                addRequest(() => newRequestedPromise);
                return new Promise((resolve, reject) => {
                  deferred1 = { resolve, reject };
                });
              });
              const authenticationInterceptor = getAuthenticationInterceptor();
              const errorFromRequest = {
                response: {
                  status: 401,
                  headers: {},
                },
              };

              const interceptorResolution = authenticationInterceptor.rejected(errorFromRequest);

              interceptorResolution.then(() => {
                expect(getRequests().length).toBe(2);
                deferred2.resolve();
                setTimeout(() => {
                  expect(getRequests().length).toBe(0);
                  done();
                }, 0);
              });
              deferred1.resolve();
            });

            it('clears any remaining requests if authentication is not successful or cancelled', (done) => {
              let deferred;
              spyOn(loginModalService, 'authenticate').and.callFake(() => {
                return new Promise((resolve, reject) => {
                  deferred = { resolve, reject };
                });
              });
              const authenticationInterceptor = getAuthenticationInterceptor();
              const errorFromRequest = {
                response: {
                  status: 401,
                  headers: {},
                },
              };

              const interceptorResolution = authenticationInterceptor.rejected(errorFromRequest);

              interceptorResolution.then(promiseShouldNotBeResolvedFailure, () => {
                expect(getRequests().length).toBe(0);
                done();
              });
              deferred.reject();
            });
          });
        });
      });

      describe('server date interceptor', () => {
        const getSeverDateInterceptor = () => getInterceptorHandlerAt(1);

        it('calls the setServerDate passed function when the request returns the appropriate header', () => {
          spyOn(sessionExpirationManager, 'setServerDate');

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
