/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import gettingStartedModule from '../../../../main/frontend/configuration/gettingStarted/module';

describe('gettingStarted component', function() {

  var vm,
      $q,
      $scope,
      CLMLocations,
      $httpBackend,
      $rootScope,
      permissionServiceMock,
      telemetryServiceMock;

  beforeEach(angular.mock.module(gettingStartedModule.name, function($provide) {
    telemetryServiceMock = jasmine.createSpyObj('gettingStartedUsageTelemetryService', ['submitData']);

    $provide.service('gettingStartedUsageTelemetryService', function() {
      return telemetryServiceMock;
    });

  }));

  beforeEach(inject(function(_$q_, _$httpBackend_, _$rootScope_, $componentController, _CLMLocations_) {
    $q = _$q_;
    $httpBackend = _$httpBackend_;
    CLMLocations = _CLMLocations_;
    $rootScope = _$rootScope_;
    $scope = _$rootScope_.$new();
    permissionServiceMock = jasmine.createSpyObj('permissionServiceMock', ['getValidPermissions']);

    vm = $componentController('gettingStarted', {
      $rootScope: $rootScope,
      PermissionService: permissionServiceMock
    });
  }));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingRequest();
    $httpBackend.verifyNoOutstandingExpectation();
    $scope.$destroy();
  });

  describe('$onInit()', function() {

    it('does not do anything if application is not licensed', function() {
      $rootScope.licensed = false;

      vm.$onInit();

      expect(vm.error).toBeUndefined();
      expect(vm.validPermissions).toBeUndefined();
      expect(vm.license).toBeUndefined();
    });

    it('sets variables up correctly on successful requests', function() {
      $rootScope.licensed = true;

      var permissionsDeferred = $q.defer();
      permissionServiceMock.getValidPermissions.and.returnValue(permissionsDeferred.promise);

      $httpBackend.whenGET(CLMLocations.getLicenseSummaryUrl()).respond('license value');
      $httpBackend.whenGET(CLMLocations.getShouldDisplayDefaultPasswordWarning()).respond('false');
      $httpBackend.whenGET(CLMLocations.getIsHdsReachable()).respond({ alive: true });

      vm.$onInit();

      permissionsDeferred.resolve(['CONFIGURE_SYSTEM', 'ADD_APPLICATION']);
      $scope.$digest();
      $httpBackend.flush();

      expect(vm.error).toBeUndefined();
      expect(vm.validPermissions).toEqual(['CONFIGURE_SYSTEM', 'ADD_APPLICATION']);
      expect(vm.shouldDisplayHdsUnreachable).toBe(false);
      expect(vm.hdsUnreachableErrorMessage).toBeUndefined();
      expect(vm.hdsUnreachableIncidentId).toBeUndefined();
      expect(vm.license).toBe('license value');
    });

    it('sets validPermissions and shouldDisplayHdsUnreachable but does not retrieve any data if has no admin ' +
        'permission', function() {
      $rootScope.licensed = true;

      var permissionsDeferred = $q.defer();
      permissionServiceMock.getValidPermissions.and.returnValue(permissionsDeferred.promise);
      $httpBackend.whenGET(CLMLocations.getIsHdsReachable()).respond({
        alive: false, errorMessage: 'foo', incidentId: 'bar'
      });

      vm.$onInit();

      permissionsDeferred.resolve(['ADD_APPLICATION']);
      $scope.$digest();
      $httpBackend.flush();

      expect(vm.validPermissions).toEqual(['ADD_APPLICATION']);
      expect(vm.shouldDisplayHdsUnreachable).toBe(true);
      expect(vm.hdsUnreachableErrorMessage).toEqual('foo');
      expect(vm.hdsUnreachableIncidentId).toEqual('bar');
      expect(vm.license).toBeUndefined();
    });

    it('resets error', function() {
      $rootScope.licensed = true;
      vm.error = 'Error';

      var permissionsDeferred = $q.defer();
      permissionServiceMock.getValidPermissions.and.returnValue(permissionsDeferred.promise);
      $httpBackend.whenGET(CLMLocations.getIsHdsReachable()).respond({ alive: true });

      vm.$onInit();

      permissionsDeferred.resolve([]);
      $scope.$digest();
      $httpBackend.flush();

      expect(vm.error).toBeUndefined();
    });

    it('sets error if getValidPermissions request fails', function() {
      $rootScope.licensed = true;

      var permissionsDeferred = $q.defer();
      permissionServiceMock.getValidPermissions.and.returnValue(permissionsDeferred.promise);
      $httpBackend.whenGET(CLMLocations.getIsHdsReachable()).respond({ alive: true });

      vm.$onInit();
      permissionsDeferred.reject('get Permissions Error');
      $scope.$digest();
      $httpBackend.flush();

      expect(vm.error).toBe('get Permissions Error');
    });

    describe('"VISITED" telemetry event', function() {
      it('is not fired if application is not licensed', function() {
        $rootScope.licensed = false;
        vm.$onInit();
        expect(telemetryServiceMock.submitData).not.toHaveBeenCalled();
      });

      it('is fired before data is loaded if application is licensed', function() {
        $rootScope.licensed = true;
        permissionServiceMock.getValidPermissions.and.returnValue($q.defer().promise);
        $httpBackend.whenGET(CLMLocations.getIsHdsReachable()).respond({ alive: true });
        vm.$onInit();
        // fired before getValidPermissions result is resolved
        expect(telemetryServiceMock.submitData).toHaveBeenCalledWith('VISITED');

        $httpBackend.flush();
      });
    });
  });

  describe('isLoading()', function() {
    it('returns true if permissions are not loaded', function() {
      vm.validPermissions = undefined;
      expect(vm.isLoading()).toBe(true);
    });

    it('returns false if permissions are loaded and has no admin permission', function() {
      vm.validPermissions = [];
      expect(vm.isLoading()).toBe(false);
    });

    it('returns true if has admin permission, but license is not loaded', function() {
      vm.validPermissions = ['CONFIGURE_SYSTEM'];
      vm.license = undefined;
      expect(vm.isLoading()).toBe(true);
    });

    it('returns false if has admin permission and license is loaded', function() {
      vm.validPermissions = ['CONFIGURE_SYSTEM'];
      vm.license = {};
      expect(vm.isLoading()).toBe(false);
    });

    it('returns false if failed to get permissions', function() {
      vm.error = {};
      expect(vm.isLoading()).toBe(false);
    });

    it('returns false if failed to get license', function() {
      vm.validPermissions = ['CONFIGURE_SYSTEM'];
      vm.error = {};
      expect(vm.isLoading()).toBe(false);
    });
  });

  describe('isDataLoaded()', function() {
    it('returns false if permissions are not loaded', function() {
      vm.validPermissions = undefined;
      expect(vm.isDataLoaded()).toBe(false);
    });

    it('returns true if permissions are loaded and has no admin permission', function() {
      vm.validPermissions = [];
      expect(vm.isDataLoaded()).toBe(true);
    });

    it('returns false if has admin permission, but license is not loaded', function() {
      vm.validPermissions = ['CONFIGURE_SYSTEM'];
      vm.license = undefined;
      expect(vm.isDataLoaded()).toBe(false);
    });

    it('returns true if has admin permission and license is loaded', function() {
      vm.validPermissions = ['CONFIGURE_SYSTEM'];
      vm.license = {};
      expect(vm.isDataLoaded()).toBe(true);
    });
  });

  describe('isAuthorizedToViewSystemSetup()', function() {
    it('returns true if has only CONFIGURE_SYSTEM permission', function() {
      vm.validPermissions = ['CONFIGURE_SYSTEM'];
      expect(vm.isAuthorizedToViewSystemSetup()).toBe(true);
    });

    it('returns true if has only ADD_APPLICATION permission', function() {
      vm.validPermissions = ['ADD_APPLICATION'];
      expect(vm.isAuthorizedToViewSystemSetup()).toBe(true);
    });

    it('returns false if has no permission', function() {
      vm.validPermissions = [];
      expect(vm.isAuthorizedToViewSystemSetup()).toBe(false);
    });
  });
});
