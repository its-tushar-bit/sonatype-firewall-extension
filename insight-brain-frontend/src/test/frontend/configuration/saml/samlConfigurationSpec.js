/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import samlModule from '../../../../main/frontend/configuration/saml/module';
import { omit } from 'ramda';

describe('samlConfiguration', function () {
  let $scope,
    $httpBackend,
    CLMContextLocations,
    mockFileReader,
    Dialog,
    wrapReturn,
    vm;

  const defaultSaml = {
    identityProviderName: 'identity provider',
    identityProviderMetadataXml: undefined,
    entityId: 'http://localhost/api/v2/config/saml/metadata',
    usernameAttributeName: 'username',
    firstNameAttributeName: 'firstName',
    lastNameAttributeName: 'lastName',
    emailAttributeName: 'email',
    groupsAttributeName: 'groups',
    validateResponseSignature: null,
    validateAssertionSignature: null,
  };

  const saml1 = {
    identityProviderName: 'idp1',
    identityProviderMetadataXml: '<xml>1</xml>',
    entityId: 'entityId1',
    usernameAttributeName: 'username1',
    firstNameAttributeName: 'firstName1',
    lastNameAttributeName: 'lastName1',
    emailAttributeName: 'email1',
    groupsAttributeName: 'groups1',
    validateResponseSignature: true,
    validateAssertionSignature: false,
  };

  const saml2 = {
    identityProviderName: 'idp2',
    identityProviderMetadataXml: '<xml>2</xml>',
    entityId: 'entityId2',
    usernameAttributeName: 'username2',
    firstNameAttributeName: 'firstName2',
    lastNameAttributeName: 'lastName2',
    emailAttributeName: 'email2',
    groupsAttributeName: 'groups2',
    validateResponseSignature: false,
    validateAssertionSignature: true,
  };

  beforeEach(angular.mock.module(samlModule.name));

  beforeEach(inject(function (
    $rootScope,
    _$httpBackend_,
    _CLMContextLocations_,
    _Dialog_,
    BaseUrl,
    $componentController
  ) {
    $scope = $rootScope.$new();
    $httpBackend = _$httpBackend_;
    CLMContextLocations = _CLMContextLocations_;
    Dialog = _Dialog_;
    spyOn(BaseUrl, 'get').and.returnValue('http://localhost');
    mockFileReader = {
      addEventListener: jasmine.createSpy(),
      readAsText: function (file) {
        // In reality a FileReader would read the content of the given file but we just pretend we did
        this.result = file.content;
        // Call whatever function was most recently added as an event listener (i.e. for load)
        this.addEventListener.calls.mostRecent().args[1]();
      },
      abort: jasmine.createSpy(),
    };
    spyOn(window, 'FileReader').and.returnValue(mockFileReader);

    // This initializes our SAML controller which creates a mockFileReader
    vm = $componentController('samlConfiguration');
    wrapReturn = {
      catch: jasmine.createSpy('catch'),
    };
    vm.samlConfigurationMask = {
      wrap: jasmine.createSpy('wrap').and.returnValue(wrapReturn),
    };
  }));

  afterEach(function () {
    $httpBackend.verifyNoOutstandingRequest();
    $httpBackend.verifyNoOutstandingExpectation();
  });

  describe('load', function () {
    it('sets the given saml values if a configuration exists', function () {
      $httpBackend
        .expectGET(CLMContextLocations.getSamlConfigurationUrl())
        .respond(saml1);

      $scope.$digest();

      $httpBackend.flush();
      expect(vm.saml).toEqual(saml1);
      expect(vm.isUpdating).toBe(true);
      expect(vm.loadError).toBeUndefined();
      expect(vm.saveOrDeleteError).toBeUndefined();
    });

    it('sets the default saml values if no configuration exists', function () {
      $httpBackend
        .expectGET(CLMContextLocations.getSamlConfigurationUrl())
        .respond(404, 'not found');

      $scope.$digest();

      $httpBackend.flush();
      expect(vm.saml).toEqual(defaultSaml);
      expect(vm.isUpdating).toBe(false);
      expect(vm.loadError).toBeUndefined();
      expect(vm.saveOrDeleteError).toBeUndefined();
    });

    it('sets the error message on failure', function () {
      $httpBackend
        .expectGET(CLMContextLocations.getSamlConfigurationUrl())
        .respond(400, 'bad request');

      $scope.$digest();

      $httpBackend.flush();
      expect(vm.saml).toBeUndefined();
      expect(vm.isUpdating).toBeUndefined();
      expect(vm.loadError).toBe('bad request');
      expect(vm.saveOrDeleteError).toBeUndefined();
    });
  });

  describe('readIdentityProviderMetadataXml', function () {
    beforeEach(function () {
      $httpBackend
        .expectGET(CLMContextLocations.getSamlConfigurationUrl())
        .respond(saml1);
      $scope.$digest();
      $httpBackend.flush();
    });

    it('does nothing if the given file is undefined', function () {
      expect(vm.saml.identityProviderMetadataXml).toBe(
        saml1.identityProviderMetadataXml
      );

      vm.readIdentityProviderMetadataXml();

      expect(vm.saml.identityProviderMetadataXml).toBe(
        saml1.identityProviderMetadataXml
      );
    });

    it('sets the identityProviderMetadataXml to the file content', function () {
      expect(vm.saml.identityProviderMetadataXml).toBe(
        saml1.identityProviderMetadataXml
      );
      let file = { content: '<xml>different</xml>' };

      vm.readIdentityProviderMetadataXml(file);

      expect(mockFileReader.addEventListener.calls.mostRecent().args[0]).toBe(
        'load'
      );
      expect(mockFileReader.result).toBe('<xml>different</xml>');
    });
  });

  describe('isChanged', function () {
    beforeEach(function () {
      $httpBackend
        .expectGET(CLMContextLocations.getSamlConfigurationUrl())
        .respond(saml1);
      $scope.$digest();
      $httpBackend.flush();
    });

    it('returns true if the identityProviderName is changed', function () {
      expect(vm.isChanged()).toBe(false);

      vm.saml.identityProviderName = vm.saml.identityProviderName + '2';

      expect(vm.isChanged()).toBe(true);
    });

    it('returns true if the identityProviderMetadataXml is changed', function () {
      expect(vm.isChanged()).toBe(false);

      vm.saml.identityProviderMetadataXml = '<xml>changed</xml>';

      expect(vm.isChanged()).toBe(true);
    });

    it('returns true if the entityId is changed', function () {
      expect(vm.isChanged()).toBe(false);

      vm.saml.entityId = vm.saml.entityId + '2';

      expect(vm.isChanged()).toBe(true);
    });

    it('returns true if the usernameNameAttributeName is changed', function () {
      expect(vm.isChanged()).toBe(false);

      vm.saml.usernameNameAttributeName =
        vm.saml.usernameNameAttributeName + '2';

      expect(vm.isChanged()).toBe(true);
    });

    it('returns true if the firstNameAttributeName is changed', function () {
      expect(vm.isChanged()).toBe(false);

      vm.saml.firstNameAttributeName = vm.saml.firstNameAttributeName + '2';

      expect(vm.isChanged()).toBe(true);
    });

    it('returns true if the lastNameAttributeName is changed', function () {
      expect(vm.isChanged()).toBe(false);

      vm.saml.lastNameAttributeName = vm.saml.lastNameAttributeName + '2';

      expect(vm.isChanged()).toBe(true);
    });

    it('returns true if the emailAttributeName is changed', function () {
      expect(vm.isChanged()).toBe(false);

      vm.saml.emailAttributeName = vm.saml.emailAttributeName + '2';

      expect(vm.isChanged()).toBe(true);
    });

    it('returns true if the groupsAttributeName is changed', function () {
      expect(vm.isChanged()).toBe(false);

      vm.saml.groupsAttributeName = vm.saml.groupsAttributeName + '2';

      expect(vm.isChanged()).toBe(true);
    });

    it('returns true if validateResponseSignature is changed', function () {
      expect(vm.isChanged()).toBe(false);

      vm.saml.validateResponseSignature = !vm.saml.validateResponseSignature;

      expect(vm.isChanged()).toBe(true);
    });

    it('returns true if validateAssertionSignature is changed', function () {
      expect(vm.isChanged()).toBe(false);

      vm.saml.validateAssertionSignature = !vm.saml.validateAssertionSignature;

      expect(vm.isChanged()).toBe(true);
    });

    it('returns false if nothing is changed', function () {
      expect(vm.isChanged()).toBe(false);
      vm.saml = defaultSaml;
      expect(vm.isChanged()).toBe(true);

      vm.saml = saml1;

      expect(vm.isChanged()).toBe(false);
    });
  });

  describe('save', function () {
    beforeEach(function () {
      $httpBackend
        .expectGET(CLMContextLocations.getSamlConfigurationUrl())
        .respond(404, 'not found');
      $scope.$digest();
      $httpBackend.flush();
    });

    it('sets the original saml values to the saml values on success', function () {
      expect(vm.isUpdating).toBe(false);
      expect(vm.isChanged()).toBe(false);
      vm.saml = saml2;
      expect(vm.isChanged()).toBe(true);
      $httpBackend
        .expectPUT(
          CLMContextLocations.getSamlConfigurationUrl(),
          function (data) {
            expect(data).not.toBeUndefined();
            expect(data.get('identityProviderXml')).toBe(
              vm.saml.identityProviderMetadataXml
            );
            expect(data.get('samlConfiguration')).toBe(
              JSON.stringify(omit(['identityProviderMetadataXml'], vm.saml))
            );
            return true;
          },
          function (headers) {
            expect(headers).not.toBeUndefined();
            expect(headers['Content-Type']).toBe(undefined);
            return true;
          }
        )
        .respond(204);
      $httpBackend
        .expectGET(CLMContextLocations.getSamlConfigurationUrl())
        .respond(vm.saml);
      expect(vm.samlConfigurationMask.wrap).not.toHaveBeenCalled();

      vm.save();

      expect(vm.samlConfigurationMask.wrap).toHaveBeenCalled();
      let maskArg = vm.samlConfigurationMask.wrap.calls.mostRecent().args[0];
      let maskArgResolvedSpy = jasmine.createSpy();
      maskArg.then(maskArgResolvedSpy);
      expect(maskArgResolvedSpy).not.toHaveBeenCalled();

      $httpBackend.flush();

      expect(maskArgResolvedSpy).toHaveBeenCalled();
      expect(vm.saml).toEqual(saml2);
      expect(vm.isUpdating).toBe(true);
      expect(vm.isChanged()).toBe(false);
      expect(vm.loadError).toBeUndefined();
      expect(vm.saveOrDeleteError).toBeUndefined();
    });

    it('sets the error message on failure', function () {
      expect(vm.isUpdating).toBe(false);
      expect(vm.isChanged()).toBe(false);
      vm.saml = saml2;
      expect(vm.isChanged()).toBe(true);
      $httpBackend
        .expectPUT(CLMContextLocations.getSamlConfigurationUrl())
        .respond(400, 'bad request');
      expect(vm.samlConfigurationMask.wrap).not.toHaveBeenCalled();

      vm.save();

      expect(vm.samlConfigurationMask.wrap).toHaveBeenCalled();
      let maskArg = vm.samlConfigurationMask.wrap.calls.mostRecent().args[0];
      let maskArgResolvedSpy = jasmine.createSpy();
      let maskArgFailedSpy = jasmine.createSpy();
      maskArg.then(maskArgResolvedSpy).catch(maskArgFailedSpy);
      expect(maskArgResolvedSpy).not.toHaveBeenCalled();
      expect(maskArgFailedSpy).not.toHaveBeenCalled();

      $httpBackend.flush();

      expect(maskArgResolvedSpy).not.toHaveBeenCalled();
      expect(maskArgFailedSpy).toHaveBeenCalled();
      wrapReturn.catch.calls
        .mostRecent()
        .args[0](maskArgFailedSpy.calls.mostRecent().args[0]);
      expect(vm.saml).toBe(saml2);
      expect(vm.isUpdating).toBe(false);
      expect(vm.isChanged()).toBe(true);
      expect(vm.loadError).toBeUndefined();
      expect(vm.saveOrDeleteError).toBe('bad request');
    });
  });

  describe('cancel', function () {
    beforeEach(function () {
      $httpBackend
        .expectGET(CLMContextLocations.getSamlConfigurationUrl())
        .respond(saml1);
      $scope.$digest();
      $httpBackend.flush();
    });

    it('sets the saml values back to their original values', function () {
      vm.saml = saml2;

      vm.cancel();

      expect(vm.saml).toEqual(saml1);
    });
  });

  describe('delete', function () {
    beforeEach(function () {
      $httpBackend
        .expectGET(CLMContextLocations.getSamlConfigurationUrl())
        .respond(saml1);
      $scope.$digest();
      $httpBackend.flush();
    });

    it('sets the default saml values on confirming delete and success', function () {
      expect(vm.saml).toEqual(saml1);
      expect(vm.isUpdating).toBe(true);
      spyOn(Dialog, 'open');

      vm.delete();

      expect(Dialog.open).toHaveBeenCalled();
      $httpBackend
        .expectDELETE(CLMContextLocations.getSamlConfigurationUrl())
        .respond(200);
      $httpBackend
        .expectGET(CLMContextLocations.getSamlConfigurationUrl())
        .respond(404, 'not found');
      expect(vm.samlConfigurationMask.wrap).not.toHaveBeenCalled();

      Dialog.open.calls.mostRecent().args[0].buttons[0].click();

      expect(vm.samlConfigurationMask.wrap).toHaveBeenCalled();
      let maskArg = vm.samlConfigurationMask.wrap.calls.mostRecent().args[0];
      let maskArgResolvedSpy = jasmine.createSpy();
      maskArg.then(maskArgResolvedSpy);
      expect(maskArgResolvedSpy).not.toHaveBeenCalled();

      $httpBackend.flush();

      expect(maskArgResolvedSpy).toHaveBeenCalled();
      expect(vm.saml).toEqual(defaultSaml);
      expect(vm.isUpdating).toBe(false);
      expect(vm.loadError).toBeUndefined();
      expect(vm.saveOrDeleteError).toBeUndefined();
    });

    it('sets the error message on confirming delete and failure', function () {
      expect(vm.saml).toEqual(saml1);
      expect(vm.isUpdating).toBe(true);
      spyOn(Dialog, 'open');

      vm.delete();

      expect(Dialog.open).toHaveBeenCalled();
      $httpBackend
        .expectDELETE(CLMContextLocations.getSamlConfigurationUrl())
        .respond(404, 'not found');
      expect(vm.samlConfigurationMask.wrap).not.toHaveBeenCalled();

      Dialog.open.calls.mostRecent().args[0].buttons[0].click();

      expect(vm.samlConfigurationMask.wrap).toHaveBeenCalled();
      let maskArg = vm.samlConfigurationMask.wrap.calls.mostRecent().args[0];
      let maskArgResolvedSpy = jasmine.createSpy();
      let maskArgFailedSpy = jasmine.createSpy();
      maskArg.then(maskArgResolvedSpy).catch(maskArgFailedSpy);
      expect(maskArgResolvedSpy).not.toHaveBeenCalled();
      expect(maskArgFailedSpy).not.toHaveBeenCalled();

      $httpBackend.flush();

      expect(maskArgResolvedSpy).not.toHaveBeenCalled();
      expect(maskArgFailedSpy).toHaveBeenCalled();
      wrapReturn.catch.calls
        .mostRecent()
        .args[0](maskArgFailedSpy.calls.mostRecent().args[0]);
      expect(vm.saml).toEqual(saml1);
      expect(vm.isUpdating).toBe(true);
      expect(vm.loadError).toBeUndefined();
      expect(vm.saveOrDeleteError).toBe('not found');
    });

    it('does nothing on cancel', function () {
      expect(vm.saml).toEqual(saml1);
      expect(vm.isUpdating).toBe(true);
      spyOn(Dialog, 'open');

      vm.delete();

      expect(Dialog.open).toHaveBeenCalled();
      expect(Dialog.open.calls.mostRecent().args[0].buttons[1].click).toBe(
        undefined
      );
      expect(vm.saml).toEqual(saml1);
      expect(vm.isUpdating).toBe(true);
      expect(vm.loadError).toBeUndefined();
      expect(vm.saveOrDeleteError).toBeUndefined();
    });
  });

  describe('defaultsToTooltipText', function () {
    beforeEach(function () {
      $httpBackend
        .expectGET(CLMContextLocations.getSamlConfigurationUrl())
        .respond(saml1);
      $scope.$digest();
      $httpBackend.flush();
    });

    it('returns the expected text given a default value', function () {
      let defaultValue = 'defaultValue';
      expect(vm.defaultsToTooltipText(defaultValue)).toBe(
        'If empty will default to "' + defaultValue + '"'
      );
    });
  });

  describe('resetToDefaultValueIfEmpty', function () {
    beforeEach(function () {
      $httpBackend
        .expectGET(CLMContextLocations.getSamlConfigurationUrl())
        .respond(saml1);
      $scope.$digest();
      $httpBackend.flush();
    });

    it('resets the given saml variable to its default value if it is empty', function () {
      vm.saml = {
        identityProviderMetadataXml: '',
        entityId: '',
        usernameAttributeName: '',
        firstNameAttributeName: '',
        lastNameAttributeName: '',
        emailAttributeName: '',
        groupsAttributeName: '',
      };

      vm.resetToDefaultValueIfEmpty('entityId');
      vm.resetToDefaultValueIfEmpty('usernameAttributeName');
      vm.resetToDefaultValueIfEmpty('firstNameAttributeName');
      vm.resetToDefaultValueIfEmpty('lastNameAttributeName');
      vm.resetToDefaultValueIfEmpty('emailAttributeName');
      vm.resetToDefaultValueIfEmpty('groupsAttributeName');

      expect(vm.saml.entityId).toBe(defaultSaml.entityId);
      expect(vm.saml.usernameAttributeName).toBe(
        defaultSaml.usernameAttributeName
      );
      expect(vm.saml.firstNameAttributeName).toBe(
        defaultSaml.firstNameAttributeName
      );
      expect(vm.saml.lastNameAttributeName).toBe(
        defaultSaml.lastNameAttributeName
      );
      expect(vm.saml.emailAttributeName).toBe(defaultSaml.emailAttributeName);
      expect(vm.saml.groupsAttributeName).toBe(defaultSaml.groupsAttributeName);
    });

    it('does not reset the given saml variable to its default value if it is not empty', function () {
      vm.saml = saml1;

      vm.resetToDefaultValueIfEmpty('entityId');
      vm.resetToDefaultValueIfEmpty('usernameAttributeName');
      vm.resetToDefaultValueIfEmpty('firstNameAttributeName');
      vm.resetToDefaultValueIfEmpty('lastNameAttributeName');
      vm.resetToDefaultValueIfEmpty('emailAttributeName');
      vm.resetToDefaultValueIfEmpty('groupsAttributeName');

      expect(vm.saml.entityId).toBe(saml1.entityId);
      expect(vm.saml.usernameAttributeName).toBe(saml1.usernameAttributeName);
      expect(vm.saml.firstNameAttributeName).toBe(saml1.firstNameAttributeName);
      expect(vm.saml.lastNameAttributeName).toBe(saml1.lastNameAttributeName);
      expect(vm.saml.emailAttributeName).toBe(saml1.emailAttributeName);
      expect(vm.saml.groupsAttributeName).toBe(saml1.groupsAttributeName);
    });
  });

  describe('downloadMetadataForIE', function () {
    beforeEach(function () {
      $httpBackend
        .expectGET(CLMContextLocations.getSamlConfigurationUrl())
        .respond(saml1);
      $scope.$digest();
      $httpBackend.flush();
    });

    afterEach(function () {
      if (window.navigator.msSaveBlob) {
        delete window.navigator.msSaveBlob;
      }
    });

    it('calls msSaveBlob if a configuration is saved and it is IE', function () {
      vm.isUpdating = true;
      window.navigator.msSaveBlob = jasmine.createSpy();
      spyOn(window, 'Blob');
      $httpBackend
        .expectGET(CLMContextLocations.getSamlConfigurationUrl() + '/metadata')
        .respond('<xml>metadata</xml>');

      vm.downloadMetadataForIE();

      expect(window.navigator.msSaveBlob).not.toHaveBeenCalled();

      $httpBackend.flush();

      expect(window.Blob).toHaveBeenCalledWith(['<xml>metadata</xml>']);
      expect(window.navigator.msSaveBlob).toHaveBeenCalled();
      expect(
        window.navigator.msSaveBlob.calls.mostRecent().args[0]
      ).not.toBeNull();
      expect(window.navigator.msSaveBlob.calls.mostRecent().args[1]).toBe(
        'metadata.xml'
      );
    });

    it('does nothing if a configuration is saved and it is not IE', function () {
      vm.isUpdating = true;
      spyOn(window, 'Blob');

      vm.downloadMetadataForIE();

      expect(window.Blob).not.toHaveBeenCalled();
    });

    it('does nothing if a configuration is not saved and it is IE', function () {
      vm.isUpdating = false;
      window.navigator.msSaveBlob = jasmine.createSpy();

      vm.downloadMetadataForIE();

      expect(window.navigator.msSaveBlob).not.toHaveBeenCalled();
    });

    it('does nothing if a configuration is not saved and it is not IE', function () {
      vm.isUpdating = false;
      spyOn(window, 'Blob');

      vm.downloadMetadataForIE();

      expect(window.Blob).not.toHaveBeenCalled();
    });
  });

  describe('shouldEnableDownloadMetadataLink', function () {
    beforeEach(function () {
      $httpBackend
        .expectGET(CLMContextLocations.getSamlConfigurationUrl())
        .respond(saml1);
      $scope.$digest();
      $httpBackend.flush();
    });

    afterEach(function () {
      if (window.navigator.msSaveBlob) {
        delete window.navigator.msSaveBlob;
      }
    });

    it('returns true if a configuration exists and it is not IE', function () {
      vm.isUpdating = true;

      expect(vm.shouldEnableDownloadMetadataLink()).toBe(true);
    });

    it('returns false if a configuration exists and it is IE', function () {
      vm.isUpdating = true;
      window.navigator.msSaveBlob = jasmine.createSpy();

      expect(vm.shouldEnableDownloadMetadataLink()).toBe(false);
    });

    it('returns false if a configuration does not exist and it is not IE', function () {
      vm.isUpdating = false;

      expect(vm.shouldEnableDownloadMetadataLink()).toBe(false);
    });

    it('returns false if a configuration does not exist and it is IE', function () {
      vm.isUpdating = false;
      window.navigator.msSaveBlob = jasmine.createSpy();

      expect(vm.shouldEnableDownloadMetadataLink()).toBe(false);
    });
  });
});
