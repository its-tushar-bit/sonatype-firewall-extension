import retentionModule from '../../../../main/frontend/owner.manager/retention/module';
import {inheritedRetentionPolicies, customRetentionPolicies} from './retentionMockData';
import clmContextLocationModule from '../../../../main/frontend/util/CLMContextLocation';

describe('retentionService', function() {
  beforeEach(angular.mock.module(retentionModule.name, clmContextLocationModule.name));

  let retentionService,
      CLMContextLocations,
      $httpBackend,
      successSpy,
      failSpy;

  beforeEach(inject(function(_retentionService_, _CLMContextLocations_, _$httpBackend_) {
    retentionService = _retentionService_;
    CLMContextLocations = _CLMContextLocations_;
    spyOn(CLMContextLocations, 'getEntityId').and.returnValue('organizationId');
    $httpBackend = _$httpBackend_;
    successSpy = jasmine.createSpy('successSpy');
    failSpy = jasmine.createSpy('failSpy');
  }));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  describe('getRootOrganizationRetentionPolicies', function() {
    it('returns the root organization data retention policies', function() {
      retentionService.getRootOrganizationRetentionPolicies().then(successSpy).catch(failSpy);

      $httpBackend.expectGET(CLMContextLocations.getRetentionPoliciesUrl('ROOT_ORGANIZATION_ID')).respond(
          customRetentionPolicies);
      $httpBackend.flush();

      expect(successSpy).toHaveBeenCalledWith(customRetentionPolicies);
      expect(failSpy).not.toHaveBeenCalled();
    });

    it('throws a failed request', function() {
      retentionService.getRootOrganizationRetentionPolicies().then(successSpy).catch(failSpy);

      $httpBackend.expectGET(CLMContextLocations.getRetentionPoliciesUrl('ROOT_ORGANIZATION_ID')).respond(404,
          'not found');
      $httpBackend.flush();

      expect(successSpy).not.toHaveBeenCalled();
      expect(failSpy).toHaveBeenCalledWith(jasmine.objectContaining({status: 404, data: 'not found'}));
    });
  });

  describe('getRetentionPolicies', function() {
    it('returns the current organization data retention policies', function() {
      retentionService.getRetentionPolicies().then(successSpy).catch(failSpy);

      $httpBackend.expectGET(CLMContextLocations.getRetentionPoliciesUrl('organizationId')).respond(
          inheritedRetentionPolicies);
      $httpBackend.flush();

      expect(successSpy).toHaveBeenCalledWith(inheritedRetentionPolicies);
      expect(failSpy).not.toHaveBeenCalled();
    });

    it('throws a failed request', function() {
      retentionService.getRetentionPolicies().then(successSpy).catch(failSpy);

      $httpBackend.expectGET(CLMContextLocations.getRetentionPoliciesUrl('organizationId')).respond(404,
          'not found');
      $httpBackend.flush();

      expect(successSpy).not.toHaveBeenCalled();
      expect(failSpy).toHaveBeenCalledWith(jasmine.objectContaining({status: 404, data: 'not found'}));
    });
  });

  describe('setRetentionPolicies', function() {
    it('sets the data retention policies for the current organization', function() {
      retentionService.setRetentionPolicies(customRetentionPolicies).then(
          successSpy).catch(failSpy);

      $httpBackend.expectPUT(CLMContextLocations.getRetentionPoliciesUrl('organizationId'),
          customRetentionPolicies).respond(204, 'no content');
      $httpBackend.flush();

      expect(successSpy).toHaveBeenCalledWith('no content');
      expect(failSpy).not.toHaveBeenCalled();
    });

    it('throws a failed request', function() {
      retentionService.setRetentionPolicies(customRetentionPolicies).then(successSpy).catch(failSpy);

      $httpBackend.expectPUT(CLMContextLocations.getRetentionPoliciesUrl('organizationId'),
          customRetentionPolicies).respond(404, 'not found');
      $httpBackend.flush();

      expect(successSpy).not.toHaveBeenCalled();
      expect(failSpy).toHaveBeenCalledWith(jasmine.objectContaining({status: 404, data: 'not found'}));
    });
  });
});
