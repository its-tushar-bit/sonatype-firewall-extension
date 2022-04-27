/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';

describe('move.application.service.js', function () {
  var $httpBackend, moveApplicationService, CLMLocations, moveAppMessages;

  beforeEach(angular.mock.module(ownerManagerModule.name));

  beforeEach(inject(function ($injector) {
    $httpBackend = $injector.get('$httpBackend');
    moveApplicationService = $injector.get('move.application.service');
    CLMLocations = $injector.get('CLMLocations');
    moveAppMessages = $injector.get('move.application.messages.constant');
  }));

  afterEach(function () {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  describe('getDestinationOrganizations()', function () {
    it('returns data on success', function () {
      $httpBackend.expectGET(CLMLocations.getDestinationOrganizationsUrl(1)).respond(['message1', 'message2']);

      moveApplicationService.getDestinationOrganizations(1).then(function (messages) {
        expect(messages).toEqual(['message1', 'message2']);
      });

      $httpBackend.flush();
    });

    it('handles error response by rejecting with provided error message', function () {
      $httpBackend.expectGET(CLMLocations.getDestinationOrganizationsUrl(1)).respond(400, 'not found');

      moveApplicationService
        .getDestinationOrganizations(1)
        .then(function () {
          throw 'promise should have been rejected';
        })
        .catch(function (error) {
          expect(error).toEqual('not found');
        });

      $httpBackend.flush();
    });

    it('rejects with error message when no data received', function () {
      $httpBackend.expectGET(CLMLocations.getDestinationOrganizationsUrl(1)).respond();

      moveApplicationService
        .getDestinationOrganizations(1)
        .then(function () {
          throw 'promise should have been rejected';
        })
        .catch(function (error) {
          expect(error).toEqual(moveAppMessages.ERROR_NO_DESTINATIONS);
        });

      $httpBackend.flush();
    });

    it('rejects with error message when empty list is received', function () {
      $httpBackend.expectGET(CLMLocations.getDestinationOrganizationsUrl(1)).respond([]);

      moveApplicationService
        .getDestinationOrganizations(1)
        .then(function () {
          throw 'promise should have been rejected';
        })
        .catch(function (error) {
          expect(error).toEqual(moveAppMessages.ERROR_NO_DESTINATIONS);
        });

      $httpBackend.flush();
    });
  });
});
