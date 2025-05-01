/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import permissionServiceModule from 'MainRoot/utilAngular/PermissionService';
import { getPermissionContextTestUrl } from 'MainRoot/utilAngular/CLMContextLocation';
import { getProductFeaturesUrl } from 'MainRoot/util/CLMLocation';

describe('PermissionService.js', function () {
  var successSpy, errorSpy;

  beforeEach(angular.mock.module(permissionServiceModule.name));

  beforeEach(function () {
    successSpy = jasmine.createSpy('successSpy');
    errorSpy = jasmine.createSpy('errorSpy');
  });

  afterEach(inject(function ($httpBackend) {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

  describe('isAuthorized', function () {
    it('Single Perm, Allowed', inject(function (PermissionService, CLMContextLocations, $httpBackend) {
      $httpBackend.expectPUT(CLMContextLocations.getPermissionTestUrl(true), ['ADMIN']).respond(['ADMIN']);
      PermissionService.isAuthorized(['ADMIN'], true).then(successSpy, errorSpy);
      $httpBackend.flush();
      expect(successSpy).toHaveBeenCalledWith(true);
      expect(errorSpy).not.toHaveBeenCalled();
    }));

    it('Single Perm, Disallowed', inject(function (PermissionService, CLMContextLocations, $httpBackend) {
      $httpBackend.expectPUT(CLMContextLocations.getPermissionTestUrl(true), ['ADMIN']).respond([]);
      PermissionService.isAuthorized(['ADMIN'], true).then(successSpy, errorSpy);
      $httpBackend.flush();
      expect(successSpy).toHaveBeenCalledWith(false);
      expect(errorSpy).not.toHaveBeenCalled();
    }));

    it('Multiple Perms, Allowed', inject(function (PermissionService, CLMContextLocations, $httpBackend) {
      $httpBackend
        .expectPUT(CLMContextLocations.getPermissionTestUrl(true), ['ADMIN', 'ADMIN2'])
        .respond(['ADMIN', 'ADMIN2']);
      PermissionService.isAuthorized(['ADMIN', 'ADMIN2'], true).then(successSpy, errorSpy);
      $httpBackend.flush();
      expect(successSpy).toHaveBeenCalledWith(true);
      expect(errorSpy).not.toHaveBeenCalled();
    }));

    it('Multiple Perms, Disallowed', inject(function (PermissionService, CLMContextLocations, $httpBackend) {
      $httpBackend.expectPUT(CLMContextLocations.getPermissionTestUrl(true), ['ADMIN', 'ADMIN2']).respond(['ADMIN2']);
      PermissionService.isAuthorized(['ADMIN', 'ADMIN2'], true).then(successSpy, errorSpy);
      $httpBackend.flush();
      expect(successSpy).toHaveBeenCalledWith(false);
      expect(errorSpy).not.toHaveBeenCalled();
    }));

    it('Multiple Perms in different order, Allowed', inject(function (
      PermissionService,
      CLMContextLocations,
      $httpBackend
    ) {
      $httpBackend
        .expectPUT(CLMContextLocations.getPermissionTestUrl(true), ['ADMIN', 'ADMIN2'])
        .respond(['ADMIN2', 'ADMIN']);
      PermissionService.isAuthorized(['ADMIN', 'ADMIN2'], true).then(successSpy, errorSpy);
      $httpBackend.flush();
      expect(successSpy).toHaveBeenCalledWith(true);
      expect(errorSpy).not.toHaveBeenCalled();
    }));

    it('Server Error', inject(function (PermissionService, CLMContextLocations, $httpBackend) {
      $httpBackend.expectPUT(CLMContextLocations.getPermissionTestUrl(true), ['ADMIN', 'ADMIN2']).respond(500, 'foo');
      PermissionService.isAuthorized(['ADMIN', 'ADMIN2'], true).then(successSpy, errorSpy);
      $httpBackend.flush();
      expect(successSpy).not.toHaveBeenCalled();
      var response = errorSpy.calls.first().args[0][0];
      expect(response.data).toEqual('foo');
      expect(response.status).toEqual(500);
      expect(response.statusText).toEqual('');
    }));
  });

  describe('isContextAuthorized', function () {
    it('Single Perm, Allowed', inject(function (PermissionService, CLMContextLocations, $httpBackend) {
      $httpBackend.expectPUT(getPermissionContextTestUrl('repository_container'), ['ADMIN']).respond(['ADMIN']);
      PermissionService.isContextAuthorized(['ADMIN'], 'repository_container').then(successSpy, errorSpy);
      $httpBackend.flush();
      expect(successSpy).toHaveBeenCalledWith(true);
      expect(errorSpy).not.toHaveBeenCalled();
    }));

    it('Single Perm, Disallowed', inject(function (PermissionService, CLMContextLocations, $httpBackend) {
      $httpBackend.expectPUT(getPermissionContextTestUrl('repository_container'), ['ADMIN']).respond([]);
      PermissionService.isContextAuthorized(['ADMIN'], 'repository_container').then(successSpy, errorSpy);
      $httpBackend.flush();
      expect(successSpy).toHaveBeenCalledWith(false);
      expect(errorSpy).not.toHaveBeenCalled();
    }));

    it('Multiple Perms, Allowed', inject(function (PermissionService, CLMContextLocations, $httpBackend) {
      $httpBackend
        .expectPUT(getPermissionContextTestUrl('repository_container'), ['ADMIN', 'ADMIN2'])
        .respond(['ADMIN', 'ADMIN2']);
      PermissionService.isContextAuthorized(['ADMIN', 'ADMIN2'], 'repository_container').then(successSpy, errorSpy);
      $httpBackend.flush();
      expect(successSpy).toHaveBeenCalledWith(true);
      expect(errorSpy).not.toHaveBeenCalled();
    }));

    it('Multiple Perms, Disallowed', inject(function (PermissionService, CLMContextLocations, $httpBackend) {
      $httpBackend
        .expectPUT(getPermissionContextTestUrl('repository_container'), ['ADMIN', 'ADMIN2'])
        .respond(['ADMIN2']);
      PermissionService.isContextAuthorized(['ADMIN', 'ADMIN2'], 'repository_container').then(successSpy, errorSpy);
      $httpBackend.flush();
      expect(successSpy).toHaveBeenCalledWith(false);
      expect(errorSpy).not.toHaveBeenCalled();
    }));

    it('Multiple Perms in different order, Allowed', inject(function (
      PermissionService,
      CLMContextLocations,
      $httpBackend
    ) {
      $httpBackend
        .expectPUT(getPermissionContextTestUrl('repository_container'), ['ADMIN', 'ADMIN2'])
        .respond(['ADMIN2', 'ADMIN']);
      PermissionService.isContextAuthorized(['ADMIN', 'ADMIN2'], 'repository_container').then(successSpy, errorSpy);
      $httpBackend.flush();
      expect(successSpy).toHaveBeenCalledWith(true);
      expect(errorSpy).not.toHaveBeenCalled();
    }));

    it('Server Error', inject(function (PermissionService, CLMContextLocations, $httpBackend) {
      $httpBackend
        .expectPUT(getPermissionContextTestUrl('repository_container'), ['ADMIN', 'ADMIN2'])
        .respond(500, 'foo');
      PermissionService.isContextAuthorized(['ADMIN', 'ADMIN2'], 'repository_container').then(successSpy, errorSpy);
      $httpBackend.flush();
      expect(successSpy).not.toHaveBeenCalled();

      var response = errorSpy.calls.first().args[0][0];
      expect(response.data).toEqual('foo');
      expect(response.status).toEqual(500);
      expect(response.statusText).toEqual('');
    }));
  });

  describe('isFeatureEnabled', () => {
    it('Feature is enabled: should return true', inject((PermissionService, $httpBackend) => {
      $httpBackend.expectGET(getProductFeaturesUrl()).respond(['feature1', 'feature2', 'feature3']);
      PermissionService.isFeatureEnabled('feature1').then(successSpy, errorSpy);
      $httpBackend.flush();

      expect(successSpy).toHaveBeenCalledWith(true);
      expect(errorSpy).not.toHaveBeenCalled();
    }));

    it('Feature is not enabled: should return false', inject((PermissionService, $httpBackend) => {
      $httpBackend.expectGET(getProductFeaturesUrl()).respond(['feature1', 'feature2', 'feature3']);
      PermissionService.isFeatureEnabled('feature4').then(successSpy, errorSpy);
      $httpBackend.flush();

      expect(successSpy).toHaveBeenCalledWith(false);
      expect(errorSpy).not.toHaveBeenCalled();
    }));

    it('HTTP request error: should return false', inject((PermissionService, $httpBackend) => {
      $httpBackend.expectGET(getProductFeaturesUrl()).respond(500);
      PermissionService.isFeatureEnabled('feature1').then(successSpy, errorSpy);
      $httpBackend.flush();

      expect(successSpy).toHaveBeenCalledWith(false);
      expect(errorSpy).not.toHaveBeenCalled();
    }));
  });
});
