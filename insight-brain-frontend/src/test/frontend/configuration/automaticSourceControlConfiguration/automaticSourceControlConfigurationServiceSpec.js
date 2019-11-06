import configurationModule from '../../../../main/frontend/configuration/module';

describe('automaticSourceControlConfigurationServiceSpec.js', function() {
  beforeEach(angular.mock.module(configurationModule.name));

  var automaticSourceControlConfigurationService,
      $httpBackend,
      successSpy,
      failSpy,
      CLMLocations;

  beforeEach(inject(function(_automaticSourceControlConfigurationService_, _$httpBackend_, _CLMLocations_) {
    automaticSourceControlConfigurationService = _automaticSourceControlConfigurationService_;
    $httpBackend = _$httpBackend_;
    successSpy = jasmine.createSpy('successSpy');
    failSpy = jasmine.createSpy('failSpy');
    CLMLocations = _CLMLocations_;
  }));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  describe('getting the current configuration', function() {
    it('returns it when the request succeeds', function() {
      var configuration = {
        enabled: true
      };

      automaticSourceControlConfigurationService.getConfiguration().then(successSpy).catch(failSpy);
      $httpBackend.expectGET(CLMLocations.getAutomaticSourceControlConfigurationUrl()).respond(configuration);
      $httpBackend.flush();

      expect(successSpy).toHaveBeenCalledWith(configuration);
      expect(failSpy).not.toHaveBeenCalled();
    });

    it('handles a failed request by rejecting the promise', function() {
      automaticSourceControlConfigurationService.getConfiguration().then(successSpy).catch(failSpy);
      $httpBackend.expectGET(CLMLocations.getAutomaticSourceControlConfigurationUrl()).respond(404, 'not found');
      $httpBackend.flush();

      expect(successSpy).not.toHaveBeenCalled();
      expect(failSpy).toHaveBeenCalled();
      expect(failSpy.calls.mostRecent().args[0].status).toEqual(404);
      expect(failSpy.calls.mostRecent().args[0].data).toEqual('not found');
    });
  });

  describe('saving the system notice and putting it in the request', function() {
    it('returns the saved content when the request succeeds', function() {
      var configuration = {
        enabled: true
      };

      automaticSourceControlConfigurationService.saveConfiguration(configuration).then(successSpy).catch(failSpy);
      $httpBackend.expectPUT(CLMLocations.getAutomaticSourceControlConfigurationUrl(), configuration)
          .respond(configuration);
      $httpBackend.flush();

      expect(successSpy).toHaveBeenCalledWith(configuration);
      expect(failSpy).not.toHaveBeenCalled();
    });

    it('handles a failed request by rejecting the promise', function() {
      var configuration = {
        enabled: true,
        parentOrganizationId: 'organizationId'
      };

      automaticSourceControlConfigurationService.saveConfiguration(configuration).then(successSpy).catch(failSpy);
      $httpBackend.expectPUT(CLMLocations.getAutomaticSourceControlConfigurationUrl(), configuration)
          .respond(400, 'bad request');
      $httpBackend.flush();

      expect(successSpy).not.toHaveBeenCalled();
      expect(failSpy).toHaveBeenCalled();
      expect(failSpy.calls.mostRecent().args[0].status).toEqual(400);
      expect(failSpy.calls.mostRecent().args[0].data).toEqual('bad request');
    });
  });
});
