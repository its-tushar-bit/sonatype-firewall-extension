import retentionModule from '../../../../main/frontend/owner.manager/retention/module';
import {disabledRetentionPolicies, inheritedRetentionPolicies} from './retentionMockData';
import utilityModule from '../../../../main/frontend/utility/utility.module';

describe('retentionTile', function() {
  const ORGANIZATION_ID = 'organizationId';

  let $rootScope,
      $scope,
      EventNameConstant,
      $q,
      $componentController,
      mockCLMContextLocations,
      mockOrganizationStore,
      getByIdDeferred,
      getRetentionPoliciesDeferred,
      mockRetentionService,
      vm;

  beforeEach(angular.mock.module(retentionModule.name, utilityModule.name));

  beforeEach(inject(function(_$rootScope_, $injector, _$q_, _$componentController_) {
    $rootScope = _$rootScope_;
    $scope = $rootScope.$new();
    EventNameConstant = $injector.get('event.name.constant');
    $q = _$q_;
    $componentController = _$componentController_;
    mockCLMContextLocations = jasmine.createSpyObj('CLMContextLocations', ['isOrganization', 'getEntityId']);
    mockCLMContextLocations.isOrganization.and.returnValue(true);
    mockCLMContextLocations.getEntityId.and.returnValue(ORGANIZATION_ID);
    getByIdDeferred = $q.defer();
    getRetentionPoliciesDeferred = $q.defer();
    mockRetentionService = {
      getRetentionPolicies: jasmine.createSpy().and.callFake(function() {
        return getRetentionPoliciesDeferred.promise;
      })
    };
    mockOrganizationStore = jasmine.createSpyObj('mockOrganizationStore', ['getById']);
    mockOrganizationStore.getById.and.callFake(function(id) {
      return id === 'organizationId' ? getByIdDeferred.promise : null;
    });
    vm = $componentController('retentionTile', {
      $scope: $scope,
      CLMContextLocations: mockCLMContextLocations,
      retentionService: mockRetentionService,
      OrganizationStore: mockOrganizationStore
    });
  }));

  describe('load', function() {
    it('expects whether or not this is an organization to have been set', function() {
      expect(mockCLMContextLocations.isOrganization).toHaveBeenCalled();
      expect(vm.isOrganization).toBe(true);

      mockCLMContextLocations.isOrganization.and.returnValue(false);
      vm = $componentController('retentionTile', {
        $scope: $scope,
        CLMContextLocations: mockCLMContextLocations,
        retentionService: mockRetentionService,
        OrganizationStore: mockOrganizationStore
      });
      expect(mockCLMContextLocations.isOrganization).toHaveBeenCalled();
      expect(vm.isOrganization).toBe(false);
    });

    it('loads the owner name and reports on success', function() {
      getByIdDeferred.resolve({name: 'organizationName'});
      getRetentionPoliciesDeferred.resolve(inheritedRetentionPolicies);

      $scope.$digest();

      expect(mockCLMContextLocations.getEntityId).toHaveBeenCalled();
      expect(mockOrganizationStore.getById).toHaveBeenCalledWith(ORGANIZATION_ID);
      expect(vm.ownerName).toBe('organizationName');
      expect(vm.applicationReports).toEqual(inheritedRetentionPolicies.applicationReports);
      expect(vm.successMetrics).toEqual(inheritedRetentionPolicies.successMetrics);
      expect(vm.error).toBeUndefined();
    });

    it('sets the error message on failure', function() {
      getByIdDeferred.reject({status: 404, data: 'not found'});

      $scope.$digest();

      expect(vm.ownerName).toBeUndefined();
      expect(vm.applicationReports).toBeUndefined();
      expect(vm.error).toEqual('not found');
    });

    it('reloads on broadcasted owner summary reload event', function() {
      getByIdDeferred.resolve({name: 'organizationName'});
      getRetentionPoliciesDeferred.resolve(inheritedRetentionPolicies);

      $scope.$digest();

      getByIdDeferred = $q.defer();
      getRetentionPoliciesDeferred = $q.defer();

      $rootScope.$broadcast(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA);

      getByIdDeferred.resolve({name: 'organizationNameUpdated'});
      getRetentionPoliciesDeferred.resolve(disabledRetentionPolicies);

      $scope.$digest();

      expect(vm.ownerName).toBe('organizationNameUpdated');
      expect(vm.applicationReports).toEqual(disabledRetentionPolicies.applicationReports);
      expect(vm.error).toBeUndefined();
    });
  });

  describe('getMaxReports', function() {
    it('returns the correct text if purging is disabled', function() {
      expect(vm.getMaxReports({
        enablePurging: false
      })).toBe('Don\'t Purge');
    });

    it('returns the correct text if purging is enabled and max count does not exist', function() {
      expect(vm.getMaxReports({
        enablePurging: true
      })).toBe('N/A');
    });

    it('returns max count if purging is enabled and it exists', function() {
      expect(vm.getMaxReports({
        enablePurging: true,
        maxCount: 1
      })).toBe(1);
    });
  });

  describe('getMaxAge', function() {
    it('returns the correct text if purging is disabled', function() {
      expect(vm.getMaxAge({
        enablePurging: false
      })).toBe('Don\'t Purge');
    });

    it('returns the correct text if purging is enabled and max age does not exist', function() {
      expect(vm.getMaxAge({
        enablePurging: true
      })).toBe('N/A');
    });

    it('returns max age if purging is enabled and it exists', function() {
      expect(vm.getMaxAge({
        enablePurging: true,
        maxAge: '1 day'
      })).toBe('1 day');
    });
  });

  describe('getSuccessMetricsMaxAge', function() {
    it('returns the correct text if purging is disabled', function() {
      vm.successMetrics = {enablePurging: false};
      expect(vm.getSuccessMetricsMaxAge()).toBe('Don\'t Purge');
    });

    it('returns max age if purging is enabled', function() {
      vm.successMetrics = {enablePurging: true, maxAge: '1 year'};
      expect(vm.getSuccessMetricsMaxAge()).toBe('Max Age 1 year');
    });
  });
});
