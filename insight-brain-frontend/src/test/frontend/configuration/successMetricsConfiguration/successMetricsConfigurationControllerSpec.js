/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import configurationModule from '../../../../main/frontend/configuration/module';

describe('successMetricsConfigurationSpec.js', function() {
  beforeEach(angular.mock.module(configurationModule.name, function($provide) {
    SpecUtil.mockPermissionService($provide);
  }));

  var $scope,
      isSuccessMetricsEnabledDeferred,
      saveSuccessMetricsEnabledDeferred,
      mockSystemConfigurationPropertyService,
      vm;

  beforeEach(inject(function(_$rootScope_, $q, $componentController) {
    $scope = _$rootScope_.$new();
    isSuccessMetricsEnabledDeferred = $q.defer();
    saveSuccessMetricsEnabledDeferred = $q.defer();
    mockSystemConfigurationPropertyService = {
      isSuccessMetricsEnabled: jasmine.createSpy().and.returnValue(isSuccessMetricsEnabledDeferred.promise),
      saveSuccessMetricsEnabled: jasmine.createSpy().and.returnValue(saveSuccessMetricsEnabledDeferred.promise)
    };
    vm = $componentController('successMetricsConfiguration', {
      systemConfigurationPropertyService: mockSystemConfigurationPropertyService
    });
  }));

  afterEach(function() {
    $scope.$destroy();
  });

  describe('loading the success metrics flag', function() {
    it('loads it if the request succeeds', function() {
      isSuccessMetricsEnabledDeferred.resolve(true);
      $scope.$digest();

      expect(mockSystemConfigurationPropertyService.isSuccessMetricsEnabled).toHaveBeenCalled();
      expect(vm.successMetricsEnabled).toBe(true);
    });

    it('sets the error if the request fails', function() {
      isSuccessMetricsEnabledDeferred.reject({status: 404, data: 'not found'});
      $scope.$digest();

      expect(mockSystemConfigurationPropertyService.isSuccessMetricsEnabled).toHaveBeenCalled();
      expect(vm.successMetricsEnabled).toBe(undefined);
      expect(vm.error.status).toEqual(404);
      expect(vm.error.data).toEqual('not found');
    });

    it('deletes any error', function() {
      isSuccessMetricsEnabledDeferred.reject({status: 404, data: 'not found'});
      $scope.$digest();
      vm.load();

      expect(vm.error).toBeUndefined();
    });
  });

  describe('saving the success metrics flag', function() {
    it('sends it to the server and updates its saved values', function() {
      isSuccessMetricsEnabledDeferred.resolve(true);
      $scope.$digest();
      vm.successMetricsEnabled = false;
      vm.save();
      saveSuccessMetricsEnabledDeferred.resolve({status: 204, data: 'no content'});
      $scope.$digest();

      expect(mockSystemConfigurationPropertyService.saveSuccessMetricsEnabled).toHaveBeenCalledWith(false);
      expect(vm.error).toBeUndefined();
      expect(vm.savedSuccessMetricsEnabled).toBe(false);
    });

    it('sets the error if it fails to send it to the server', function() {
      isSuccessMetricsEnabledDeferred.resolve(true);
      $scope.$digest();
      vm.save();
      saveSuccessMetricsEnabledDeferred.reject({status: 401, data: 'unauthorized'});
      $scope.$digest();

      expect(mockSystemConfigurationPropertyService.saveSuccessMetricsEnabled).toHaveBeenCalledWith(true);
      expect(vm.error.status).toEqual(401);
      expect(vm.error.data).toEqual('unauthorized');
      expect(vm.savedSuccessMetricsEnabled).toBe(true);
    });

    it('deletes any error', function() {
      isSuccessMetricsEnabledDeferred.resolve(true);
      $scope.$digest();
      vm.error = 'error';
      vm.save();

      expect(vm.error).toBeUndefined();
    });
  });
});
