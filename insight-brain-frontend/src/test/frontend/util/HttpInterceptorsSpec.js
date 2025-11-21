/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { unauthenticatedResponseHttpInterceptor } from 'MainRoot/utilAngular/HttpInterceptors';

/* Further test coverage for this interceptor will be added with CLM-20631 */
describe('HttpInterceptors.js', function () {
  beforeEach(
    angular.mock.module(unauthenticatedResponseHttpInterceptor.name, 'legacyConfiguration', function ($provide) {
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

  it('Validate that a GET/POST/PUT/DELETE request has a timestamp param', inject(function ($q, $http, $httpBackend) {
    $httpBackend.expectGET(SpecUtil.toRegExp('/rest/test')).respond(200);
    $httpBackend.expectPOST(SpecUtil.toRegExp('/rest/test')).respond(200);
    $httpBackend.expectPUT(SpecUtil.toRegExp('/rest/test')).respond(200);
    $httpBackend.expectDELETE(SpecUtil.toRegExp('/rest/test')).respond(200);

    $http.get('/rest/test');
    $http.post('/rest/test');
    $http.put('/rest/test');
    $http['delete']('/rest/test');

    expect($httpBackend.flush).not.toThrow();
  }));

  it('Validate that /rest/ and .json paths contains cachebuster, others ignored', inject(function (
    $http,
    $httpBackend
  ) {
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

    expect($httpBackend.flush).not.toThrow();
  }));
});
