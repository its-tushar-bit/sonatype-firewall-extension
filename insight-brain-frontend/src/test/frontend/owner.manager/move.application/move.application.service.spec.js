/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';

describe('move.application.service.js', function () {
  var $q,
    $httpBackend,
    moveApplicationService,
    CLMLocations,
    moveAppMessages,
    applicationStore;

  beforeEach(angular.mock.module(ownerManagerModule.name));

  beforeEach(
    angular.mock.module(function ($provide) {
      applicationStore = jasmine.createSpyObj('applicationStore', ['refresh']);
      $provide.value('ApplicationStore', applicationStore);
    })
  );

  beforeEach(inject(function ($injector) {
    $q = $injector.get('$q');
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
      $httpBackend
        .expectGET(CLMLocations.getDestinationOrganizationsUrl(1))
        .respond(['message1', 'message2']);

      moveApplicationService
        .getDestinationOrganizations(1)
        .then(function (messages) {
          expect(messages).toEqual(['message1', 'message2']);
        });

      $httpBackend.flush();
    });

    it('handles error response by rejecting with provided error message', function () {
      $httpBackend
        .expectGET(CLMLocations.getDestinationOrganizationsUrl(1))
        .respond(400, 'not found');

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
      $httpBackend
        .expectGET(CLMLocations.getDestinationOrganizationsUrl(1))
        .respond();

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
      $httpBackend
        .expectGET(CLMLocations.getDestinationOrganizationsUrl(1))
        .respond([]);

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

  describe('moveApplication()', function () {
    it('refreshes application cache and returns data on success', function (done) {
      $httpBackend
        .expectPOST(CLMLocations.getMoveApplicationUrl(1, 2))
        .respond({ warnings: ['message1', 'message2'] });

      applicationStore.refresh.and.returnValue($q.resolve());

      moveApplicationService.moveApplication(1, 2).then(function (messages) {
        expect(messages).toEqual(['message1', 'message2']);
        expect(applicationStore.refresh).toHaveBeenCalled();
        done();
      });

      $httpBackend.flush();
    });

    it(
      'refreshes application cache and returns nothing on success' +
        'if provided array of messages is empty',
      function (done) {
        $httpBackend
          .expectPOST(CLMLocations.getMoveApplicationUrl(1, 2))
          .respond({});

        applicationStore.refresh.and.returnValue($q.resolve());

        moveApplicationService.moveApplication(1, 2).then(function (messages) {
          expect(messages).toBeNull();
          expect(applicationStore.refresh).toHaveBeenCalled();
          done();
        });

        $httpBackend.flush();
      }
    );

    it('does not resolve until application cache is refreshed', function () {
      $httpBackend
        .expectPOST(CLMLocations.getMoveApplicationUrl(1, 2))
        .respond({ warnings: ['message1', 'message2'] });

      var refreshPromise = $q.defer();

      applicationStore.refresh.and.returnValue(refreshPromise.promise);

      moveApplicationService.moveApplication(1, 2).then(function () {
        throw 'promise should not have been resolved';
      });

      expect($httpBackend.flush).not.toThrow();
    });

    it('handles 409 response with incompatibilities list', function () {
      $httpBackend
        .expectPOST(CLMLocations.getMoveApplicationUrl(1, 2))
        .respond(409, { errors: ['incompatibility1', 'incompatibility2'] });

      moveApplicationService
        .moveApplication(1, 2)
        .then(function () {
          throw 'promise should have been rejected';
        })
        .catch(function (error) {
          expect(error.message).toEqual(
            moveAppMessages.ERROR_INCOMPATIBLE_DESTINATION
          );
          expect(error.incompatibilities).toEqual([
            'incompatibility1',
            'incompatibility2',
          ]);
        });

      $httpBackend.flush();
    });

    it('handles 409 response with error message by rejecting with provided error message', function () {
      $httpBackend
        .expectPOST(CLMLocations.getMoveApplicationUrl(1, 2))
        .respond(409, 'some error has occurred');

      moveApplicationService
        .moveApplication(1, 2)
        .then(function () {
          throw 'promise should have been rejected';
        })
        .catch(function (error) {
          expect(error.message).toEqual('some error has occurred');
          expect(error.incompatibilities).toBeUndefined();
        });

      $httpBackend.flush();
    });

    it('handles 409 response with no data', function () {
      $httpBackend
        .expectPOST(CLMLocations.getMoveApplicationUrl(1, 2))
        .respond(409);

      moveApplicationService
        .moveApplication(1, 2)
        .then(function () {
          throw 'promise should have been rejected';
        })
        .catch(function (error) {
          expect(error.message).toEqual('Error 409');
          expect(error.incompatibilities).toBeUndefined();
        });

      $httpBackend.flush();
    });

    it('handles 4XX response by rejecting with provided error message', function () {
      $httpBackend
        .expectPOST(CLMLocations.getMoveApplicationUrl(1, 2))
        .respond(400, 'not found');

      moveApplicationService
        .moveApplication(1, 2)
        .then(function () {
          throw 'promise should have been rejected';
        })
        .catch(function (error) {
          expect(error.message).toEqual('not found');
          expect(error.incompatibilities).toBeUndefined();
        });

      $httpBackend.flush();
    });
  });
});
