/* global describe, beforeEach, module, it, inject, expect, afterEach */
describe('PermissionService.js', function() {
  var successSpy, errorSpy;

  beforeEach(module('PermissionServiceModule'));

  beforeEach(function() {
    successSpy = jasmine.createSpy('successSpy');
    errorSpy = jasmine.createSpy('errorSpy');
  });

  afterEach(inject(function($httpBackend) {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

  describe('isAuthorized', function () {
    it('Single Perm, Allowed', inject(function(PermissionService, CLMAppLocations, $httpBackend) {
      $httpBackend.expectPUT(CLMAppLocations.getPermissionTestUrl(true), ['ADMIN']).respond(['ADMIN']);
      PermissionService.isAuthorized(['ADMIN'], true).then(successSpy, errorSpy);
      $httpBackend.flush();
      expect(successSpy).toHaveBeenCalledWith(true);
      expect(errorSpy).not.toHaveBeenCalled();
    }));

    it('Single Perm, Disallowed', inject(function(PermissionService, CLMAppLocations, $httpBackend) {
      $httpBackend.expectPUT(CLMAppLocations.getPermissionTestUrl(true), ['ADMIN']).respond([]);
      PermissionService.isAuthorized(['ADMIN'], true).then(successSpy, errorSpy);
      $httpBackend.flush();
      expect(successSpy).toHaveBeenCalledWith(false);
      expect(errorSpy).not.toHaveBeenCalled();
    }));

    it('Multiple Perms, Allowed', inject(function(PermissionService, CLMAppLocations, $httpBackend) {
      $httpBackend.expectPUT(CLMAppLocations.getPermissionTestUrl(true), ['ADMIN', 'ADMIN2']).respond(
              ['ADMIN', 'ADMIN2']);
      PermissionService.isAuthorized(['ADMIN', 'ADMIN2'], true).then(successSpy, errorSpy);
      $httpBackend.flush();
      expect(successSpy).toHaveBeenCalledWith(true);
      expect(errorSpy).not.toHaveBeenCalled();
    }));

    it('Multiple Perms, Disallowed', inject(function(PermissionService, CLMAppLocations, $httpBackend) {
      $httpBackend.expectPUT(CLMAppLocations.getPermissionTestUrl(true), ['ADMIN', 'ADMIN2']).respond(['ADMIN2']);
      PermissionService.isAuthorized(['ADMIN', 'ADMIN2'], true).then(successSpy, errorSpy);
      $httpBackend.flush();
      expect(successSpy).toHaveBeenCalledWith(false);
      expect(errorSpy).not.toHaveBeenCalled();
    }));

    it('Server Error', inject(function(PermissionService, CLMAppLocations, $httpBackend) {
      $httpBackend.expectPUT(CLMAppLocations.getPermissionTestUrl(true), ['ADMIN', 'ADMIN2']).respond(500, 'foo');
      PermissionService.isAuthorized(['ADMIN', 'ADMIN2'], true).then(successSpy, errorSpy);
      $httpBackend.flush();
      expect(successSpy).not.toHaveBeenCalled();
      expect(errorSpy).toHaveBeenCalledWith([{ data : 'foo', status : 500, statusText : '', config : jasmine.any(Object), headers : jasmine.any(Function) }]);
    }));
  });

});
