/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import axios from 'axios';
import { attachAxiosInterceptors } from 'MainRoot/utility/axiosConfig';
import loginModalModule from 'MainRoot/user/LoginModal/module';
import * as isIqIframeUtil from 'MainRoot/util/isIqFrame';
import utilityServicesModule from 'MainRoot/utility/services/utility.services.module';

describe('axiosConfig', () => {
  describe('attachAxiosInterceptors', () => {
    let $rootScope, $window, setServerDateSpy, loginModalService, queueService, attachInterceptors;

    beforeEach(
      angular.mock.module(utilityServicesModule.name, loginModalModule.name, function ($provide) {
        const sessionExpiredSpy = jasmine.createSpy(),
          $window = {
            sessionExpired: sessionExpiredSpy,
            location: {
              assign: jasmine.createSpy(),
            },
          };

        $window.top = $window;
        $provide.value('$window', $window);
      })
    );

    beforeEach(inject(function (_$rootScope_, _$window_, _UnauthenticatedRequestQueueService_, _LoginModalService_) {
      $rootScope = _$rootScope_.$new();
      $window = _$window_;
      setServerDateSpy = jasmine.createSpy('setServerDate');
      queueService = _UnauthenticatedRequestQueueService_;
      loginModalService = _LoginModalService_;

      attachInterceptors = () =>
        attachAxiosInterceptors(setServerDateSpy, $rootScope, $window, loginModalService, queueService);
    }));

    afterEach(() => {
      // clear any active interceptors in axios and requests/promises in queue
      const requestInterceptors = axios.interceptors.request.handlers;
      const responseInterceptors = axios.interceptors.response.handlers;
      requestInterceptors.splice(0, requestInterceptors.length);
      responseInterceptors.splice(0, responseInterceptors.length);
      queueService.clearRequests();
    });

    it('attaches interceptors for the request and response of the rest calls', () => {
      const userResponseInterceptor = spyOn(axios.interceptors.response, 'use').and.callThrough();
      const useRequestInterceptor = spyOn(axios.interceptors.request, 'use').and.callThrough();

      attachInterceptors();

      expect(userResponseInterceptor).toHaveBeenCalledTimes(2);
      expect(useRequestInterceptor).toHaveBeenCalledTimes(1);
    });

    describe('request interceptors', () => {
      const getInterceptorHandlerAt = (index) => axios.interceptors.request.handlers[index];

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
      const getInterceptorHandlerAt = (index) => axios.interceptors.response.handlers[index];

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
            expect(queueService.getRequests().length).toBe(0);
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
              expect($window.sessionExpired).toHaveBeenCalledTimes(1);
              expect(queueService.getRequests().length).toBe(0);
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
              expect($window.sessionExpired).toHaveBeenCalledTimes(1);
              expect(queueService.getRequests().length).toBe(0);
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
              expect(queueService.getRequests().length).toBe(0);
              expect(error).toEqual(errorFromRequest.response);
              done();
            });
          }
        );

        describe('intercepts a request that is waiting for login and is rejected due to authentication', () => {
          it('adds the request to the UnauthenticatedRequestQueueService if it was waiting for login', (done) => {
            const authenticationInterceptor = getAuthenticationInterceptor();
            const errorFromRequest = {
              response: {
                status: 401,
                headers: () => '',
              },
            };

            const interceptorResolution = authenticationInterceptor.rejected(errorFromRequest);
            interceptorResolution.then(promiseShouldNotBeResolvedFailure, () => {
              expect(queueService.getRequests().length).toBe(1);
              done();
            });
          });

          describe('when there is a single request in the queue', () => {
            it('requests the opening of the login modal without SSO if the appropriate header is not present', (done) => {
              // Spy on the opening of the login modal but don't resolve or reject the promise yet
              const loginModalOpenSpy = spyOn(loginModalService, 'open').and.callThrough();
              const authenticationInterceptor = getAuthenticationInterceptor();
              const errorFromRequest = {
                response: {
                  status: 401,
                  headers: () => '',
                },
              };

              const interceptorResolution = authenticationInterceptor.rejected(errorFromRequest);
              interceptorResolution.then(promiseShouldNotBeResolvedFailure, () => {
                expect(queueService.getRequests().length).toBe(1);
                expect(loginModalOpenSpy).toHaveBeenCalledOnceWith(false);
                done();
              });
            });

            it('requests the opening of the login modal with SSO if the appropriate header is present', (done) => {
              // Spy on the opening of the login modal but don't resolve or reject the promise yet
              const loginModalOpenSpy = spyOn(loginModalService, 'open').and.callThrough();
              const authenticationInterceptor = getAuthenticationInterceptor();
              const errorFromRequest = {
                response: {
                  status: 401,
                  headers: () => 'SAML',
                },
              };

              const interceptorResolution = authenticationInterceptor.rejected(errorFromRequest);
              interceptorResolution.then(promiseShouldNotBeResolvedFailure, () => {
                expect(queueService.getRequests().length).toBe(1);
                expect(loginModalOpenSpy).toHaveBeenCalledOnceWith(true);
                done();
              });
            });

            // This test will be addressed with CLM-21126
            xit('Resolves all promises in the queue and clears any requests after authentication is successful', (done) => {
              // avoid the request made by putting the original failed configuration in the queue
              spyOn(axios, 'default').and.returnValue(Promise.resolve());
              spyOn(axios, 'request').and.returnValue(Promise.resolve());

              const newRequestedPromise = new Promise((resolve) => {
                setTimeout(() => {
                  return resolve();
                }, 10);
              });
              spyOn(loginModalService, 'open').and.callFake(() => {
                queueService.addRequest(() => newRequestedPromise);
                return Promise.resolve();
              });

              const authenticationInterceptor = getAuthenticationInterceptor();
              const errorFromRequest = {
                response: {
                  config: { url: 'dummyUrl', method: 'get' },
                  status: 401,
                  headers: () => '',
                },
              };

              const interceptorResolution = authenticationInterceptor.rejected(errorFromRequest);
              interceptorResolution.then(promiseShouldNotBeResolvedFailure, () => {
                expect(queueService.getRequests().length).toBe(2);
                newRequestedPromise.then(() => {
                  expect(queueService.getRequests().length).toBe(0);
                  done();
                });
              });
            });

            it('clears any remaining requests if authentication is not successful or cancelled', (done) => {
              spyOn(loginModalService, 'open').and.rejectWith('canceled login modal');
              const authenticationInterceptor = getAuthenticationInterceptor();
              const errorFromRequest = {
                response: {
                  config: { url: 'dummyUrl' },
                  status: 401,
                  headers: () => '',
                },
              };

              const interceptorResolution = authenticationInterceptor.rejected(errorFromRequest);
              interceptorResolution.then(promiseShouldNotBeResolvedFailure, () => {
                expect(queueService.getRequests().length).toBe(0);

                done();
              });
            });
          });
        });
      });

      describe('server date interceptor', () => {
        const getSeverDateInterceptor = () => getInterceptorHandlerAt(1);

        it('calls the setServerDate passed function when the request returns the appropriate header', () => {
          const serverDateInterceptor = getSeverDateInterceptor();
          serverDateInterceptor.fulfilled({ headers: {} });
          expect(setServerDateSpy).not.toHaveBeenCalled();

          const expectedDate = new Date('Thu, 17 Mar 2022 17:36:03 GMT');
          serverDateInterceptor.fulfilled({ headers: { date: 'Thu, 17 Mar 2022 17:36:03 GMT' } });
          expect(setServerDateSpy).toHaveBeenCalledWith(expectedDate);
        });
      });
    });
  });
});
