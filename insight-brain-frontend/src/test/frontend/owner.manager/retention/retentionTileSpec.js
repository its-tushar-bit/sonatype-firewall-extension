/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import retentionModule from 'MainRoot/owner.manager/retention/module';
import utilityModule from 'MainRoot/utility/utility.module';
import { mapStateToThis } from 'MainRoot/owner.manager/retention/retentionTile';
import { disabledRetentionPolicies, inheritedRetentionPolicies } from './retentionMockData';

describe('retentionTile', function () {
  let $rootScope,
    $scope,
    EventNameConstant,
    $q,
    $componentController,
    mockCLMContextLocations,
    getByIdDeferred,
    getRetentionPoliciesDeferred,
    mockRetentionService,
    vm;

  beforeEach(
    angular.mock.module(retentionModule.name, utilityModule.name, function ($provide) {
      SpecUtil.mockNgRedux($provide);
    })
  );

  beforeEach(inject(function (_$rootScope_, $injector, _$q_, _$componentController_) {
    $rootScope = _$rootScope_;
    $scope = $rootScope.$new();
    EventNameConstant = $injector.get('event.name.constant');
    $q = _$q_;
    $componentController = _$componentController_;
    mockCLMContextLocations = jasmine.createSpyObj('CLMContextLocations', ['isOrganization']);
    mockCLMContextLocations.isOrganization.and.returnValue(true);
    getByIdDeferred = $q.defer();
    getRetentionPoliciesDeferred = $q.defer();
    mockRetentionService = {
      getRetentionPolicies: jasmine.createSpy().and.callFake(function () {
        return getRetentionPoliciesDeferred.promise;
      }),
    };
    vm = $componentController('retentionTile', {
      $scope: $scope,
      CLMContextLocations: mockCLMContextLocations,
      retentionService: mockRetentionService,
    });
  }));

  describe('mapStateToThis', () => {
    it('sets ownerName', () => {
      const state = {
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              name: 'ownerName',
            },
          },
        },
      };

      const output = mapStateToThis(state);
      expect(output.ownerName).toBe('ownerName');
    });
  });

  describe('on $destroy()', () => {
    it('unsubscribes from redux store', () => {
      expect(vm.unsubscribe).not.toHaveBeenCalled();
      $rootScope.$destroy();
      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });

  describe('load', function () {
    it('expects whether or not this is an organization to have been set', function () {
      expect(mockCLMContextLocations.isOrganization).toHaveBeenCalled();
      expect(vm.isOrganization).toBe(true);

      mockCLMContextLocations.isOrganization.and.returnValue(false);
      vm = $componentController('retentionTile', {
        $scope: $scope,
        CLMContextLocations: mockCLMContextLocations,
        retentionService: mockRetentionService,
      });
      expect(mockCLMContextLocations.isOrganization).toHaveBeenCalled();
      expect(vm.isOrganization).toBe(false);
    });

    it('loads the owner name and reports on success', function () {
      getByIdDeferred.resolve({ name: 'organizationName' });
      getRetentionPoliciesDeferred.resolve(inheritedRetentionPolicies);

      $scope.$digest();

      expect(vm.applicationReports).toEqual(inheritedRetentionPolicies.applicationReports);
      expect(vm.successMetrics).toEqual(inheritedRetentionPolicies.successMetrics);
      expect(vm.error).toBeUndefined();
    });

    it('sets the error message on failure', function () {
      getRetentionPoliciesDeferred.reject({ status: 404, data: 'not found' });

      $scope.$digest();

      expect(vm.ownerName).toBeUndefined();
      expect(vm.applicationReports).toBeUndefined();
      expect(vm.error).toEqual('not found');
    });

    it('reloads on broadcasted owner summary reload event', function () {
      getByIdDeferred.resolve({ name: 'organizationName' });
      getRetentionPoliciesDeferred.resolve(inheritedRetentionPolicies);

      $scope.$digest();

      getByIdDeferred = $q.defer();
      getRetentionPoliciesDeferred = $q.defer();

      $rootScope.$broadcast(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA);

      getByIdDeferred.resolve({ name: 'organizationNameUpdated' });
      getRetentionPoliciesDeferred.resolve(disabledRetentionPolicies);

      $scope.$digest();

      expect(vm.applicationReports).toEqual(disabledRetentionPolicies.applicationReports);
      expect(vm.error).toBeUndefined();
    });
  });

  describe('getMaxReports', function () {
    it('returns the correct text if purging is disabled', function () {
      expect(
        vm.getMaxReports({
          enablePurging: false,
        })
      ).toBe("Don't Purge");
    });

    it('returns the correct text if purging is enabled and max count does not exist', function () {
      expect(
        vm.getMaxReports({
          enablePurging: true,
        })
      ).toBe('N/A');
    });

    it('returns max count if purging is enabled and it exists', function () {
      expect(
        vm.getMaxReports({
          enablePurging: true,
          maxCount: 1,
        })
      ).toBe(1);
    });
  });

  describe('getMaxAge', function () {
    it('returns the correct text if purging is disabled', function () {
      expect(
        vm.getMaxAge({
          enablePurging: false,
        })
      ).toBe("Don't Purge");
    });

    it('returns the correct text if purging is enabled and max age does not exist', function () {
      expect(
        vm.getMaxAge({
          enablePurging: true,
        })
      ).toBe('N/A');
    });

    it('returns max age if purging is enabled and it exists', function () {
      expect(
        vm.getMaxAge({
          enablePurging: true,
          maxAge: '1 day',
        })
      ).toBe('1 day');
    });
  });

  describe('getSuccessMetricsMaxAge', function () {
    it('returns the correct text if purging is disabled', function () {
      vm.successMetrics = { enablePurging: false };
      expect(vm.getSuccessMetricsMaxAge()).toBe("Don't Purge");
    });

    it('returns max age if purging is enabled', function () {
      vm.successMetrics = { enablePurging: true, maxAge: '1 year' };
      expect(vm.getSuccessMetricsMaxAge()).toBe('Max Age 1 year');
    });
  });
});
