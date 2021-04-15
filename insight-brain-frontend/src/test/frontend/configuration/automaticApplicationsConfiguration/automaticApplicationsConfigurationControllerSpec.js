/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import configurationModule from '../../../../main/frontend/configuration/module';

describe('automaticApplicationsConfigurationControllerSpec.js', function () {
  beforeEach(
    angular.mock.module(configurationModule.name, function ($provide) {
      SpecUtil.mockPermissionService($provide);
    })
  );

  var $scope,
    organizationStoreGetDeferred,
    getConfigurationDeferred,
    saveConfigurationDeferred,
    mockOrganizationStore,
    mockAutomaticApplicationsConfigurationService,
    vm;

  beforeEach(inject(function (_$rootScope_, $q, $componentController) {
    $scope = _$rootScope_.$new();
    organizationStoreGetDeferred = $q.defer();
    getConfigurationDeferred = $q.defer();
    saveConfigurationDeferred = $q.defer();
    mockOrganizationStore = {
      get: jasmine
        .createSpy()
        .and.returnValue(organizationStoreGetDeferred.promise),
    };
    mockAutomaticApplicationsConfigurationService = {
      getConfiguration: jasmine
        .createSpy()
        .and.returnValue(getConfigurationDeferred.promise),
      saveConfiguration: jasmine
        .createSpy()
        .and.returnValue(saveConfigurationDeferred.promise),
    };
    vm = $componentController('automaticApplicationsConfiguration', {
      OrganizationStore: mockOrganizationStore,
      automaticApplicationsConfigurationService: mockAutomaticApplicationsConfigurationService,
    });
    vm.automaticApplicationsConfigurationForm = {
      $valid: true,
    };
    vm.automaticApplicationsConfigurationFormMask = {
      wrap: SpecUtil.promiseWrapper($q),
    };
  }));

  describe('loading options and settings from the server', function () {
    it('loads it if the request succeeds', function () {
      vm.$onInit();
      organizationStoreGetDeferred.resolve([
        { name: 'Root Organization', id: 'ROOT_ORGANIZATION_ID' },
        { name: 'Test Organization', id: 'TEST_ORGANIZATION_ID' },
      ]);
      getConfigurationDeferred.resolve({
        enabled: true,
        parentOrganizationId: 'organizationId',
      });
      $scope.$digest();

      expect(mockOrganizationStore.get).toHaveBeenCalled();
      expect(vm.organizationOptions).toEqual([
        { name: 'Test Organization', id: 'TEST_ORGANIZATION_ID' },
      ]);

      expect(
        mockAutomaticApplicationsConfigurationService.getConfiguration
      ).toHaveBeenCalled();
      expect(vm.automaticApplicationCreationEnabled).toEqual(true);
      expect(vm.automaticApplicationCreationOrganizationId).toEqual(
        'organizationId'
      );
      expect(vm.savedAutomaticApplicationCreationEnabled).toEqual(true);
      expect(vm.savedAutomaticApplicationCreationOrganizationId).toEqual(
        'organizationId'
      );

      expect(vm.loaded).toEqual(true);
    });

    it('sets the error if the request for organization options fails', function () {
      vm.$onInit();
      organizationStoreGetDeferred.reject({ status: 404, data: 'not found' });
      $scope.$digest();

      expect(mockOrganizationStore.get).toHaveBeenCalled();
      expect(vm.organizationOptions).toBe(undefined);
      expect(vm.error.status).toEqual(404);
      expect(vm.error.data).toEqual('not found');
      expect(vm.loaded).toEqual(false);
    });

    it('sets the error if the request for current configuration settings fails', function () {
      vm.$onInit();
      getConfigurationDeferred.reject({ status: 404, data: 'not found' });
      $scope.$digest();

      expect(
        mockAutomaticApplicationsConfigurationService.getConfiguration
      ).toHaveBeenCalled();
      expect(vm.automaticApplicationCreationEnabled).toBe(undefined);
      expect(vm.automaticApplicationCreationOrganizationId).toBe(undefined);
      expect(vm.savedAutomaticApplicationCreationEnabled).toBe(undefined);
      expect(vm.savedAutomaticApplicationCreationOrganizationId).toBe(
        undefined
      );
      expect(vm.error.status).toEqual(404);
      expect(vm.error.data).toEqual('not found');
      expect(vm.loaded).toEqual(false);
    });

    it('deletes any error', function () {
      vm.error = { status: 404, data: 'not found' };

      vm.load();
      organizationStoreGetDeferred.resolve([]);
      getConfigurationDeferred.resolve({});
      $scope.$digest();

      expect(vm.error).toBeUndefined();
    });
  });

  describe('saving settings to the server', function () {
    it('saves and updates the settings if the request succeeds', function () {
      var configuration = {
        enabled: true,
        parentOrganizationId: 'organizationId',
      };

      vm.automaticApplicationCreationEnabled = true;
      vm.automaticApplicationCreationOrganizationId = 'organizationId';
      vm.save();
      saveConfigurationDeferred.resolve(configuration);
      $scope.$digest();

      expect(
        mockAutomaticApplicationsConfigurationService.saveConfiguration
      ).toHaveBeenCalledWith(configuration);
      expect(vm.savedAutomaticApplicationCreationEnabled).toEqual(true);
      expect(vm.savedAutomaticApplicationCreationOrganizationId).toEqual(
        'organizationId'
      );
    });

    it('sets the error if the save request fails', function () {
      var configuration = {
        enabled: true,
        parentOrganizationId: 'organizationId',
      };

      vm.automaticApplicationCreationEnabled = true;
      vm.automaticApplicationCreationOrganizationId = 'organizationId';
      vm.save();
      saveConfigurationDeferred.reject({ status: 400, data: 'bad request' });
      $scope.$digest();

      expect(
        mockAutomaticApplicationsConfigurationService.saveConfiguration
      ).toHaveBeenCalledWith(configuration);
      expect(vm.error.status).toEqual(400);
      expect(vm.error.data).toEqual('bad request');
    });

    it('deletes any error', function () {
      vm.error = { status: 404, data: 'not found' };
      vm.automaticApplicationCreationEnabled = false;
      vm.automaticApplicationCreationOrganizationId = 'a';
      vm.savedAutomaticApplicationCreationEnabled = true;
      vm.savedAutomaticApplicationCreationOrganizationId = 'b';

      vm.save();
      saveConfigurationDeferred.resolve({});
      $scope.$digest();

      expect(vm.error).toBeUndefined();
    });

    it('reverts any changes if the cancel button is pressed', function () {
      vm.$onInit();
      organizationStoreGetDeferred.resolve([]);
      getConfigurationDeferred.resolve({
        enabled: true,
        parentOrganizationId: 'organizationId',
      });
      $scope.$digest();

      vm.automaticApplicationCreationEnabled = false;
      vm.automaticApplicationCreationOrganizationId = 'foo';
      vm.cancel();

      expect(vm.automaticApplicationCreationEnabled).toEqual(true);
      expect(vm.automaticApplicationCreationOrganizationId).toEqual(
        'organizationId'
      );
    });

    it('does not save if the form is invalid', function () {
      vm.automaticApplicationCreationEnabled = false;
      vm.automaticApplicationCreationOrganizationId = 'a';
      vm.savedAutomaticApplicationCreationEnabled = true;
      vm.savedAutomaticApplicationCreationOrganizationId = 'b';
      vm.automaticApplicationsConfigurationForm.$valid = false;
      vm.save();

      expect(
        mockAutomaticApplicationsConfigurationService.saveConfiguration
      ).not.toHaveBeenCalled();
    });

    it('does not save if the form is unchanged', function () {
      vm.$onInit();
      organizationStoreGetDeferred.resolve([]);
      getConfigurationDeferred.resolve({});
      $scope.$digest();

      vm.automaticApplicationCreationEnabled = false;
      vm.automaticApplicationCreationOrganizationId = 'a';
      vm.savedAutomaticApplicationCreationEnabled = false;
      vm.savedAutomaticApplicationCreationOrganizationId = 'a';
      vm.save();

      expect(
        mockAutomaticApplicationsConfigurationService.saveConfiguration
      ).not.toHaveBeenCalled();
    });
  });

  describe('checking if there are changes by calling isChanged', function () {
    it('returns true if the saved settings do not equal the current settings', function () {
      vm.automaticApplicationCreationEnabled = true;
      vm.automaticApplicationCreationOrganizationId = 'organizationId';

      vm.savedAutomaticApplicationCreationEnabled = true;
      vm.savedAutomaticApplicationCreationOrganizationId = 'foo';
      expect(vm.isChanged()).toBe(true);

      vm.savedAutomaticApplicationCreationEnabled = false;
      vm.savedAutomaticApplicationCreationOrganizationId = 'organizationId';
      expect(vm.isChanged()).toBe(true);
    });

    it('returns false if the saved settings equal the current settings', function () {
      vm.automaticApplicationCreationEnabled = true;
      vm.automaticApplicationCreationOrganizationId = 'organizationId';
      vm.savedAutomaticApplicationCreationEnabled = true;
      vm.savedAutomaticApplicationCreationOrganizationId = 'organizationId';
      expect(vm.isChanged()).toBe(false);
    });
  });
});
