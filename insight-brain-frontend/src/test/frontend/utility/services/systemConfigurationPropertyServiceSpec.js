/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import utilityServicesModule from '../../../../main/frontend/utility/services/utility.services.module';

describe('systemConfigurationPropertyServiceSpec.js', function() {
  beforeEach(angular.mock.module(utilityServicesModule.name));

  var systemConfigurationPropertyService,
      $httpBackend,
      $rootScope,
      CLMLocations,
      successSpy,
      failSpy;

  beforeEach(inject(function(_systemConfigurationPropertyService_, _$httpBackend_, _$rootScope_, _CLMLocations_) {
    systemConfigurationPropertyService = _systemConfigurationPropertyService_;
    $httpBackend = _$httpBackend_;
    CLMLocations = _CLMLocations_;
    $rootScope = _$rootScope_;
    successSpy = jasmine.createSpy('successSpy');
    failSpy = jasmine.createSpy('failSpy');
  }));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  describe('getting success metrics flag', function() {
    it('returns enabled when the request succeeds', function() {
      systemConfigurationPropertyService.isSuccessMetricsEnabled().then(successSpy).catch(failSpy);
      $httpBackend.expectGET(CLMLocations.getSuccessMetricsConfigUrl()).respond({enabled: true});
      $httpBackend.flush();

      expect(successSpy).toHaveBeenCalledWith(true);
      expect(failSpy).not.toHaveBeenCalled();
    });

    it('throws a failed request', function() {
      systemConfigurationPropertyService.isSuccessMetricsEnabled().then(successSpy).catch(failSpy);
      $httpBackend.expectGET(CLMLocations.getSuccessMetricsConfigUrl()).respond(404, 'not found');
      $httpBackend.flush();

      expect(successSpy).not.toHaveBeenCalled();
      expect(failSpy).toHaveBeenCalled();
      expect(failSpy.calls.mostRecent().args[0].status).toEqual(404);
      expect(failSpy.calls.mostRecent().args[0].data).toEqual('not found');
    });
  });

  describe('checking success metrics flag has not been disabled by sys admin', function() {
    it('returns a resolved promise when it is enabled', function() {
      systemConfigurationPropertyService.checkSuccessMetricsEnabled().then(successSpy).catch(failSpy);
      $httpBackend.expectGET(CLMLocations.getSuccessMetricsConfigUrl()).respond({enabled: true});
      $httpBackend.flush();

      expect(successSpy).toHaveBeenCalledWith(true);
      expect(failSpy).not.toHaveBeenCalled();
    });

    it('returns a rejected promise with error message when it is disabled', function() {
      systemConfigurationPropertyService.checkSuccessMetricsEnabled().then(successSpy).catch(failSpy);
      $httpBackend.expectGET(CLMLocations.getSuccessMetricsConfigUrl()).respond({enabled: false});
      $httpBackend.flush();

      expect(successSpy).not.toHaveBeenCalled();
      expect(failSpy).toHaveBeenCalled();
      expect(failSpy.calls.mostRecent().args[0]).toEqual(
          'Success metrics have been disabled by your system administrator.');
    });
  });

  describe('saving the success metrics flag', function() {
    it('returns property when the request succeeds and broadcasts event', function() {
      spyOn($rootScope, '$broadcast').and.callThrough();
      systemConfigurationPropertyService.saveSuccessMetricsEnabled(true).then(successSpy).catch(failSpy);
      var config = {enabled: true};
      $httpBackend.expectPUT(CLMLocations.getSuccessMetricsConfigUrl(), config).respond(config);
      $httpBackend.flush();

      expect(successSpy).toHaveBeenCalledWith(config);
      expect(failSpy).not.toHaveBeenCalled();
      expect($rootScope.$broadcast).toHaveBeenCalledWith('successMetricsConfigurationUpdated', true);
    });

    it('throws a failed request', function() {
      spyOn($rootScope, '$broadcast').and.callThrough();
      systemConfigurationPropertyService.saveSuccessMetricsEnabled(false).then(successSpy).catch(failSpy);
      var config = {enabled: false};
      $httpBackend.expectPUT(CLMLocations.getSuccessMetricsConfigUrl(), config).respond(401, 'unauthorized');
      $httpBackend.flush();

      expect(successSpy).not.toHaveBeenCalled();
      expect(failSpy).toHaveBeenCalled();
      expect(failSpy.calls.mostRecent().args[0].status).toEqual(401);
      expect(failSpy.calls.mostRecent().args[0].data).toEqual('unauthorized');
      expect($rootScope.$broadcast).not.toHaveBeenCalledWith('successMetricsConfigurationUpdated', false);
    });
  });

  describe('getting advanced search flag', function() {
    it('returns enabled when the request succeeds', function() {
      systemConfigurationPropertyService.isAdvancedSearchEnabled().then(successSpy).catch(failSpy);
      $httpBackend.expectGET(CLMLocations.getAdvancedSearchConfigUrl()).respond({isEnabled: true});
      $httpBackend.flush();

      expect(successSpy).toHaveBeenCalledWith(true);
      expect(failSpy).not.toHaveBeenCalled();
    });

    it('throws a failed request', function() {
      systemConfigurationPropertyService.isAdvancedSearchEnabled().then(successSpy).catch(failSpy);
      $httpBackend.expectGET(CLMLocations.getAdvancedSearchConfigUrl()).respond(404, 'not found');
      $httpBackend.flush();

      expect(successSpy).not.toHaveBeenCalled();
      expect(failSpy).toHaveBeenCalled();
      expect(failSpy.calls.mostRecent().args[0].status).toEqual(404);
      expect(failSpy.calls.mostRecent().args[0].data).toEqual('not found');
    });
  });
});
