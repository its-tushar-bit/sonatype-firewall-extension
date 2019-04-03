import policyViolationGrandfatheringModule
  from '../../../../main/frontend/owner.manager/policyViolationGrandfathering/module';

describe('PolicyViolationGrandfatheringService', function() {

  var PolicyViolationGrandfatheringService,
      CLMContextLocations,
      $httpBackend,
      successSpy,
      failSpy;

  beforeEach(angular.mock.module(policyViolationGrandfatheringModule.name));

  beforeEach(inject(function(policyViolationGrandfatheringService, _CLMContextLocations_, _$httpBackend_) {
    PolicyViolationGrandfatheringService = policyViolationGrandfatheringService;
    CLMContextLocations = _CLMContextLocations_;
    $httpBackend = _$httpBackend_;
    successSpy = jasmine.createSpy('successSpy');
    failSpy = jasmine.createSpy('failSpy');
  }));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  describe('getting the current settings', function () {
    it('returns appropriate content for the root organization when enabled is absent', function() {
      spyOn(CLMContextLocations, 'isRootOrg').and.returnValue(true);

      PolicyViolationGrandfatheringService.getGrandfathering().then(successSpy).catch(failSpy);

      $httpBackend.expectGET(CLMContextLocations.getGrandfatheringUrl()).respond({
        enabled: null,
        inheritedFromOrganizationName: null,
        allowChange: true,
        allowOverride: true
      });
      $httpBackend.flush();

      expect(successSpy).toHaveBeenCalledWith({
        enabled: false,
        calculatedEnabled: false,
        inheritedFromOrganizationName: null,
        allowChange: true,
        allowOverride: true
      });
      expect(failSpy).not.toHaveBeenCalled();
    });

    it('returns appropriate content for the root organization when enabled is present', function() {
      spyOn(CLMContextLocations, 'isRootOrg').and.returnValue(true);

      PolicyViolationGrandfatheringService.getGrandfathering().then(successSpy).catch(failSpy);

      $httpBackend.expectGET(CLMContextLocations.getGrandfatheringUrl()).respond({
        enabled: true,
        inheritedFromOrganizationName: null,
        allowChange: true,
        allowOverride: true
      });
      $httpBackend.flush();

      expect(successSpy).toHaveBeenCalledWith({
        enabled: true,
        calculatedEnabled: true,
        inheritedFromOrganizationName: null,
        allowChange: true,
        allowOverride: true
      });
      expect(failSpy).not.toHaveBeenCalled();
    });

    it('returns appropriate content for other owners when config is inherited', function() {
      PolicyViolationGrandfatheringService.getGrandfathering().then(successSpy).catch(failSpy);

      $httpBackend.expectGET(CLMContextLocations.getGrandfatheringUrl()).respond({
        enabled: true,
        inheritedFromOrganizationName: 'Test Organization',
        allowChange: true,
        allowOverride: true
      });
      $httpBackend.flush();

      expect(successSpy).toHaveBeenCalledWith({
        enabled: null,
        calculatedEnabled: true,
        inheritedFromOrganizationName: 'Test Organization',
        allowChange: true,
        allowOverride: true
      });
      expect(failSpy).not.toHaveBeenCalled();
    });

    it('returns appropriate content for other owners when config is not inherited', function() {
      PolicyViolationGrandfatheringService.getGrandfathering().then(successSpy).catch(failSpy);

      $httpBackend.expectGET(CLMContextLocations.getGrandfatheringUrl()).respond({
        enabled: true,
        inheritedFromOrganizationName: null,
        allowChange: true,
        allowOverride: true
      });
      $httpBackend.flush();

      expect(successSpy).toHaveBeenCalledWith({
        enabled: true,
        calculatedEnabled: true,
        inheritedFromOrganizationName: null,
        allowChange: true,
        allowOverride: true
      });
      expect(failSpy).not.toHaveBeenCalled();
    });

    it('correctly handles a failure by rejecting the promise', function() {
      PolicyViolationGrandfatheringService.getGrandfathering().then(successSpy).catch(failSpy);

      $httpBackend.expectGET(CLMContextLocations.getGrandfatheringUrl()).respond(404, 'not found');
      $httpBackend.flush();

      expect(successSpy).not.toHaveBeenCalled();
      expect(failSpy.calls.mostRecent().args[0].status).toEqual(404);
      expect(failSpy.calls.mostRecent().args[0].data).toEqual('not found');
    });
  });

  describe('setting the current settings', function () {
    it('stores appropriate content for an organization or application', function() {
      const config = {
        enabled: true,
        allowOverride: true
      };

      PolicyViolationGrandfatheringService.setGrandfathering(config).then(successSpy).catch(failSpy);

      $httpBackend.expectPUT(CLMContextLocations.getGrandfatheringUrl(), config).respond(204, '');
      $httpBackend.flush();

      expect(successSpy).toHaveBeenCalled();
      expect(failSpy).not.toHaveBeenCalled();
    });

    it('stores appropriate content for an organization or application', function() {
      const config = {
        enabled: true,
        allowOverride: true
      };

      PolicyViolationGrandfatheringService.setGrandfathering(config).then(successSpy).catch(failSpy);

      $httpBackend.expectPUT(CLMContextLocations.getGrandfatheringUrl(), config).respond(404, 'not found');
      $httpBackend.flush();

      expect(successSpy).not.toHaveBeenCalled();
      expect(failSpy.calls.mostRecent().args[0].status).toEqual(404);
      expect(failSpy.calls.mostRecent().args[0].data).toEqual('not found');
    });
  });

  describe('building status messages for configuration settings', function () {
    it('builds an appropriate summary for inherited configuration', function() {
      const result = PolicyViolationGrandfatheringService.getStatusMessage({
        inheritedFromOrganizationName: 'Test Organization',
        calculatedEnabled: false,
        allowOverride: false
      });

      expect(result).toBe('Inherit from Test Organization (Grandfathering is disabled)');
    });

    it('builds an appropriate summary when grandfathering is enabled', function() {
      const result = PolicyViolationGrandfatheringService.getStatusMessage({
        inheritedFromOrganizationName: null,
        calculatedEnabled: true,
        allowOverride: false
      });

      expect(result).toBe('Grandfathering is enabled');
    });

    it('builds an appropriate summary when overrides are enabled', function() {
      const result = PolicyViolationGrandfatheringService.getStatusMessage({
        inheritedFromOrganizationName: null,
        calculatedEnabled: false,
        allowOverride: true
      });

      expect(result).toBe('Grandfathering is disabled');
    });
  });
});
