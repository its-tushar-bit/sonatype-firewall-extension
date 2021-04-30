/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
describe('PolicyViolationsService', function () {
  var policyViolationsService, $httpBackend, $state;

  beforeEach(
    angular.mock.module('cip.policy.violations', function ($provide) {
      $provide.value('SelectedComponent', {
        get: angular.noop,
        set: angular.noop,
      });

      $provide.value('OwnerContext', {
        ownerId: 'some-application-id',
        ownerType: 'application',
      });

      $state = {
        params: {},
      };
      $provide.value('$state', $state);
    })
  );

  beforeEach(inject(function (_$httpBackend_, PolicyViolations) {
    $httpBackend = _$httpBackend_;
    policyViolationsService = PolicyViolations;
  }));

  afterEach(function () {
    $httpBackend.verifyNoOutstandingRequest();
    $httpBackend.verifyNoOutstandingExpectation();
  });

  describe('get', function () {
    it('uses "policythreats.json" relative to current url when called from within an iframe in report', function () {
      $httpBackend.expectGET(/^policythreats\.json\?timestamp=[0-9]+/).respond(200, { version: 3 });
      policyViolationsService.get();
      expect($httpBackend.flush).not.toThrow();
    });

    it('uses absolute-path url when called from IQ application', function () {
      $state.params.scanId = 'testScanId';
      $state.params.publicId = 'testPublicId';
      $httpBackend
        .expectGET(/^\/rest\/report\/testPublicId\/testScanId\/browseReport\/policythreats\.json\?timestamp=[0-9]+/)
        .respond(200, { version: 3 });
      policyViolationsService.get();
      expect($httpBackend.flush).not.toThrow();
    });
  });
});
