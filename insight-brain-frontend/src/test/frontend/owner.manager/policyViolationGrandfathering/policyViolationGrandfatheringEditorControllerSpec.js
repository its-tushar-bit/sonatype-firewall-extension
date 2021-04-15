/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import policyViolationGrandfatheringModule from '../../../../main/frontend/owner.manager/policyViolationGrandfathering/module';

describe('PolicyViolationGrandfatheringEditorController', function () {
  beforeEach(angular.mock.module(policyViolationGrandfatheringModule.name));

  var $scope,
    $timeout,
    $httpBackend,
    getGrandfatheringDeferred,
    setGrandfatheringDeferred,
    mockPolicyViolationGrandfatheringService,
    vm;

  beforeEach(inject(function (
    _$rootScope_,
    $q,
    _$timeout_,
    _$httpBackend_,
    $componentController,
    CLMLocations
  ) {
    $scope = _$rootScope_.$new();
    $timeout = _$timeout_;
    $httpBackend = _$httpBackend_;
    getGrandfatheringDeferred = $q.defer();
    setGrandfatheringDeferred = $q.defer();
    mockPolicyViolationGrandfatheringService = {
      getGrandfathering: jasmine
        .createSpy()
        .and.returnValue(getGrandfatheringDeferred.promise),
      setGrandfathering: jasmine
        .createSpy()
        .and.returnValue(setGrandfatheringDeferred.promise),
      getStatusMessage: JSON.stringify,
    };
    $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond([]);
    vm = $componentController('policyViolationGrandfatheringEditor', {
      $scope: $scope,
      policyViolationGrandfatheringService: mockPolicyViolationGrandfatheringService,
    });
    vm.violationGrandfatheringEditorMask = {
      wrap: SpecUtil.promiseWrapper($q),
    };
  }));

  describe('loading violation grandfathering configuration', function () {
    it('loads configuration on success', inject(function () {
      const config = {
        enabled: true,
        calculatedEnabled: true,
        inheritedFromOrganizationName: null,
        allowChange: true,
        allowOverride: true,
      };

      vm.$onInit();

      getGrandfatheringDeferred.resolve(config);

      $httpBackend.flush();

      expect(
        mockPolicyViolationGrandfatheringService.getGrandfathering
      ).toHaveBeenCalled();
      expect(vm.currentConfiguration).toEqual(config);
      expect(vm.originalConfiguration).toEqual(config);
      expect(vm.statusMessage).toEqual(JSON.stringify(config));
      expect(vm.loadError).toEqual(undefined);
    }));

    it('sets the error message on failure', inject(function () {
      vm.$onInit();

      getGrandfatheringDeferred.reject({ status: 404, data: 'not found' });

      $timeout.flush();

      expect(
        mockPolicyViolationGrandfatheringService.getGrandfathering
      ).toHaveBeenCalled();
      expect(vm.currentConfiguration).toEqual(undefined);
      expect(vm.originalConfiguration).toEqual(undefined);
      expect(vm.statusMessage).toEqual(undefined);
      expect(vm.loadError).toEqual('not found');
    }));
  });

  describe('saving configuration', function () {
    it('saves configuration and reloads on success', inject(function () {
      const oldConfig = {
        enabled: true,
        allowOverride: true,
      };
      const newConfig = {
        enabled: true,
        allowOverride: false,
      };
      vm.originalConfiguration = angular.copy(oldConfig);
      vm.currentConfiguration = angular.copy(newConfig);

      vm.save();

      setGrandfatheringDeferred.resolve({});
      getGrandfatheringDeferred.resolve(newConfig);

      $httpBackend.flush();

      expect(
        mockPolicyViolationGrandfatheringService.setGrandfathering
      ).toHaveBeenCalledWith(newConfig);
      expect(
        mockPolicyViolationGrandfatheringService.getGrandfathering
      ).toHaveBeenCalled();
      expect(vm.currentConfiguration).toEqual(newConfig);
      expect(vm.originalConfiguration).toEqual(newConfig);
    }));

    it('sets the error message on failure', inject(function () {
      const oldConfig = {
        enabled: true,
        allowOverride: true,
      };
      const newConfig = {
        enabled: true,
        allowOverride: false,
      };
      vm.originalConfiguration = angular.copy(oldConfig);
      vm.currentConfiguration = angular.copy(newConfig);

      vm.save();

      setGrandfatheringDeferred.reject({ status: 404, data: 'not found' });

      $timeout.flush();

      expect(
        mockPolicyViolationGrandfatheringService.setGrandfathering
      ).toHaveBeenCalledWith(newConfig);
      expect(
        mockPolicyViolationGrandfatheringService.getGrandfathering
      ).not.toHaveBeenCalled();
      expect(vm.currentConfiguration).toEqual(newConfig);
      expect(vm.originalConfiguration).toEqual(oldConfig);
      expect(vm.submitError).toEqual('not found');
    }));
  });

  describe('detecting changed configuration', function () {
    it('correctly identifies no changes as not dirty', inject(function () {
      vm.originalConfiguration = {
        enabled: true,
        allowOverride: true,
      };
      vm.currentConfiguration = {
        enabled: true,
        allowOverride: true,
      };

      expect(vm.isDirty()).toBe(false);
    }));

    it('correctly identifies changes as dirty when the enabled flag has changed', inject(function () {
      vm.originalConfiguration = {
        enabled: true,
        allowOverride: true,
      };
      vm.currentConfiguration = {
        enabled: false,
        allowOverride: true,
      };

      expect(vm.isDirty()).toBe(true);
    }));

    it('correctly identifies changes as dirty when the allow override flag has changed', inject(function () {
      vm.originalConfiguration = {
        enabled: true,
        allowOverride: true,
      };
      vm.currentConfiguration = {
        enabled: true,
        allowOverride: false,
      };

      expect(vm.isDirty()).toBe(true);
    }));
  });
});
