/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import clmLocationModule from '../../../../main/frontend/util/CLMLocation';
import InnerSourceRepositoryModule from '../../../../main/frontend/owner.manager/innersource.repository/module';

describe('InnerSourceRepositoryService', function () {
  beforeEach(angular.mock.module(InnerSourceRepositoryModule.name, clmLocationModule.name));

  let InnerSourceRepositoryService, CLMLocations, $httpBackend, successSpy, failSpy;

  beforeEach(inject(function (_InnerSourceRepositoryService_, _CLMLocations_, _$httpBackend_) {
    InnerSourceRepositoryService = _InnerSourceRepositoryService_;
    CLMLocations = _CLMLocations_;
    $httpBackend = _$httpBackend_;
    successSpy = jasmine.createSpy('successSpy');
    failSpy = jasmine.createSpy('failSpy');
  }));

  afterEach(function () {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  describe('getRepositoryConnections', function () {
    it('returns the InnerSource repository connections request data for the given owner', function () {
      InnerSourceRepositoryService.getRepositoryConnections('ownerType', 'ownerId', 'inherit')
        .then(successSpy)
        .catch(failSpy);

      $httpBackend
        .expectGET(CLMLocations.getRepositoryConnections('ownerType', 'ownerId', 'inherit'))
        .respond('response');
      $httpBackend.flush();

      expect(successSpy).toHaveBeenCalledWith('response');
      expect(failSpy).not.toHaveBeenCalled();
    });

    it('returns an error status and data on a failed request', function () {
      InnerSourceRepositoryService.getRepositoryConnections('ownerType', 'ownerId', 'inherit')
        .then(successSpy)
        .catch(failSpy);

      $httpBackend
        .expectGET(CLMLocations.getRepositoryConnections('ownerType', 'ownerId', 'inherit'))
        .respond(404, 'not found');
      $httpBackend.flush();

      expect(successSpy).not.toHaveBeenCalled();
      expect(failSpy).toHaveBeenCalledWith(jasmine.objectContaining({ status: 404, data: 'not found' }));
    });
  });
});
