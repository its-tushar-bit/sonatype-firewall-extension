/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {unauthenticatedResponseHttpInterceptor} from '../../../main/frontend/util/HttpInterceptors';
import utilityServicesModule from '../../../main/frontend/utility/services/utility.services.module';

describe('HttpInterceptors.js', function() {
  var scope,
      modalSuccess,
      modalFailure,
      modalConfig;

  beforeEach(angular.mock.module(unauthenticatedResponseHttpInterceptor.name, utilityServicesModule.name,
      'legacyConfiguration',
      function($provide) {
        $provide.value('$modalInstance', {
          close: function() {}
        });
        $provide.value('Modal', {
          open: function(config) {
            modalConfig = config;
            scope.$close = function() {
            };
            inject(function($controller) {
              $controller(config.controller, {
                $scope: scope,
                showSamlSso: undefined,
                identityProviderName: undefined
              });
            });
            return {
              result: {
                then: function(success, failure) {
                  modalSuccess = success;
                  modalFailure = failure;
                }
              }
            };
          }
        });
        let sessionExpiredSpy = jasmine.createSpy();
        $provide.value('$window', {
          sessionExpired: sessionExpiredSpy,
          top: {
            sessionExpired: sessionExpiredSpy
          },
          location: {
            assign: jasmine.createSpy()
          }
        });
      }
  ));

  beforeEach(inject(function($rootScope) {
    scope = $rootScope.$new();
  }));

  it('Validate that a failed request is in the queue',
      inject(function($q, $http, $httpBackend, UnauthenticatedRequestQueueService) {
        expect(UnauthenticatedRequestQueueService.getRequests().length).toEqual(0);
        $httpBackend.expectPOST('test').respond(401);
        $http.post('test');
        $httpBackend.flush();
        expect(UnauthenticatedRequestQueueService.getRequests().length).toEqual(1);
      })
  );

  it('Validate that a GET/POST/PUT/DELETE request has a timestamp param', inject(function($q, $http, $httpBackend) {
    $httpBackend.expectGET(SpecUtil.toRegExp('/rest/test')).respond(200);
    $httpBackend.expectPOST(SpecUtil.toRegExp('/rest/test')).respond(200);
    $httpBackend.expectPUT(SpecUtil.toRegExp('/rest/test')).respond(200);
    $httpBackend.expectDELETE(SpecUtil.toRegExp('/rest/test')).respond(200);

    $http.get('/rest/test');
    $http.post('/rest/test');
    $http.put('/rest/test');
    $http['delete']('/rest/test');

    $httpBackend.flush();
  }));

  it('Validate that window.sessionExpired is called if a 401 happens when $rootScope.username is already defined',
      inject(function($rootScope, $http, $httpBackend, UnauthenticatedRequestQueueService, $window) {
        var rootScopeHasUsername = $rootScope.hasOwnProperty('username'),
            oldUsername = $rootScope.username;

        $rootScope.username = 'testUser';

        $httpBackend.expectPOST('test').respond(401);

        $http.post('test');

        $httpBackend.flush();
        $rootScope.$digest();

        expect($window.sessionExpired).toHaveBeenCalled();
        expect(UnauthenticatedRequestQueueService.getRequests().length).toEqual(0);

        // cleanup
        if (rootScopeHasUsername) {
          $rootScope.username = oldUsername;
        }
        else {
          delete $rootScope.username;
        }
        // else it is currently a spy wrapped around the original, and jasmine will automatically clean the spy
      })
  );

  it('Validate that the login modal is told to show SAML SSO when the WWW-Authenticate header is set to SAML',
      inject(function($rootScope, $http, $httpBackend) {

        $httpBackend.expectPOST('test').respond(401, undefined, {
          'WWW-Authenticate': 'SAML'
        });

        $http.post('test');
        $httpBackend.flush();
        $rootScope.$digest();

        expect(modalConfig.resolve.showSamlSso()).toBe(true);
      })
  );

  it('Validate that the login modal is told to not show SAML SSO without the WWW-Authenticate header',
      inject(function($rootScope, $http, $httpBackend) {
        $httpBackend.expectPOST('test').respond(401);

        $http.post('test');
        $httpBackend.flush();
        $rootScope.$digest();

        expect(modalConfig.resolve.showSamlSso()).toBe(false);
      })
  );

  it('Validate that the login modal is told the identity provider name when the X-SAML-IdP is set to it',
      inject(function($rootScope, $http, $httpBackend) {

        $httpBackend.expectPOST('test').respond(401, undefined, {
          'X-SAML-IdP': 'My Awesome IdP'
        });

        $http.post('test');
        $httpBackend.flush();
        $rootScope.$digest();

        expect(modalConfig.resolve.identityProviderName()).toBe('My Awesome IdP');
      })
  );

  it('Validate that /rest/ and .json paths contains cachebuster, others ignored', inject(function($http, $httpBackend) {
    $httpBackend.expectGET(SpecUtil.toRegExp('/rest/test')).respond(200);
    $httpBackend.expectPOST(SpecUtil.toRegExp('/test/rest/test')).respond(200);
    $httpBackend.expectGET(SpecUtil.toRegExp('test.json')).respond(200);
    $httpBackend.expectGET('/unrest/test').respond(200);
    $httpBackend.expectPOST('/test/unrest/test').respond(200);
    $httpBackend.expectGET('test.notjson').respond(200);
    $httpBackend.expectGET(SpecUtil.toRegExp('/api/test')).respond(200);
    $httpBackend.expectPOST(SpecUtil.toRegExp('/test/api/test')).respond(200);

    $http.get('/rest/test');
    $http.post('/test/rest/test');
    $http.get('test.json');
    $http.get('/unrest/test');
    $http.post('/test/unrest/test');
    $http.get('test.notjson');
    $http.get('/api/test');
    $http.post('/test/api/test');

    $httpBackend.flush();
  }));

  it('Validate that failed requests are retried and cleared out of the queue after the modal promise fires success',
      inject(function($q, $http, $httpBackend, UnauthenticatedRequestQueueService) {
        $httpBackend.expectPOST('test').respond(401);

        var success = false;
        $http.post('test').then(function() {
          success = true;
        });

        $httpBackend.flush();

        expect(UnauthenticatedRequestQueueService.getRequests().length).toEqual(1);

        $httpBackend.expectPOST('test').respond(200);

        // trigger the retry
        modalSuccess();

        $httpBackend.flush();

        expect(UnauthenticatedRequestQueueService.getRequests().length).toEqual(0);
        expect(success).toEqual(true);
      }));

  it('does not retry requests that have a waitForLogin property set to false nor add them to the queue',
      inject(function($q, $http, $httpBackend, UnauthenticatedRequestQueueService) {
        $httpBackend.expectPOST('test').respond(401);

        const resolvedSpy = jasmine.createSpy(),
            rejectedSpy = jasmine.createSpy();
        $http.post('test', {}, { waitForLogin: false }).then(resolvedSpy, rejectedSpy);

        $httpBackend.flush();

        expect(UnauthenticatedRequestQueueService.getRequests().length).toEqual(0);
        expect(rejectedSpy).toHaveBeenCalledWith(jasmine.objectContaining({ status: 401 }));
        expect(resolvedSpy).not.toHaveBeenCalled();
      })
  );

  it('clears the queue of retried requests if authentication is cancelled, and neither resolves nor rejects ' +
      'the promise', inject(function($q, $http, $httpBackend, UnauthenticatedRequestQueueService) {
    $httpBackend.expectPOST('test').respond(401);

    const resolvedSpy = jasmine.createSpy(),
        rejectedSpy = jasmine.createSpy();
    $http.post('test').then(resolvedSpy, rejectedSpy);

    $httpBackend.flush();

    expect(UnauthenticatedRequestQueueService.getRequests().length).toEqual(1);

    $httpBackend.expectPOST('test').respond(200);

    // emulate dismissal of login modal
    modalFailure();

    expect(UnauthenticatedRequestQueueService.getRequests().length).toEqual(0);
    expect(rejectedSpy).not.toHaveBeenCalled();
    expect(resolvedSpy).not.toHaveBeenCalled();
  }));
});
