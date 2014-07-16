/* global describe, beforeEach, module, it, inject, expect, afterEach */
describe('PermissionService.js', function() {
  var failed, permissionService;

  function doIsAuthorized(perms, shouldFail, required, condition) {
    permissionService.isAuthorized(perms, required, condition).then(function() {
      if (shouldFail) {
        failed = true;
      }
    }, function() {
      if (!shouldFail) {
        failed = true;
      }
    });
  }

  beforeEach(module('PermissionServiceModule'));
  beforeEach(inject(function(PermissionService) {
    failed = false;
    permissionService = PermissionService;
  }));
  afterEach(inject(function($httpBackend) {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
    expect(failed).toEqual(false);
  }));

  it('Test that isAuthorized works with valid permission',
      inject(function(CLMAppLocations, $httpBackend) {
        $httpBackend.expectPUT(CLMAppLocations.getPermissionTestUrl(), ['ADMIN']).respond(['ADMIN']);
        doIsAuthorized(['ADMIN'], false, false, true);
        $httpBackend.flush();
      }));

  it('Test that isAuthorized works with invalid permission',
      inject(function(CLMAppLocations, $httpBackend) {
        $httpBackend.expectPUT(CLMAppLocations.getPermissionTestUrl(), ['ADMIN']).respond([]);
        doIsAuthorized(['ADMIN'], true, false, true);
        $httpBackend.flush();
      }));

  it('Test that isAuthorized works with multiple valid permissions',
      inject(function(CLMAppLocations, $httpBackend) {
        $httpBackend.expectPUT(CLMAppLocations.getPermissionTestUrl(), ['ADMIN', 'ADMIN2']).respond([
          'ADMIN', 'ADMIN2'
        ]);
        doIsAuthorized(['ADMIN', 'ADMIN2'], false, false, true);
        $httpBackend.flush();
      }));

  it('Test that isAuthorized works with multiple invalid permissions',
      inject(function(CLMAppLocations, $httpBackend) {
        $httpBackend.expectPUT(CLMAppLocations.getPermissionTestUrl(), ['ADMIN', 'ADMIN2']).respond(['ADMIN2']);
        doIsAuthorized(['ADMIN', 'ADMIN2'], true, false, true);
        $httpBackend.flush();
      }));

  it('Test that isAuthorized works with valid permission and force true',
      inject(function(CLMAppLocations, $httpBackend, $rootScope) {
        $httpBackend.expectPUT(CLMAppLocations.getPermissionTestUrl(), ['ADMIN']).respond(['ADMIN']);
        doIsAuthorized(['ADMIN'], false, true, true);
        $httpBackend.flush();
        expect($rootScope.error).toBeFalsy();
      }));

  it('Test that isAuthorized works with invalid permission and force true',
      inject(function(CLMAppLocations, $httpBackend, $rootScope) {
        $httpBackend.expectPUT(CLMAppLocations.getPermissionTestUrl(), ['ADMIN']).respond([]);
        doIsAuthorized(['ADMIN'], true, true, true);
        $httpBackend.flush();
        expect($rootScope.error).toEqual('Insufficient Permissions');
      }));

  it('Test that isAuthorized works with multiple valid permissions and force true',
      inject(function(CLMAppLocations, $httpBackend, $rootScope) {
        $httpBackend.expectPUT(CLMAppLocations.getPermissionTestUrl(), ['ADMIN', 'ADMIN2']).respond([
          'ADMIN', 'ADMIN2'
        ]);
        doIsAuthorized(['ADMIN', 'ADMIN2'], false, true, true);
        $httpBackend.flush();
        expect($rootScope.error).toBeFalsy();
      }));

  it('Test that isAuthorized works with multiple invalid permissions and force true',
      inject(function(CLMAppLocations, $httpBackend, $rootScope) {
        $httpBackend.expectPUT(CLMAppLocations.getPermissionTestUrl(), ['ADMIN', 'ADMIN2']).respond(['ADMIN2']);
        doIsAuthorized(['ADMIN', 'ADMIN2'], true, true, true);
        $httpBackend.flush();
        expect($rootScope.error).toEqual('Insufficient Permissions');
      }));

  it('Test that isAuthorized sends request when condition is true',
      inject(function(CLMAppLocations, $httpBackend) {
        $httpBackend.expectPUT(CLMAppLocations.getPermissionTestUrl(), ['ADMIN', 'ADMIN2']).respond([
          'ADMIN', 'ADMIN2'
        ]);
        doIsAuthorized(['ADMIN', 'ADMIN2'], false, false, true);
        $httpBackend.flush();
      }));

  it('Test that isAuthorized does not send request when condition is false',
      inject(function() {
        doIsAuthorized(['ADMIN', 'ADMIN2'], false, false, false);
      }));
});
