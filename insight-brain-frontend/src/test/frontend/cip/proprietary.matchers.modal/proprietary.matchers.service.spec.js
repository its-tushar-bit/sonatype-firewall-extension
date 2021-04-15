/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
describe('proprietary.matchers.service.spec', function () {
  var $httpBackend, proprietaryMatchersService;

  beforeEach(angular.mock.module('proprietary.matchers'));

  beforeEach(inject(function ($injector) {
    window.CLM = {
      path: '../',
    };
    $httpBackend = $injector.get('$httpBackend');
    proprietaryMatchersService = $injector.get('proprietary.matchers.service');
  }));

  afterEach(function () {
    window.CLM = {};
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  describe('addComponentMatchers()', function () {
    it('creates proper payload', function () {
      var expectedUrl = '../rest/proprietary/application/testApp123/add';
      var expectedPayload = {
        paths: ['foo', 'bar'],
        regex: '(testRegex)',
      };
      $httpBackend.expectPOST(expectedUrl, expectedPayload).respond(200, '');
      proprietaryMatchersService.addComponentMatchers(
        'testApp123',
        ['foo', 'bar'],
        '(testRegex)'
      );
      expect($httpBackend.flush).not.toThrow();
    });

    it('returns response json', function () {
      var expectedUrl = '../rest/proprietary/application/testApp123/add';
      var updatedConfig = {
        id: 'c1f2ae301f6f4a6095db78d63f63ba5b',
        regexes: ['regex1', 'regex2'],
      };
      $httpBackend.expectPOST(expectedUrl).respond(200, updatedConfig);
      proprietaryMatchersService
        .addComponentMatchers('testApp123', ['foo', 'bar'], '(testRegex)')
        .then(function (config) {
          expect(config).toEqual(updatedConfig);
        });

      $httpBackend.flush();
    });

    it('rejects with error message in case of failure', function () {
      var expectedUrl = '../rest/proprietary/application/testApp123/add';
      $httpBackend.expectPOST(expectedUrl).respond(400, 'Invalid regex');
      var result = undefined;
      proprietaryMatchersService
        .addComponentMatchers('testApp123', ['foo', 'bar'], '(testRegex)')
        .catch(function (message) {
          result = message;
        });

      $httpBackend.flush();
      expect(result).toBe('Invalid regex');
    });
  });

  describe('getApplicationInfo()', function () {
    it('returns response json', function () {
      var expectedUrl = '../rest/application/testApp123';
      var applicationInfo = {
        name: 'Test Application',
      };
      $httpBackend.expectGET(expectedUrl).respond(200, applicationInfo);
      proprietaryMatchersService
        .getApplicationInfo('testApp123')
        .then(function (info) {
          expect(info).toEqual(applicationInfo);
        });
      $httpBackend.flush();
    });

    it('rejects with error message in case of failure', function () {
      var expectedUrl = '../rest/application/testApp123';
      $httpBackend.expectGET(expectedUrl).respond(400, 'not found');
      var result = undefined;
      proprietaryMatchersService
        .getApplicationInfo('testApp123')
        .catch(function (message) {
          result = message;
        });
      $httpBackend.flush();
      expect(result).toBe('not found');
    });
  });
});
