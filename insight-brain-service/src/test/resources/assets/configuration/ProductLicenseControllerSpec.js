describe('ProduceLicenseController', function() {
  'use strict';

  var scope, controller, mockLicenseSummary = {expiryTimestamp: 1601182800000};

  function getController($controller, scope) {
    return $controller('ProductLicenseController', {
      $scope: scope
    });
  }

  beforeEach(module('ProductLicense', function($provide) {
    $provide.factory('hudson', [
      '$http', function($http) {
        return $http;
      }
    ]);
  }));

  beforeEach(inject(function($rootScope) {
    scope = $rootScope.$new();
  }));

  afterEach(inject(function($httpBackend) {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
    scope.$destroy();
  }));

  describe('successful load', function() {
    beforeEach(inject(function($controller, CLMLocations, $httpBackend) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getLicenseSummaryUrl().split('?')[0]))
          .respond(mockLicenseSummary);
      controller = getController($controller, scope);
      $httpBackend.flush();
    }));

    it('should be set with data', function() {
      expect(scope.summaryUrl).toBeDefined();
      expect(scope.uploadUrl).toBeDefined();
      expect(scope.license).toBe(mockLicenseSummary);
      expect(scope.isLoaded()).toBeTruthy();
    });

    it('should be able to uninstall the license', inject(function($compile, $httpBackend, $timeout) {
      $httpBackend.expectDELETE(scope.uploadUrl).respond(200);
      spyOn(window.location, 'reload');

      scope.uninstallLicense();

      $httpBackend.flush();
      $timeout.flush();
      expect(window.location.reload).toHaveBeenCalled();
    }));

    it('should broadcast an error if uninstall fails on the server', inject(function($compile, $httpBackend) {
      $httpBackend.expectDELETE(scope.uploadUrl).respond(500);
      var broadCastError = spyOn(scope, '$broadcast');

      scope.uninstallLicense();

      $httpBackend.flush();
      expect(broadCastError).toHaveBeenCalledWith('showServerError', jasmine.any(Object));
    }));
  });

  describe('402 Payment Required failure', function() {
    beforeEach(inject(function($controller, $rootScope, CLMLocations, $httpBackend) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getLicenseSummaryUrl().split('?')[0]))
          .respond(402);
      controller = getController($controller, scope);
      $httpBackend.flush();
    }));

    it('should be not be set with data', function() {
      expect(scope.summaryUrl).toBeDefined();
      expect(scope.uploadUrl).toBeDefined();
      expect(scope.license).toBeFalsy();
      expect(scope.isLoaded()).toBeTruthy();
      expect(scope.error).toBeNull();
    });
  });

  describe('Server error other than 402', function() {
    beforeEach(inject(function($controller, $rootScope, CLMLocations, $httpBackend) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getLicenseSummaryUrl().split('?')[0]))
          .respond(500, 'ERROR');
      controller = getController($controller, scope);
      $httpBackend.flush();
    }));

    it('should record an error to show', function() {
      expect(scope.summaryUrl).toBeDefined();
      expect(scope.uploadUrl).toBeDefined();
      expect(scope.license).toBeFalsy();
      expect(scope.error).not.toBeNull();
      expect(scope.error.status).toBe(500);
      expect(scope.error.data).toBe('ERROR');
    });
  });

});
