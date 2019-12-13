/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import clmLocationModule from '../../../../main/frontend/util/CLMLocation';
import sourceControlModule from '../../../../main/frontend/owner.manager/source.control/module';

describe('SourceControlService', function() {

  beforeEach(angular.mock.module(sourceControlModule.name, clmLocationModule.name));

  let SourceControlService,
      CLMLocations,
      $httpBackend,
      successSpy,
      failSpy;

  beforeEach(inject(function(_SourceControlService_, _CLMLocations_, _$httpBackend_) {
    SourceControlService = _SourceControlService_;
    CLMLocations = _CLMLocations_;
    $httpBackend = _$httpBackend_;
    successSpy = jasmine.createSpy('successSpy');
    failSpy = jasmine.createSpy('failSpy');
  }));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  describe('getCompositeSourceControlRecord_ForOrg', function() {
    const resultOrgRecord = {
      token: {
        value: null,
        parentValue: 'TOKEN',
        parentName: 'PARENT'
      },
      provider: 'github',
      repositoryUrl: null,
      id: null,
      ownerId: 'ORGANIZATION_ID'
    };

    it('returns the source control record with inherited fields for an organization', function() {
      SourceControlService.getCompositeSourceControlRecord('organization', 'ORGANIZATION_ID').then(
          successSpy).catch(failSpy);

      $httpBackend.expectGET(
          CLMLocations.getCompositeSourceControlUrl('organization', 'ORGANIZATION_ID')).respond(
          resultOrgRecord);
      $httpBackend.flush();

      expect(successSpy).toHaveBeenCalledWith(jasmine.objectContaining(resultOrgRecord));
      expect(failSpy).not.toHaveBeenCalled();
    });

    it('returns 404 on a failed request', function() {
      SourceControlService.getCompositeSourceControlRecord('organization', 'non-existent').then(successSpy).catch(
          failSpy);

      $httpBackend.expectGET(
          CLMLocations.getCompositeSourceControlUrl('organization', 'non-existent')).respond(404, 'not found');
      $httpBackend.flush();

      expect(successSpy).not.toHaveBeenCalled();
      expect(failSpy).toHaveBeenCalledWith(jasmine.objectContaining({status: 404, data: 'not found'}));
    });
  });

  describe('addSourceControlRecord_ForOrg', function() {
    const orgRecord = {
      'id': '1234',
      'ownerId': 'ORGANIZATION_ID',
      'repositoryUrl': null,
      'token': 'secret_token',
      'provider': 'github'
    };

    const inputOrgRecord = {
      'token': 'secret_token',
      'provider': 'github'
    };

    it('creates the source control record for an organization', function() {
      SourceControlService.addSourceControlRecord(
          'organization', 'ROOT_ORGANIZATION_ID', orgRecord).then(successSpy).catch(failSpy);

      $httpBackend.expectPOST(
          CLMLocations.getSourceControlUrl('organization', 'ROOT_ORGANIZATION_ID'), inputOrgRecord).respond(orgRecord);
      $httpBackend.flush();

      expect(successSpy).toHaveBeenCalledWith(jasmine.objectContaining(orgRecord));
      expect(failSpy).not.toHaveBeenCalled();
    });

    it('returns 400 on a failed request', function() {
      SourceControlService.addSourceControlRecord(
          'organization', 'ROOT_ORGANIZATION_ID', orgRecord).then(successSpy).catch(failSpy);

      $httpBackend.expectPOST(CLMLocations.getSourceControlUrl('organization', 'ROOT_ORGANIZATION_ID'), inputOrgRecord)
          .respond(400, 'SourceControl already exists');
      $httpBackend.flush();

      expect(successSpy).not.toHaveBeenCalled();
      expect(failSpy).toHaveBeenCalledWith(
          jasmine.objectContaining({status: 400, data: 'SourceControl already exists'}));
    });

    it('returns 404 on a failed request', function() {
      SourceControlService.addSourceControlRecord(
          'organization', 'ROOT_ORGANIZATION_ID', orgRecord).then(successSpy).catch(failSpy);

      $httpBackend.expectPOST(CLMLocations.getSourceControlUrl('organization', 'ROOT_ORGANIZATION_ID'), inputOrgRecord)
          .respond(404, 'not found');
      $httpBackend.flush();

      expect(successSpy).not.toHaveBeenCalled();
      expect(failSpy).toHaveBeenCalledWith(jasmine.objectContaining({status: 404, data: 'not found'}));
    });
  });

  describe('updateSourceControlRecord_ForOrg', function() {
    const orgRecord = {
      'id': '1234',
      'ownerId': 'ORGANIZATION_ID',
      'repositoryUrl': null,
      'token': 'secret_token',
      'provider': 'gitlab'
    };

    const inputOrgRecord = {
      'token': 'secret_token',
      'provider': 'gitlab'
    };

    it('updates the source control record for an organization', function() {
      SourceControlService.updateSourceControlRecord(
          'organization', 'ROOT_ORGANIZATION_ID', orgRecord).then(successSpy).catch(failSpy);

      $httpBackend.expectPUT(
          CLMLocations.getSourceControlUrl('organization', 'ROOT_ORGANIZATION_ID'), inputOrgRecord).respond(orgRecord);
      $httpBackend.flush();

      expect(successSpy).toHaveBeenCalledWith(jasmine.objectContaining(orgRecord));
      expect(failSpy).not.toHaveBeenCalled();
    });

    it('returns 400 on a failed request', function() {
      SourceControlService.updateSourceControlRecord(
          'organization', 'ROOT_ORGANIZATION_ID', orgRecord).then(successSpy).catch(failSpy);

      $httpBackend.expectPUT(CLMLocations.getSourceControlUrl('organization', 'ROOT_ORGANIZATION_ID'), inputOrgRecord)
          .respond(400, 'SourceControl provider is invalid');
      $httpBackend.flush();

      expect(successSpy).not.toHaveBeenCalled();
      expect(failSpy).toHaveBeenCalledWith(
          jasmine.objectContaining({status: 400, data: 'SourceControl provider is invalid'}));
    });

    it('returns 404 on a failed request', function() {
      SourceControlService.updateSourceControlRecord(
          'organization', 'ROOT_ORGANIZATION_ID', orgRecord).then(successSpy).catch(failSpy);

      $httpBackend.expectPUT(CLMLocations.getSourceControlUrl('organization', 'ROOT_ORGANIZATION_ID'), inputOrgRecord)
          .respond(404, 'not found');
      $httpBackend.flush();

      expect(successSpy).not.toHaveBeenCalled();
      expect(failSpy).toHaveBeenCalledWith(jasmine.objectContaining({status: 404, data: 'not found'}));
    });
  });

  describe('deleteSourceControlRecord_ForOrg', function() {
    it('deletes the source control record for an organization', function() {
      SourceControlService.deleteSourceControlRecord('organization', 'ORGANIZATION_ID').then(successSpy).catch(
          failSpy);

      $httpBackend.expectDELETE(
          CLMLocations.getSourceControlUrl('organization', 'ORGANIZATION_ID')).respond('no content');
      $httpBackend.flush();

      expect(successSpy).toHaveBeenCalledWith('no content');
      expect(failSpy).not.toHaveBeenCalled();
    });

    it('returns 404 on a failed request', function() {
      SourceControlService.deleteSourceControlRecord('organization', 'non-existent').then(successSpy).catch(failSpy);

      $httpBackend.expectDELETE(CLMLocations.getSourceControlUrl('organization', 'non-existent'))
          .respond(404, 'not found');
      $httpBackend.flush();

      expect(successSpy).not.toHaveBeenCalled();
      expect(failSpy).toHaveBeenCalledWith(jasmine.objectContaining({status: 404, data: 'not found'}));
    });
  });

  describe('getCompositeSourceControlRecord_ForApp', function() {
    const resultAppRecord = {
      token: {
        value: null,
        parentValue: 'TOKEN',
        parentName: 'PARENT'
      },
      provider: 'github',
      repositoryUrl: 'URL',
      id: null,
      ownerId: 'ORGANIZATION_ID'
    };

    it('returns the source control record with inherited fields for an application', function() {
      SourceControlService.getCompositeSourceControlRecord('application', 'APPLICATION_ID').then(
          successSpy).catch(failSpy);

      $httpBackend.expectGET(
          CLMLocations.getCompositeSourceControlUrl('application', 'APPLICATION_ID')).respond(
          resultAppRecord);
      $httpBackend.flush();

      expect(successSpy).toHaveBeenCalledWith(jasmine.objectContaining(resultAppRecord));
      expect(failSpy).not.toHaveBeenCalled();
    });

    it('returns 404 on a failed request', function() {
      SourceControlService.getCompositeSourceControlRecord('APPLICATION_ID', 'non-existent').then(
          successSpy).catch(
          failSpy);

      $httpBackend.expectGET(
          CLMLocations.getCompositeSourceControlUrl('APPLICATION_ID', 'non-existent')).respond(404,
          'not found');
      $httpBackend.flush();

      expect(successSpy).not.toHaveBeenCalled();
      expect(failSpy).toHaveBeenCalledWith(jasmine.objectContaining({status: 404, data: 'not found'}));
    });
  });

  describe('addSourceControlRecord_ForApp', function() {
    const appRecord = {
      'id': '1234',
      'ownerId': 'APPLICATION_ID',
      'repositoryUrl': 'repoUrl',
      'token': 'secret_token',
      'provider': 'null'
    };

    const inputAppRecord = {
      'repositoryUrl': 'repoUrl',
      'token': 'secret_token'
    };

    it('creates the source control record for an application', function() {
      SourceControlService.addSourceControlRecord(
          'application', 'APPLICATION_ID', appRecord).then(successSpy).catch(failSpy);

      $httpBackend.expectPOST(
          CLMLocations.getSourceControlUrl('application', 'APPLICATION_ID'), inputAppRecord).respond(appRecord);
      $httpBackend.flush();

      expect(successSpy).toHaveBeenCalledWith(jasmine.objectContaining(appRecord));
      expect(failSpy).not.toHaveBeenCalled();
    });

    it('returns 400 on a failed request', function() {
      SourceControlService.addSourceControlRecord(
          'application', 'APPLICATION_ID', appRecord).then(successSpy).catch(failSpy);

      $httpBackend.expectPOST(CLMLocations.getSourceControlUrl('application', 'APPLICATION_ID'), inputAppRecord)
          .respond(400, 'SourceControl already exists');
      $httpBackend.flush();

      expect(successSpy).not.toHaveBeenCalled();
      expect(failSpy).toHaveBeenCalledWith(
          jasmine.objectContaining({status: 400, data: 'SourceControl already exists'}));
    });

    it('returns 404 on a failed request', function() {
      SourceControlService.addSourceControlRecord(
          'application', 'non-existent', appRecord).then(successSpy).catch(failSpy);

      $httpBackend.expectPOST(CLMLocations.getSourceControlUrl('application', 'non-existent'), inputAppRecord)
          .respond(404, 'not found');
      $httpBackend.flush();

      expect(successSpy).not.toHaveBeenCalled();
      expect(failSpy).toHaveBeenCalledWith(jasmine.objectContaining({status: 404, data: 'not found'}));
    });
  });

  describe('updateSourceControlRecord_ForApp', function() {
    const appRecord = {
      'id': '1234',
      'ownerId': 'APPLICATION_ID',
      'repositoryUrl': 'repoUrl',
      'token': 'secret_token',
      'provider': 'gitlab'
    };

    const inputAppRecord = {
      'repositoryUrl': 'repoUrl',
      'token': 'secret_token'
    };

    it('updates the source control record for an application', function() {
      SourceControlService.updateSourceControlRecord(
          'application', 'APPLICATION_ID', appRecord).then(successSpy).catch(failSpy);

      $httpBackend.expectPUT(
          CLMLocations.getSourceControlUrl('application', 'APPLICATION_ID'), inputAppRecord).respond(appRecord);
      $httpBackend.flush();

      expect(successSpy).toHaveBeenCalledWith(jasmine.objectContaining(appRecord));
      expect(failSpy).not.toHaveBeenCalled();
    });

    it('returns 400 on a failed request', function() {
      SourceControlService.updateSourceControlRecord(
          'application', 'APPLICATION_ID', appRecord).then(successSpy).catch(failSpy);

      $httpBackend.expectPUT(CLMLocations.getSourceControlUrl('application', 'APPLICATION_ID'), inputAppRecord)
          .respond(400, 'SourceControl provider is invalid');
      $httpBackend.flush();

      expect(successSpy).not.toHaveBeenCalled();
      expect(failSpy).toHaveBeenCalledWith(
          jasmine.objectContaining({status: 400, data: 'SourceControl provider is invalid'}));
    });

    it('returns 404 on a failed request', function() {
      SourceControlService.updateSourceControlRecord(
          'application', 'non-existent', appRecord).then(successSpy).catch(failSpy);

      $httpBackend.expectPUT(CLMLocations.getSourceControlUrl('application', 'non-existent'), inputAppRecord)
          .respond(404, 'not found');
      $httpBackend.flush();

      expect(successSpy).not.toHaveBeenCalled();
      expect(failSpy).toHaveBeenCalledWith(jasmine.objectContaining({status: 404, data: 'not found'}));
    });
  });

  describe('deleteSourceControlRecord_ForApp', function() {
    it('deletes the source control record for an application', function() {
      SourceControlService.deleteSourceControlRecord('application', 'APPLICATION_ID').then(successSpy).catch(
          failSpy);

      $httpBackend.expectDELETE(
          CLMLocations.getSourceControlUrl('application', 'APPLICATION_ID')).respond('no content');
      $httpBackend.flush();

      expect(successSpy).toHaveBeenCalledWith('no content');
      expect(failSpy).not.toHaveBeenCalled();
    });

    it('returns 404 on a failed request', function() {
      SourceControlService.deleteSourceControlRecord('application', 'non-existent').then(successSpy).catch(failSpy);

      $httpBackend.expectDELETE(CLMLocations.getSourceControlUrl('application', 'non-existent'))
          .respond(404, 'not found');
      $httpBackend.flush();

      expect(successSpy).not.toHaveBeenCalled();
      expect(failSpy).toHaveBeenCalledWith(jasmine.objectContaining({status: 404, data: 'not found'}));
    });
  });
});
